/** Controller for root path and health check; returns application status and documentation links. */
package com.example.chatapp.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    /**
     * Root endpoint to serve health checks and welcome info.
     * Prevents 404/500 errors when cloud providers ping the root URL '/'.
     */
    @GetMapping("/")
    @Hidden
    public ResponseEntity<Map<String, Object>> rootHealthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Redis Chat Application",
                "swaggerUi", "/swagger-ui.html",
                "apiDocs", "/v3/api-docs",
                "chatroomsApi", "/api/chatapp/chatrooms"
        ));
    }
}
