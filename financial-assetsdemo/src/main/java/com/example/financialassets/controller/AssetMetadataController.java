package com.example.financialassets.controller;

import com.example.financialassets.model.AssetMetadata;
import com.example.financialassets.repository.AssetMetadataRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asset-metadata")
public class AssetMetadataController {

    private final AssetMetadataRepository assetMetadataRepository;

    public AssetMetadataController(AssetMetadataRepository assetMetadataRepository) {
        this.assetMetadataRepository = assetMetadataRepository;
    }

    @GetMapping
    public List<AssetMetadata> getAllAssetMetadata() {
        return assetMetadataRepository.findAll();
    }

    @PostMapping
    public AssetMetadata createAssetMetadata(@RequestBody AssetMetadata assetMetadata) {
        return assetMetadataRepository.save(assetMetadata);
    }

    @PutMapping("/{id}")
    public AssetMetadata updateAssetMetadata(@PathVariable String id,
                                             @RequestBody AssetMetadata updatedMetadata) {

        AssetMetadata metadata = assetMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset metadata not found"));

        metadata.setTicker(updatedMetadata.getTicker());
        metadata.setAssetName(updatedMetadata.getAssetName());
        metadata.setCategory(updatedMetadata.getCategory());
        metadata.setRegion(updatedMetadata.getRegion());
        metadata.setProperties(updatedMetadata.getProperties());
        metadata.setValidFrom(updatedMetadata.getValidFrom());
        metadata.setValidUntil(updatedMetadata.getValidUntil());

        return assetMetadataRepository.save(metadata);
    }

    @DeleteMapping("/{id}")
    public String deleteAssetMetadata(@PathVariable String id) {
        assetMetadataRepository.deleteById(id);
        return "Asset metadata deleted successfully";
    }
}