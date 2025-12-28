# Requirements Document

## Introduction

Bu özellik, MovieBackend uygulamasının güvenlik altyapısını güçlendirmeyi amaçlar. Üç ana perde halinde: şifre güvenliği ve secret key yönetimi, çift token sistemi (Access & Refresh), ve JWT tabanlı kimlik doğrulama filtresi implementasyonu gerçekleştirilecektir.

## Glossary

- **Backend**: Spring Boot tabanlı MovieBackend uygulaması
- **BCrypt**: Tek yönlü şifre hash algoritması
- **Access_Token**: 15 dakikalık kısa ömürlü JWT token
- **Refresh_Token**: 30 günlük uzun ömürlü JWT token
- **JWT_Filter**: Gelen istekleri doğrulayan Spring Security filtresi
- **Security_Context**: Doğrulanmış kullanıcı bilgisini tutan Spring Security bileşeni
- **Refresh_Token_Entity**: Veritabanında refresh token'ları saklayan JPA entity
- **Environment_Variable**: Uygulama dışında tanımlanan konfigürasyon değişkeni

## Requirements

### Requirement 1: BCrypt Şifre Güvenliği

**User Story:** As a user, I want my password to be securely hashed, so that my credentials are protected even if the database is compromised.

#### Acceptance Criteria

1. WHEN a user registers, THE Backend SHALL hash the password using BCrypt before storing
2. WHEN a user logs in, THE Backend SHALL verify the password against the BCrypt hash
3. THE Backend SHALL never store plain text passwords in the database

### Requirement 2: Secret Key Güvenliği

**User Story:** As a developer, I want the JWT secret key to be stored securely outside the codebase, so that sensitive credentials are not exposed in version control.

#### Acceptance Criteria

1. THE Backend SHALL read the JWT secret key from the JWT_SECRET environment variable
2. IF the JWT_SECRET environment variable is not set, THEN THE Backend SHALL fail to start with a clear error message
3. THE Backend SHALL NOT contain hardcoded secret keys in the source code

### Requirement 3: Refresh Token Veritabanı Yönetimi

**User Story:** As a system administrator, I want refresh tokens to be tracked in the database, so that I can revoke tokens and manage user sessions.

#### Acceptance Criteria

1. THE Refresh_Token_Entity SHALL store token value, user reference, expiration date, and revocation status
2. WHEN a refresh token is issued, THE Backend SHALL persist it to the database
3. WHEN a user logs out, THE Backend SHALL mark the refresh token as revoked
4. WHEN validating a refresh token, THE Backend SHALL check both JWT validity and database revocation status

### Requirement 4: Çift Token Sistemi (Access & Refresh)

**User Story:** As a user, I want to stay logged in without frequently re-entering credentials, so that I have a seamless experience while maintaining security.

#### Acceptance Criteria

1. WHEN a user successfully logs in, THE Backend SHALL return both an Access_Token (15 min) and a Refresh_Token (30 days)
2. THE Access_Token SHALL expire after 15 minutes
3. THE Refresh_Token SHALL expire after 30 days
4. WHEN serializing tokens to JSON, THE Backend SHALL include accessToken, refreshToken, and username fields

### Requirement 5: Token Yenileme Endpoint'i

**User Story:** As a mobile app, I want to refresh expired access tokens, so that users don't need to log in again when their short-lived token expires.

#### Acceptance Criteria

1. WHEN a valid refresh token is sent to /api/auth/refresh, THE Backend SHALL return a new Access_Token
2. IF the refresh token is expired, THEN THE Backend SHALL return 401 Unauthorized
3. IF the refresh token is revoked in database, THEN THE Backend SHALL return 401 Unauthorized
4. IF the refresh token is invalid, THEN THE Backend SHALL return 401 Unauthorized

### Requirement 6: JWT Kimlik Doğrulama Filtresi

**User Story:** As a backend developer, I want incoming requests to be automatically authenticated via JWT, so that protected endpoints can identify the current user.

#### Acceptance Criteria

1. WHEN a request contains a valid Bearer token in Authorization header, THE JWT_Filter SHALL extract and validate the token
2. WHEN the token is valid, THE JWT_Filter SHALL set the authenticated user in Security_Context
3. WHEN the token is missing or invalid, THE JWT_Filter SHALL allow the request to proceed without authentication
4. THE JWT_Filter SHALL execute before Spring Security's authentication filter

### Requirement 7: Korumalı Endpoint Erişimi

**User Story:** As a user, I want my personal data endpoints to be protected, so that only I can access my watchlist and preferences.

#### Acceptance Criteria

1. WHEN an unauthenticated request hits a protected endpoint, THE Backend SHALL return 401 Unauthorized
2. WHEN an authenticated request hits a protected endpoint, THE Backend SHALL allow access
3. THE Backend SHALL permit unauthenticated access to /api/auth/** endpoints

### Requirement 8: Logout İşlevi

**User Story:** As a user, I want to log out securely, so that my session is terminated and tokens are invalidated.

#### Acceptance Criteria

1. WHEN a user sends a logout request with a valid refresh token, THE Backend SHALL revoke the token in database
2. WHEN the logout is successful, THE Backend SHALL return a success message
3. IF the refresh token is not found, THEN THE Backend SHALL return 400 Bad Request
