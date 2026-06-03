package com.example.financialassets.controller;

import com.example.financialassets.model.MarketData;
import com.example.financialassets.repository.MarketDataRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

    private final MarketDataRepository marketDataRepository;

    public MarketDataController(MarketDataRepository marketDataRepository) {
        this.marketDataRepository = marketDataRepository;
    }

    @GetMapping
    public List<MarketData> getAllCurrentMarketData(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return marketDataRepository.findByCurrentTrueAndDeletedFalse()
                .stream()
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @GetMapping("/search")
    public List<MarketData> searchMarketData(
            @RequestParam String assetId,
            @RequestParam String providerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime asOf
    ) {
        List<MarketData> result;

        if (asOf != null) {
            if (startDate != null && endDate != null) {
                result = marketDataRepository.findValidMarketDataBetweenAt(
                        assetId,
                        providerId,
                        startDate,
                        endDate,
                        asOf
                );
            } else {
                result = marketDataRepository.findValidMarketDataAt(
                        assetId,
                        asOf
                );
            }

            return result.stream()
                    .sorted(Comparator.comparing(MarketData::getDataDate))
                    .toList();
        }

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

    @GetMapping("/{timeSeriesId}")
    public MarketData getByTimeSeriesId(@PathVariable String timeSeriesId) {
        return marketDataRepository.findByTimeSeriesIdAndCurrentTrueAndDeletedFalse(timeSeriesId)
                .orElseThrow(() -> new RuntimeException("Market data not found"));
    }

    @GetMapping("/{timeSeriesId}/history")
    public List<MarketData> getMarketDataHistory(@PathVariable String timeSeriesId) {
        return marketDataRepository.findByTimeSeriesIdOrderByValidFromDesc(timeSeriesId);
    }

    @PostMapping
    public MarketData createMarketData(@RequestBody MarketData marketData) {
        if (marketData.getTimeSeriesId() == null || marketData.getTimeSeriesId().isBlank()) {
            marketData.setTimeSeriesId(
                    marketData.getAssetId() + "_" +
                            marketData.getProviderId() + "_" +
                            marketData.getDataDate()
            );
        }

        marketData.setVersionId(null);
        marketData.setValidFrom(LocalDateTime.now());
        marketData.setValidUntil(null);
        marketData.setCurrent(true);
        marketData.setDeleted(false);

        if (marketData.getIngestionTime() == null) {
            marketData.setIngestionTime(LocalDateTime.now());
        }

        return marketDataRepository.save(marketData);
    }

    @PutMapping("/{timeSeriesId}")
    public MarketData updateMarketData(
            @PathVariable String timeSeriesId,
            @RequestBody MarketData updatedData
    ) {
        MarketData currentData = marketDataRepository
                .findByTimeSeriesIdAndCurrentTrueAndDeletedFalse(timeSeriesId)
                .orElseThrow(() -> new RuntimeException("Current market data not found"));

        LocalDateTime now = LocalDateTime.now();

        currentData.setCurrent(false);
        currentData.setValidUntil(now);
        marketDataRepository.save(currentData);

        MarketData newVersion = new MarketData();
        newVersion.setVersionId(null);
        newVersion.setTimeSeriesId(currentData.getTimeSeriesId());

        newVersion.setAssetId(
                updatedData.getAssetId() != null ? updatedData.getAssetId() : currentData.getAssetId()
        );
        newVersion.setProviderId(
                updatedData.getProviderId() != null ? updatedData.getProviderId() : currentData.getProviderId()
        );
        newVersion.setDataDate(
                updatedData.getDataDate() != null ? updatedData.getDataDate() : currentData.getDataDate()
        );

        newVersion.setOpenPrice(
                updatedData.getOpenPrice() != null ? updatedData.getOpenPrice() : currentData.getOpenPrice()
        );
        newVersion.setHighPrice(
                updatedData.getHighPrice() != null ? updatedData.getHighPrice() : currentData.getHighPrice()
        );
        newVersion.setLowPrice(
                updatedData.getLowPrice() != null ? updatedData.getLowPrice() : currentData.getLowPrice()
        );
        newVersion.setClosePrice(
                updatedData.getClosePrice() != null ? updatedData.getClosePrice() : currentData.getClosePrice()
        );
        newVersion.setVolume(
                updatedData.getVolume() != null ? updatedData.getVolume() : currentData.getVolume()
        );

        newVersion.setAttributes(
                updatedData.getAttributes() != null ? updatedData.getAttributes() : currentData.getAttributes()
        );

        newVersion.setSourceEndpoint(
                updatedData.getSourceEndpoint() != null ? updatedData.getSourceEndpoint() : currentData.getSourceEndpoint()
        );
        newVersion.setRawProviderSymbol(
                updatedData.getRawProviderSymbol() != null ? updatedData.getRawProviderSymbol() : currentData.getRawProviderSymbol()
        );
        newVersion.setRawDataHash(
                updatedData.getRawDataHash() != null ? updatedData.getRawDataHash() : currentData.getRawDataHash()
        );
        newVersion.setIngestionTime(
                updatedData.getIngestionTime() != null ? updatedData.getIngestionTime() : currentData.getIngestionTime()
        );

        newVersion.setValidFrom(now);
        newVersion.setValidUntil(null);
        newVersion.setCurrent(true);
        newVersion.setDeleted(false);

        return marketDataRepository.save(newVersion);
    }

    @DeleteMapping("/{timeSeriesId}")
    public String deleteMarketData(@PathVariable String timeSeriesId) {
        MarketData currentData = marketDataRepository
                .findByTimeSeriesIdAndCurrentTrueAndDeletedFalse(timeSeriesId)
                .orElseThrow(() -> new RuntimeException("Current market data not found"));

        LocalDateTime now = LocalDateTime.now();

        currentData.setCurrent(false);
        currentData.setValidUntil(now);
        marketDataRepository.save(currentData);

        MarketData deletedVersion = new MarketData();
        deletedVersion.setVersionId(null);
        deletedVersion.setTimeSeriesId(currentData.getTimeSeriesId());
        deletedVersion.setAssetId(currentData.getAssetId());
        deletedVersion.setProviderId(currentData.getProviderId());
        deletedVersion.setDataDate(currentData.getDataDate());

        deletedVersion.setOpenPrice(currentData.getOpenPrice());
        deletedVersion.setHighPrice(currentData.getHighPrice());
        deletedVersion.setLowPrice(currentData.getLowPrice());
        deletedVersion.setClosePrice(currentData.getClosePrice());
        deletedVersion.setVolume(currentData.getVolume());

        deletedVersion.setAttributes(currentData.getAttributes());

        deletedVersion.setSourceEndpoint(currentData.getSourceEndpoint());
        deletedVersion.setRawProviderSymbol(currentData.getRawProviderSymbol());
        deletedVersion.setRawDataHash(currentData.getRawDataHash());
        deletedVersion.setIngestionTime(currentData.getIngestionTime());

        deletedVersion.setValidFrom(now);
        deletedVersion.setValidUntil(null);
        deletedVersion.setCurrent(true);
        deletedVersion.setDeleted(true);

        marketDataRepository.save(deletedVersion);

        return "Market data marked as deleted from " + now;
    }
}