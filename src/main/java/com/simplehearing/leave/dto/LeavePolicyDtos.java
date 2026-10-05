package com.simplehearing.leave.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Request/response shapes for leave categories, allocations and balances. */
public final class LeavePolicyDtos {
    private LeavePolicyDtos() {}

    public record CategoryResponse(UUID id, String name, Integer annualDays, boolean active) {}

    public record CreateCategoryRequest(
            @NotBlank @Size(max = 80) String name,
            /** Days per leave year each person gets by default; omit for no limit. */
            @Min(0) @Max(366) Integer annualDays) {}

    public record UpdateCategoryRequest(
            @Size(max = 80) String name,
            @Min(0) @Max(366) Integer annualDays,
            /** True removes the yearly limit (annualDays is then ignored). */
            Boolean clearAnnualDays,
            Boolean active) {}

    public record SettingsDto(@Min(1) @Max(12) int yearStartMonth) {}

    public record UpsertAllocationRequest(
            @NotNull UUID userId,
            @NotNull UUID categoryId,
            /** The calendar year the leave year starts in. */
            @NotNull Integer year,
            /** Null removes this person's override, restoring the category default. */
            @Min(0) @Max(366) Integer days) {}

    /**
     * One category's position for one person in one leave year.
     *
     * @param allocated  days available this year; null = no limit
     * @param used       working days of approved leave
     * @param pending    working days awaiting approval
     * @param remaining  allocated - used - pending; null when there's no limit
     * @param custom     true when this person's allocation overrides the category default
     */
    public record Balance(UUID categoryId, String categoryName, Integer allocated, int used, int pending,
                          Integer remaining, boolean custom) {}

    public record PersonBalances(UUID userId, String name, String role, List<Balance> balances,
                                 int pendingRequests, int approvedRequests, int rejectedRequests) {}

    public record BalancesResponse(int year, LocalDate yearStart, LocalDate yearEnd, List<PersonBalances> people) {}

    public record MyBalancesResponse(int year, LocalDate yearStart, LocalDate yearEnd, List<Balance> balances) {}
}
