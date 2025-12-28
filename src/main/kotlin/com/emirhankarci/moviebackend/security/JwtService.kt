package com.emirhankarci.moviebackend.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService {
    // Bu anahtarı normalde environment variable'dan almak daha güvenlidir.
    // Şimdilik buraya uzun ve karmaşık bir string koyuyoruz (En az 32 karakter olmalı).
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(
        "benim_cok_gizli_ve_uzun_jwt_sifreleme_anahtarim_12345".toByteArray()
    )

    fun generateToken(username: String): String {
        return Jwts.builder()
            .subject(username)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24 Saat geçerli
            .signWith(secretKey)
            .compact()
    }

    // Token'dan kullanıcı adını okuma
    fun extractUsername(token: String): String {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
    }
}