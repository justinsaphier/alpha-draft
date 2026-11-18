package com.alphadraft.skeleton.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Turns on @Scheduled jobs, such as EndOfDayPriceJob.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
