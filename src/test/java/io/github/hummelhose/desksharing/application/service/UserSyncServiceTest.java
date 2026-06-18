package io.github.hummelhose.desksharing.application.service;

import io.github.hummelhose.desksharing.domain.model.AppRole;
import io.github.hummelhose.desksharing.domain.model.AppUser;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSyncServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    private UserSyncService userSyncService;

    @BeforeEach
    void setUp() {
        userSyncService = new UserSyncService(appUserRepository);
    }

    @Test
    void syncUser_shouldCreateNewUser_whenUserDoesNotExist() {
        String entraOid = "entra-123";
        String email = "max.mustermann@example.com";
        String displayName = "Max Mustermann";

        when(appUserRepository.findByEntraOid(entraOid)).thenReturn(Optional.empty());

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);

        when(appUserRepository.save(userCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = userSyncService.syncUser(
                entraOid,
                email,
                displayName,
                AppRole.USER
        );

        AppUser savedUser = userCaptor.getValue();

        assertEquals(entraOid, savedUser.getEntraOid());
        assertEquals(email, savedUser.getEmail());
        assertEquals(displayName, savedUser.getDisplayName());
        assertEquals(AppRole.USER, savedUser.getRole());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getLastLoginAt());

        assertEquals(savedUser, result);

        verify(appUserRepository).findByEntraOid(entraOid);
        verify(appUserRepository).save(savedUser);
    }

    @Test
    void syncUser_shouldUpdateExistingUser_whenUserAlreadyExists() {
        String entraOid = "entra-123";

        AppUser existingUser = new AppUser(
                entraOid,
                "old.email@example.com",
                "Alter Name",
                AppRole.USER,
                LocalDateTime.now().minusDays(10)
        );

        existingUser.setLastLoginAt(LocalDateTime.now().minusDays(5));

        when(appUserRepository.findByEntraOid(entraOid)).thenReturn(Optional.of(existingUser));
        when(appUserRepository.save(existingUser)).thenReturn(existingUser);

        AppUser result = userSyncService.syncUser(
                entraOid,
                "new.email@example.com",
                "Neuer Name",
                AppRole.USER
        );

        assertEquals("new.email@example.com", existingUser.getEmail());
        assertEquals("Neuer Name", existingUser.getDisplayName());
        assertNotNull(existingUser.getLastLoginAt());
        assertEquals(existingUser, result);

        verify(appUserRepository).findByEntraOid(entraOid);
        verify(appUserRepository).save(existingUser);
    }

    @Test
    void syncUser_shouldNotOverwriteRole_whenExistingUserLogsInAgain() {
        String entraOid = "entra-admin-123";

        AppUser existingAdmin = new AppUser(
                entraOid,
                "admin@example.com",
                "Admin User",
                AppRole.ADMIN,
                LocalDateTime.now().minusDays(10)
        );

        when(appUserRepository.findByEntraOid(entraOid)).thenReturn(Optional.of(existingAdmin));
        when(appUserRepository.save(existingAdmin)).thenReturn(existingAdmin);

        AppUser result = userSyncService.syncUser(
                entraOid,
                "admin.neu@example.com",
                "Admin Neuer Name",
                AppRole.USER
        );

        assertEquals(AppRole.ADMIN, result.getRole());
        assertEquals("admin.neu@example.com", result.getEmail());
        assertEquals("Admin Neuer Name", result.getDisplayName());

        verify(appUserRepository).findByEntraOid(entraOid);
        verify(appUserRepository).save(existingAdmin);
    }
}