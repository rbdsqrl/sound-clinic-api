package com.simplehearing.successcriteria.dto;

public record SuccessCriteriaResponse(
        Double goalMasteryPct,
        Boolean goalMasteryMet,
        boolean therapistSignedOff,
        Double parentSatisfactionPct,
        Boolean parentSatisfactionMet,
        boolean overallSuccessful
) {
}
