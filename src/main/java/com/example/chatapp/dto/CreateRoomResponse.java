/** Data transfer object carrying CreateRoomResponse data after room creation. */
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
@Schema(description = "Response returned after successfully creating a chat room")
public class CreateRoomResponse {

    @Schema(description = "Status description message", example = "Chat room 'general' created successfully.")
    private String message;

    @Schema(description = "Identifier of the created chat room", example = "general")
    private String roomId;

    @Schema(description = "Operation status indicator", example = "success")
    private String status;
}
