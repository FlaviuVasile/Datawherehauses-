package com.example.financialassets.service;

import org.springframework.beans.factory.annotation.Value;
import com.example.financialassets.model.Asset;
import com.example.financialassets.model.DataProvider;
import com.example.financialassets.model.MarketData;
import com.example.financialassets.repository.AssetRepository;
import com.example.financialassets.repository.DataProviderRepository;
import com.example.financialassets.repository.MarketDataRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class IngestService {

    @Value("${alpha.vantage.api.key}")
    private String alphaVantageApiKey;


    private static final String STOOQ_PROVIDER_ID = "STOOQ";
    private static final String STOOQ_BASE_URL = "https://stooq.com";

    private static final String ALPHA_PROVIDER_ID = "ALPHA_VANTAGE";
    private static final String ALPHA_BASE_URL = "https://www.alphavantage.co";

    private static final String STOOQ_API_KEY = "PASTE_API_KEY_HERE";
    private final RestTemplate restTemplate;
    private final AssetRepository assetRepository;
    private final DataProviderRepository dataProviderRepository;
    private final MarketDataRepository marketDataRepository;

    public IngestService(
            RestTemplate restTemplate,
            AssetRepository assetRepository,
            DataProviderRepository dataProviderRepository,
            MarketDataRepository marketDataRepository
    ) {
        this.restTemplate = restTemplate;
        this.assetRepository = assetRepository;
        this.dataProviderRepository = dataProviderRepository;
        this.marketDataRepository = marketDataRepository;
    }

    public Map<String, Object> ingestFromStooq(String symbol, String assetId) {
        String normalizedSymbol = symbol.toLowerCase();
        String normalizedAssetId = assetId.toUpperCase();

        if (!normalizedSymbol.contains(".")) {
            normalizedSymbol = normalizedSymbol + ".us";
        }

        String endpoint = STOOQ_BASE_URL
                + "/q/d/l/?s=" + normalizedSymbol
                + "&i=d"
                + "&apikey=" + STOOQ_API_KEY;

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Accept", "text/csv,*/*");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                endpoint,
                HttpMethod.GET,
                entity,
                String.class
        );

        String csv = response.getBody();

        if (csv != null && csv.contains("Get your apikey")) {
            Map<String, Object> result = new HashMap<>();
            result.put("providerId", STOOQ_PROVIDER_ID);
            result.put("assetId", normalizedAssetId);
            result.put("symbol", normalizedSymbol);
            result.put("status", "API_KEY_REQUIRED");
            result.put("message", "Stooq requires an API key/captcha. The platform handled this provider limitation without crashing.");
            result.put("endpoint", endpoint);
            return result;
        }

        if (csv == null || csv.isBlank()) {
            throw new RuntimeException("Empty response from Stooq for symbol=" + normalizedSymbol);
        }

        String firstLine = csv.split("\\R")[0].trim();

        if (!firstLine.equalsIgnoreCase("Date,Open,High,Low,Close,Volume")) {
            String preview = csv.substring(0, Math.min(csv.length(), 500));
            throw new RuntimeException("Invalid Stooq CSV for symbol=" + normalizedSymbol + ". Preview=" + preview);
        }

        ensureStooqProviderExists();
        ensureAssetExists(normalizedAssetId, normalizedSymbol);

        return saveCsvMarketData(
                csv,
                normalizedAssetId,
                normalizedSymbol,
                STOOQ_PROVIDER_ID,
                endpoint,
                "Stooq",
                true
        );
    }

    public Map<String, Object> ingestFromAlphaVantage(String symbol, String assetId) {
        String normalizedSymbol = symbol.toUpperCase();
        String normalizedAssetId = assetId.toUpperCase();



        String endpoint = ALPHA_BASE_URL
                + "/query?function=TIME_SERIES_DAILY"
                + "&symbol=" + normalizedSymbol
                + "&apikey=" + alphaVantageApiKey
                + "&datatype=csv";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 FinancialAssetsDemo/1.0");
        headers.set("Accept", "text/csv,text/plain,*/*");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                endpoint,
                HttpMethod.GET,
                entity,
                String.class
        );

        String csv = response.getBody();

        if (csv == null || csv.isBlank() || !csv.contains("timestamp")) {
            String preview = csv == null ? "null" : csv.substring(0, Math.min(csv.length(), 300));
            throw new RuntimeException(
                    "No CSV data returned from Alpha Vantage for symbol=" + symbol +
                            ". HTTP status=" + response.getStatusCode() +
                            ". Response preview=" + preview
            );
        }



        ensureAlphaProviderExists();
        ensureAssetExists(normalizedAssetId, normalizedSymbol);

        return saveCsvMarketData(
                csv,
                normalizedAssetId,
                normalizedSymbol,
                ALPHA_PROVIDER_ID,
                endpoint,
                "Alpha Vantage",
                false
        );
    }

    private Map<String, Object> saveCsvMarketData(
            String csv,
            String assetId,
            String rawSymbol,
            String providerId,
            String endpoint,
            String providerName,
            boolean stooqFormat
    ) {
        String rawHash = sha256(csv);
        LocalDateTime ingestionTime = LocalDateTime.now();

        String[] lines = csv.split("\\R");

        int inserted = 0;
        int versioned = 0;
        int skipped = 0;

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];

            if (line == null || line.isBlank()) {
                continue;
            }

            String[] columns = line.split(",");

            if (columns.length < 6) {
                skipped++;
                continue;
            }

            LocalDate dataDate = LocalDate.parse(columns[0]);

            Double open = parseDouble(columns[1]);
            Double high = parseDouble(columns[2]);
            Double low = parseDouble(columns[3]);
            Double close = parseDouble(columns[4]);
            Long volume = parseLong(columns[5]);

            String timeSeriesId = assetId + "_" + providerId + "_" + dataDate;

            var existing = marketDataRepository
                    .findByAssetIdAndProviderIdAndDataDateAndCurrentTrueAndDeletedFalse(
                            assetId,
                            providerId,
                            dataDate
                    );

            if (existing.isPresent()) {
                MarketData current = existing.get();

                boolean changed =
                        !equalsDouble(current.getOpenPrice(), open) ||
                                !equalsDouble(current.getHighPrice(), high) ||
                                !equalsDouble(current.getLowPrice(), low) ||
                                !equalsDouble(current.getClosePrice(), close) ||
                                !equalsLong(current.getVolume(), volume);

                if (!changed) {
                    skipped++;
                    continue;
                }

                current.setCurrent(false);
                current.setValidUntil(ingestionTime);
                marketDataRepository.save(current);
                versioned++;
            }

            MarketData marketData = new MarketData();
            marketData.setVersionId(null);
            marketData.setTimeSeriesId(timeSeriesId);
            marketData.setAssetId(assetId);
            marketData.setProviderId(providerId);
            marketData.setDataDate(dataDate);

            marketData.setOpenPrice(open);
            marketData.setHighPrice(high);
            marketData.setLowPrice(low);
            marketData.setClosePrice(close);
            marketData.setVolume(volume);

            marketData.setSourceEndpoint(endpoint);
            marketData.setRawProviderSymbol(rawSymbol);
            marketData.setRawDataHash(rawHash);
            marketData.setIngestionTime(ingestionTime);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("interval", "daily");
            attributes.put("format", "csv");
            attributes.put("provider", providerName);
            attributes.put("sourceFormat", stooqFormat ? "Date,Open,High,Low,Close,Volume" : "timestamp,open,high,low,close,volume");
            marketData.setAttributes(attributes);

            marketData.setValidFrom(ingestionTime);
            marketData.setValidUntil(null);
            marketData.setCurrent(true);
            marketData.setDeleted(false);

            marketDataRepository.save(marketData);
            inserted++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("providerId", providerId);
        result.put("assetId", assetId);
        result.put("symbol", rawSymbol);
        result.put("endpoint", endpoint);
        result.put("rawDataHash", rawHash);
        result.put("ingestionTime", ingestionTime);
        result.put("insertedRecords", inserted);
        result.put("versionedRecords", versioned);
        result.put("skippedRecords", skipped);

        return result;
    }

    private void ensureStooqProviderExists() {
        boolean exists = dataProviderRepository
                .findByProviderIdAndCurrentTrueAndDeletedFalse(STOOQ_PROVIDER_ID)
                .isPresent();

        if (exists) {
            return;
        }

        DataProvider provider = new DataProvider();
        provider.setVersionId(null);
        provider.setProviderId(STOOQ_PROVIDER_ID);
        provider.setProviderName("Stooq");
        provider.setBaseUrl(STOOQ_BASE_URL);
        provider.setDescription("Public financial data provider exposing historical OHLCV CSV data.");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("apiType", "REST/CSV");
        attributes.put("requiresApiKey", false);
        attributes.put("dataType", "daily historical prices");
        provider.setAttributes(attributes);

        provider.setValidFrom(LocalDateTime.now());
        provider.setValidUntil(null);
        provider.setCurrent(true);
        provider.setDeleted(false);

        dataProviderRepository.save(provider);
    }

    private void ensureAlphaProviderExists() {
        boolean exists = dataProviderRepository
                .findByProviderIdAndCurrentTrueAndDeletedFalse(ALPHA_PROVIDER_ID)
                .isPresent();

        if (exists) {
            return;
        }

        DataProvider provider = new DataProvider();
        provider.setVersionId(null);
        provider.setProviderId(ALPHA_PROVIDER_ID);
        provider.setProviderName("Alpha Vantage");
        provider.setBaseUrl(ALPHA_BASE_URL);
        provider.setDescription("Public REST financial data provider exposing historical OHLCV market data.");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("apiType", "REST/CSV");
        attributes.put("requiresApiKey", true);
        attributes.put("dataType", "daily historical prices");
        provider.setAttributes(attributes);

        provider.setValidFrom(LocalDateTime.now());
        provider.setValidUntil(null);
        provider.setCurrent(true);
        provider.setDeleted(false);

        dataProviderRepository.save(provider);
    }

    private void ensureAssetExists(String assetId, String rawSymbol) {
        boolean exists = assetRepository
                .findByAssetIdAndCurrentTrueAndDeletedFalse(assetId)
                .isPresent();

        if (exists) {
            return;
        }

        Asset asset = new Asset();
        asset.setVersionId(null);
        asset.setAssetId(assetId);
        asset.setTicker(assetId);
        asset.setAssetName(assetId);
        asset.setCategory("STOCK");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("rawProviderSymbol", rawSymbol);
        attributes.put("source", "created automatically during ingest");
        asset.setAttributes(attributes);

        asset.setValidFrom(LocalDateTime.now());
        asset.setValidUntil(null);
        asset.setCurrent(true);
        asset.setDeleted(false);

        assetRepository.save(asset);
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("null")) {
            return null;
        }

        return Double.parseDouble(value);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("null")) {
            return null;
        }

        return Long.parseLong(value);
    }

    private boolean equalsDouble(Double a, Double b) {
        if (a == null && b == null) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        return Double.compare(a, b) == 0;
    }

    private boolean equalsLong(Long a, Long b) {
        if (a == null && b == null) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        return Long.compare(a, b) == 0;
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (Exception exception) {
            throw new RuntimeException("Could not calculate SHA-256 hash", exception);
        }
    }
}