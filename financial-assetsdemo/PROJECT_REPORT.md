# Project Report

## Overview

This project implements a data warehouse platform for Acme Ltd to ingest, store, query, analyse, and explain financial market data. The backend is a Spring Boot application with MongoDB as mandatory NoSQL storage. The platform supports heterogeneous financial assets and provider-specific market data through flexible attribute maps.

## Data Model

The main entities are:

- `Asset`: financial instrument/asset, including asset id, ticker, category, name, and flexible attributes such as region or custom instrument properties.
- `DataProvider`: market data source, including provider id, name, base URL, description, and flexible provider attributes.
- `MarketData`: daily time-series points linked to asset and provider, including OHLCV values and provenance metadata.

All main entities include temporal fields: `validFrom`, `validUntil`, `current`, and `deleted`.

## Temporal Warehouse Approach

Records are not updated or deleted in place. When an asset, provider, or market data row changes, the current version is closed by setting `current=false` and `validUntil=<change time>`. A new version is inserted with `current=true`. Delete operations insert a marker version with `deleted=true`.

Historical reads are supported through `asOf` parameters on asset, provider, and time-series endpoints.

## Ingestion And Provenance

The app supports provider ingestion from Alpha Vantage and Stooq-style CSV data. Stored market rows include:

- provider id
- source endpoint
- raw provider symbol
- raw data hash
- ingestion time

For reliable demos, `/api/demo/seed` inserts deterministic local data for `AAPL`, `MSFT`, and `BTC` under provider `DEMO_VENDOR`.

## REST API Coverage

- Q1: `GET /api/assets`
- Q2: `GET /api/assets/{assetId}`
- Q3: `GET /api/providers`
- Q4: `GET /api/providers/{providerId}`
- Q5: `GET /api/market-data/search?assetId=...&providerId=...`

The API also exposes history endpoints and temporal update/delete behavior.

## Analytics

The analytics controller provides:

- min, max, average, first/last close, trend, and percentage change
- next-day forecast using a simple moving average
- two-asset comparison
- Spark/ML-friendly JSON export

`SparkAnalyticsJob` shows how Apache Spark can read current MongoDB market data, aggregate it, and write summary results back to MongoDB.

## LLM And MCP Integration

The assistant is grounded in platform data through explicit tools:

- list assets
- get asset details
- list data sources
- fetch time series
- summarize trends
- forecast next day
- compare assets
- explain latest change

The `mcp-server` exposes equivalent MCP tools for an LLM client. The Spring Boot assistant endpoints also provide a deterministic chat-style router and optional Ollama integration.

## Reproducibility

Run MongoDB, start the Spring Boot app, open `http://localhost:8080`, click `0. Seed Local Demo Data`, then run the demo buttons in order. This demonstrates ingestion/storage, Q1-Q5 API exploration, analytics, assistant tooling, and temporal history without requiring external API availability.

