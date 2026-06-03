package com.example.financialassets.repository;

import com.example.financialassets.model.DataProvider;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DataProviderRepository extends MongoRepository<DataProvider, String> {

    List<DataProvider> findByCurrentTrueAndDeletedFalse();

    Optional<DataProvider> findByProviderIdAndCurrentTrueAndDeletedFalse(String providerId);

    List<DataProvider> findByProviderIdOrderByValidFromDesc(String providerId);

    @Query("{ 'providerId': ?0, 'deleted': false, 'validFrom': { $lte: ?1 }, " +
            "$or: [ { 'validUntil': null }, { 'validUntil': { $gt: ?1 } } ] }")
    Optional<DataProvider> findValidVersionAt(String providerId, LocalDateTime asOf);
}