package br.com.realize.digitalbank.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_username", columnNames = "username")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 160, unique = true)
    private String email;

    @Column(nullable = false, length = 80, unique = true)
    private String username;

    @Column(nullable = false, length = 120)
    private String password;

    @Column(length = 128)
    private String currentTokenHash;

    private OffsetDateTime tokenExpiresAt;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected User() {
    }

    public User(String name, String email, String username, String password) {
        this.name = name;
        this.email = email;
        this.username = username;
        this.password = password;
        this.createdAt = OffsetDateTime.now();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public void updateCurrentToken(String tokenHash, OffsetDateTime tokenExpiresAt) {
        this.currentTokenHash = tokenHash;
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public boolean hasValidToken(String tokenHash, OffsetDateTime now) {
        return currentTokenHash != null
                && currentTokenHash.equals(tokenHash)
                && tokenExpiresAt != null
                && tokenExpiresAt.isAfter(now);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getCurrentTokenHash() { return currentTokenHash; }
    public OffsetDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
