package com.example.financialassets.service;

import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SparkPredictionJob {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/financial_assets}")
    private String mongoUri;

    private static final String DATABASE = "financial_assets";

    public Map<String, Object> runPrediction() {

        SparkSession spark = SparkSession.builder()
                .appName("FinancialAssets-Spark-ML")
                .master("local[*]")
                .config("spark.ui.enabled", "false")
                .config("spark.mongodb.read.connection.uri", mongoUri)
                .config("spark.mongodb.write.connection.uri", mongoUri)
                .getOrCreate();

        try {

            Dataset<Row> df = spark.read()
                    .format("mongodb")
                    .option("database", DATABASE)
                    .option("collection", "market_data")
                    .load();

            Dataset<Row> trainingData = df
                    .select("volume", "closePrice")
                    .na()
                    .drop();

            VectorAssembler assembler = new VectorAssembler()
                    .setInputCols(new String[]{"volume"})
                    .setOutputCol("features");

            Dataset<Row> dataset =
                    assembler.transform(trainingData);

            LinearRegression lr = new LinearRegression()
                    .setFeaturesCol("features")
                    .setLabelCol("closePrice");

            LinearRegressionModel model =
                    lr.fit(dataset);

            Dataset<Row> predictions =
                    model.transform(dataset);

            predictions.select(
                            "volume",
                            "closePrice",
                            "prediction")
                    .write()
                    .format("mongodb")
                    .mode("overwrite")
                    .option("database", DATABASE)
                    .option("collection", "spark_predictions")
                    .save();

            Map<String, Object> result = new HashMap<>();

            result.put(
                    "mandatoryRequirement",
                    "M7 Apache Spark ML Prediction Workflow");

            result.put(
                    "engine",
                    "Apache Spark MLlib Linear Regression");

            result.put(
                    "predictionCollection",
                    "spark_predictions");

            result.put(
                    "trainedAt",
                    LocalDateTime.now().toString());

            return result;

        } finally {
            spark.close();
        }
    }
}