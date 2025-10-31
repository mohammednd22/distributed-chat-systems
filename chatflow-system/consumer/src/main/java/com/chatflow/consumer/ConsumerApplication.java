package com.chatflow.consumer;

import java.util.Scanner;

public class ConsumerApplication {
    private static ConsumerPool consumerPool;
    private static final int DEFAULT_CONSUMER_COUNT = 10;

    public static void main(String[] args) {
        System.out.println("=== ChatFlow Consumer Application ===");

        // Get consumer count from command line or use default
        int numConsumers = DEFAULT_CONSUMER_COUNT;
        if (args.length > 0) {
            try {
                numConsumers = Integer.parseInt(args[0]);
                if (numConsumers < 1 || numConsumers > 50) {
                    System.out.println("Consumer count must be between 1 and 50, using default: " + DEFAULT_CONSUMER_COUNT);
                    numConsumers = DEFAULT_CONSUMER_COUNT;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid consumer count, using default: " + DEFAULT_CONSUMER_COUNT);
            }
        }

        System.out.println("Starting with " + numConsumers + " consumer threads");

        try {
            // Initialize consumer pool
            consumerPool = new ConsumerPool(numConsumers);

            // Add shutdown hook for graceful exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutdown signal received...");
                if (consumerPool != null) {
                    consumerPool.shutdown();
                }
            }));

            // Start consuming messages
            consumerPool.start();

            System.out.println("\nConsumer application started successfully!");
            System.out.println("Commands: 'status', 'metrics', 'quit'");

            // Interactive console for monitoring
            runInteractiveConsole();

        } catch (Exception e) {
            System.out.println("Failed to start consumer application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runInteractiveConsole() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nconsumer> ");
            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "status":
                    printStatus();
                    break;

                case "metrics":
                    printDetailedMetrics();
                    break;

                case "rooms":
                    consumerPool.getRoomManager().printStats();
                    break;

                case "help":
                    printHelp();
                    break;

                case "quit":
                case "exit":
                    System.out.println("Shutting down...");
                    consumerPool.shutdown();
                    System.exit(0);
                    break;

                default:
                    System.out.println("Unknown command. Type 'help' for available commands.");
            }
        }
    }

    private static void printStatus() {
        System.out.println("\n=== Consumer Status ===");
        ConsumerMetrics metrics = consumerPool.getMetrics();

        System.out.println("Messages processed: " + metrics.getMessagesProcessed());
        System.out.println("Messages delivered: " + metrics.getMessagesDelivered());
        System.out.println("Messages failed: " + metrics.getMessagesFailed());
        System.out.println("Duplicates filtered: " + metrics.getDuplicatesFiltered());

        System.out.println("Active rooms: " + consumerPool.getRoomManager().getActiveRooms());
        System.out.println("Total sessions: " + consumerPool.getRoomManager().getTotalSessions());
    }

    private static void printDetailedMetrics() {
        System.out.println("\n=== Detailed Metrics ===");
        consumerPool.getMetrics().printMetrics();
        consumerPool.getRoomManager().printStats();
    }

    private static void printHelp() {
        System.out.println("\n=== Available Commands ===");
        System.out.println("status  - Show basic consumer statistics");
        System.out.println("metrics - Show detailed metrics and room stats");
        System.out.println("rooms   - Show room manager statistics");
        System.out.println("help    - Show this help message");
        System.out.println("quit    - Shutdown consumer application");
    }
}