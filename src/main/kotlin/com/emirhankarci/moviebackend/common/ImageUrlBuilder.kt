package com.emirhankarci.moviebackend.common

/**
 * Centralized TMDB image URL builder
 * Ensures consistent full URLs across all services
 */
object ImageUrlBuilder {
    private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p"
    const val POSTER_SIZE = "w500"
    const val BACKDROP_SIZE = "w780"
    const val PROFILE_SIZE = "w185"
    const val ORIGINAL_SIZE = "original"

    /**
     * Build full poster URL from path
     * @param path The poster path (e.g., "/abc123.jpg")
     * @return Full URL or null if path is null/blank
     */
    fun buildPosterUrl(path: String?): String? {
        return path?.takeIf { it.isNotBlank() }?.let { "$IMAGE_BASE_URL/$POSTER_SIZE$it" }
    }

    /**
     * Build full backdrop URL from path
     */
    fun buildBackdropUrl(path: String?): String? {
        return path?.takeIf { it.isNotBlank() }?.let { "$IMAGE_BASE_URL/$BACKDROP_SIZE$it" }
    }

    /**
     * Build full profile URL from path
     */
    fun buildProfileUrl(path: String?): String? {
        return path?.takeIf { it.isNotBlank() }?.let { "$IMAGE_BASE_URL/$PROFILE_SIZE$it" }
    }

    /**
     * Build original size image URL
     */
    fun buildOriginalUrl(path: String?): String? {
        return path?.takeIf { it.isNotBlank() }?.let { "$IMAGE_BASE_URL/$ORIGINAL_SIZE$it" }
    }
}
