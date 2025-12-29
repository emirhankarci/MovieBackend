package com.emirhankarci.moviebackend.user

import jakarta.persistence.*

@Entity
@Table(name = "app_users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    val username: String,

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = false)
    val password: String, // Şimdilik düz metin, sonra şifreleyeceğiz (BCrypt)

    @Column(nullable = false)
    val firstName: String,

    @Column(nullable = false)
    val lastName: String
)