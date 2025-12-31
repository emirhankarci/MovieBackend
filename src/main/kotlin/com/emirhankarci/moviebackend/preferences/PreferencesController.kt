package com.emirhankarci.moviebackend.preferences

import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/preferences")
class PreferencesController(
    private val preferencesService: PreferencesService
) {

    @PostMapping
    fun savePreferences(@RequestBody request: SavePreferencesRequest): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        return when (val result = preferencesService.saveOrUpdatePreferences(username, request)) {
            is PreferencesResult.Success -> ResponseEntity.ok(result.data)
            is PreferencesResult.Error -> ResponseEntity.badRequest().body(mapOf("message" to result.message))
        }
    }

    @GetMapping
    fun getPreferences(): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        return when (val result = preferencesService.getUserPreferences(username)) {
            is PreferencesResult.Success -> {
                if (result.data != null) {
                    ResponseEntity.ok(result.data)
                } else {
                    ResponseEntity.ok(mapOf("message" to "No preferences found"))
                }
            }
            is PreferencesResult.Error -> ResponseEntity.badRequest().body(mapOf("message" to result.message))
        }
    }

    @GetMapping("/status")
    fun checkPreferencesStatus(): ResponseEntity<Any> {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        return when (val result = preferencesService.hasPreferences(username)) {
            is PreferencesResult.Success -> ResponseEntity.ok(result.data)
            is PreferencesResult.Error -> ResponseEntity.badRequest().body(mapOf("message" to result.message))
        }
    }

    @GetMapping("/curated-films")
    fun getCuratedFilms(): ResponseEntity<List<CuratedFilm>> {
        return ResponseEntity.ok(preferencesService.getCuratedFilms())
    }
}
