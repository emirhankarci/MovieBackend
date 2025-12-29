package com.emirhankarci.moviebackend.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService {

    private val secretKey: SecretKey = initSecretKey()

    private fun initSecretKey(): SecretKey {
        val secret = System.getenv("JWT_SECRET")
            ?: throw IllegalStateException("JWT_SECRET environment variable must be set! Please configure it before starting the application.")
        
        val keyBytes = try {
            Base64.getDecoder().decode(secret)
        } catch (e: IllegalArgumentException) {
            secret.toByteArray(Charsets.UTF_8)
        }

        if (keyBytes.size < 32) {
            throw IllegalStateException("JWT_SECRET must be at least 32 characters (256 bits) long for security!")
        }
        
        return Keys.hmacShaKeyFor(keyBytes)
    }

    fun generateAccessToken(username: String): String {
        return Jwts.builder()
            .subject(username)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 1000 * 60 * 15)) // 15 dakika
            .signWith(secretKey)
            .compact()
    }

    fun generateRefreshToken(username: String): String {
        return Jwts.builder()
            .subject(username)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30)) // 30 gün
            .signWith(secretKey)
            .compact()
    }

    fun extractUsername(token: String): String? {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
                .subject
        } catch (e: JwtException) {
            null
        }
    }

    fun extractExpiration(token: String): Date? {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
                .expiration
        } catch (e: JwtException) {
            null
        }
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
            
            val expiration = claims.expiration
            expiration != null && expiration.after(Date())
        } catch (e: ExpiredJwtException) {
            false
        } catch (e: JwtException) {
            false
        }
    }
}