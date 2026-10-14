/** Data transfer object carrying SendMessageResponse data. */
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
@Schema(description = "Response returned after sending a message")
public class SendMessageResponse {

    @Schema(description = "Status description message", example = "Message sent successfully.")
    private String message;

    @Schema(description = "Operation status indicator", example = "success")
    private String status;
}
