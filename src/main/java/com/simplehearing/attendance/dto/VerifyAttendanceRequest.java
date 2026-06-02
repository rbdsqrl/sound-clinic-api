package com.simplehearing.attendance.dto;

import java.util.List;

public record VerifyAttendanceRequest(
        Double latitude,
        Double longitude,
        List<Double> faceDescriptor
) {}
