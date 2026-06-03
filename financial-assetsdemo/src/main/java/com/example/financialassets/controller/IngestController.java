package com.example.financialassets.controller;

import com.example.financialassets.service.IngestService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ingest")
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/stooq")
    public Map<String, Object> ingestFromStooq(
            @RequestParam String symbol,
            @RequestParam String assetId
    ) {
        return ingestService.ingestFromStooq(symbol, assetId);
    }

    @PostMapping("/alpha-vantage")
    public Map<String, Object> ingestFromAlphaVantage(
            @RequestParam String symbol,
            @RequestParam String assetId
    ) {
        return ingestService.ingestFromAlphaVantage(symbol, assetId);
    }
}