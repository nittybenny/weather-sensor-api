package com.weathersensor.weather_sensor_api.repository;

import com.weathersensor.weather_sensor_api.model.SensorReading;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Database access layer for storing and querying sensor telemetry.
 */
@Repository
public interface SensorRepository extends JpaRepository<SensorReading, Long> {

    /**
     * Calculates the average temperature for a specific sensor within a time window.
     * Uses an explicit sensorId filter for optimal database index usage.
     *
     * @param sensorId The specific ID to search for.
     * @param start    The start of the time window.
     * @param end      The end of the time window.
     * @return The calculated average, or empty if no records match.
     */
    @Query("SELECT AVG(s.temperature) FROM SensorReading s WHERE s.sensorId = :sensorId AND s.timestamp >= :start AND s.timestamp <= :end")
    Optional<Double> findAverageTemperatureBySensorAndRange(
            @Param("sensorId") String sensorId, 
            @Param("start") Instant start, 
            @Param("end") Instant end
    );

    /**
     * Calculates the global average temperature across all sensors within a time window.
     * Kept as a separate query to avoid Hibernate type-resolution issues 
     * with dynamic null parameters
     *
     * @param start The start of the time window.
     * @param end   The end of the time window.
     * @return The calculated average, or empty if no records match.
     */
    @Query("SELECT AVG(s.temperature) FROM SensorReading s WHERE s.timestamp >= :start AND s.timestamp <= :end")
    Optional<Double> findGlobalAverageTemperatureByRange(
            @Param("start") Instant start, 
            @Param("end") Instant end
    );
}