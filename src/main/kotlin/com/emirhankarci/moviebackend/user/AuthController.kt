package com.emirhankarci.moviebackend.user

import com.emirhankarci.moviebackend.security.JwtService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// Gelen JSON verisini karşılayacak sınıf (DTO)
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
    val token: String,
    val username: String
)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<Any> {
        // 1. Kullanıcı zaten var mı kontrol et
        if (userRepository.findByEmail(request.email) != null) {
            return ResponseEntity.badRequest().body("Email is already taken!")
        }
        if (userRepository.findByUsername(request.username) != null) {
            return ResponseEntity.badRequest().body("Username is already taken!")
        }

        // 2. Yeni kullanıcıyı oluştur
        val newUser = User(
            username = request.username,
            email = request.email,
            password = request.password // Şimdilik şifreleme yok, sonra ekleyeceğiz
        )

        // 3. Veritabanına kaydet
        userRepository.save(newUser)

        return ResponseEntity.ok("User registered successfully!")
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        // 1. Kullanıcıyı bul
        val user = userRepository.findByEmail(request.email)
            ?: return ResponseEntity.status(401).body("User not found!")

        // 2. Şifreyi kontrol et
        if (user.password != request.password) {
            return ResponseEntity.status(401).body("Invalid password!")
        }

        // 3. Token üret
        val token = jwtService.generateToken(user.username)

        // 4. Token'ı döndür
        return ResponseEntity.ok(LoginResponse(token, user.username))
    }
}