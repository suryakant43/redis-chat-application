/** REST controller exposing ChatRoom API endpoints; manages room creation, messaging, and chat history. */
package com.example.chatapp.controller;

import com.example.chatapp.dto.ChatHistoryResponse;
import com.example.chatapp.dto.CreateRoomRequest;
import com.example.chatapp.dto.CreateRoomResponse;
import com.example.chatapp.dto.DeleteRoomResponse;
import com.example.chatapp.dto.JoinRoomRequest;
import com.example.chatapp.dto.JoinRoomResponse;
import com.example.chatapp.dto.SendMessageRequest;
import com.example.chatapp.dto.SendMessageResponse;
import com.example.chatapp.exceptions.ApiExceptionResponse;
import com.example.chatapp.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatapp/chatrooms")
@RequiredArgsConstructor
@Tag(name = "Chat Rooms", description = "APIs for managing chat rooms, participants, and real-time messaging using Redis")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /**
     * POST /api/chatapp/chatrooms
     * Create a new chat room with a unique name.
     */
    @PostMapping
    @Operation(
            summary = "Create a chat room",
            description = "Creates a new chat room with a unique name and initializes room metadata in Redis."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Chat room created successfully",
                    content = @Content(schema = @Schema(implementation = CreateRoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request payload",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Chat room with this name already exists",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))
            )
    })
    public ResponseEntity<CreateRoomResponse> createChatRoom(@Valid @RequestBody CreateRoomRequest request) {
        CreateRoomResponse response = chatRoomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/chatapp/chatrooms/{roomId}/join
     * Join an existing chat room.
     */
    @PostMapping("/{roomId}/join")
    @Operation(
            summary = "Join a chat room",
            description = "Adds a participant to an existing chat room's Redis Set."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Participant joined room successfully",
                    content = @Content(schema = @Schema(implementation = JoinRoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request payload",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Chat room not found",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))
            )
    })
    public ResponseEntity<JoinRoomResponse> joinChatRoom(
            @Parameter(description = "Room identifier", example = "general")
            @PathVariable("roomId") String roomId,
            @Valid @RequestBody JoinRoomRequest request) {
        JoinRoomResponse response = chatRoomService.joinRoom(roomId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/chatapp/chatrooms/{roomId}/messages
     * Send a message to a chat room.
     */
    @PostMapping("/{roomId}/messages")
    @Operation(
            summary = "Send a message",
            description = "Saves a message to the chat room's Redis List and broadcasts it to participants via Redis Pub/Sub."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Message sent successfully",
                    content = @Content(schema = @Schema(implementation = SendMessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request payload",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Chat room not found",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))
            )
    })
    public ResponseEntity<SendMessageResponse> sendMessage(
            @Parameter(description = "Room identifier", example = "general")
            @PathVariable("roomId") String roomId,
            @Valid @RequestBody SendMessageRequest request) {
        SendMessageResponse response = chatRoomService.sendMessage(roomId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/chatapp/chatrooms/{roomId}/messages?limit=10
     * Retrieve chat history from a chat room.
     */
    @GetMapping("/{roomId}/messages")
    @Operation(
            summary = "Retrieve chat history",
            description = "Retrieves the last N messages from a chat room in chronological order."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Chat history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ChatHistoryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Chat room not found",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))
            )
    })
    public ResponseEntity<ChatHistoryResponse> getChatHistory(
            @Parameter(description = "Room identifier", example = "general")
            @PathVariable("roomId") String roomId,
            @Parameter(description = "Number of recent messages to retrieve", example = "10")
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        ChatHistoryResponse response = chatRoomService.getChatHistory(roomId, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/chatapp/chatrooms/{roomId}
     * Delete a chat room and clean up its Redis keys.
     */
    @DeleteMapping("/{roomId}")
    @Operation(
            summary = "Delete a chat room",
            description = "Deletes a chat room and removes its metadata, participants, and message history from Redis."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Chat room deleted successfully",
                    content = @Content(schema = @Schema(implementation = DeleteRoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Chat room not found",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))
            )
    })
    public ResponseEntity<DeleteRoomResponse> deleteChatRoom(
            @Parameter(description = "Room identifier", example = "general")
            @PathVariable("roomId") String roomId) {
        DeleteRoomResponse response = chatRoomService.deleteRoom(roomId);
        return ResponseEntity.ok(response);
    }
}
