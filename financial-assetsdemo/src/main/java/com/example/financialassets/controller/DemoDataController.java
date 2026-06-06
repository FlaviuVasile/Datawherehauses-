package com.example.financialassets.controller;

import com.example.financialassets.model.Asset;
import com.example.financialassets.model.DataProvider;
import com.example.financialassets.model.MarketData;
import com.example.financialassets.repository.AssetRepository;
import com.example.financialassets.repository.DataProviderRepository;
import com.example.financialassets.repository.MarketDataRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoDataController {

    private static final String PROVIDER_ID = "DEMO_VENDOR";

    private final AssetRepository assetRepository;
    private final DataProviderRepository dataProviderRepository;
    private final MarketDataRepository marketDataRepository;

    public DemoDataController(
            AssetRepository assetRepository,
            DataProviderRepository dataProviderRepository,
            MarketDataRepository marketDataRepository
    ) {
        this.assetRepository = assetRepository;
        this.dataProviderRepository = dataProviderRepository;
        this.marketDataRepository = marketDataRepository;
    }

    @PostMapping("/seed")
    public Map<String, Object> seedDemoData() {
        LocalDateTime seedTime = LocalDateTime.now();

        boolean providerCreated = ensureProvider(seedTime);
        int assetsCreated = 0;
        int rowsCreated = 0;

        assetsCreated += ensureAsset("AAPL", "AAPL", "Apple Inc.", "STOCK", "US", seedTime) ? 1 : 0;
        assetsCreated += ensureAsset("MSFT", "MSFT", "Microsoft Corporation", "STOCK", "US", seedTime) ? 1 : 0;
        assetsCreated += ensureAsset("BTC", "BTC", "Bitcoin", "CRYPTO", "Global", seedTime) ? 1 : 0;

        rowsCreated += seedSeries("AAPL", List.of(185.64, 187.15, 186.80, 189.25, 191.12), seedTime);
        rowsCreated += seedSeries("MSFT", List.of(410.34, 413.50, 409.90, 416.77, 418.22), seedTime);
        rowsCreated += seedSeries("BTC", List.of(67250.0, 68110.5, 67940.2, 69015.8, 70122.4), seedTime);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "OK");
        result.put("providerId", PROVIDER_ID);
        result.put("providerCreated", providerCreated);
        result.put("assetsCreated", assetsCreated);
        result.put("marketRowsCreated", rowsCreated);
        result.put("message", "Demo data is ready for Q1-Q5, analytics, forecast, assistant tools, and temporal update demos.");
        return result;
    }

    private boolean ensureProvider(LocalDateTime seedTime) {
        boolean exists = dataProviderRepository
                .findByProviderIdAndCurrentTrueAndDeletedFalse(PROVIDER_ID)
                .isPresent();

        if (exists) {
            return false;
        }

        DataProvider provider = new DataProvider();
        provider.setProviderId(PROVIDER_ID);
        provider.setProviderName("Demo Market Data Vendor");
        provider.setBaseUrl("local://demo-data");
        provider.setDescription("Deterministic local sample provider used for reproducible demos without external API calls.");
        provider.setAttributes(Map.of(
                "apiType", "LOCAL_SAMPLE",
                "requiresApiKey", false,
                "dataType", "daily OHLCV prices"
        ));
        provider.setValidFrom(seedTime);
        provider.setCurrent(true);
        provider.setDeleted(false);

        dataProviderRepository.save(provider);
        return true;
    }

    private boolean ensureAsset(
            String assetId,
            String ticker,
            String name,
            String category,
            String region,
            LocalDateTime seedTime
    ) {
        boolean exists = assetRepository
                .findByAssetIdAndCurrentTrueAndDeletedFalse(assetId)
                .isPresent();

        if (exists) {
            return false;
        }

        Asset asset = new Asset();
        asset.setAssetId(assetId);
        asset.setTicker(ticker);
        asset.setAssetName(name);
        asset.setCategory(category);
        asset.setAttributes(Map.of(
                "region", region,
                "description", name + " demo financial instrument",
                "demoSeed", true
        ));
        asset.setValidFrom(seedTime);
        asset.setCurrent(true);
        asset.setDeleted(false);

        assetRepository.save(asset);
        return true;
    }

    private int seedSeries(String assetId, List<Double> closes, LocalDateTime seedTime) {
        int created = 0;
        LocalDate start = LocalDate.of(2026, 1, 5);

        for (int i = 0; i < closes.size(); i++) {
            LocalDate date = start.plusDays(i);
            boolean exists = marketDataRepository
                    .findByAssetIdAndProviderIdAndDataDateAndCurrentTrueAndDeletedFalse(assetId, PROVIDER_ID, date)
                    .isPresent();

            if (exists) {
                continue;
            }

            double close = closes.get(i);
            MarketData row = new MarketData();
            row.setTimeSeriesId(assetId + "_" + PROVIDER_ID + "_" + date);
            row.setAssetId(assetId);
            row.setProviderId(PROVIDER_ID);
            row.setDataDate(date);
            row.setOpenPrice(close - 1.20);
            row.setHighPrice(close + 2.10);
            row.setLowPrice(close - 2.40);
            row.setClosePrice(close);
            row.setVolume(1_000_000L + (i * 125_000L));
            row.setSourceEndpoint("local://demo-data/" + assetId);
            row.setRawProviderSymbol(assetId);
            row.setRawDataHash("demo-" + assetId + "-" + date);
            row.setIngestionTime(seedTime);
            row.setAttributes(Map.of(
                    "interval", "daily",
                    "format", "seed",
                    "provider", "Demo Market Data Vendor"
            ));
            row.setValidFrom(seedTime);
            row.setCurrent(true);
            row.setDeleted(false);

            marketDataRepository.save(row);
            created++;
        }

        return created;
    }
}
