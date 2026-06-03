package com.example.financialassets.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "market_data")
public class MarketData {

    @Id
    private String versionId;

    private String timeSeriesId;

    private String assetId;
    private String providerId;

    private LocalDate dataDate;

    private Double openPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double closePrice;
    private Long volume;

    private Map<String, Object> attributes;

    // provenance
    private String sourceEndpoint;
    private String rawProviderSymbol;
    private String rawDataHash;
    private LocalDateTime ingestionTime;

    // temporal warehouse
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private boolean current;
    private boolean deleted;

    public MarketData() {
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getTimeSeriesId() {
        return timeSeriesId;
    }

    public void setTimeSeriesId(String timeSeriesId) {
        this.timeSeriesId = timeSeriesId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public LocalDate getDataDate() {
        return dataDate;
    }

    public void setDataDate(LocalDate dataDate) {
        this.dataDate = dataDate;
    }

    public Double getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(Double openPrice) {
        this.openPrice = openPrice;
    }

    public Double getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(Double highPrice) {
        this.highPrice = highPrice;
    }

    public Double getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(Double lowPrice) {
        this.lowPrice = lowPrice;
    }

    public Double getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(Double closePrice) {
        this.closePrice = closePrice;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String getSourceEndpoint() {
        return sourceEndpoint;
    }

    public void setSourceEndpoint(String sourceEndpoint) {
        this.sourceEndpoint = sourceEndpoint;
    }

    public String getRawProviderSymbol() {
        return rawProviderSymbol;
    }

    public void setRawProviderSymbol(String rawProviderSymbol) {
        this.rawProviderSymbol = rawProviderSymbol;
    }

    public String getRawDataHash() {
        return rawDataHash;
    }

    public void setRawDataHash(String rawDataHash) {
        this.rawDataHash = rawDataHash;
    }

    public LocalDateTime getIngestionTime() {
        return ingestionTime;
    }

    public void setIngestionTime(LocalDateTime ingestionTime) {
        this.ingestionTime = ingestionTime;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}