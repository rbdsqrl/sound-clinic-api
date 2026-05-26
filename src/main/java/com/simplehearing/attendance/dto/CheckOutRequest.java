package com.simplehearing.attendance.dto;

import java.util.List;

public record CheckOutRequest(
        Double latitude,
        Double longitude,
        List<Double> faceDescriptor
) {}
