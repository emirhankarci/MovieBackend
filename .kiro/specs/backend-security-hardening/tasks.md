# Implementation Plan: Backend Security Hardening

## Overview

Bu plan, MovieBackend güvenlik altyapısını 3 perde halinde güçlendirir. Her task mevcut kodu değiştirmeyi veya yeni bileşenler eklemeyi içerir.

## Tasks

- [x] 1. Secret Key Güvenliği
  - [x] 1.1 JwtService'i environment variable zorunlu olacak şekilde güncelle
    - `initSecretKey()` fonksiyonunu ekle
    - JWT_SECRET yoksa `IllegalStateException` fırlat
    - Fallback/default değeri kaldır
    - _Requirements: 2.1, 2.2, 2.3_

- [x] 2. Refresh Token Veritabanı Altyapısı
  - [x] 2.1 RefreshToken entity'sini oluştur
    - `src/main/kotlin/com/emirhankarci/moviebackend/token/RefreshToken.kt`
    - id, token, user (ManyToOne), expiresAt, revoked alanları
    - _Requirements: 3.1_
  - [x] 2.2 RefreshTokenRepository'yi oluştur
    - `findByToken()` ve `deleteByUser()` metodları
    - _Requirements: 3.1_

- [x] 3. JwtService Token Üretim Güncellemesi
  - [x] 3.1 Token üretim metodlarını güncelle
    - `generateAccessToken()` - 15 dakika expiration
    - `generateRefreshToken()` - 30 gün expiration
    - `isTokenValid()` metodu ekle
    - `extractExpiration()` metodu ekle
    - _Requirements: 4.2, 4.3_
  - [ ]* 3.2 Property test: Token expiration doğruluğu
    - **Property 2: Token Expiration Correctness**
    - **Validates: Requirements 4.2, 4.3**

- [x] 4. Login Response Güncellemesi
  - [x] 4.1 DTO'ları güncelle
    - `LoginResponse` → accessToken, refreshToken, username
    - `RefreshRequest`, `TokenResponse`, `LogoutRequest` ekle
    - _Requirements: 4.4_
  - [x] 4.2 AuthController login metodunu güncelle
    - Her iki token'ı da üret ve döndür
    - Refresh token'ı veritabanına kaydet
    - _Requirements: 4.1, 3.2_
  - [ ]* 4.3 Property test: Login response completeness
    - **Property 3: Login Response Completeness**
    - **Validates: Requirements 4.1, 4.4**
  - [ ]* 4.4 Property test: Refresh token persistence
    - **Property 4: Refresh Token Persistence**
    - **Validates: Requirements 3.2**

- [x] 5. Checkpoint - Temel Token Sistemi
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Refresh Endpoint Implementasyonu
  - [x] 6.1 /api/auth/refresh endpoint'ini ekle
    - Refresh token'ı validate et (JWT + DB revoked check)
    - Yeni access token döndür
    - _Requirements: 5.1, 3.4_
  - [ ]* 6.2 Property test: Valid refresh returns new access token
    - **Property 9: Valid Refresh Returns New Access Token**
    - **Validates: Requirements 5.1**
  - [ ]* 6.3 Unit test: Refresh error cases
    - Expired token → 401
    - Revoked token → 401
    - Invalid token → 401
    - _Requirements: 5.2, 5.3, 5.4_

- [x] 7. Logout Endpoint Implementasyonu
  - [x] 7.1 /api/auth/logout endpoint'ini ekle
    - Refresh token'ı revoke et (revoked = true)
    - Success message döndür
    - Token bulunamazsa 400 döndür
    - _Requirements: 8.1, 8.2, 8.3_
  - [ ]* 7.2 Property test: Logout revocation
    - **Property 5: Logout Revocation**
    - **Validates: Requirements 3.3, 8.1**
  - [ ]* 7.3 Property test: Revoked token rejection
    - **Property 6: Revoked Token Rejection**
    - **Validates: Requirements 3.4, 5.3**

- [x] 8. Checkpoint - Token Yönetimi Tamamlandı
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. JWT Authentication Filter
  - [x] 9.1 JwtAuthenticationFilter'ı oluştur
    - `src/main/kotlin/com/emirhankarci/moviebackend/security/JwtAuthenticationFilter.kt`
    - OncePerRequestFilter'dan extend et
    - Authorization header'dan Bearer token'ı çıkar
    - Token geçerliyse SecurityContext'e user'ı set et
    - _Requirements: 6.1, 6.2, 6.3_
  - [x] 9.2 SecurityConfig'e filter'ı ekle
    - UsernamePasswordAuthenticationFilter'dan önce çalışacak şekilde ekle
    - _Requirements: 6.4_
  - [ ]* 9.3 Property test: JWT filter authentication
    - **Property 7: JWT Filter Authentication**
    - **Validates: Requirements 6.1, 6.2, 6.3**

- [x] 10. Endpoint Protection Doğrulaması
  - [ ]* 10.1 Property test: Endpoint protection
    - **Property 8: Endpoint Protection**
    - **Validates: Requirements 7.1, 7.2**
  - [ ]* 10.2 Unit test: Auth endpoints public access
    - /api/auth/** unauthenticated erişim
    - _Requirements: 7.3_

- [x] 11. BCrypt Doğrulama Testleri
  - [ ]* 11.1 Property test: BCrypt password round-trip
    - **Property 1: BCrypt Password Round-Trip**
    - **Validates: Requirements 1.1, 1.2**

- [x] 12. Final Checkpoint - Tüm Güvenlik Katmanları Tamamlandı
  - Ensure all tests pass, ask the user if questions arise.
  - Manuel test: Register → Login → Protected endpoint → Refresh → Logout akışı

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Mevcut BCrypt implementasyonu zaten çalışıyor, sadece test ekleniyor
- JwtService'deki mevcut metodlar güncelleniyor, yeni metodlar ekleniyor
- Kotest property testing framework kullanılacak
