package com.exercise.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Disabled in tests, which call the scheduled jobs directly. */
@Profile("!test")
@Configuration
@EnableScheduling
public class SchedulingConfiguration {}
