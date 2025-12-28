package com.emirhankarci.moviebackend.user

import com.emirhankarci.moviebackend.security.JwtService
import com.emirhankarci.moviebackend.token.RefreshToken
import com.emirhankarci.moviebackend.token.RefreshTokenRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val refreshTokenRepository: RefreshTokenRepository
) {

    fun register(username: String, email: String, password: String): AuthResult<String> {
        // Validation
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            return AuthResult.Error("All fields are required!")
        }

        if (!isValidEmail(email)) {
            return AuthResult.Error("Invalid email format!")
        }

        if (password.length < 8) {
            return AuthResult.Error("Password must be at least 8 characters!")
        }

        if (userRepository.findByEmail(email) != null) {
            return AuthResult.Error("Email is already taken!")
        }

        if (userRepository.findByUsername(username) != null) {
            return AuthResult.Error("Username is already taken!")
        }

        val encodedPassword = passwordEncoder.encode(password)
            ?: return AuthResult.Error("Password encoding failed!")

        val newUser = User(
            username = username,
            email = email,
            password = encodedPassword
        )

        userRepository.save(newUser)
        return AuthResult.Success("User registered successfully!")
    }

    fun login(email: String, password: String): AuthResult<LoginResponse> {
        val user = userRepository.findByEmail(email)
            ?: return AuthResult.Error("User not found!")

        if (!passwordEncoder.matches(password, user.password)) {
            return AuthResult.Error("Invalid password!")
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

        return AuthResult.Success(LoginResponse(accessToken, refreshToken, user.username))
    }

    fun refresh(refreshToken: String): AuthResult<TokenResponse> {
        val storedToken = refreshTokenRepository.findByToken(refreshToken)
            ?: return AuthResult.Error("Refresh token not found!")

        if (storedToken.revoked) {
            return AuthResult.Error("Refresh token revoked!")
        }

        if (storedToken.expiresAt.isBefore(Instant.now())) {
            return AuthResult.Error("Refresh token expired!")
        }

        if (!jwtService.isTokenValid(refreshToken)) {
            return AuthResult.Error("Invalid refresh token!")
        }

        val username = jwtService.extractUsername(refreshToken)
            ?: return AuthResult.Error("Invalid refresh token!")

        val newAccessToken = jwtService.generateAccessToken(username)
        return AuthResult.Success(TokenResponse(newAccessToken))
    }

    @Transactional
    fun logout(refreshToken: String): AuthResult<String> {
        refreshTokenRepository.findByToken(refreshToken)
            ?: return AuthResult.Error("Refresh token not found!")

        refreshTokenRepository.revokeByToken(refreshToken)
        return AuthResult.Success("Logged out successfully!")
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }
}
