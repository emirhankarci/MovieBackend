package com.emirhankarci.moviebackend.security

import com.emirhankarci.moviebackend.user.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // 1. Authorization header'ı al
        val authHeader = request.getHeader("Authorization")
        
        // 2. Bearer token yoksa devam et
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        // 3. Token'ı çıkar
        val token = authHeader.substring(7)

        // 4. Token geçerli mi kontrol et
        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response)
            return
        }

        // 5. Username'i çıkar
        val username = jwtService.extractUsername(token)
        if (username == null) {
            filterChain.doFilter(request, response)
            return
        }

        // 6. SecurityContext'te zaten authentication yoksa set et
        if (SecurityContextHolder.getContext().authentication == null) {
            val user = userRepository.findByUsername(username)
            if (user != null) {
                val authToken = UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    emptyList() // Şimdilik rol yok
                )
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        }

        filterChain.doFilter(request, response)
    }
}
