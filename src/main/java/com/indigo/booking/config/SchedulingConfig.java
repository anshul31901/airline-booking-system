package com.indigo.booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for scheduled tasks.
 * Enables the BookingCleanupService scheduled job.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
