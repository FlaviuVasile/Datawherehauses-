package com.example.financialassets.controller;

import com.example.financialassets.model.Asset;
import com.example.financialassets.repository.AssetRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AssetControllerTest {

    @Test
    void getAssetByAssetIdReturnsCurrentAsset() {
        AssetRepository assetRepository = mock(AssetRepository.class);
        AssetController controller = new AssetController(assetRepository);

        Asset asset = new Asset();
        asset.setAssetId("AAPL");
        asset.setTicker("AAPL");
        asset.setAssetName("Apple Inc.");
        asset.setCategory("Stock");
        asset.setCurrent(true);
        asset.setDeleted(false);

        when(assetRepository.findByAssetIdAndCurrentTrueAndDeletedFalse("AAPL"))
                .thenReturn(Optional.of(asset));

        Asset result = controller.getAssetByAssetId("AAPL", null);

        assertEquals("AAPL", result.getAssetId());
        assertEquals("Apple Inc.", result.getAssetName());
        verify(assetRepository).findByAssetIdAndCurrentTrueAndDeletedFalse("AAPL");
    }

    @Test
    void getAllCurrentAssetsReturnsLimitedResponse() {
        AssetRepository assetRepository = mock(AssetRepository.class);
        AssetController controller = new AssetController(assetRepository);

        Asset asset = new Asset();
        asset.setAssetId("MSFT");
        asset.setTicker("MSFT");
        asset.setAssetName("Microsoft");
        asset.setCategory("Stock");

        when(assetRepository.findByCurrentTrueAndDeletedFalse())
                .thenReturn(List.of(asset));

        var result = controller.getAllCurrentAssets(0, 50);

        assertEquals(1, result.size());
        assertEquals("MSFT", result.get(0).get("assetId"));
        assertEquals("Microsoft", result.get(0).get("assetName"));
    }

    @Test
    void createAssetSetsTemporalFields() {
        AssetRepository assetRepository = mock(AssetRepository.class);
        AssetController controller = new AssetController(assetRepository);

        Asset asset = new Asset();
        asset.setTicker("IBM");
        asset.setAssetName("IBM Corp.");
        asset.setCategory("Stock");

        when(assetRepository.save(any(Asset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Asset result = controller.createAsset(asset);

        assertEquals("IBM", result.getAssetId());
        assertTrue(result.isCurrent());
        assertFalse(result.isDeleted());
        assertNotNull(result.getValidFrom());
        assertNull(result.getValidUntil());
    }
}