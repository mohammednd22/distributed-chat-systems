package com.chatflow.client;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

public class MessageGenerator {
    private static final Random random = new Random();

    private static final String[] MESSAGE_POOL = {
            "Hello everyone!", "How are you doing?", "Great to be here!",
            "What's up?", "Anyone online?", "Good morning!",
            "Have a great day!", "See you later!", "Thanks for the help!",
            "That's awesome!", "I agree with that", "Interesting point!",
            "Can you help me?", "Sure, no problem!", "Let me check that",
            "Working on a project", "Almost done here", "Need a break",
            "Coffee time!", "Lunch break!", "Back to work",
            "Meeting in 5 mins", "Running late", "On my way",
            "Check this out", "Did you see that?", "Amazing stuff",
            "LOL that's funny", "Haha good one", "Made my day",
            "Weekend plans?", "Any recommendations?", "Sounds good to me",
            "Count me in!", "I'm interested", "Tell me more",
            "Got it, thanks!", "Perfect timing", "Exactly what I needed",
            "Appreciate it!", "You're welcome", "No worries",
            "Let's do this!", "Ready when you are", "All set here",
            "Question for you", "Quick update", "FYI everyone",
            "Heads up team", "Note to self", "Reminder set"
    };

    public static String generateMessage(int roomId) {
        int userId = random.nextInt(100000) + 1;
        String username = "user" + userId;
        String message = MESSAGE_POOL[random.nextInt(MESSAGE_POOL.length)];
        String timestamp = Instant.now().toString();
        String messageType = getRandomMessageType();
        String trackingId = UUID.randomUUID().toString();

        return String.format(
                "{\"userId\":\"%d\",\"username\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\",\"messageType\":\"%s\",\"trackingId\":\"%s\"}",
                userId, username, message, timestamp, messageType, trackingId
        );
    }

    private static String getRandomMessageType() {
        int rand = random.nextInt(100);
        if (rand < 90) {
            return "TEXT";
        } else if (rand < 95) {
            return "JOIN";
        } else {
            return "LEAVE";
        }
    }
}
