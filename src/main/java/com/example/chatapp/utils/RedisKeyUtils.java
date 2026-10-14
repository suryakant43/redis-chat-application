/** Utility class defining Redis key prefixes and helpers for chat rooms. */
package com.example.chatapp.utils;

public final class RedisKeyUtils {

    private RedisKeyUtils() {
        // Prevent instantiation of utility class
    }

    /**
     * Key for chat room metadata stored as a Redis Hash.
     * Pattern: chatroom:{roomId}:meta
     */
    public static String getRoomMetaKey(String roomId) {
        return "chatroom:" + roomId + ":meta";
    }

    /**
     * Key for chat room participants stored as a Redis Set.
     * Pattern: chatroom:{roomId}:participants
     */
    public static String getRoomParticipantsKey(String roomId) {
        return "chatroom:" + roomId + ":participants";
    }

    /**
     * Key for chat room messages stored as a Redis List in chronological order.
     * Pattern: chatroom:{roomId}:messages
     */
    public static String getRoomMessagesKey(String roomId) {
        return "chatroom:" + roomId + ":messages";
    }

    /**
     * Pub/Sub channel for real-time messaging in a chat room.
     * Pattern: chatroom:{roomId}
     */
    public static String getRoomChannel(String roomId) {
        return "chatroom:" + roomId;
    }
}
