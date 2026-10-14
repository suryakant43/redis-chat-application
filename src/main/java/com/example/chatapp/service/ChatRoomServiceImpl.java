/** Service implementation for ChatRoom business logic; interacts with Redis data structures. */
package com.example.chatapp.service;

import com.example.chatapp.dto.ChatHistoryResponse;
import com.example.chatapp.dto.ChatMessageDto;
import com.example.chatapp.dto.CreateRoomRequest;
import com.example.chatapp.dto.CreateRoomResponse;
import com.example.chatapp.dto.DeleteRoomResponse;
import com.example.chatapp.dto.JoinRoomRequest;
import com.example.chatapp.dto.JoinRoomResponse;
import com.example.chatapp.dto.SendMessageRequest;
import com.example.chatapp.dto.SendMessageResponse;
import com.example.chatapp.exceptions.ConflictException;
import com.example.chatapp.exceptions.NotFoundException;
import com.example.chatapp.utils.RedisKeyUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Create a new chat room and store its metadata in a Redis Hash.
     */
    @Override
    public CreateRoomResponse createRoom(CreateRoomRequest request) {
        String roomName = request.getRoomName().trim();
        String metaKey = RedisKeyUtils.getRoomMetaKey(roomName);

        // Check if the chat room already exists
        if (Boolean.TRUE.equals(redisTemplate.hasKey(metaKey))) {
            log.warn("Attempt to create duplicate chat room: {}", roomName);
            throw new ConflictException("Chat room '" + roomName + "' already exists.");
        }

        // Store chat room metadata in Redis Hash
        Map<String, String> metadata = Map.of(
                "roomId", roomName,
                "roomName", roomName,
                "createdAt", Instant.now().toString()
        );
        redisTemplate.opsForHash().putAll(metaKey, metadata);

        log.info("Chat room '{}' created successfully", roomName);
        return CreateRoomResponse.builder()
                .message("Chat room '" + roomName + "' created successfully.")
                .roomId(roomName)
                .status("success")
                .build();
    }

    /**
     * Join an existing chat room by adding participant to Redis Set.
     */
    @Override
    public JoinRoomResponse joinRoom(String roomId, JoinRoomRequest request) {
        String cleanRoomId = roomId.trim();
        String participant = request.getParticipant().trim();

        // Verify that the room exists before joining
        ensureRoomExists(cleanRoomId);

        // Add participant to the room's Redis Set
        String participantsKey = RedisKeyUtils.getRoomParticipantsKey(cleanRoomId);
        redisTemplate.opsForSet().add(participantsKey, participant);

        log.info("Participant '{}' joined chat room '{}'", participant, cleanRoomId);
        return JoinRoomResponse.builder()
                .message("User '" + participant + "' joined chat room '" + cleanRoomId + "'.")
                .status("success")
                .build();
    }

    /**
     * Send a message to a chat room: store in Redis List and broadcast via Redis Pub/Sub.
     */
    @Override
    public SendMessageResponse sendMessage(String roomId, SendMessageRequest request) {
        String cleanRoomId = roomId.trim();
        String participant = request.getParticipant().trim();
        String messageText = request.getMessage().trim();

        // Verify that the room exists before sending message
        ensureRoomExists(cleanRoomId);

        // Ensure participant is added to participants set
        String participantsKey = RedisKeyUtils.getRoomParticipantsKey(cleanRoomId);
        redisTemplate.opsForSet().add(participantsKey, participant);

        // Create message object with ISO-8601 UTC timestamp
        ChatMessageDto chatMessage = ChatMessageDto.builder()
                .participant(participant)
                .message(messageText)
                .timestamp(Instant.now().toString())
                .build();

        // Serialize message to JSON
        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(chatMessage);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat message: {}", e.getMessage());
            throw new RuntimeException("Error processing chat message", e);
        }

        // Store message at the end of Redis List (maintains chronological order)
        String messagesKey = RedisKeyUtils.getRoomMessagesKey(cleanRoomId);
        redisTemplate.opsForList().rightPush(messagesKey, messageJson);

        // Broadcast message to all room participants using Redis Pub/Sub
        String channel = RedisKeyUtils.getRoomChannel(cleanRoomId);
        redisTemplate.convertAndSend(channel, messageJson);

        log.info("Message from '{}' broadcast and saved in room '{}'", participant, cleanRoomId);
        return SendMessageResponse.builder()
                .message("Message sent successfully.")
                .status("success")
                .build();
    }

    /**
     * Retrieve the last N messages from a chat room in chronological order.
     */
    @Override
    public ChatHistoryResponse getChatHistory(String roomId, int limit) {
        String cleanRoomId = roomId.trim();

        // Verify that the room exists before fetching history
        ensureRoomExists(cleanRoomId);

        // Default limit to 10 if invalid
        int fetchLimit = limit > 0 ? limit : 10;

        // Retrieve last N messages from Redis List using negative indexing
        String messagesKey = RedisKeyUtils.getRoomMessagesKey(cleanRoomId);
        List<String> rawMessages = redisTemplate.opsForList().range(messagesKey, -fetchLimit, -1);

        List<ChatMessageDto> messages = new ArrayList<>();
        if (rawMessages != null) {
            for (String raw : rawMessages) {
                try {
                    ChatMessageDto dto = objectMapper.readValue(raw, ChatMessageDto.class);
                    messages.add(dto);
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse chat message JSON: {}", e.getMessage());
                }
            }
        }

        return ChatHistoryResponse.builder()
                .messages(messages)
                .build();
    }

    /**
     * Delete a chat room and clean up its metadata, participants, and message history keys.
     */
    @Override
    public DeleteRoomResponse deleteRoom(String roomId) {
        String cleanRoomId = roomId.trim();

        // Verify room exists before deletion
        ensureRoomExists(cleanRoomId);

        // Remove all Redis keys associated with this room
        redisTemplate.delete(List.of(
                RedisKeyUtils.getRoomMetaKey(cleanRoomId),
                RedisKeyUtils.getRoomParticipantsKey(cleanRoomId),
                RedisKeyUtils.getRoomMessagesKey(cleanRoomId)
        ));

        log.info("Chat room '{}' and its Redis keys deleted", cleanRoomId);
        return DeleteRoomResponse.builder()
                .message("Chat room '" + cleanRoomId + "' deleted successfully.")
                .status("success")
                .build();
    }

    /**
     * Check if a room exists in Redis by its metadata key.
     */
    @Override
    public boolean roomExists(String roomId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeyUtils.getRoomMetaKey(roomId.trim())));
    }

    /**
     * Helper to validate room existence or throw NotFoundException.
     */
    private void ensureRoomExists(String roomId) {
        if (!roomExists(roomId)) {
            log.warn("Room not found: {}", roomId);
            throw new NotFoundException("Chat room '" + roomId + "' does not exist.");
        }
    }
}
