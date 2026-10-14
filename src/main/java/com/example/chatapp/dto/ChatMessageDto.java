/** Data transfer object carrying ChatMessage data stored in Redis and returned in chat history. */
package com.example.chatapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat message representation stored in Redis and returned in history")
public class ChatMessageDto {

    @Schema(description = "Participant who authored the message", example = "guest_user")
    private String participant;

    @Schema(description = "Message content", example = "Hello, everyone!")
    private String message;

    @Schema(description = "Timestamp when message was sent in ISO-8601 UTC format", example = "2024-01-01T10:00:00Z")
    private String timestamp;
}
