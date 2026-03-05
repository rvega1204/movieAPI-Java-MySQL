package com.rvg.movieapi.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Movies REST API")
                        .version("1.0.0")
                        .description("""
                                ## Movies REST API
                                
                                Full REST API for movie management, user authentication, \
                                file handling, and password recovery.
                                
                                ### Available modules
                                - **Movie**: CRUD operations with pagination and sorting support
                                - **Auth**: Registration, login, and JWT token refresh
                                - **File**: Upload and retrieve files by name
                                - **Forgot Password**: OTP-based email password recovery
                                
                                ### Security
                                All protected endpoints require a **Bearer JWT token**.
                                Obtain your token via `POST /api/v1/auth/login`.
                                """)
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }

}
