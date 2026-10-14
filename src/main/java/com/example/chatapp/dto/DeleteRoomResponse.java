/** Data transfer object carrying DeleteRoomResponse data after room deletion. */
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
@Schema(description = "Response returned after deleting a chat room")
public class DeleteRoomResponse {

    @Schema(description = "Status description message", example = "Chat room 'general' deleted successfully.")
    private String message;

    @Schema(description = "Operation status indicator", example = "success")
    private String status;
}
