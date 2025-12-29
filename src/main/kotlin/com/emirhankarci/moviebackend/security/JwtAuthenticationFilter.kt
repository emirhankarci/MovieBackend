package com.emirhankarci.moviebackend.security

import com.emirhankarci.moviebackend.user.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
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

    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val method = request.method
        val path = request.requestURI
        val authHeader = request.getHeader("Authorization")
        
        log.debug("[{}] Request - Path: {}, AuthHeader present: {}", method, path, authHeader != null)

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)

        if (!jwtService.isTokenValid(token)) {
            log.debug("[{}] Invalid or expired token for {}", method, path)
            filterChain.doFilter(request, response)
            return
        }

        val username = jwtService.extractUsername(token)
        if (username == null) {
            log.debug("[{}] Extract username failed for {}", method, path)
            filterChain.doFilter(request, response)
            return
        }

        if (SecurityContextHolder.getContext().authentication == null) {
            val user = userRepository.findByUsername(username)
            if (user != null) {
                log.debug("[{}] User '{}' authenticated for {}", method, username, path)
                val authToken = UsernamePasswordAuthenticationToken(
                    user.username,
                    null,
                    listOf(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                )
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            } else {
                log.warn("[{}] User '{}' not found in DB!", method, username)
            }
        }

        filterChain.doFilter(request, response)
    }
}
