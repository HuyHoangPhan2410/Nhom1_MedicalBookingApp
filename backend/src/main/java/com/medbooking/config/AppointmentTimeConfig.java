package com.medbooking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppointmentTimeConfig {
    @Bean
    public Clock appointmentClock(@Value("${app.time-zone:Asia/Ho_Chi_Minh}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
