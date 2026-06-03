package com.example.financialassets.controller;

import com.example.financialassets.model.MarketData;
import com.example.financialassets.repository.MarketDataRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final MarketDataRepository marketDataRepository;

    public AnalyticsController(MarketDataRepository marketDataRepository) {
        this.marketDataRepository = marketDataRepository;
    }

    // Example:
    // GET /api/analytics/summary?assetId=AAPL&providerId=STOOQ
    // GET /api/analytics/summary?assetId=AAPL&providerId=STOOQ&startDate=2024-01-01&endDate=2024-12-31
    @GetMapping("/summary")
    public Map<String, Object> getSummary(
            @RequestParam String assetId,
            @RequestParam String providerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        List<MarketData> data = loadTimeSeries(assetId, providerId, startDate, endDate);

        if (data.isEmpty()) {
            throw new RuntimeException("No market data found for assetId=" + assetId + " and providerId=" + providerId);
        }

        List<Double> closePrices = data.stream()
                .map(MarketData::getClosePrice)
                .filter(price -> price != null)
                .toList();

        if (closePrices.isEmpty()) {
            throw new RuntimeException("No close prices available for assetId=" + assetId);
        }

        double minClose = closePrices.stream().min(Double::compareTo).orElse(0.0);
        double maxClose = closePrices.stream().max(Double::compareTo).orElse(0.0);
        double avgClose = closePrices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        MarketData first = data.stream()
                .filter(row -> row.getClosePrice() != null)
                .min(Comparator.comparing(MarketData::getDataDate))
                .orElseThrow();

        MarketData last = data.stream()
                .filter(row -> row.getClosePrice() != null)
                .max(Comparator.comparing(MarketData::getDataDate))
                .orElseThrow();

        String trend;
        if (last.getClosePrice() > first.getClosePrice()) {
            trend = "UP";
        } else if (last.getClosePrice() < first.getClosePrice()) {
            trend = "DOWN";
        } else {
            trend = "FLAT";
        }

        double absoluteChange = last.getClosePrice() - first.getClosePrice();
        double percentageChange = first.getClosePrice() == 0
                ? 0
                : (absoluteChange / first.getClosePrice()) * 100;

        Map<String, Object> result = new HashMap<>();
        result.put("assetId", assetId);
        result.put("providerId", providerId);
        result.put("recordsCount", data.size());
        result.put("firstDate", first.getDataDate());
        result.put("lastDate", last.getDataDate());
        result.put("firstClose", first.getClosePrice());
        result.put("lastClose", last.getClosePrice());
        result.put("minClose", minClose);
        result.put("maxClose", maxClose);
        result.put("avgClose", avgClose);
        result.put("trend", trend);
        result.put("absoluteChange", absoluteChange);
        result.put("percentageChange", percentageChange);

        return result;
    }

    // Simple forecast:
    // Uses the average of the last 3 close prices as the next-day predicted close.
    //
    // Example:
    // GET /api/analytics/forecast/next-day?assetId=AAPL&providerId=STOOQ
    @GetMapping("/forecast/next-day")
    public Map<String, Object> forecastNextDay(
            @RequestParam String assetId,
            @RequestParam String providerId
    ) {
        List<MarketData> data = marketDataRepository
                .findByAssetIdAndProviderIdAndCurrentTrueAndDeletedFalseOrderByDataDateAsc(assetId, providerId)
                .stream()
                .filter(row -> row.getClosePrice() != null)
                .toList();

        if (data.isEmpty()) {
            throw new RuntimeException("No market data found for forecast.");
        }

        int fromIndex = Math.max(0, data.size() - 3);
        List<MarketData> recent = data.subList(fromIndex, data.size());

        double predictedClose = recent.stream()
                .map(MarketData::getClosePrice)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElseThrow();

        MarketData last = data.get(data.size() - 1);

        Map<String, Object> result = new HashMap<>();
        result.put("assetId", assetId);
        result.put("providerId", providerId);
        result.put("method", "Simple moving average of last 3 close prices");
        result.put("lastKnownDate", last.getDataDate());
        result.put("lastKnownClose", last.getClosePrice());
        result.put("predictedNextDayClose", predictedClose);
        result.put("usedRecords", recent.size());

        return result;
    }

    // Compare two assets using the same provider.
    //
    // Example:
    // GET /api/analytics/compare?assetId1=AAPL&assetId2=MSFT&providerId=STOOQ
    @GetMapping("/compare")
    public Map<String, Object> compareAssets(
            @RequestParam String assetId1,
            @RequestParam String assetId2,
            @RequestParam String providerId
    ) {
        Map<String, Object> summary1 = getSummary(assetId1, providerId, null, null);
        Map<String, Object> summary2 = getSummary(assetId2, providerId, null, null);

        double pct1 = ((Number) summary1.get("percentageChange")).doubleValue();
        double pct2 = ((Number) summary2.get("percentageChange")).doubleValue();

        String winner;
        if (pct1 > pct2) {
            winner = assetId1;
        } else if (pct2 > pct1) {
            winner = assetId2;
        } else {
            winner = "TIE";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("asset1", summary1);
        result.put("asset2", summary2);
        result.put("betterPerformerByPercentageChange", winner);
        result.put("differenceInPercentagePoints", Math.abs(pct1 - pct2));

        return result;
    }

    // ML/Spark-friendly export.
    // The idea is that Spark or another analytics tool can consume this JSON.
    //
    // Example:
    // GET /api/analytics/export?assetId=AAPL&providerId=STOOQ
    @GetMapping("/export")
    public Map<String, Object> exportForAnalytics(
            @RequestParam String assetId,
            @RequestParam String providerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        List<MarketData> data = loadTimeSeries(assetId, providerId, startDate, endDate);

        Map<String, Object> result = new HashMap<>();
        result.put("assetId", assetId);
        result.put("providerId", providerId);
        result.put("format", "json-records");
        result.put("intendedConsumers", List.of("Apache Spark", "ML tools", "BI dashboards"));
        result.put("records", data);

        return result;
    }

    private List<MarketData> loadTimeSeries(
            String assetId,
            String providerId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate != null && endDate != null) {
            return marketDataRepository
                    .findByAssetIdAndProviderIdAndDataDateBetweenAndCurrentTrueAndDeletedFalseOrderByDataDateAsc(
                            assetId,
                            providerId,
                            startDate,
                            endDate
                    );
        }

        return marketDataRepository
                .findByAssetIdAndProviderIdAndCurrentTrueAndDeletedFalseOrderByDataDateAsc(
                        assetId,
                        providerId
                );
    }
}