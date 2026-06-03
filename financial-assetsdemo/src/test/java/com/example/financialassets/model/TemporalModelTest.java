package com.example.financialassets.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TemporalModelTest {

    @Test
    void assetConstructorCreatesCurrentNonDeletedVersion() {
        Asset asset = new Asset("AAPL", "AAPL", "Apple Inc.", "STOCK");

        assertEquals("AAPL", asset.getAssetId());
        assertEquals("AAPL", asset.getTicker());
        assertEquals("Apple Inc.", asset.getAssetName());
        assertEquals("STOCK", asset.getCategory());
        assertNotNull(asset.getValidFrom());
        assertTrue(asset.isCurrent());
        assertFalse(asset.isDeleted());
    }

    @Test
    void dataProviderConstructorCreatesCurrentNonDeletedVersion() {
        DataProvider provider = new DataProvider(
                "STOOQ",
                "Stooq",
                "https://stooq.com",
                "Market data provider"
        );

        assertEquals("STOOQ", provider.getProviderId());
        assertEquals("Stooq", provider.getProviderName());
        assertNotNull(provider.getValidFrom());
        assertNull(provider.getValidUntil());
        assertTrue(provider.isCurrent());
        assertFalse(provider.isDeleted());
    }

    @Test
    void marketDataCanRepresentDeletedVersionWithoutPhysicalDelete() {
        MarketData data = new MarketData();

        data.setTimeSeriesId("AAPL_STOOQ_2024-01-01");
        data.setCurrent(true);
        data.setDeleted(true);
        data.setValidFrom(LocalDateTime.now());
        data.setValidUntil(null);

        assertEquals("AAPL_STOOQ_2024-01-01", data.getTimeSeriesId());
        assertTrue(data.isCurrent());
        assertTrue(data.isDeleted());
        assertNotNull(data.getValidFrom());
        assertNull(data.getValidUntil());
    }
}