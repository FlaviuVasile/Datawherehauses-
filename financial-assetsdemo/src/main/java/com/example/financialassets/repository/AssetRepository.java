package com.example.financialassets.repository;

import com.example.financialassets.model.Asset;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AssetRepository extends MongoRepository<Asset, String> {

    List<Asset> findByCurrentTrueAndDeletedFalse();

    Optional<Asset> findByAssetIdAndCurrentTrueAndDeletedFalse(String assetId);

    List<Asset> findByCategoryAndCurrentTrueAndDeletedFalse(String category);

    List<Asset> findByAssetIdOrderByValidFromDesc(String assetId);

    @Query("{ 'assetId': ?0, 'deleted': false, 'validFrom': { $lte: ?1 }, " +
            "$or: [ { 'validUntil': null }, { 'validUntil': { $gt: ?1 } } ] }")
    Optional<Asset> findValidVersionAt(String assetId, LocalDateTime asOf);
}