package com.example.chatapp;

import com.example.chatapp.dto.ChatHistoryResponse;
import com.example.chatapp.dto.CreateRoomRequest;
import com.example.chatapp.dto.CreateRoomResponse;
import com.example.chatapp.dto.DeleteRoomResponse;
import com.example.chatapp.dto.JoinRoomRequest;
import com.example.chatapp.dto.JoinRoomResponse;
import com.example.chatapp.dto.SendMessageRequest;
import com.example.chatapp.dto.SendMessageResponse;
import com.example.chatapp.exceptions.ConflictException;
import com.example.chatapp.exceptions.NotFoundException;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.RedisMessageSubscriber;
import com.example.chatapp.utils.RedisKeyUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class ChatAppIntegrationTest {

    private static final String TEST_ROOM = "integration_test_room";

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisMessageSubscriber messageSubscriber;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        // Clean up test room keys before and after each test
        redisTemplate.delete(RedisKeyUtils.getRoomMetaKey(TEST_ROOM));
        redisTemplate.delete(RedisKeyUtils.getRoomParticipantsKey(TEST_ROOM));
        redisTemplate.delete(RedisKeyUtils.getRoomMessagesKey(TEST_ROOM));
        messageSubscriber.getReceivedMessages().clear();
    }

    @Test
    @DisplayName("End-to-End: Test Case 1 - Create and Join Chat Room")
    void testCase1_CreateAndJoinRoom() {
        // 1. Create chat room
        CreateRoomResponse createResponse = chatRoomService.createRoom(new CreateRoomRequest(TEST_ROOM));
        assertThat(createResponse.getStatus()).isEqualTo("success");
        assertThat(createResponse.getRoomId()).isEqualTo(TEST_ROOM);

        // Verify room metadata in Redis Hash
        String metaKey = RedisKeyUtils.getRoomMetaKey(TEST_ROOM);
        assertThat(redisTemplate.hasKey(metaKey)).isTrue();
        assertThat(redisTemplate.opsForHash().get(metaKey, "roomName")).isEqualTo(TEST_ROOM);

        // 2. Join room as guest_user
        JoinRoomResponse joinResponse = chatRoomService.joinRoom(TEST_ROOM, new JoinRoomRequest("guest_user"));
        assertThat(joinResponse.getStatus()).isEqualTo("success");
        assertThat(joinResponse.getMessage()).contains("guest_user");

        // Verify participant added to Redis Set
        String participantsKey = RedisKeyUtils.getRoomParticipantsKey(TEST_ROOM);
        assertThat(redisTemplate.opsForSet().isMember(participantsKey, "guest_user")).isTrue();
    }

    @Test
    @DisplayName("End-to-End: Test Case 2 & 3 - Send, Retrieve Messages and Real-Time Pub/Sub Broadcast")
    void testCase2And3_SendRetrieveAndPubSub() {
        chatRoomService.createRoom(new CreateRoomRequest(TEST_ROOM));

        // Send two messages
        SendMessageResponse sendResp1 = chatRoomService.sendMessage(
                TEST_ROOM, new SendMessageRequest("guest_user", "Hello, everyone!"));
        assertThat(sendResp1.getStatus()).isEqualTo("success");

        SendMessageResponse sendResp2 = chatRoomService.sendMessage(
                TEST_ROOM, new SendMessageRequest("another_user", "Hi, guest_user!"));
        assertThat(sendResp2.getStatus()).isEqualTo("success");

        // Retrieve last 10 messages
        ChatHistoryResponse history = chatRoomService.getChatHistory(TEST_ROOM, 10);
        assertThat(history.getMessages()).hasSize(2);

        // Verify chronological order
        assertThat(history.getMessages().get(0).getParticipant()).isEqualTo("guest_user");
        assertThat(history.getMessages().get(0).getMessage()).isEqualTo("Hello, everyone!");
        assertThat(history.getMessages().get(1).getParticipant()).isEqualTo("another_user");
        assertThat(history.getMessages().get(1).getMessage()).isEqualTo("Hi, guest_user!");

        // Retrieve with limit 1
        ChatHistoryResponse lastOne = chatRoomService.getChatHistory(TEST_ROOM, 1);
        assertThat(lastOne.getMessages()).hasSize(1);
        assertThat(lastOne.getMessages().get(0).getMessage()).isEqualTo("Hi, guest_user!");

        // Verify Pub/Sub broadcast received by subscriber
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(messageSubscriber.getReceivedMessages()).hasSizeGreaterThanOrEqualTo(2);
        });
    }

    @Test
    @DisplayName("End-to-End: Test Case 4 - Error Handling for Duplicate and Non-Existent Rooms")
    void testCase4_ErrorHandling() {
        chatRoomService.createRoom(new CreateRoomRequest(TEST_ROOM));

        // Duplicate room creation
        assertThatThrownBy(() -> chatRoomService.createRoom(new CreateRoomRequest(TEST_ROOM)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Chat room '" + TEST_ROOM + "' already exists.");

        // Sending message to non-existent room
        assertThatThrownBy(() -> chatRoomService.sendMessage(
                "invalid_room", new SendMessageRequest("guest_user", "hello")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Chat room 'invalid_room' does not exist.");

        // Joining non-existent room
        assertThatThrownBy(() -> chatRoomService.joinRoom(
                "invalid_room", new JoinRoomRequest("guest_user")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Chat room 'invalid_room' does not exist.");
    }

    @Test
    @DisplayName("End-to-End: Test Case 5 - Chat Room Deletion and Redis Cleanup")
    void testCase5_DeleteRoom() {
        chatRoomService.createRoom(new CreateRoomRequest(TEST_ROOM));
        chatRoomService.sendMessage(TEST_ROOM, new SendMessageRequest("guest_user", "Temp message"));

        // Delete room
        DeleteRoomResponse deleteResp = chatRoomService.deleteRoom(TEST_ROOM);
        assertThat(deleteResp.getStatus()).isEqualTo("success");

        // Verify Redis keys are removed
        assertThat(redisTemplate.hasKey(RedisKeyUtils.getRoomMetaKey(TEST_ROOM))).isFalse();
        assertThat(redisTemplate.hasKey(RedisKeyUtils.getRoomParticipantsKey(TEST_ROOM))).isFalse();
        assertThat(redisTemplate.hasKey(RedisKeyUtils.getRoomMessagesKey(TEST_ROOM))).isFalse();

        // Deleting again should throw NotFoundException
        assertThatThrownBy(() -> chatRoomService.deleteRoom(TEST_ROOM))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Chat room '" + TEST_ROOM + "' does not exist.");
    }
}
