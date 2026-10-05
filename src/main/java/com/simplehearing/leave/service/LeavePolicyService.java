package com.simplehearing.leave.service;

import com.simplehearing.common.exception.ApiException;
import com.simplehearing.holiday.entity.PublicHoliday;
import com.simplehearing.holiday.repository.PublicHolidayRepository;
import com.simplehearing.leave.dto.LeavePolicyDtos.Balance;
import com.simplehearing.leave.dto.LeaveResponse;
import com.simplehearing.leave.entity.Leave;
import com.simplehearing.leave.entity.LeaveAllocation;
import com.simplehearing.leave.entity.LeaveCategory;
import com.simplehearing.leave.enums.LeaveStatus;
import com.simplehearing.leave.repository.LeaveAllocationRepository;
import com.simplehearing.leave.repository.LeaveCategoryRepository;
import com.simplehearing.leave.repository.LeaveRepository;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Leave quotas: categories with a yearly allowance (overridable per person), and balances computed from
 * approved and pending leave. Only working days count — weekly off days and public holidays inside a
 * leave range don't use up the allowance.
 */
@Service
public class LeavePolicyService {

    private final LeaveCategoryRepository categoryRepository;
    private final LeaveAllocationRepository allocationRepository;
    private final LeaveRepository leaveRepository;
    private final OrganisationRepository organisationRepository;
    private final PublicHolidayRepository holidayRepository;

    public LeavePolicyService(LeaveCategoryRepository categoryRepository, LeaveAllocationRepository allocationRepository,
                              LeaveRepository leaveRepository, OrganisationRepository organisationRepository,
                              PublicHolidayRepository holidayRepository) {
        this.categoryRepository = categoryRepository;
        this.allocationRepository = allocationRepository;
        this.leaveRepository = leaveRepository;
        this.organisationRepository = organisationRepository;
        this.holidayRepository = holidayRepository;
    }

    // ── Leave year ───────────────────────────────────────────────────────────

    private Organisation org(UUID orgId) {
        return organisationRepository.findById(orgId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organisation not found"));
    }

    public int yearStartMonth(UUID orgId) { return org(orgId).getLeaveYearStartMonth(); }

    /** The calendar year a leave year starts in, for the leave year containing {@code date}. */
    public int leaveYearOf(UUID orgId, LocalDate date) {
        return date.getMonthValue() >= yearStartMonth(orgId) ? date.getYear() : date.getYear() - 1;
    }

    public LocalDate yearStart(UUID orgId, int year) { return LocalDate.of(year, yearStartMonth(orgId), 1); }

    public LocalDate yearEnd(UUID orgId, int year) { return yearStart(orgId, year).plusYears(1).minusDays(1); }

    // ── Working days ─────────────────────────────────────────────────────────

    private Set<LocalDate> holidayDates(UUID orgId, LocalDate from, LocalDate to) {
        return holidayRepository.findByOrgIdAndHolidayDateBetweenOrderByHolidayDateAsc(orgId, from, to).stream()
                .map(PublicHoliday::getHolidayDate).collect(Collectors.toSet());
    }

    private static int countWorking(Set<DayOfWeek> off, Set<LocalDate> holidays, LocalDate from, LocalDate to) {
        int n = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (!off.contains(d.getDayOfWeek()) && !holidays.contains(d)) n++;
        }
        return n;
    }

    /** Working days in [from, to] inclusive. */
    public int workingDays(UUID orgId, LocalDate from, LocalDate to) {
        return countWorking(org(orgId).getWeeklyOffDays(), holidayDates(orgId, from, to), from, to);
    }

    // ── Categories ───────────────────────────────────────────────────────────

    public List<LeaveCategory> activeCategories(UUID orgId) {
        return categoryRepository.findByOrgIdAndIsActiveTrueOrderByNameAsc(orgId);
    }

    // ── Balances ─────────────────────────────────────────────────────────────

