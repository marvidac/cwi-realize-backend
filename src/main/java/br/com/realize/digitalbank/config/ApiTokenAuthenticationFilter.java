package br.com.realize.digitalbank.config;

import br.com.realize.digitalbank.domain.User;
import br.com.realize.digitalbank.repository.UserRepository;
import br.com.realize.digitalbank.service.AccessTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_PREFIX = "Bearer ";

    private final UserRepository userRepository;
    private final AccessTokenService accessTokenService;

    public ApiTokenAuthenticationFilter(UserRepository userRepository,
                                        AccessTokenService accessTokenService) {
        this.userRepository = userRepository;
        this.accessTokenService = accessTokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return !requestUri.startsWith("/api/") || "/api/auth/login".equals(requestUri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestToken = resolveToken(request);

        if (!StringUtils.hasText(requestToken)) {
            writeUnauthorizedResponse(response);
            return;
        }

        String tokenHash = accessTokenService.hashToken(requestToken);
        User user = userRepository.findByCurrentTokenHash(tokenHash)
                .filter(foundUser -> foundUser.hasValidToken(tokenHash, OffsetDateTime.now()))
                .orElse(null);

        if (user == null) {
            writeUnauthorizedResponse(response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(TOKEN_PREFIX)) {
            return authorizationHeader.substring(TOKEN_PREFIX.length()).trim();
        }
        return null;
    }

    private void writeUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"messages\":[\"Token de autenticação inválido ou ausente\"]}");
    }
}
