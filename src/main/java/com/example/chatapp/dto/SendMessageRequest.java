/** Data transfer object carrying SendMessageRequest data. */
package com.example.chatapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload to send a message to a chat room")
public class SendMessageRequest {

    @NotBlank(message = "Participant name cannot be blank")
    @Schema(description = "Sender of the message", example = "guest_user")
    private String participant;

    @NotBlank(message = "Message text cannot be blank")
    @Schema(description = "Message body text", example = "Hello, everyone!")
    private String message;
}
