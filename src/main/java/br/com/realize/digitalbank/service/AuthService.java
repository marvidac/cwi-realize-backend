package br.com.realize.digitalbank.service;

import br.com.realize.digitalbank.api.dto.LoginRequest;
import br.com.realize.digitalbank.api.dto.TokenResponse;
import br.com.realize.digitalbank.domain.User;
import br.com.realize.digitalbank.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.OffsetDateTime;

@Service
public class AuthService {

    private static final long TOKEN_EXPIRATION_HOURS = 2;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AccessTokenService accessTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = accessTokenService.generateToken();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS);

        try {
            user.updateCurrentToken(accessTokenService.hashToken(token), expiresAt);
            return new TokenResponse(token, "Bearer", expiresAt);
        } catch (RuntimeException exception) {
            markCurrentTransactionForRollback();
            throw new PersistenceOperationException("Falha ao persistir o token de autenticação. Nenhuma alteração deve permanecer gravada.", exception);
        }
    }

    private void markCurrentTransactionForRollback() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }
}
