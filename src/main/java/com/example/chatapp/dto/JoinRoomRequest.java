/** Data transfer object carrying JoinRoomRequest data. */
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
@Schema(description = "Request payload to join a chat room")
public class JoinRoomRequest {

    @NotBlank(message = "Participant name cannot be blank")
    @Schema(description = "Name of the participant joining the room", example = "guest_user")
    private String participant;
}
