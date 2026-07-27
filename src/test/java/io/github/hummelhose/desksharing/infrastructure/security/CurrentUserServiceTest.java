package io.github.hummelhose.desksharing.infrastructure.security;

import io.github.hummelhose.desksharing.application.service.UserSyncService;
import io.github.hummelhose.desksharing.domain.model.AppRole;
import io.github.hummelhose.desksharing.domain.model.AppUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CurrentUserServiceTest {

    private UserSyncService userSyncService;
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        userSyncService = mock(UserSyncService.class);
        currentUserService = new CurrentUserService(userSyncService);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getOrCreateCurrentUser_shouldSyncLocalAdminUser() {
        AppUser syncedUser = mock(AppUser.class);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        "admin",
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN")
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        when(userSyncService.syncUser(
                "local-admin",
                "admin@local.dev",
                "admin",
                AppRole.ADMIN
        )).thenReturn(syncedUser);

        AppUser result =
                currentUserService.getOrCreateCurrentUser();

        assertSame(syncedUser, result);

        verify(userSyncService).syncUser(
                "local-admin",
                "admin@local.dev",
                "admin",
                AppRole.ADMIN
        );
    }

    @Test
    void getOrCreateCurrentUser_shouldSyncLocalNormalUser() {
        AppUser syncedUser = mock(AppUser.class);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "user",
                        "user",
                        List.of(
                                new SimpleGrantedAuthority("ROLE_USER")
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        when(userSyncService.syncUser(
                "local-user",
                "user@local.dev",
                "user",
                AppRole.USER
        )).thenReturn(syncedUser);

        AppUser result =
                currentUserService.getOrCreateCurrentUser();

        assertSame(syncedUser, result);

        verify(userSyncService).syncUser(
                "local-user",
                "user@local.dev",
                "user",
                AppRole.USER
        );
    }

    @Test
    void getOrCreateCurrentUser_shouldSyncMicrosoftUser() {
        AppUser syncedUser = mock(AppUser.class);

        Map<String, Object> claims = Map.of(
                "sub", "sub-123",
                "oid", "entra-123",
                "preferred_username", "developer@example.com",
                "email", "developer@example.com",
                "name", "Example Developer"
        );

        Instant issuedAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant expiresAt = issuedAt.plusSeconds(3600);

        OidcIdToken idToken = new OidcIdToken(
                "token-value",
                issuedAt,
                expiresAt,
                claims
        );

        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER")
                ),
                idToken,
                "oid"
        );

        Authentication authentication =
                new OAuth2AuthenticationToken(
                        oidcUser,
                        oidcUser.getAuthorities(),
                        "microsoft"
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        when(userSyncService.syncUser(
                "entra-123",
                "developer@example.com",
                "Example Developer",
                AppRole.USER
        )).thenReturn(syncedUser);

        AppUser result =
                currentUserService.getOrCreateCurrentUser();

        assertSame(syncedUser, result);

        verify(userSyncService).syncUser(
                "entra-123",
                "developer@example.com",
                "Example Developer",
                AppRole.USER
        );
    }

    @Test
    void getOrCreateCurrentUser_shouldThrowException_whenNoUserIsAuthenticated() {
        SecurityContextHolder.clearContext();

        CurrentUserResolutionException exception =
                assertThrows(
                        CurrentUserResolutionException.class,
                        () -> currentUserService.getOrCreateCurrentUser()
                );

        assertEquals(
                "Kein angemeldeter Benutzer gefunden.",
                exception.getMessage()
        );

        verifyNoInteractions(userSyncService);
    }

    @Test
    void getOrCreateCurrentUser_shouldThrowException_whenUserIsAnonymous() {
        Authentication authentication =
                new AnonymousAuthenticationToken(
                        "anonymous-key",
                        "anonymousUser",
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ANONYMOUS")
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        CurrentUserResolutionException exception =
                assertThrows(
                        CurrentUserResolutionException.class,
                        () -> currentUserService.getOrCreateCurrentUser()
                );

        assertEquals(
                "Kein angemeldeter Benutzer gefunden.",
                exception.getMessage()
        );

        verifyNoInteractions(userSyncService);
    }

    @Test
    void getOrCreateCurrentUser_shouldThrowException_whenMicrosoftIdentityIsMissing() {
        DefaultOAuth2User oauth2User = new DefaultOAuth2User(
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER")
                ),
                Map.of(
                        "name",
                        "Unknown User"
                ),
                "name"
        );

        Authentication authentication =
                new OAuth2AuthenticationToken(
                        oauth2User,
                        oauth2User.getAuthorities(),
                        "microsoft"
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        CurrentUserResolutionException exception =
                assertThrows(
                        CurrentUserResolutionException.class,
                        () -> currentUserService.getOrCreateCurrentUser()
                );

        assertEquals(
                "Der angemeldete Microsoft-Benutzer enthält keine verwertbare Benutzerkennung.",
                exception.getMessage()
        );

        verifyNoInteractions(userSyncService);
    }
}