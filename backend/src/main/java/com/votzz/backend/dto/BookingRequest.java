package com.votzz.backend.dto;

import java.time.LocalDate;

public record BookingRequest(
    String areaId,
    String userId,
    String unit,
    LocalDate date,
    String startTime,
    String endTime
) {}