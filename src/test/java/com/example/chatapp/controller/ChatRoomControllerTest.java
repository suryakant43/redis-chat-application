package com.example.chatapp.controller;

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
import com.example.chatapp.service.ChatRoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomControllerTest {

    @Mock
    private ChatRoomService chatRoomService;

    @InjectMocks
    private ChatRoomController controller;

    @Test
    @DisplayName("createChatRoom - returns 201 Created and response body")
    void createChatRoom_success_returnsCreated() {
        CreateRoomRequest request = new CreateRoomRequest("general");
        CreateRoomResponse expected = CreateRoomResponse.builder()
                .message("Chat room 'general' created successfully.")
                .roomId("general")
                .status("success")
                .build();

        when(chatRoomService.createRoom(request)).thenReturn(expected);

        ResponseEntity<CreateRoomResponse> response = controller.createChatRoom(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(chatRoomService).createRoom(request);
    }

    @Test
    @DisplayName("createChatRoom - duplicate name propagates ConflictException")
    void createChatRoom_duplicate_throwsConflictException() {
        CreateRoomRequest request = new CreateRoomRequest("general");
        when(chatRoomService.createRoom(request))
                .thenThrow(new ConflictException("Chat room 'general' already exists."));

        assertThatThrownBy(() -> controller.createChatRoom(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Chat room 'general' already exists.");

        verify(chatRoomService).createRoom(request);
    }

    @Test
    @DisplayName("joinChatRoom - returns 200 OK and response body")
    void joinChatRoom_success_returnsOk() {
        JoinRoomRequest request = new JoinRoomRequest("guest_user");
        JoinRoomResponse expected = JoinRoomResponse.builder()
                .message("User 'guest_user' joined chat room 'general'.")
                .status("success")
                .build();

        when(chatRoomService.joinRoom("general", request)).thenReturn(expected);

        ResponseEntity<JoinRoomResponse> response = controller.joinChatRoom("general", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(chatRoomService).joinRoom("general", request);
    }

    @Test
    @DisplayName("joinChatRoom - non-existent room propagates NotFoundException")
    void joinChatRoom_notFound_throwsNotFoundException() {
        JoinRoomRequest request = new JoinRoomRequest("guest_user");
        when(chatRoomService.joinRoom("general", request))
                .thenThrow(new NotFoundException("Chat room 'general' does not exist."));

        assertThatThrownBy(() -> controller.joinChatRoom("general", request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Chat room 'general' does not exist.");

        verify(chatRoomService).joinRoom("general", request);
    }

    @Test
    @DisplayName("sendMessage - returns 200 OK and response body")
    void sendMessage_success_returnsOk() {
        SendMessageRequest request = new SendMessageRequest("guest_user", "Hello, everyone!");
        SendMessageResponse expected = SendMessageResponse.builder()
                .message("Message sent successfully.")
                .status("success")
                .build();

        when(chatRoomService.sendMessage("general", request)).thenReturn(expected);

        ResponseEntity<SendMessageResponse> response = controller.sendMessage("general", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(chatRoomService).sendMessage("general", request);
    }

    @Test
    @DisplayName("sendMessage - non-existent room propagates NotFoundException")
    void sendMessage_notFound_throwsNotFoundException() {
        SendMessageRequest request = new SendMessageRequest("guest_user", "Hello, everyone!");
        when(chatRoomService.sendMessage("general", request))
                .thenThrow(new NotFoundException("Chat room 'general' does not exist."));

        assertThatThrownBy(() -> controller.sendMessage("general", request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Chat room 'general' does not exist.");

        verify(chatRoomService).sendMessage("general", request);
    }

    @Test
    @DisplayName("getChatHistory - returns 200 OK and response body")
    void getChatHistory_success_returnsOk() {
        List<ChatMessageDto> messages = List.of(
                new ChatMessageDto("guest_user", "Hello, everyone!", "2024-01-01T10:00:00Z"),
                new ChatMessageDto("another_user", "Hi, guest_user!", "2024-01-01T10:01:00Z")
        );
        ChatHistoryResponse expected = new ChatHistoryResponse(messages);

        when(chatRoomService.getChatHistory("general", 10)).thenReturn(expected);

        ResponseEntity<ChatHistoryResponse> response = controller.getChatHistory("general", 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(chatRoomService).getChatHistory("general", 10);
    }

    @Test
    @DisplayName("getChatHistory - non-existent room propagates NotFoundException")
    void getChatHistory_notFound_throwsNotFoundException() {
        when(chatRoomService.getChatHistory("general", 10))
                .thenThrow(new NotFoundException("Chat room 'general' does not exist."));

        assertThatThrownBy(() -> controller.getChatHistory("general", 10))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Chat room 'general' does not exist.");

        verify(chatRoomService).getChatHistory("general", 10);
    }

    @Test
    @DisplayName("deleteChatRoom - returns 200 OK and response body")
    void deleteChatRoom_success_returnsOk() {
        DeleteRoomResponse expected = DeleteRoomResponse.builder()
                .message("Chat room 'general' deleted successfully.")
                .status("success")
                .build();

        when(chatRoomService.deleteRoom("general")).thenReturn(expected);

        ResponseEntity<DeleteRoomResponse> response = controller.deleteChatRoom("general");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(chatRoomService).deleteRoom("general");
    }

    @Test
    @DisplayName("deleteChatRoom - non-existent room propagates NotFoundException")
    void deleteChatRoom_notFound_throwsNotFoundException() {
        when(chatRoomService.deleteRoom("general"))
                .thenThrow(new NotFoundException("Chat room 'general' does not exist."));

        assertThatThrownBy(() -> controller.deleteChatRoom("general"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Chat room 'general' does not exist.");

        verify(chatRoomService).deleteRoom("general");
    }
}
