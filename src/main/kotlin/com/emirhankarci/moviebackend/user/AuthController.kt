package com.emirhankarci.moviebackend.user

import com.emirhankarci.moviebackend.security.JwtService
import com.emirhankarci.moviebackend.token.RefreshToken
import com.emirhankarci.moviebackend.token.RefreshTokenRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
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
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val refreshTokenRepository: RefreshTokenRepository
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<Any> {
        val rawPassword = request.password
        val username = request.username
        val email = request.email

        if (rawPassword.isBlank() || username.isBlank() || email.isBlank()) {
            return ResponseEntity.badRequest().body("All fields are required!")
        }

        if (userRepository.findByEmail(email) != null) {
            return ResponseEntity.badRequest().body("Email is already taken!")
        }

        val encodedPassword: String = passwordEncoder.encode(rawPassword)
            ?: throw IllegalStateException("Password encoding failed")

        val newUser = User(
            username = username,
            email = email,
            password = encodedPassword
        )

        userRepository.save(newUser)
        return ResponseEntity.ok("User registered successfully!")
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        val user = userRepository.findByEmail(request.email)
            ?: return ResponseEntity.status(401).body("User not found!")

        if (!passwordEncoder.matches(request.password, user.password)) {
            return ResponseEntity.status(401).body("Invalid password!")
        }

        val accessToken = jwtService.generateAccessToken(user.username)
        val refreshToken = jwtService.generateRefreshToken(user.username)

        val refreshTokenEntity = RefreshToken(
            token = refreshToken,
            user = user,
            expiresAt = Instant.now().plusSeconds(60 * 60 * 24 * 30),
            revoked = false
        )
        refreshTokenRepository.save(refreshTokenEntity)

        return ResponseEntity.ok(LoginResponse(accessToken, refreshToken, user.username))
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): ResponseEntity<Any> {
        val storedToken = refreshTokenRepository.findByToken(request.refreshToken)
            ?: return ResponseEntity.status(401).body("Refresh token not found!")

        if (storedToken.revoked) {
            return ResponseEntity.status(401).body("Refresh token revoked!")
        }

        if (storedToken.expiresAt.isBefore(Instant.now())) {
            return ResponseEntity.status(401).body("Refresh token expired!")
        }

        if (!jwtService.isTokenValid(request.refreshToken)) {
            return ResponseEntity.status(401).body("Invalid refresh token!")
        }

        val username: String = jwtService.extractUsername(request.refreshToken)
            ?: return ResponseEntity.status(401).body("Invalid refresh token!")

        val newAccessToken = jwtService.generateAccessToken(username)
        return ResponseEntity.ok(TokenResponse(newAccessToken))
    }

    @PostMapping("/logout")
    @Transactional
    fun logout(@RequestBody request: LogoutRequest): ResponseEntity<Any> {
        refreshTokenRepository.findByToken(request.refreshToken)
            ?: return ResponseEntity.badRequest().body("Refresh token not found!")

        refreshTokenRepository.revokeByToken(request.refreshToken)
        return ResponseEntity.ok("Logged out successfully!")
    }
}
