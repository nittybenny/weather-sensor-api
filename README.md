1. Architecture & Design Decisions

Wide Table Ingestion: I use a wide database table with optional columns for each metric. This allows us to save the whole payload in one fast insert, avoiding the performance hit of saving metrics individually.
Extensible Routing Matrix: By using modern Java switch statements combined with MetricType and AggregationFunction enums, makes routing flexible. We can add new metric types or calculations in the future without changing the controller or the URL structure.

2. Technical Trade-offs

Explicit vs. Dynamic Queries: I use two specific queries instead of one flexible one. This prevents bugs caused by passing blank values to the database. More importantly, it ensures the database uses its index for lightning-fast searches instead of slowly scanning the entire table.
RESTful Hierarchy: I separate the specific item we are looking for (/sensors/123) from the details we want to filter (?metric=TEMPERATURE). This standard layout acts like a clear mailing address. It allows network tools (like API Gateways) to quickly scan the main URL and route the traffic to the right place.

3. Future Scalability

Decoupled Ingestion: The API will drop incoming payloads into a message broker (Kafka) and return a 202 Accepted immediately. This unblocks the connection instantly while a background process handles the actual database saving.

Storage Migration : For millions of records, we will migrate to a specialized Time-Series Database (TSDB). Standard databases struggle at this scale, whereas TSDBs are perfectly optimized for high-speed, time-based data ingestion and querying.

In-Flight Processing: Instead of waiting for the user to request the data, you calculate the sum and count as the data arrives.
The POST endpoint stops writing to the database and instead just publishes the raw JSON payload to a Kafka topic.
Then deploy a Kafka Streams application. Kafka Streams application consumes the topic. It applies a 1-minute Tumbling Window, maintaining the running sum and message count for each sensor in memory. As each 1-minute window closes, it pushes that (sum, count) bucket into memory.
When a client requests the average for a specific 1-hour window, the Spring Boot GET API no longer executes a heavy SQL scan. It simply fetches the 60 pre-aggregated (sum, count) buckets from memory, adds them together, and performs one final calculation (totalSum / totalCount). This guarantees mathematically accurate averages in milliseconds while completely shielding the database from read-heavy dashboard traffic.

4. How to Run and Test the Application
Step 1: Build the Application
./mvnw clean package

Step 2: Start the Server
Run the Spring Boot application (starts on localhost:8080).
./mvnw spring-boot:run

Step 3: Ingest Sensor Data (POST)
Populate the sensor_readings database table by sending telemetry data for a sensor (e.g., 00001).

Open a new terminal window and run:

Bash
curl -X POST "http://localhost:8080/api/v1/sensors/00001/readings" -H "Content-Type: application/json" -d "{\"temperature\": 18.0, \"timestamp\": \"2026-08-31T14:00:00.000Z\"}"

curl -X POST "http://localhost:8080/api/v1/sensors/00001/readings" -H "Content-Type: application/json" -d "{\"temperature\": 10.0, \"timestamp\": \"2026-08-31T14:01:00.000Z\"}"

Step 4: Query Average Temperature for a Specific Sensor (GET)
Calculate the metric for a specific sensor over a given time window.

curl "http://localhost:8080/api/v1/sensors/00001/readings/aggregate?metric=TEMPERATURE&function=AVG&start=2026-08-31T13:00:00Z&end=2026-08-31T15:00:00Z"
Response:

JSON
{
  "sensorId": "00001",
  "metric": "TEMPERATURE",
  "function": "AVG",
  "value": 14.0,
  "start": "2026-08-31T13:00:00Z",
  "end": "2026-08-31T15:00:00Z"
}
Note: The presence of the sensorId field in the response payload confirms this is a localized average specific to sensor 00001.

Step 5: Query Global Average Temperature Across All Sensors (GET)
Calculate the system-wide metric across all sensors.

Bash
curl -X GET "http://localhost:8080/api/v1/sensors/readings/aggregate?metric=TEMPERATURE&function=AVG&start=2026-08-31T10:32:54.696Z&end=2026-08-31T15:32:54.696Z" -H "accept: */*"
Response:

JSON
{
  "metric": "TEMPERATURE",
  "function": "AVG",
  "value": 14.0,
  "start": "2026-08-31T10:32:54.696Z",
  "end": "2026-08-31T15:32:54.696Z"
}
Note: There is no sensorId field in this response, indicating that the value represents a global aggregation across the entire system.
