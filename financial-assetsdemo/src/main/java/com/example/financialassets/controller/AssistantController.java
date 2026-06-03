package com.example.financialassets.controller;

import com.example.financialassets.service.OllamaService;
import com.example.financialassets.model.Asset;
import com.example.financialassets.model.DataProvider;
import com.example.financialassets.model.MarketData;
import com.example.financialassets.repository.AssetRepository;
import com.example.financialassets.repository.DataProviderRepository;
import com.example.financialassets.repository.MarketDataRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final OllamaService ollamaService;
    private final AssetRepository assetRepository;
    private final DataProviderRepository dataProviderRepository;
    private final MarketDataRepository marketDataRepository;
    private final AnalyticsController analyticsController;
    private final IngestController ingestController;

    public AssistantController(
            OllamaService ollamaService, AssetRepository assetRepository,
            DataProviderRepository dataProviderRepository,
            MarketDataRepository marketDataRepository,
            AnalyticsController analyticsController,
            IngestController ingestController
    ) {
        this.ollamaService = ollamaService;
        this.assetRepository = assetRepository;
        this.dataProviderRepository = dataProviderRepository;
        this.marketDataRepository = marketDataRepository;
        this.analyticsController = analyticsController;
        this.ingestController = ingestController;
    }


    @GetMapping("/tools")
    public List<Map<String, Object>> listTools() {
        return List.of(
                tool("list_assets", "Lists all current financial assets."),
                tool("get_asset_details", "Returns details for one asset."),
                tool("list_data_sources", "Lists all financial data providers."),
                tool("fetch_time_series", "Returns market time-series data for asset/provider."),
                tool("summarize_trends", "Computes min, max, avg, trend and percentage change."),
                tool("forecast_next_day", "Predicts next close price using simple moving average."),
                tool("compare_assets", "Compares two assets using percentage change."),
                tool("explain_change", "Explains latest close-to-close change using stored data."),
                tool("agent_demo", "Runs a multi-step agent workflow."),
                tool("llm_chat", "Uses a local Ollama LLM to answer using stored analytics data.")
        );
    }

    @GetMapping("/tools/list-assets")
    public List<Asset> listAssets() {
        return assetRepository.findByCurrentTrueAndDeletedFalse();
    }

    @GetMapping("/tools/get-asset-details")
    public Asset getAssetDetails(@RequestParam String assetId) {
        return assetRepository.findByAssetIdAndCurrentTrueAndDeletedFalse(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetId));
    }

    @GetMapping("/tools/list-data-sources")
    public List<DataProvider> listDataSources() {
        return dataProviderRepository.findByCurrentTrueAndDeletedFalse();
    }

    @GetMapping("/tools/fetch-time-series")
    public List<MarketData> fetchTimeSeries(
            @RequestParam String assetId,
            @RequestParam String providerId
    ) {
        return marketDataRepository
                .findByAssetIdAndProviderIdAndCurrentTrueAndDeletedFalseOrderByDataDateAsc(assetId, providerId);
    }

    @GetMapping("/tools/summarize-trends")
    public Map<String, Object> summarizeTrends(
            @RequestParam String assetId,
            @RequestParam String providerId
    ) {
        return analyticsController.getSummary(assetId, providerId, null, null);
    }

    @GetMapping("/tools/forecast-next-day")
    public Map<String, Object> forecastNextDay(
            @RequestParam String assetId,
            @RequestParam String providerId
    ) {
        return analyticsController.forecastNextDay(assetId, providerId);
    }

    @GetMapping("/tools/compare-assets")
    public Map<String, Object> compareAssets(
            @RequestParam String assetId1,
            @RequestParam String assetId2,
            @RequestParam String providerId
    ) {
        return analyticsController.compareAssets(assetId1, assetId2, providerId);
    }

    @GetMapping("/tools/explain-change")
    public Map<String, Object> explainChange(
            @RequestParam String assetId,
            @RequestParam String providerId
    ) {
        List<MarketData> data = marketDataRepository
                .findByAssetIdAndProviderIdAndCurrentTrueAndDeletedFalseOrderByDataDateAsc(assetId, providerId)
                .stream()
                .filter(row -> row.getClosePrice() != null)
                .toList();

        if (data.size() < 2) {
            throw new RuntimeException("Not enough data to explain latest change.");
        }

        MarketData previous = data.get(data.size() - 2);
        MarketData latest = data.get(data.size() - 1);

        double previousClose = previous.getClosePrice();
        double latestClose = latest.getClosePrice();

        double absoluteChange = latestClose - previousClose;
        double percentageChange = previousClose == 0
                ? 0
                : (absoluteChange / previousClose) * 100;

        String direction;
        if (absoluteChange > 0) {
            direction = "UP";
        } else if (absoluteChange < 0) {
            direction = "DOWN";
        } else {
            direction = "FLAT";
        }

        Map<String, Object> result = new HashMap<>();
        result.put("assetId", assetId);
        result.put("providerId", providerId);
        result.put("previousDate", previous.getDataDate());
        result.put("latestDate", latest.getDataDate());
        result.put("previousClose", previousClose);
        result.put("latestClose", latestClose);
        result.put("absoluteChange", absoluteChange);
        result.put("percentageChange", percentageChange);
        result.put("direction", direction);
        result.put("grounding", "Answer computed only from stored platform market data.");

        return result;
    }

    @PostMapping("/agent-demo")
    public Map<String, Object> agentDemo(
            @RequestParam(defaultValue = "IBM") String symbol,
            @RequestParam(defaultValue = "IBM") String assetId
    ) {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> ingest = ingestController.ingestFromAlphaVantage(symbol, assetId);
        List<Asset> assets = listAssets();
        List<DataProvider> providers = listDataSources();
        Map<String, Object> summary = summarizeTrends(assetId, "ALPHA_VANTAGE");
        Map<String, Object> forecast = forecastNextDay(assetId, "ALPHA_VANTAGE");
        Map<String, Object> explanation = explainChange(assetId, "ALPHA_VANTAGE");

        result.put("step1_ingest", ingest);
        result.put("step2_assets", assets);
        result.put("step3_dataSources", providers);
        result.put("step4_summary", summary);
        result.put("step5_forecast", forecast);
        result.put("step6_explanation", explanation);
        result.put("agentStyle", "find asset -> ingest/fetch data -> compute summary -> forecast -> explain change");

        return result;
    }

    @PostMapping("/llm-chat")
    public Map<String, Object> llmChat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        String assetId = request.getOrDefault("assetId", "IBM");
        String providerId = request.getOrDefault("providerId", "ALPHA_VANTAGE");

        Map<String, Object> summary = summarizeTrends(assetId, providerId);

        String prompt = """
            You are a financial data assistant.
            Answer only using the following stored platform data.
            Do not invent facts.

            User question:
            %s

            Stored analytics summary:
            %s
            """.formatted(message, summary);

        String answer = ollamaService.ask(prompt);

        Map<String, Object> result = new HashMap<>();
        result.put("userMessage", message);
        result.put("assetId", assetId);
        result.put("providerId", providerId);
        result.put("grounding", "Answer generated by local Ollama LLM using stored platform analytics data.");
        result.put("data", summary);
        result.put("answer", answer);

        return result;
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        String originalMessage = request.getOrDefault("message", "");
        String message = originalMessage.toLowerCase();

        String assetId = request.getOrDefault("assetId", "IBM");
        String providerId = request.getOrDefault("providerId", "ALPHA_VANTAGE");

        Map<String, Object> result = new HashMap<>();
        result.put("userMessage", originalMessage);
        result.put("grounding", "Answer generated only from stored platform data and internal assistant tools.");

        if ((message.contains("list") || message.contains("show")) && message.contains("asset")) {
            result.put("intent", "list_assets");
            result.put("data", listAssets());
            result.put("answer", "These are the current assets available in the data warehouse.");
            return result;
        }

        if (message.contains("provider") || message.contains("source")) {
            result.put("intent", "list_data_sources");
            result.put("data", listDataSources());
            result.put("answer", "These are the current financial data providers available in the platform.");
            return result;
        }


        if (message.contains("compare")) {
            String assetId1 = request.getOrDefault("assetId1", assetId);
            String assetId2 = request.getOrDefault("assetId2", "MSFT");

            result.put("intent", "compare_assets");
            result.put("data", compareAssets(assetId1, assetId2, providerId));
            result.put("answer", "The two assets were compared using percentage change analytics.");
            return result;
        }

        if (message.contains("forecast") || message.contains("predict")) {
            result.put("intent", "forecast_next_day");
            result.put("data", forecastNextDay(assetId, providerId));
            result.put("answer", "The next-day forecast was computed using the stored market data.");
            return result;
        }


        if (message.contains("summary") || message.contains("trend")) {
            result.put("intent", "summarize_trends");
            result.put("data", summarizeTrends(assetId, providerId));
            result.put("answer", "The trend summary was computed from stored time-series market data.");
            return result;
        }

        if (message.contains("explain") || message.contains("change")) {
            result.put("intent", "explain_change");
            result.put("data", explainChange(assetId, providerId));
            result.put("answer", "The latest change was explained using the last two stored close prices.");
            return result;
        }

        result.put("intent", "unknown");
        result.put("answer", "I can help with: list assets, list providers, summarize trends, forecast next day, or explain latest change.");
        return result;
    }

    private Map<String, Object> tool(String name, String description) {
        Map<String, Object> tool = new HashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        return tool;
    }
}