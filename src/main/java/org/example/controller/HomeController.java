package org.example.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@Tag(name = "Home Controller", description = "API endpoints for home and health check operations")
public class HomeController {

    @GetMapping("/")
    @Operation(summary = "Redirect to Swagger UI", description = "Redirects to the Swagger UI documentation page")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "Successfully redirected to Swagger UI")
    })
    @Hidden
    public RedirectView home() {
        return new RedirectView("/swagger-ui/index.html");
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns the health status of the application")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application is healthy")
    })
    public String health() {
        return "OK";
    }
}