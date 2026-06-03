package com.example.financialassets.repository;

import com.example.financialassets.model.AssetMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AssetMetadataRepository extends MongoRepository<AssetMetadata, String> {

    List<AssetMetadata> findByRegion(String region);

    List<AssetMetadata> findByCategory(String category);
}