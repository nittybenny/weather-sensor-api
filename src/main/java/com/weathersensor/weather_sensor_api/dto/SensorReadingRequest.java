package com.weathersensor.weather_sensor_api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record SensorReadingRequest(
        Double temperature,
        
        @NotNull(message = "Timestamp is required")
        Instant timestamp
) {}