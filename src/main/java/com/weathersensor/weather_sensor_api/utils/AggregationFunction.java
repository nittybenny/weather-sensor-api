package com.weathersensor.weather_sensor_api.utils;

/**
 * The mathematical operations supported by the aggregation endpoints.
 * Adding new functions here (like MIN or MAX) allows the API to easily 
 * support new calculations without changing the URL structure.
 */
public enum AggregationFunction {
    AVG
}