package com.votzz.backend.dto;

public record AnnouncementRequest(
    String title,
    String content,
    String priority, // HIGH, NORMAL, LOW
    String targetType,
    boolean requiresConfirmation
) {}