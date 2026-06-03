package com.example.financialassets.repository;

import com.example.financialassets.model.ProviderMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProviderMetadataRepository extends MongoRepository<ProviderMetadata, String> {

    List<ProviderMetadata> findByProviderName(String providerName);
}