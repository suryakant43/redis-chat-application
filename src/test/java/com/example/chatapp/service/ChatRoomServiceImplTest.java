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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    private ObjectMapper objectMapper;

    private ChatRoomServiceImpl chatRoomService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        chatRoomService = new ChatRoomServiceImpl(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("Test Case 1a: Successfully create chat room")
    void testCreateRoom_Success() {
        CreateRoomRequest request = new CreateRoomRequest("general");
        String metaKey = RedisKeyUtils.getRoomMetaKey("general");

        when(redisTemplate.hasKey(metaKey)).thenReturn(false);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        CreateRoomResponse response = chatRoomService.createRoom(request);

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("general", response.getRoomId());
        assertEquals("Chat room 'general' created successfully.", response.getMessage());

        verify(redisTemplate).hasKey(metaKey);
        verify(hashOperations).putAll(eq(metaKey), any(Map.class));
    }

    @Test
    @DisplayName("Test Case 4a: Create chat room with duplicate name throws ConflictException")
    void testCreateRoom_DuplicateConflict() {
        CreateRoomRequest request = new CreateRoomRequest("general");
        String metaKey = RedisKeyUtils.getRoomMetaKey("general");

        when(redisTemplate.hasKey(metaKey)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> chatRoomService.createRoom(request));
        assertEquals("Chat room 'general' already exists.", ex.getMessage());

        verify(hashOperations, never()).putAll(anyString(), anyMap());
    }

    @Test
    @DisplayName("Test Case 1b: Successfully join an existing chat room")
    void testJoinRoom_Success() {
        JoinRoomRequest request = new JoinRoomRequest("guest_user");
        String metaKey = RedisKeyUtils.getRoomMetaKey("general");
        String participantsKey = RedisKeyUtils.getRoomParticipantsKey("general");

        when(redisTemplate.hasKey(metaKey)).thenReturn(true);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        JoinRoomResponse response = chatRoomService.joinRoom("general", request);

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("User 'guest_user' joined chat room 'general'.", response.getMessage());

        verify(setOperations).add(participantsKey, "guest_user");
    }

    @Test
    @DisplayName("Test Case 4b: Join non-existent chat room throws NotFoundException")
    void testJoinRoom_RoomNotFound() {
        JoinRoomRequest request = new JoinRoomRequest("guest_user");
        String metaKey = RedisKeyUtils.getRoomMetaKey("non_existent");

        when(redisTemplate.hasKey(metaKey)).thenReturn(false);

        NotFoundException ex = assertThrows(NotFoundException.class, () -> chatRoomService.joinRoom("non_existent", request));
        assertEquals("Chat room 'non_existent' does not exist.", ex.getMessage());

        verify(setOperations, never()).add(anyString(), anyString());
    }

    @Test
    @DisplayName("Test Case 2a & 3: Send message saves to Redis List and publishes via Pub/Sub")
    void testSendMessage_Success() {
        SendMessageRequest request = new SendMessageRequest("guest_user", "Hello, everyone!");
        String metaKey = RedisKeyUtils.getRoomMetaKey("general");
        String messagesKey = RedisKeyUtils.getRoomMessagesKey("general");
        String participantsKey = RedisKeyUtils.getRoomParticipantsKey("general");
        String channel = RedisKeyUtils.getRoomChannel("general");

        when(redisTemplate.hasKey(metaKey)).thenReturn(true);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        SendMessageResponse response = chatRoomService.sendMessage("general", request);

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("Message sent successfully.", response.getMessage());

        // Verify participant added to set
        verify(setOperations).add(participantsKey, "guest_user");

        // Verify message pushed to Redis List
        verify(listOperations).rightPush(eq(messagesKey), contains("Hello, everyone!"));

        // Verify broadcast over Pub/Sub channel
        verify(redisTemplate).convertAndSend(eq(channel), contains("Hello, everyone!"));
    }

    @Test
    @DisplayName("Test Case 4c: Send message to non-existent chat room throws NotFoundException")
    void testSendMessage_RoomNotFound() {
        SendMessageRequest request = new SendMessageRequest("guest_user", "Hello, everyone!");
        String metaKey = RedisKeyUtils.getRoomMetaKey("non_existent");

        when(redisTemplate.hasKey(metaKey)).thenReturn(false);

        NotFoundException ex = assertThrows(NotFoundException.class, () -> chatRoomService.sendMessage("non_existent", request));
        assertEquals("Chat room 'non_existent' does not exist.", ex.getMessage());

        verify(listOperations, never()).rightPush(anyString(), anyString());
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("Test Case 2b: Retrieve chat history in correct order")
    void testGetChatHistory_Success() throws JsonProcessingException {
        String metaKey = RedisKeyUtils.getRoomMetaKey("general");
        String messagesKey = RedisKeyUtils.getRoomMessagesKey("general");

        when(redisTemplate.hasKey(metaKey)).thenReturn(true);
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        ChatMessageDto msg1 = new ChatMessageDto("guest_user", "Hello, everyone!", "2024-01-01T10:00:00Z");
        ChatMessageDto msg2 = new ChatMessageDto("another_user", "Hi, guest_user!", "2024-01-01T10:01:00Z");

        String json1 = objectMapper.writeValueAsString(msg1);
        String json2 = objectMapper.writeValueAsString(msg2);

        when(listOperations.range(messagesKey, -10, -1)).thenReturn(List.of(json1, json2));

        ChatHistoryResponse response = chatRoomService.getChatHistory("general", 10);

        assertNotNull(response);
        assertEquals(2, response.getMessages().size());
        assertEquals("guest_user", response.getMessages().get(0).getParticipant());
        assertEquals("Hello, everyone!", response.getMessages().get(0).getMessage());
        assertEquals("another_user", response.getMessages().get(1).getParticipant());
        assertEquals("Hi, guest_user!", response.getMessages().get(1).getMessage());

        verify(listOperations).range(messagesKey, -10, -1);
    }

    @Test
    @DisplayName("Test Case 4d: Retrieve chat history from non-existent room throws NotFoundException")
    void testGetChatHistory_RoomNotFound() {
        String metaKey = RedisKeyUtils.getRoomMetaKey("non_existent");
        when(redisTemplate.hasKey(metaKey)).thenReturn(false);

        NotFoundException ex = assertThrows(NotFoundException.class, () -> chatRoomService.getChatHistory("non_existent", 10));
        assertEquals("Chat room 'non_existent' does not exist.", ex.getMessage());
    }

    @Test
    @DisplayName("Test Case 5: Delete chat room cleans up all keys")
    void testDeleteRoom_Success() {
        String metaKey = RedisKeyUtils.getRoomMetaKey("general");
        String participantsKey = RedisKeyUtils.getRoomParticipantsKey("general");
        String messagesKey = RedisKeyUtils.getRoomMessagesKey("general");

        when(redisTemplate.hasKey(metaKey)).thenReturn(true);

        DeleteRoomResponse response = chatRoomService.deleteRoom("general");

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("Chat room 'general' deleted successfully.", response.getMessage());

        verify(redisTemplate).delete(List.of(metaKey, participantsKey, messagesKey));
    }
}
