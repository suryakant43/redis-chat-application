/** Service interface defining ChatRoom operations; implemented by ChatRoomServiceImpl. */
package com.example.chatapp.service;

import com.example.chatapp.dto.ChatHistoryResponse;
import com.example.chatapp.dto.CreateRoomRequest;
import com.example.chatapp.dto.CreateRoomResponse;
import com.example.chatapp.dto.DeleteRoomResponse;
import com.example.chatapp.dto.JoinRoomRequest;
import com.example.chatapp.dto.JoinRoomResponse;
import com.example.chatapp.dto.SendMessageRequest;
import com.example.chatapp.dto.SendMessageResponse;

public interface ChatRoomService {

    /**
     * Create a new chat room with unique name.
     */
    CreateRoomResponse createRoom(CreateRoomRequest request);

    /**
     * Join an existing chat room.
     */
    JoinRoomResponse joinRoom(String roomId, JoinRoomRequest request);

    /**
     * Send a message to a chat room and broadcast via Pub/Sub.
     */
    SendMessageResponse sendMessage(String roomId, SendMessageRequest request);

    /**
     * Retrieve the last N messages from a chat room in chronological order.
     */
    ChatHistoryResponse getChatHistory(String roomId, int limit);

    /**
     * Delete a chat room and clean up related Redis keys.
     */
    DeleteRoomResponse deleteRoom(String roomId);

    /**
     * Check if a chat room exists.
     */
    boolean roomExists(String roomId);
}
