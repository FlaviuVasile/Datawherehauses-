package com.example.financialassets.service;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.apache.spark.sql.functions.*;

@Service
public class SparkAnalyticsJob {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/financial_assets}")
    private String mongoUri;

    private static final String DATABASE = "financial_assets";
    private static final String SOURCE_COLLECTION = "market_data";
    private static final String OUTPUT_COLLECTION = "spark_analytics_summary";

    public Map<String, Object> runAggregation() {
        SparkSession spark = SparkSession.builder()
                .appName("FinancialAssets-Spark-Aggregation")
                .master("local[*]")
                .config("spark.ui.enabled", "false")
                .config("spark.mongodb.read.connection.uri", mongoUri)
                .config("spark.mongodb.write.connection.uri", mongoUri)
                .getOrCreate();

        try {
            Dataset<Row> marketData = spark.read()
                    .format("mongodb")
                    .option("database", DATABASE)
                    .option("collection", SOURCE_COLLECTION)
                    .load()
                    .filter(col("current").equalTo(true))
                    .filter(col("deleted").equalTo(false))
                    .filter(col("closePrice").isNotNull());

            Dataset<Row> summary = marketData
                    .groupBy(col("assetId"), col("providerId"))
                    .agg(
                            count(lit(1)).alias("recordsCount"),
                            min(col("dataDate")).alias("firstDate"),
                            max(col("dataDate")).alias("lastDate"),
                            min(col("closePrice")).alias("minClose"),
                            max(col("closePrice")).alias("maxClose"),
                            avg(col("closePrice")).alias("avgClose"),
                            stddev(col("closePrice")).alias("volatility"),
                            avg(col("volume")).alias("avgVolume")
                    )
                    .withColumn("analyticsEngine", lit("Apache Spark SQL/DataFrame"))
                    .withColumn("computedAt", current_timestamp());

            long rows = summary.count();

            summary.write()
                    .format("mongodb")
                    .mode("overwrite")
                    .option("database", DATABASE)
                    .option("collection", OUTPUT_COLLECTION)
                    .save();

            Map<String, Object> result = new HashMap<>();
            result.put("status", "OK");
            result.put("mandatoryRequirement", "M6 Apache Spark Analytics Aggregation");
            result.put("engine", "Apache Spark SQL/DataFrame");
            result.put("sourceCollection", SOURCE_COLLECTION);
            result.put("outputCollection", OUTPUT_COLLECTION);
            result.put("rowsWritten", rows);
            result.put("computedAt", LocalDateTime.now().toString());

            return result;
        } finally {
            spark.close();
        }
    }
}