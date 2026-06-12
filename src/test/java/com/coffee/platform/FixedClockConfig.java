package com.coffee.platform;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@TestConfiguration
public class FixedClockConfig {

    @Bean
    @Primary
    public Clock fixedClock() {
        // 2025-01-15T04:00:00Z = 12:00 Singapore time (UTC+8)
        return Clock.fixed(Instant.parse("2025-01-15T04:00:00Z"), ZoneOffset.UTC);
    }
}
