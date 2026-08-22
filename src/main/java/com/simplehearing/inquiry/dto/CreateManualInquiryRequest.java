package com.simplehearing.inquiry.dto;

import com.simplehearing.inquiry.enums.InquirySource;
import com.simplehearing.inquiry.enums.InquiryStatus;
import com.simplehearing.inquiry.enums.PreferredTime;
import jakarta.validation.constraints.NotBlank;

/**
 * An inquiry entered by staff rather than submitted through the public website —
 * a walk-in at the front desk, or a call taken by hand.
 *
 * Unlike the public form there is no orgId: it comes from the signed-in user, so a
 * caller cannot file an inquiry into somebody else's organisation.
 */
public record CreateManualInquiryRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Phone is required") String phone,
        String email,
        String reason,
        PreferredTime preferredTime,

        /**
         * Where the enquiry starts in the pipeline. A walk-in is already in front of you,
         * so it defaults to VISITED rather than NEW. Restricted to the early stages —
         * converting is a separate action.
         */
        InquiryStatus status,

        /** How they reached the clinic. WEBSITE is rejected — that is the public form's to stamp. */
        InquirySource source
) {}
