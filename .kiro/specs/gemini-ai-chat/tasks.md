# Implementation Plan: Gemini AI Chat

## Overview

Bu plan, Gemini AI chat özelliğinin backend implementasyonunu adım adım gerçekleştirir. Kotlin ve Spring Boot kullanılarak mevcut proje yapısına uyumlu şekilde geliştirilecektir.

## Tasks

- [x] 1. Veritabanı şeması ve entity oluşturma
  - [x] 1.1 Flyway migration dosyası oluştur (V6__create_chat_messages_table.sql)
    - chat_messages tablosu: id, user_id, content, role, created_at
    - user_id için foreign key ve index'ler
    - _Requirements: 1.3_

  - [x] 1.2 ChatMessage entity ve MessageRole enum oluştur
    - Entity sınıfı: ChatMessage.kt
    - Enum: MessageRole (USER, ASSISTANT)
    - User ile ManyToOne ilişki
    - _Requirements: 1.3_

  - [x] 1.3 ChatMessageRepository interface oluştur
    - findByUserIdOrderByCreatedAtAsc
    - findTop10ByUserIdOrderByCreatedAtDesc
    - countUserMessagesTodayByUserId (JPQL query)
    - _Requirements: 1.4, 2.1, 3.3_

- [x] 2. DTO'lar ve validation oluşturma
  - [x] 2.1 Request ve Response DTO'larını oluştur (ChatDTOs.kt)
    - SendMessageRequest (validation ile)
    - ChatResponse, ChatMessageResponse, QuotaResponse, ErrorResponse
    - _Requirements: 4.1, 4.2, 4.3, 4.5_

  - [ ]* 2.2 Property test: Empty message validation
    - **Property 7: Empty Message Validation**
    - **Validates: Requirements 4.5, 5.3**

- [x] 3. AI Service implementasyonu
  - [x] 3.1 AiService interface ve result types oluştur
    - AiResult sealed class (Success, Error)
    - AiErrorCode enum
    - _Requirements: 3.4, 3.5_

  - [x] 3.2 GeminiAiService implementasyonu
    - RestTemplate ile Gemini API çağrısı
    - System prompt tanımı
    - Conversation context oluşturma
    - Error handling (timeout, API error)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ]* 3.3 Property test: Conversation context limit
    - **Property 5: Conversation Context Limit**
    - **Validates: Requirements 3.3**

- [x] 4. Chat Service implementasyonu
  - [x] 4.1 ChatService sınıfını oluştur
    - sendMessage: mesaj gönderme ve AI yanıtı alma
    - getConversationHistory: geçmiş mesajları getirme
    - getRemainingQuota: kalan kota sorgulama
    - Günlük limit kontrolü (DAILY_MESSAGE_LIMIT = 5)
    - _Requirements: 1.1, 1.2, 1.4, 2.1, 2.2, 2.3, 2.4, 2.5_

  - [ ]* 4.2 Property test: Message persistence round-trip
    - **Property 1: Message Persistence Round-Trip**
    - **Validates: Requirements 1.1, 1.4**

  - [ ]* 4.3 Property test: Conversation history ordering
    - **Property 2: Conversation History Ordering**
    - **Validates: Requirements 1.4**

  - [ ]* 4.4 Property test: Daily limit enforcement
    - **Property 3: Daily Limit Enforcement**
    - **Validates: Requirements 2.2, 2.5**

  - [ ]* 4.5 Property test: Quota calculation consistency
    - **Property 4: Quota Calculation Consistency**
    - **Validates: Requirements 2.5**

- [x] 5. Checkpoint - Core logic tamamlandı
  - Tüm testlerin geçtiğinden emin ol
  - Kullanıcıya soru varsa sor

- [x] 6. Controller ve API endpoints
  - [x] 6.1 ChatController oluştur
    - POST /api/chat/send - mesaj gönderme
    - GET /api/chat/history - geçmiş mesajlar
    - GET /api/chat/quota - kalan kota
    - JWT authentication ile koruma
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [x] 6.2 GlobalExceptionHandler'a chat hatalarını ekle
    - ChatErrorCode için uygun HTTP status mapping
    - 429 Too Many Requests handling
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ]* 6.3 Property test: Authentication requirement
    - **Property 6: Authentication Requirement**
    - **Validates: Requirements 4.4, 5.1**

- [x] 7. Konfigürasyon ve entegrasyon
  - [x] 7.1 application.properties'e Gemini API ayarlarını ekle
    - Timeout konfigürasyonu
    - API URL (environment variable'dan)
    - _Requirements: 3.1_

  - [x] 7.2 RestTemplate bean konfigürasyonu
    - Timeout ayarları
    - Error handler
    - _Requirements: 3.5, 5.4_

  - [x] 7.3 build.gradle.kts'e Kotest dependency ekle
    - kotest-runner-junit5
    - kotest-property
    - _Requirements: Testing Strategy_

- [x] 8. Final checkpoint - Tüm implementasyon tamamlandı
  - Tüm testlerin geçtiğinden emin ol
  - API endpoint'lerini manuel test et
  - Kullanıcıya soru varsa sor

## Notes

- `*` ile işaretli task'lar opsiyoneldir ve hızlı MVP için atlanabilir
- Her task belirli requirements'ları referans alır
- Checkpoint'ler incremental validation sağlar
- Property testler Kotest framework'ü ile yazılacak
