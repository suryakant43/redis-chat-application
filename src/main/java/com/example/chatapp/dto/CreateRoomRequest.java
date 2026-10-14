/** Data transfer object carrying CreateRoomRequest data between API layer and service. */
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
@Schema(description = "Request payload to create a new chat room")
public class CreateRoomRequest {

    @NotBlank(message = "Room name cannot be blank")
    @Schema(description = "Unique name of the chat room", example = "general")
    private String roomName;
}
