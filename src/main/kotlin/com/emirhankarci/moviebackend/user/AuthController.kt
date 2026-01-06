package com.emirhankarci.moviebackend.user

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class RegisterRequest(
    @field:NotBlank(message = "Username cannot be empty")
    @field:Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    val username: String,
    
    @field:NotBlank(message = "Email cannot be empty")
    @field:Email(message = "Invalid email format")
    val email: String,
    
    @field:NotBlank(message = "Password cannot be empty")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,
    
    @field:NotBlank(message = "Confirm password cannot be empty")
    val confirmPassword: String,
    
    @field:NotBlank(message = "First name cannot be empty")
    val firstName: String,
    
    @field:NotBlank(message = "Last name cannot be empty")
    val lastName: String
)

data class LoginRequest(
    @field:NotBlank(message = "Email cannot be empty")
    @field:Email(message = "Invalid email format")
    val email: String,
    
    @field:NotBlank(message = "Password cannot be empty")
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val username: String
)

data class RefreshRequest(
    @field:NotBlank(message = "Refresh token cannot be empty")
    val refreshToken: String
)

data class TokenResponse(
    val accessToken: String
)

data class LogoutRequest(
    @field:NotBlank(message = "Refresh token cannot be empty")
    val refreshToken: String
)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<Any> {
        return when (val result = authService.register(
            username = request.username,
            email = request.email,
            password = request.password,
            confirmPassword = request.confirmPassword,
            firstName = request.firstName,
            lastName = request.lastName
        )) {
            is AuthResult.Success -> ResponseEntity.ok(mapOf("message" to result.data))
            is AuthResult.Error -> ResponseEntity.badRequest().body(mapOf("message" to result.message))
        }
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<Any> {
        return when (val result = authService.login(request.email, request.password)) {
            is AuthResult.Success -> ResponseEntity.ok(result.data)
            is AuthResult.Error -> ResponseEntity.status(401).body(mapOf("message" to result.message))
        }
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): ResponseEntity<Any> {
        return when (val result = authService.refresh(request.refreshToken)) {
            is AuthResult.Success -> ResponseEntity.ok(result.data)
            is AuthResult.Error -> ResponseEntity.status(401).body(mapOf("message" to result.message))
        }
    }

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: LogoutRequest): ResponseEntity<Any> {
        return when (val result = authService.logout(request.refreshToken)) {
            is AuthResult.Success -> ResponseEntity.ok(mapOf("message" to result.data))
            is AuthResult.Error -> ResponseEntity.badRequest().body(mapOf("message" to result.message))
        }
    }
}
