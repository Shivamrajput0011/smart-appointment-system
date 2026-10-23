package com.yuvaintern.sams.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application-wide beans.
 * The Clock is injected into services instead of calling LocalDate.now() directly,
 * so unit tests can "freeze" time and test date rules reliably.
 */
@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
