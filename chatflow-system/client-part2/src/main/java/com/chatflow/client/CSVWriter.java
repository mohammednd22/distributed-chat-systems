package com.chatflow.client;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSVWriter {

    public static void writeMetrics(List<MessageMetric> metrics, String filename) {
        try (FileWriter writer = new FileWriter(filename);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("Timestamp", "MessageType", "LatencyMs", "StatusCode", "RoomId"))) {

            for (MessageMetric metric : metrics) {
                csvPrinter.printRecord(
                        metric.getTimestamp(),
                        metric.getMessageType(),
                        metric.getLatencyMs(),
                        metric.getStatusCode(),
                        metric.getRoomId()
                );
            }

            System.out.println("CSV file written: " + filename);
            System.out.println("Total records: " + metrics.size());

        } catch (IOException e) {
            System.out.println("Error writing CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
}