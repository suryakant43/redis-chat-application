/** Data transfer object carrying ChatHistoryResponse containing list of chat messages. */
package com.example.chatapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload containing the list of chat messages")
public class ChatHistoryResponse {

    @Schema(description = "Chronological list of chat messages")
    private List<ChatMessageDto> messages;
}
