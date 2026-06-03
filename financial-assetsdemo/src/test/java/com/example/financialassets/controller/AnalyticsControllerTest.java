package com.example.financialassets.controller;

import com.example.financialassets.model.MarketData;
import com.example.financialassets.repository.MarketDataRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsControllerTest {

    @Test
    void getSummaryCalculatesCorrectValues() {
        MarketDataRepository repository = mock(MarketDataRepository.class);
        AnalyticsController controller = new AnalyticsController(repository);

        MarketData first = marketData("AAPL", "STOOQ", "2024-01-01", 100.0);
        MarketData second = marketData("AAPL", "STOOQ", "2024-01-02", 110.0);
        MarketData third = marketData("AAPL", "STOOQ", "2024-01-03", 130.0);

        when(repository.findByAssetIdAndProviderIdAndCurrentTrueAndDeletedFalseOrderByDataDateAsc("AAPL", "STOOQ"))
                .thenReturn(List.of(first, second, third));

        Map<String, Object> result = controller.getSummary("AAPL", "STOOQ", null, null);

        assertEquals(3, result.get("recordsCount"));
        assertEquals(100.0, result.get("minClose"));
        assertEquals(130.0, result.get("maxClose"));
        assertEquals(113.33333333333333, (Double) result.get("avgClose"), 0.001);
        assertEquals("UP", result.get("trend"));
        assertEquals(30.0, result.get("absoluteChange"));
        assertEquals(30.0, (Double) result.get("percentageChange"), 0.001);
    }

    @Test
    void forecastNextDayUsesLastThreeClosePrices() {
        MarketDataRepository repository = mock(MarketDataRepository.class);
        AnalyticsController controller = new AnalyticsController(repository);

        when(repository.findByAssetIdAndProviderIdAndCurrentTrueAndDeletedFalseOrderByDataDateAsc("AAPL", "STOOQ"))
                .thenReturn(List.of(
                        marketData("AAPL", "STOOQ", "2024-01-01", 100.0),
                        marketData("AAPL", "STOOQ", "2024-01-02", 110.0),
                        marketData("AAPL", "STOOQ", "2024-01-03", 120.0),
                        marketData("AAPL", "STOOQ", "2024-01-04", 130.0)
                ));

        Map<String, Object> result = controller.forecastNextDay("AAPL", "STOOQ");

        assertEquals(120.0, (Double) result.get("predictedNextDayClose"), 0.001);
        assertEquals(3, result.get("usedRecords"));
    }

    private MarketData marketData(String assetId, String providerId, String date, Double close) {
        MarketData data = new MarketData();
        data.setAssetId(assetId);
        data.setProviderId(providerId);
        data.setDataDate(LocalDate.parse(date));
        data.setClosePrice(close);
        data.setCurrent(true);
        data.setDeleted(false);
        return data;
    }
}