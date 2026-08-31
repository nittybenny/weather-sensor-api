package com.weathersensor.weather_sensor_api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.weathersensor.weather_sensor_api.utils.*;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetricAggregationResponse(
        String sensorId,
        MetricType metric,
        AggregationFunction function,
        Double value,
        Instant start,
        Instant end
) {}