package com.example.financialassets.controller;

import com.example.financialassets.model.Asset;
import com.example.financialassets.repository.AssetRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetRepository assetRepository;

    public AssetController(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @GetMapping
    public List<Map<String, Object>> getAllCurrentAssets(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return assetRepository.findByCurrentTrueAndDeletedFalse()
                .stream()
                .skip(offset)
                .limit(limit)
                .map(this::toLimitedAssetResponse)
                .toList();
    }

    @GetMapping("/{assetId}")
    public Asset getAssetByAssetId(
            @PathVariable String assetId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime asOf
    ) {
        if (asOf != null) {
            return assetRepository.findValidVersionAt(assetId, asOf)
                    .orElseThrow(() -> new RuntimeException("No valid asset version found at " + asOf));
        }

        return assetRepository.findByAssetIdAndCurrentTrueAndDeletedFalse(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
    }

    @GetMapping("/{assetId}/history")
    public List<Asset> getAssetHistory(@PathVariable String assetId) {
        return assetRepository.findByAssetIdOrderByValidFromDesc(assetId);
    }

    @GetMapping("/category/{category}")
    public List<Asset> getAssetsByCategory(@PathVariable String category) {
        return assetRepository.findByCategoryAndCurrentTrueAndDeletedFalse(category);
    }

    @PostMapping
    public Asset createAsset(@RequestBody Asset asset) {
        if (asset.getAssetId() == null || asset.getAssetId().isBlank()) {
            asset.setAssetId(asset.getTicker());
        }

        asset.setVersionId(null);
        asset.setValidFrom(LocalDateTime.now());
        asset.setValidUntil(null);
        asset.setCurrent(true);
        asset.setDeleted(false);

        return assetRepository.save(asset);
    }

    @PutMapping("/{assetId}")
    public Asset updateAsset(@PathVariable String assetId, @RequestBody Asset updatedAsset) {
        Asset currentAsset = assetRepository.findByAssetIdAndCurrentTrueAndDeletedFalse(assetId)
                .orElseThrow(() -> new RuntimeException("Current asset not found"));

        LocalDateTime now = LocalDateTime.now();

        currentAsset.setCurrent(false);
        currentAsset.setValidUntil(now);
        assetRepository.save(currentAsset);

        Asset newVersion = new Asset();
        newVersion.setVersionId(null);
        newVersion.setAssetId(currentAsset.getAssetId());

        newVersion.setTicker(
                updatedAsset.getTicker() != null ? updatedAsset.getTicker() : currentAsset.getTicker()
        );
        newVersion.setAssetName(
                updatedAsset.getAssetName() != null ? updatedAsset.getAssetName() : currentAsset.getAssetName()
        );
        newVersion.setCategory(
                updatedAsset.getCategory() != null ? updatedAsset.getCategory() : currentAsset.getCategory()
        );
        newVersion.setAttributes(
                updatedAsset.getAttributes() != null ? updatedAsset.getAttributes() : currentAsset.getAttributes()
        );

        newVersion.setValidFrom(now);
        newVersion.setValidUntil(null);
        newVersion.setCurrent(true);
        newVersion.setDeleted(false);

        return assetRepository.save(newVersion);
    }

    @DeleteMapping("/{assetId}")
    public String deleteAsset(@PathVariable String assetId) {
        Asset currentAsset = assetRepository.findByAssetIdAndCurrentTrueAndDeletedFalse(assetId)
                .orElseThrow(() -> new RuntimeException("Current asset not found"));

        LocalDateTime now = LocalDateTime.now();

        currentAsset.setCurrent(false);
        currentAsset.setValidUntil(now);
        assetRepository.save(currentAsset);

        Asset deletedVersion = new Asset();
        deletedVersion.setVersionId(null);
        deletedVersion.setAssetId(currentAsset.getAssetId());
        deletedVersion.setTicker(currentAsset.getTicker());
        deletedVersion.setAssetName(currentAsset.getAssetName());
        deletedVersion.setCategory(currentAsset.getCategory());
        deletedVersion.setAttributes(currentAsset.getAttributes());

        deletedVersion.setValidFrom(now);
        deletedVersion.setValidUntil(null);
        deletedVersion.setCurrent(true);
        deletedVersion.setDeleted(true);

        assetRepository.save(deletedVersion);

        return "Asset marked as deleted from " + now;
    }

    private Map<String, Object> toLimitedAssetResponse(Asset asset) {
        Map<String, Object> response = new HashMap<>();
        response.put("assetId", asset.getAssetId());
        response.put("ticker", asset.getTicker());
        response.put("assetName", asset.getAssetName());
        response.put("category", asset.getCategory());
        return response;
    }
}