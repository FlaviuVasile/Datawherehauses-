package com.example.financialassets.controller;

import com.example.financialassets.model.ProviderMetadata;
import com.example.financialassets.repository.ProviderMetadataRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider-metadata")
public class ProviderMetadataController {

    private final ProviderMetadataRepository providerMetadataRepository;

    public ProviderMetadataController(ProviderMetadataRepository providerMetadataRepository) {
        this.providerMetadataRepository = providerMetadataRepository;
    }

    @GetMapping
    public List<ProviderMetadata> getAllProviderMetadata() {
        return providerMetadataRepository.findAll();
    }

    @PostMapping
    public ProviderMetadata createProviderMetadata(@RequestBody ProviderMetadata providerMetadata) {
        return providerMetadataRepository.save(providerMetadata);
    }

    @PutMapping("/{id}")
    public ProviderMetadata updateProviderMetadata(@PathVariable String id,
                                                   @RequestBody ProviderMetadata updatedMetadata) {

        ProviderMetadata metadata = providerMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider metadata not found"));

        metadata.setProviderName(updatedMetadata.getProviderName());
        metadata.setApiUrl(updatedMetadata.getApiUrl());
        metadata.setDescription(updatedMetadata.getDescription());
        metadata.setValidFrom(updatedMetadata.getValidFrom());
        metadata.setValidUntil(updatedMetadata.getValidUntil());

        return providerMetadataRepository.save(metadata);
    }

    @DeleteMapping("/{id}")
    public String deleteProviderMetadata(@PathVariable String id) {
        providerMetadataRepository.deleteById(id);
        return "Provider metadata deleted successfully";
    }
}