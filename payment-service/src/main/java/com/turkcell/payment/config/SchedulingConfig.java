package com.turkcell.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * FR-27 otomatik odeme retry job'i icin. Clock bean olarak enjekte edilir ki
 * PaymentRetryScheduler testlerde sabit bir "simdi" ile deterministik calisabilsin.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
