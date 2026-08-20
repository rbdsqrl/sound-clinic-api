package com.simplehearing.session.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * @param performanceScore 1-5 on the shared rubric, or null when the therapist did not score
 *                         the session. The scale is fixed so the value stays comparable across
 *                         therapists:
 *                         <ol>
 *                           <li>Significant support needed</li>
 *                           <li>Emerging</li>
 *                           <li>Progressing as expected</li>
 *                           <li>Consistent</li>
 *                           <li>Mastered / generalising</li>
 *                         </ol>
 */
public record UpdateSessionNotesRequest(
        String feedback,
        String progressReport,
        String notes,
        @Min(value = 1, message = "performanceScore must be between 1 and 5")
        @Max(value = 5, message = "performanceScore must be between 1 and 5")
        Integer performanceScore
) {}
