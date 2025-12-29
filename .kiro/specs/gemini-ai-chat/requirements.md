# Requirements Document

## Introduction

Bu özellik, kullanıcıların Gemini AI ile film/dizi hakkında sohbet edebilmesini sağlayan bir chat sistemi implementasyonudur. Sistem, mesajları kaydeder, günlük kullanım limitlerini takip eder ve ücretsiz kullanıcılar için günde 5 mesaj hakkı sunar.

## Glossary

- **Chat_Service**: Kullanıcı mesajlarını işleyen ve AI yanıtlarını yöneten servis katmanı
- **AI_Service**: Gemini API ile iletişim kuran servis
- **Message_Repository**: Chat mesajlarını veritabanında saklayan repository
- **Rate_Limiter**: Günlük mesaj limitini kontrol eden bileşen
- **Conversation_Context**: AI'ya gönderilen önceki mesajların listesi

## Requirements

### Requirement 1: Chat Mesajlarını Kaydetme

**User Story:** As a user, I want my chat messages to be saved, so that I can continue conversations and have a history of my interactions.

#### Acceptance Criteria

1. WHEN a user sends a message, THE Chat_Service SHALL save the message to the database with user_id, content, role, and timestamp
2. WHEN the AI responds, THE Chat_Service SHALL save the AI response to the database with the same conversation context
3. THE Message_Repository SHALL store messages with the following fields: id, user_id, content, role (USER/ASSISTANT), created_at
4. WHEN retrieving conversation history, THE Chat_Service SHALL return messages ordered by created_at ascending

### Requirement 2: Günlük Mesaj Limiti Kontrolü

**User Story:** As a system administrator, I want to limit free users to 5 messages per day, so that we can manage API costs and encourage premium upgrades.

#### Acceptance Criteria

1. WHEN a user sends a message, THE Rate_Limiter SHALL check the user's message count for the current day
2. IF the user has sent 5 or more messages today, THEN THE Chat_Service SHALL return a 429 Too Many Requests error without calling the AI
3. WHEN the daily limit is reached, THE Chat_Service SHALL include a message indicating the limit has been reached
4. THE Rate_Limiter SHALL reset the message count at midnight (UTC) each day
5. WHEN a user requests their remaining quota, THE Chat_Service SHALL return the number of messages remaining for the day

### Requirement 3: Gemini AI Entegrasyonu

**User Story:** As a user, I want to chat with an AI assistant about movies and TV shows, so that I can get recommendations and information.

#### Acceptance Criteria

1. WHEN a user sends a message and has remaining quota, THE AI_Service SHALL send the message to Gemini API
2. THE AI_Service SHALL include a system prompt that instructs the AI to act as a movie/TV show assistant
3. WHEN calling Gemini API, THE AI_Service SHALL include the last 10 messages as conversation context
4. WHEN Gemini API returns a response, THE AI_Service SHALL return the response text to the Chat_Service
5. IF Gemini API returns an error, THEN THE AI_Service SHALL return an appropriate error message to the user

### Requirement 4: Chat API Endpoints

**User Story:** As a mobile developer, I want clear API endpoints for chat functionality, so that I can integrate the chat feature into the mobile app.

#### Acceptance Criteria

1. THE Chat_Service SHALL expose a POST /api/chat/send endpoint that accepts a message and returns the AI response
2. THE Chat_Service SHALL expose a GET /api/chat/history endpoint that returns the user's conversation history
3. THE Chat_Service SHALL expose a GET /api/chat/quota endpoint that returns the user's remaining daily message count
4. WHEN any chat endpoint is called, THE Chat_Service SHALL require authentication via JWT token
5. WHEN the send endpoint is called, THE Chat_Service SHALL validate that the message is not empty or blank

### Requirement 5: Hata Yönetimi

**User Story:** As a user, I want clear error messages when something goes wrong, so that I understand what happened and what to do next.

#### Acceptance Criteria

1. IF the user is not authenticated, THEN THE Chat_Service SHALL return 401 Unauthorized
2. IF the daily limit is exceeded, THEN THE Chat_Service SHALL return 429 Too Many Requests with a descriptive message
3. IF the message is empty or blank, THEN THE Chat_Service SHALL return 400 Bad Request
4. IF Gemini API is unavailable or times out, THEN THE Chat_Service SHALL return 503 Service Unavailable
5. IF an unexpected error occurs, THEN THE Chat_Service SHALL return 500 Internal Server Error with a generic message
