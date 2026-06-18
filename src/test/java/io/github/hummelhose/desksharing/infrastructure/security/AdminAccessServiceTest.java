package io.github.hummelhose.desksharing.infrastructure.security;

import io.github.hummelhose.desksharing.domain.model.AppRole;
import io.github.hummelhose.desksharing.domain.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminAccessServiceTest {

    private CurrentUserService currentUserService;
    private AdminAccessService adminAccessService;

    @BeforeEach
    void setUp() {
        currentUserService = mock(CurrentUserService.class);
        adminAccessService = new AdminAccessService(currentUserService);
    }

    @Test
    void isCurrentUserAdmin_shouldReturnTrue_whenCurrentUserIsAdmin() {
        AppUser currentUser = mock(AppUser.class);

        when(currentUser.getRole()).thenReturn(AppRole.ADMIN);
        when(currentUserService.getOrCreateCurrentUser()).thenReturn(currentUser);

        boolean result = adminAccessService.isCurrentUserAdmin();

        assertTrue(result);
    }

    @Test
    void isCurrentUserAdmin_shouldReturnFalse_whenCurrentUserIsNormalUser() {
        AppUser currentUser = mock(AppUser.class);

        when(currentUser.getRole()).thenReturn(AppRole.USER);
        when(currentUserService.getOrCreateCurrentUser()).thenReturn(currentUser);

        boolean result = adminAccessService.isCurrentUserAdmin();

        assertFalse(result);
    }

    @Test
    void isCurrentUserAdmin_shouldReturnFalse_whenCurrentUserCannotBeLoaded() {
        when(currentUserService.getOrCreateCurrentUser())
                .thenThrow(new IllegalStateException("Kein angemeldeter Benutzer gefunden."));

        boolean result = adminAccessService.isCurrentUserAdmin();

        assertFalse(result);
    }
}