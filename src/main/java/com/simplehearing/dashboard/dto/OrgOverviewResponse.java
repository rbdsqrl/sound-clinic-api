package com.simplehearing.dashboard.dto;

/**
 * The four numbers behind the dashboard's "Organisation Overview" rings. Computed with SQL
 * counts so the dashboard doesn't have to download every patient, member and invitation just to
 * count them.
 *
 * @param activeCases    cases that are neither discharged nor manually deactivated
 * @param inactiveCases  discharged or manually deactivated cases
 * @param activeMembers  active staff accounts (not parents)
 * @param invitedMembers invitations still pending acceptance
 */
public record OrgOverviewResponse(int activeCases, int inactiveCases, int activeMembers, int invitedMembers) {}
