package com.simplehearing.session.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * @param performanceScore 0-100 percentage, or null when the therapist did not score the session.
 *                         Named bands keep the number readable the same way by different
 *                         therapists — the UI shows the band beside the figure:
 *                         <ul>
 *                           <li>0-39 &mdash; Needs work</li>
 *                           <li>40-59 &mdash; Developing</li>
 *                           <li>60-74 &mdash; On track</li>
 *                           <li>75-89 &mdash; Good</li>
 *                           <li>90-100 &mdash; Excellent</li>
 *                         </ul>
 */
public record UpdateSessionNotesRequest(
        String feedback,
        String progressReport,
        String notes,
        @Min(value = 0, message = "performanceScore must be between 0 and 100")
        @Max(value = 100, message = "performanceScore must be between 0 and 100")
        Integer performanceScore
) {}
