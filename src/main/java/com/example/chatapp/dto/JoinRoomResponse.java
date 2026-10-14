/** Data transfer object carrying JoinRoomResponse data. */
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
@Schema(description = "Response returned after successfully joining a chat room")
public class JoinRoomResponse {

    @Schema(description = "Status description message", example = "User 'guest_user' joined chat room 'general'.")
    private String message;

    @Schema(description = "Operation status indicator", example = "success")
    private String status;
}
