/** Configuration class for OpenAPI 3 documentation. */
package com.example.chatapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chatAppOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Redis Chat Application API")
                        .description("REST API for creating chat rooms, managing participants, messaging, and retrieving chat history using Redis.")
                        .version("1.0.0")
                        .contact(new Contact().name("Backend Team")));
    }
}
