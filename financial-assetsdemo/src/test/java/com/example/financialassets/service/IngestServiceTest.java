package com.example.financialassets.service;

import com.example.financialassets.repository.AssetRepository;
import com.example.financialassets.repository.DataProviderRepository;
import com.example.financialassets.repository.MarketDataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IngestServiceTest {

    @Test
    void ingestFromAlphaVantageSavesNewMarketDataRows() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        AssetRepository assetRepository = mock(AssetRepository.class);
        DataProviderRepository providerRepository = mock(DataProviderRepository.class);
        MarketDataRepository marketDataRepository = mock(MarketDataRepository.class);

        IngestService service = new IngestService(
                restTemplate,
                assetRepository,
                providerRepository,
                marketDataRepository
        );

        String csv = """
                timestamp,open,high,low,close,volume
                2024-01-01,100,110,90,105,1000
                2024-01-02,105,115,95,110,2000
                """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(csv, HttpStatus.OK));

        when(providerRepository.findByProviderIdAndCurrentTrueAndDeletedFalse(anyString()))
                .thenReturn(Optional.empty());

        when(assetRepository.findByAssetIdAndCurrentTrueAndDeletedFalse(anyString()))
                .thenReturn(Optional.empty());

        when(marketDataRepository.findByAssetIdAndProviderIdAndDataDateAndCurrentTrueAndDeletedFalse(
                anyString(),
                anyString(),
                any(LocalDate.class)
        )).thenReturn(Optional.empty());

        Map<String, Object> result = service.ingestFromAlphaVantage("AAPL", "AAPL");

        assertEquals("ALPHA_VANTAGE", result.get("providerId"));
        assertEquals("AAPL", result.get("assetId"));
        assertEquals(2, result.get("insertedRecords"));
        assertEquals(0, result.get("versionedRecords"));
        assertEquals(0, result.get("skippedRecords"));

        verify(marketDataRepository, times(2)).save(any());
        verify(providerRepository, times(1)).save(any());
        verify(assetRepository, times(1)).save(any());
    }
}