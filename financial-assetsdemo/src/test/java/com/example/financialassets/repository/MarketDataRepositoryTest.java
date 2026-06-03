package com.example.financialassets.repository;

import com.example.financialassets.FinancialAssetsApplication;
import com.example.financialassets.model.MarketData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ContextConfiguration(classes = FinancialAssetsApplication.class)
@DataMongoTest
@EnableMongoRepositories(basePackages = "com.example.financialassets.repository")
@EntityScan(basePackages = "com.example.financialassets.model")
@ActiveProfiles("test")
class MarketDataRepositoryTest {

    @Autowired
    MarketDataRepository repository;

    @Test
    void findByCurrentTrueAndDeletedFalse_returnsOnlyCurrent() {
        repository.save(buildMarketData("AAPL_STOOQ_1", true, false));
        repository.save(buildMarketData("AAPL_STOOQ_2", false, false));
        repository.save(buildMarketData("AAPL_STOOQ_3", true, true));

        List<MarketData> result = repository.findByCurrentTrueAndDeletedFalse();

        assertEquals(1, result.size());
        assertTrue(result.get(0).isCurrent());
        assertFalse(result.get(0).isDeleted());
    }

    @Test
    void findValidMarketDataAt_returnsRecordValidAtThatPoint() {
        MarketData data = buildMarketData("AAPL_STOOQ_2024", false, false);
        data.setAssetId("AAPL");
        data.setValidFrom(LocalDateTime.of(2024, 1, 1, 0, 0));
        data.setValidUntil(LocalDateTime.of(2024, 6, 1, 0, 0));

        repository.save(data);

        List<MarketData> insideRange = repository.findValidMarketDataAt(
                "AAPL",
                LocalDateTime.of(2024, 3, 1, 0, 0)
        );

        List<MarketData> outsideRange = repository.findValidMarketDataAt(
                "AAPL",
                LocalDateTime.of(2024, 8, 1, 0, 0)
        );

        assertFalse(insideRange.isEmpty());
        assertTrue(outsideRange.isEmpty());
    }

    private MarketData buildMarketData(String id, boolean current, boolean deleted) {
        MarketData data = new MarketData();

        data.setTimeSeriesId(id);
        data.setAssetId("AAPL");
        data.setProviderId("STOOQ");
        data.setDataDate(LocalDate.of(2024, 1, 1));

        data.setOpenPrice(100.0);
        data.setHighPrice(110.0);
        data.setLowPrice(90.0);
        data.setClosePrice(105.0);
        data.setVolume(1000L);

        data.setCurrent(current);
        data.setDeleted(deleted);
        data.setValidFrom(LocalDateTime.now());
        data.setValidUntil(null);

        return data;
    }
}