package com.emirhankarci.moviebackend.user

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val confirmPassword: String,
    val firstName: String,
    val lastName: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val username: String
)

data class RefreshRequest(
    val refreshToken: String
)

data class TokenResponse(
    val accessToken: String
)

data class LogoutRequest(
    val refreshToken: String
)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<Any> {
        return when (val result = authService.register(
            username = request.username,
            email = request.email,
            password = request.password,
            confirmPassword = request.confirmPassword,
            firstName = request.firstName,
            lastName = request.lastName
        )) {
            is AuthResult.Success -> ResponseEntity.ok(result.data)
            is AuthResult.Error -> ResponseEntity.badRequest().body(result.message)
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        return when (val result = authService.login(request.email, request.password)) {
            is AuthResult.Success -> ResponseEntity.ok(result.data)
            is AuthResult.Error -> ResponseEntity.status(401).body(result.message)
        }
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): ResponseEntity<Any> {
        return when (val result = authService.refresh(request.refreshToken)) {
            is AuthResult.Success -> ResponseEntity.ok(result.data)
            is AuthResult.Error -> ResponseEntity.status(401).body(result.message)
        }
    }

    @PostMapping("/logout")
    fun logout(@RequestBody request: LogoutRequest): ResponseEntity<Any> {
        return when (val result = authService.logout(request.refreshToken)) {
            is AuthResult.Success -> ResponseEntity.ok(result.data)
            is AuthResult.Error -> ResponseEntity.badRequest().body(result.message)
        }
    }
}
