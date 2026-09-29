package com.simplehearing.review.dto;

import java.time.LocalTime;
import java.util.Set;

/** Full replacement of a Clinic Head's own Review Session grid. An empty set clears the
 *  override, reverting that Clinic Head to the org-wide default. */
public record UpdateClinicHeadSlotTimesRequest(Set<LocalTime> times) {}
