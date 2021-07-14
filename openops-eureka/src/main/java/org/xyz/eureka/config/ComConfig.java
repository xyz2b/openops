package org.xyz.eureka.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class ComConfig {
    @Value("${netty.server.application.name}")
    private String nettyServerApplicationName;
}