    public Map<UUID, List<Balance>> balances(UUID orgId, Collection<UUID> userIds, int year) {
        Map<UUID, List<Balance>> out = new HashMap<>();
        if (userIds.isEmpty()) return out;

        List<LeaveCategory> categories = activeCategories(orgId);
        LocalDate start = yearStart(orgId, year), end = yearEnd(orgId, year);
        Set<DayOfWeek> off = org(orgId).getWeeklyOffDays();
        Set<LocalDate> holidays = holidayDates(orgId, start, end);

        Map<String, Integer> overrides = new HashMap<>();   // userId|categoryId -> days
        for (LeaveAllocation a : allocationRepository.findByOrgIdAndLeaveYearAndUserIdIn(orgId, year, userIds)) {
            overrides.put(a.getUserId() + "|" + a.getCategoryId(), a.getDays());
        }

        Map<String, int[]> usage = new HashMap<>();         // userId|categoryId -> {used, pending}
        for (Leave l : leaveRepository.findOverlapping(orgId, userIds, List.of(LeaveStatus.APPROVED, LeaveStatus.PENDING), start, end)) {
            if (l.getCategoryId() == null) continue;
            LocalDate from = l.getLeaveDate().isBefore(start) ? start : l.getLeaveDate();
            LocalDate to = l.getEndDate().isAfter(end) ? end : l.getEndDate();
            int days = countWorking(off, holidays, from, to);
            int[] u = usage.computeIfAbsent(l.getTherapistId() + "|" + l.getCategoryId(), k -> new int[2]);
            u[l.getStatus() == LeaveStatus.APPROVED ? 0 : 1] += days;
        }

        for (UUID userId : userIds) {
            List<Balance> list = new ArrayList<>();
            for (LeaveCategory c : categories) {
                String key = userId + "|" + c.getId();
                Integer custom = overrides.get(key);
                Integer allocated = custom != null ? custom : c.getAnnualDays();
                int[] u = usage.getOrDefault(key, new int[2]);
                list.add(new Balance(c.getId(), c.getName(), allocated, u[0], u[1],
                        allocated == null ? null : allocated - u[0] - u[1], custom != null));
            }
            out.put(userId, list);
        }
        return out;
    }

    /**
     * Rejects a leave request that would take a person past their allowance — checked per leave year,
     * so a request spanning two leave years draws on each year's balance for its own days.
     * No-op for a category with no yearly limit.
     */
    public void assertWithinBalance(UUID orgId, UUID userId, LeaveCategory category, LocalDate from, LocalDate to) {
        Set<DayOfWeek> off = org(orgId).getWeeklyOffDays();
        Set<LocalDate> holidays = holidayDates(orgId, from, to);

        for (int year = leaveYearOf(orgId, from); year <= leaveYearOf(orgId, to); year++) {
            LocalDate s = yearStart(orgId, year), e = yearEnd(orgId, year);
            int requested = countWorking(off, holidays, from.isBefore(s) ? s : from, to.isAfter(e) ? e : to);
            if (requested == 0) continue;

            Balance b = balances(orgId, List.of(userId), year).get(userId).stream()
                    .filter(x -> x.categoryId().equals(category.getId())).findFirst().orElse(null);
            if (b == null || b.remaining() == null) continue;   // no limit
            if (requested > b.remaining()) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "That's " + requested + " working day" + (requested == 1 ? "" : "s") + " of " + category.getName()
                                + " but only " + Math.max(0, b.remaining()) + " "
                                + (b.remaining() == 1 ? "is" : "are") + " left for " + yearLabel(orgId, year) + ".");
            }
        }
    }

    private String yearLabel(UUID orgId, int year) {
        return yearStartMonth(orgId) == 1 ? String.valueOf(year) : year + "-" + String.valueOf(year + 1).substring(2);
    }

    // ── Response enrichment ──────────────────────────────────────────────────

    /** Adds the category name and working-day count to leave responses. */
    public List<LeaveResponse> enrich(UUID orgId, List<LeaveResponse> leaves) {
        if (leaves.isEmpty()) return leaves;
        Map<UUID, String> names = categoryRepository.findByOrgIdOrderByNameAsc(orgId).stream()
                .collect(Collectors.toMap(LeaveCategory::getId, LeaveCategory::getName));
        LocalDate min = leaves.stream().map(LeaveResponse::leaveDate).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate max = leaves.stream().map(LeaveResponse::endDate).max(Comparator.naturalOrder()).orElseThrow();
        Set<DayOfWeek> off = org(orgId).getWeeklyOffDays();
        Set<LocalDate> holidays = holidayDates(orgId, min, max);
        return leaves.stream()
                .map(l -> l.withCategory(l.categoryId() != null ? names.get(l.categoryId()) : null,
                        countWorking(off, holidays, l.leaveDate(), l.endDate())))
                .toList();
    }
}
