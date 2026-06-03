package com.example.financialassets.controller;

import com.example.financialassets.model.DataProvider;
import com.example.financialassets.repository.DataProviderRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/providers")
public class DataProviderController {

    private final DataProviderRepository dataProviderRepository;

    public DataProviderController(DataProviderRepository dataProviderRepository) {
        this.dataProviderRepository = dataProviderRepository;
    }

    @GetMapping
    public List<Map<String, Object>> getAllCurrentProviders(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return dataProviderRepository.findByCurrentTrueAndDeletedFalse()
                .stream()
                .skip(offset)
                .limit(limit)
                .map(this::toLimitedProviderResponse)
                .toList();
    }

    @GetMapping("/{providerId}")
    public DataProvider getProviderByProviderId(
            @PathVariable String providerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime asOf
    ) {
        if (asOf != null) {
            return dataProviderRepository.findValidVersionAt(providerId, asOf)
                    .orElseThrow(() -> new RuntimeException("No valid provider version found at " + asOf));
        }

        return dataProviderRepository.findByProviderIdAndCurrentTrueAndDeletedFalse(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
    }

    @GetMapping("/{providerId}/history")
    public List<DataProvider> getProviderHistory(@PathVariable String providerId) {
        return dataProviderRepository.findByProviderIdOrderByValidFromDesc(providerId);
    }

    @PostMapping
    public DataProvider createProvider(@RequestBody DataProvider provider) {
        provider.setVersionId(null);

        if (provider.getProviderId() == null || provider.getProviderId().isBlank()) {
            provider.setProviderId(provider.getProviderName().toUpperCase().replace(" ", "_"));
        }

        provider.setValidFrom(LocalDateTime.now());
        provider.setValidUntil(null);
        provider.setCurrent(true);
        provider.setDeleted(false);

        return dataProviderRepository.save(provider);
    }

    @PutMapping("/{providerId}")
    public DataProvider updateProvider(
            @PathVariable String providerId,
            @RequestBody DataProvider updatedProvider
    ) {
        DataProvider currentProvider = dataProviderRepository
                .findByProviderIdAndCurrentTrueAndDeletedFalse(providerId)
                .orElseThrow(() -> new RuntimeException("Current provider not found"));

        LocalDateTime now = LocalDateTime.now();

        currentProvider.setCurrent(false);
        currentProvider.setValidUntil(now);
        dataProviderRepository.save(currentProvider);

        DataProvider newVersion = new DataProvider();
        newVersion.setVersionId(null);
        newVersion.setProviderId(currentProvider.getProviderId());

        newVersion.setProviderName(
                updatedProvider.getProviderName() != null
                        ? updatedProvider.getProviderName()
                        : currentProvider.getProviderName()
        );

        newVersion.setBaseUrl(
                updatedProvider.getBaseUrl() != null
                        ? updatedProvider.getBaseUrl()
                        : currentProvider.getBaseUrl()
        );

        newVersion.setDescription(
                updatedProvider.getDescription() != null
                        ? updatedProvider.getDescription()
                        : currentProvider.getDescription()
        );

        newVersion.setAttributes(
                updatedProvider.getAttributes() != null
                        ? updatedProvider.getAttributes()
                        : currentProvider.getAttributes()
        );

        newVersion.setValidFrom(now);
        newVersion.setValidUntil(null);
        newVersion.setCurrent(true);
        newVersion.setDeleted(false);

        return dataProviderRepository.save(newVersion);
    }

    @DeleteMapping("/{providerId}")
    public String deleteProvider(@PathVariable String providerId) {
        DataProvider currentProvider = dataProviderRepository
                .findByProviderIdAndCurrentTrueAndDeletedFalse(providerId)
                .orElseThrow(() -> new RuntimeException("Current provider not found"));

        LocalDateTime now = LocalDateTime.now();

        currentProvider.setCurrent(false);
        currentProvider.setValidUntil(now);
        dataProviderRepository.save(currentProvider);

        DataProvider deletedVersion = new DataProvider();
        deletedVersion.setVersionId(null);
        deletedVersion.setProviderId(currentProvider.getProviderId());
        deletedVersion.setProviderName(currentProvider.getProviderName());
        deletedVersion.setBaseUrl(currentProvider.getBaseUrl());
        deletedVersion.setDescription(currentProvider.getDescription());
        deletedVersion.setAttributes(currentProvider.getAttributes());

        deletedVersion.setValidFrom(now);
        deletedVersion.setValidUntil(null);
        deletedVersion.setCurrent(true);
        deletedVersion.setDeleted(true);

        dataProviderRepository.save(deletedVersion);

        return "Provider marked as deleted from " + now;
    }

    private Map<String, Object> toLimitedProviderResponse(DataProvider provider) {
        Map<String, Object> response = new HashMap<>();
        response.put("providerId", provider.getProviderId());
        response.put("providerName", provider.getProviderName());
        response.put("baseUrl", provider.getBaseUrl());
        return response;
    }
}