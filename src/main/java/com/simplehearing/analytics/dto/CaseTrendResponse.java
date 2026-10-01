package com.simplehearing.analytics.dto;

import com.simplehearing.analytics.dto.TimeSeriesResponse.Bucket;

import java.util.List;
import java.util.UUID;

/**
 * One active case's trend line for the Cases tab's multi-case chart — just the buckets, since
 * that chart draws nothing else. The same buckets {@code /patients/{id}/progress} returns for
 * that case, computed for every active case in one batched pass instead of one request each.
 */
public record CaseTrendResponse(UUID patientId, List<Bucket> buckets) {}
