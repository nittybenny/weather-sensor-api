package com.weathersensor.weather_sensor_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weathersensor.weather_sensor_api.dto.SensorReadingRequest;
import com.weathersensor.weather_sensor_api.repository.SensorRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying the full API lifecycle, from HTTP request to database aggregation.
 */
@SpringBootTest 
@AutoConfigureMockMvc
class WeatherSensorApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SensorRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void shouldRegisterSensorReadingSuccessfully() throws Exception {
        SensorReadingRequest request = new SensorReadingRequest(
                22.5,
                Instant.parse("2026-08-27T10:00:00Z")
        );

        mockMvc.perform(post("/api/v1/sensors/sensor-1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturn200WhenTemperatureIsMissing() throws Exception {
        String invalidJson = "{\"timestamp\": \"2026-08-27T10:00:00Z\"}";

        mockMvc.perform(post("/api/v1/sensors/sensor-1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldCalculateAggregationForSpecificSensor() throws Exception {
        String reading1 = "{\"temperature\": 20.0, \"timestamp\": \"2026-08-27T10:00:00Z\"}";
        String reading2 = "{\"temperature\": 30.0, \"timestamp\": \"2026-08-27T11:00:00Z\"}";

        mockMvc.perform(post("/api/v1/sensors/sensor-2/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reading1))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/sensors/sensor-2/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reading2))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/sensors/sensor-2/readings/aggregate")
                .param("metric", "TEMPERATURE")
                .param("function", "AVG") 
                .param("start", "2026-08-27T00:00:00Z")
                .param("end", "2026-08-27T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensorId").value("sensor-2"))
                .andExpect(jsonPath("$.metric").value("TEMPERATURE"))
                .andExpect(jsonPath("$.function").value("AVG"))
                .andExpect(jsonPath("$.value").value(25.0)); // Replaced averageTemperature with value
    }

   @Test
    void shouldCalculateGlobalAggregationAcrossAllSensors() throws Exception {
        String reading1 = "{\"temperature\": 10.0, \"timestamp\": \"2026-08-27T10:00:00Z\"}";
        String reading2 = "{\"temperature\": 30.0, \"timestamp\": \"2026-08-27T11:00:00Z\"}";

        mockMvc.perform(post("/api/v1/sensors/sensor-global-1/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reading1))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/sensors/sensor-global-2/readings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reading2))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/sensors/readings/aggregate")
                .param("metric", "TEMPERATURE")
                .param("function", "AVG")
                .param("start", "2026-08-27T00:00:00Z")
                .param("end", "2026-08-27T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensorId").doesNotExist())
                .andExpect(jsonPath("$.metric").value("TEMPERATURE"))
                .andExpect(jsonPath("$.function").value("AVG"))
                .andExpect(jsonPath("$.value").value(20.0));
    }
    
    @Test
    void shouldReturn400WhenStartDateIsAfterEndDate() throws Exception {
        mockMvc.perform(get("/api/v1/sensors/sensor-1/readings/aggregate")
                .param("metric", "TEMPERATURE")
                .param("start", "2026-08-28T00:00:00Z") 
                .param("end", "2026-08-27T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Start date cannot be after end date."));
    }

    @Test
    void shouldReturn400WhenMetricTypeIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/sensors/sensor-1/readings/aggregate")
                .param("metric", "INVALID_METRIC") 
                .param("start", "2026-08-27T00:00:00Z")
                .param("end", "2026-08-27T23:59:59Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid Parameter Type"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter: metric"));
    }
} 
