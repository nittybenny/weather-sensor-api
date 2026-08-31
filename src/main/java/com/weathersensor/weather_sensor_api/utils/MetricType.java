package com.weathersensor.weather_sensor_api.utils;

/**
 * Represents the different weather metrics a sensor can record.
 * Adding new Metrics here (like Humidity) allows the API to easily 
 * support new metrics without changing the URL structure.
*/

public enum MetricType {
    TEMPERATURE
} 