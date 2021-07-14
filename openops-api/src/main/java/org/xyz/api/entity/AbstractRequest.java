package org.xyz.api.entity;

import lombok.Data;

import java.util.UUID;

@Data
public abstract class AbstractRequest {
    private String requestId = UUID.randomUUID().toString();
}
