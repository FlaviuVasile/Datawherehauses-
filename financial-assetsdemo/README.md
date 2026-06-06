# Financial Assets Data Warehouse

Runnable Spring Boot platform for the lab project "Data Warehouse for Financial Markets Data".

## What Is Implemented

- MongoDB NoSQL storage for assets, providers, and market time series.
- Heterogeneous attributes through flexible `Map<String, Object>` fields.
- Temporal warehouse behavior: updates and deletes create new versions instead of overwriting/removing records.
- REST API for the required Q1-Q5 queries.
- Provider ingestion with provenance fields: provider id, endpoint, raw symbol, raw data hash, ingestion time.
- Analytics endpoints for summary, comparison, export, and next-day forecast.
- Apache Spark job service for aggregation over MongoDB market data.
- LLM assistant endpoints and MCP tools for grounded data exploration.
- Local deterministic demo seed, so the project can be demonstrated without an external API key.

## Prerequisites

- Java 21.
- MongoDB running locally on `mongodb://localhost:27017`.
- Maven. If `mvn` is not in PATH, IntelliJ IDEA's bundled Maven can run the same goals.
- Optional: Ollama running locally for `/api/assistant/llm-chat`.
- Optional: Python 3 for the MCP server.

## Run

From the project root:

```powershell
mvn spring-boot:run
```

If Maven is not in PATH, use the bundled IntelliJ Maven path:

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.3\plugins\maven\lib\maven3\bin\mvn.cmd' spring-boot:run
```

Open:

```text
http://localhost:8080
```

## Fast Demo Without External APIs

1. Start MongoDB.
2. Start the Spring Boot app.
3. Open `http://localhost:8080`.
4. Click `0. Seed Local Demo Data`.
5. Use provider `DEMO_VENDOR` and asset `AAPL`, `MSFT`, or `BTC`.
6. Run Q1-Q5, analytics, forecast, assistant tools, temporal update, and history buttons.

Equivalent API call:

```powershell
curl -X POST http://localhost:8080/api/demo/seed
```

## Required REST Queries

- Q1 list assets: `GET /api/assets`
- Q2 asset details: `GET /api/assets/{assetId}`
- Q3 list data sources: `GET /api/providers`
- Q4 provider details: `GET /api/providers/{providerId}`
- Q5 time series: `GET /api/market-data/search?assetId=AAPL&providerId=DEMO_VENDOR`

Historical reads use `asOf`:

```text
GET /api/assets/AAPL?asOf=2026-06-06T12:00:00
GET /api/market-data/search?assetId=AAPL&providerId=DEMO_VENDOR&startDate=2026-01-05&endDate=2026-01-09&asOf=2026-06-06T12:00:00
```

## Analytics And Assistant

- Summary: `GET /api/analytics/summary?assetId=AAPL&providerId=DEMO_VENDOR`
- Forecast: `GET /api/analytics/forecast/next-day?assetId=AAPL&providerId=DEMO_VENDOR`
- Compare: `GET /api/analytics/compare?assetId1=AAPL&assetId2=MSFT&providerId=DEMO_VENDOR`
- Assistant tools: `GET /api/assistant/tools`
- Grounded assistant chat: `POST /api/assistant/chat`

## MCP Server

Install Python dependencies in `mcp-server`:

```powershell
pip install -r mcp-server/requirements.txt
python mcp-server/server.py
```

The MCP tools call the Spring Boot API on `http://localhost:8080`.

## Tests

```powershell
mvn test
```

