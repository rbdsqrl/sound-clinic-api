package com.simplehearing.iep.dto;

import java.util.List;

public record ImportResultResponse(int plansCreated, int goalsCreated, List<String> errors) {}
