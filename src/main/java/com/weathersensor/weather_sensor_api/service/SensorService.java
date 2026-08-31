package com.weathersensor.weather_sensor_api.service;

import com.weathersensor.weather_sensor_api.controller.SensorController;
import com.weathersensor.weather_sensor_api.dto.MetricAggregationResponse;
import com.weathersensor.weather_sensor_api.dto.SensorReadingRequest;
import com.weathersensor.weather_sensor_api.model.SensorReading;
import com.weathersensor.weather_sensor_api.repository.SensorRepository;
import com.weathersensor.weather_sensor_api.utils.AggregationFunction;
import com.weathersensor.weather_sensor_api.utils.MetricType;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service layer responsible for saving sensor reading and calculating metric aggregation.
 * Designed so new metrics (like humidity) and math functions (like MIN/MAX) can be added 
 * easily in the future without breaking the API.
 */
@Service
public class SensorService {

    private static final Logger log = LoggerFactory.getLogger(SensorController.class);
    private final SensorRepository repository;

    public SensorService(SensorRepository repository) {
        this.repository = repository;
    }

    /**
     * Saves new sensor reading to database
     * Metrics are optional to support partial data if a sensor instrument fails
     * @param sensorId The unique sensor ID.
     * @param request  The payload containing the timestamp and optional metrics, only use temperature here.
     */
    @Transactional
    public void registerReading(String sensorId, SensorReadingRequest request) {

        log.debug("Saving temperature reading for sensor {} to the database", sensorId);
        SensorReading reading = new SensorReading();
        reading.setSensorId(sensorId);
        reading.setTemperature(request.temperature());
        reading.setTimestamp(request.timestamp());
        repository.save(reading);
        log.debug("Successfully saved reading for sensor {}", sensorId);

    }

    /**
     * Calculates a specific metric (like Average Temperature) for a single sensor.
     *
     * @param sensorId The unique ID of the sensor to search for.
     * @param metric   The type of weather data (e.g., TEMPERATURE).
     * @param function The math function to apply (e.g., AVG).
     * @param start    The start of the time window.
     * @param end      The end of the time window.
     * @return The calculated result as MetricAggregationResponse, or null if no data exists for that time.
     */
    public MetricAggregationResponse getAggregationForSensor( String sensorId, MetricType metric, AggregationFunction function, Instant start, Instant end) {
        
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }
        Double result = switch (metric) {
            case TEMPERATURE -> switch (function) {
                case AVG -> repository.findAverageTemperatureBySensorAndRange(sensorId, start, end).orElse(null);
                default -> throw new UnsupportedOperationException("Function " + function + " not yet implemented for Temperature");
            };
            default -> throw new UnsupportedOperationException("Metric " + metric + " not yet implemented");
        };
        if (result != null) {
            result = Math.round(result * 1000.0) / 1000.0;
        }
        return new MetricAggregationResponse(sensorId, metric, function, result, start, end);
    }

    /**
     * Calculates a metric across all sensors combined.
     * 
     * @param metric   The type of weather data (e.g., TEMPERATURE).
     * @param function The math function to apply (e.g., AVG).
     * @param start    The start of the time window.
     * @param end      The end of the time window.
     * @return The calculated result across all sensors, or null if no data exists.
     */
    public MetricAggregationResponse getGlobalAggregation( MetricType metric, AggregationFunction function, Instant start, Instant end) {
        
        if (start.isAfter(end)) {
            log.warn("Invalid global date range requested: start {} is after end {}", start, end);
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }
        Double result = switch (metric) {
            case TEMPERATURE -> switch (function) {
                case AVG -> repository.findGlobalAverageTemperatureByRange(start, end).orElse(null);
                default -> throw new UnsupportedOperationException("Function " + function + " not yet implemented for Temperature");
            };
            default -> throw new UnsupportedOperationException("Metric " + metric + " not yet implemented");
        };

        if (result != null) {
            result = Math.round(result * 1000.0) / 1000.0;
            log.info("Calculated global {} {}: {}", function, metric, result);
        } else {
            log.info("No global {} readings found in the specified time range", metric);
        }
        return new MetricAggregationResponse(null, metric, function, result, start, end);
    }
}