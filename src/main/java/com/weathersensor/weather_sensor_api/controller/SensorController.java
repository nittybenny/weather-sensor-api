package com.weathersensor.weather_sensor_api.controller;

import com.weathersensor.weather_sensor_api.dto.MetricAggregationResponse;
import com.weathersensor.weather_sensor_api.dto.SensorReadingRequest;
import com.weathersensor.weather_sensor_api.service.SensorService;
import com.weathersensor.weather_sensor_api.utils.AggregationFunction;
import com.weathersensor.weather_sensor_api.utils.MetricType;

import jakarta.validation.Valid;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST controller for ingesting and querying weather sensor telemetry.
**/

@RestController
@RequestMapping("/api/v1/sensors")
public class SensorController {

    private static final Logger log = LoggerFactory.getLogger(SensorController.class);

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    /**
     * Ingests a new telemetry reading from a specific sensor.
     * 
     * @param sensorId The unique identifier of the hardware sensor.
     * @param request  The payload containing the timestamp and recorded metrics.
     */
    @PostMapping("/{sensorId}/readings")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerReading(
            @PathVariable String sensorId,
            @Valid @RequestBody SensorReadingRequest request) {
        log.info("Received temperature reading for sensor {}: {} at {}", sensorId, request.temperature(), request.timestamp());
        sensorService.registerReading(sensorId, request);
    }
    /**
     * Retrieves an aggregated metric for a specific sensor over a defined time window.
     *
     * @param sensorId The unique identifier of the sensor.
     * @param metric   The type of telemetry data to aggregate (e.g., TEMPERATURE).
     * @param function The mathematical function to apply (defaults to AVG).
     * @param start    The start time of the query window.
     * @param end      The end time of the query window.
     * @return A standardized response containing the aggregated value, or null if no data exists.
     */
    @GetMapping("/{sensorId}/readings/aggregate")
    public MetricAggregationResponse getAverageForSensor(
        @PathVariable String sensorId,
        @RequestParam MetricType metric,
        @RequestParam(defaultValue = "AVG") AggregationFunction function, 
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {

        log.info("Calculating {} {} for sensor: {} from {} to {}", function, metric, sensorId, start, end);

        return sensorService.getAggregationForSensor(sensorId, metric, function, start, end);
    }
    
    /**
     * Retrieves an aggregated metric across all sensors over a defined time window
     *
     * @param metric   The type of telemetry data to aggregate (e.g., TEMPERATURE).
     * @param function The mathematical function to apply (defaults to AVG).
     * @param start    The start time of the query window.
     * @param end      The end time of the query window.
     * @return A standardized response containing the global aggregated value.
     */
    @GetMapping("/readings/aggregate")
    public MetricAggregationResponse getGlobalAverage(
            @RequestParam MetricType metric,
            @RequestParam(defaultValue = "AVG") AggregationFunction function,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {

        log.info("Calculating global {} {} from {} to {}", function, metric, start, end);
        return sensorService.getGlobalAggregation(metric, function, start, end);
    }
}