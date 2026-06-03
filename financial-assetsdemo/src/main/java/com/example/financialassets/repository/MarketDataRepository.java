package com.example.financialassets.repository;

import com.example.financialassets.model.MarketData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MarketDataRepository extends MongoRepository<MarketData, String> {

    List<MarketData> findByCurrentTrueAndDeletedFalse();

    List<MarketData> findByAssetIdAndProviderIdAndCurrentTrueAndDeletedFalseOrderByDataDateAsc(
            String assetId,
            String providerId
    );

    List<MarketData> findByAssetIdAndProviderIdAndDataDateBetweenAndCurrentTrueAndDeletedFalseOrderByDataDateAsc(
            String assetId,
            String providerId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<MarketData> findByTimeSeriesIdAndCurrentTrueAndDeletedFalse(String timeSeriesId);

    List<MarketData> findByTimeSeriesIdOrderByValidFromDesc(String timeSeriesId);

    Optional<MarketData> findByAssetIdAndProviderIdAndDataDateAndCurrentTrueAndDeletedFalse(
            String assetId,
            String providerId,
            LocalDate dataDate
    );

    @Query("{ 'assetId': ?0, 'deleted': false, " +
            "'validFrom': { $lte: ?1 }, " +
            "$or: [ { 'validUntil': null }, { 'validUntil': { $gt: ?1 } } ] }")
    List<MarketData> findValidMarketDataAt(
            String assetId,
            LocalDateTime asOf
    );

    @Query("{ 'assetId': ?0, 'providerId': ?1, 'dataDate': { $gte: ?2, $lte: ?3 }, " +
            "'deleted': false, 'validFrom': { $lte: ?4 }, " +
            "$or: [ { 'validUntil': null }, { 'validUntil': { $gt: ?4 } } ] }")
    List<MarketData> findValidMarketDataBetweenAt(
            String assetId,
            String providerId,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime asOf
    );
}