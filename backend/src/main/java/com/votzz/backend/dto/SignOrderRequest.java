package com.votzz.backend.dto;

import lombok.Data;

@Data
public class SignOrderRequest {
    private String type; // "RESIDENT" ou "STAFF"
}