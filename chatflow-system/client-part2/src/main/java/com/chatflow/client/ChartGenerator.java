package com.chatflow.client;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ChartGenerator {

    public static void createThroughputChart(Map<Integer, Integer> buckets, String filename) {
        // Create dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Map.Entry<Integer, Integer> entry : buckets.entrySet()) {
            int bucket = entry.getKey();
            int messages = entry.getValue();
            double messagesPerSec = messages / 10.0;

            String timeLabel = bucket * 10 + "-" + (bucket + 1) * 10 + "s";
            dataset.addValue(messagesPerSec, "Throughput", timeLabel);
        }

        // Create chart
        JFreeChart chart = ChartFactory.createLineChart(
                "Throughput Over Time",
                "Time (seconds)",
                "Messages/Second",
                dataset,
                PlotOrientation.VERTICAL,
                false, // legend
                true,  // tooltips
                false  // urls
        );

        // Save as PNG
        try {
            ChartUtils.saveChartAsPNG(new File(filename), chart, 800, 600);
            System.out.println("Chart saved to: " + filename);
        } catch (IOException e) {
            System.out.println("Error saving chart: " + e.getMessage());
        }
    }
}