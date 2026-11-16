package com.shaahil.resumeapi.auth;

import com.shaahil.resumeapi.PostgresTestContainerConfig;
import com.shaahil.resumeapi.auth.entity.AdminUser;
import com.shaahil.resumeapi.auth.repository.AdminUserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminUserRepositoryTest extends PostgresTestContainerConfig {

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Test
    void findByEmailIgnoreCaseReturnsAdminWhenCaseDiffers() {
        AdminUser admin = new AdminUser("admin", "hash");
        admin.setEmail("Admin@Example.com");
        adminUserRepository.saveAndFlush(admin);

        var result = adminUserRepository.findByEmailIgnoreCase("admin@example.com");

        assertTrue(result.isPresent());
        assertEquals("admin", result.get().getUsername());
    }

    @Test
    void findByEmailIgnoreCaseReturnsEmptyForUnknownEmail() {
        var result = adminUserRepository.findByEmailIgnoreCase("unknown@example.com");

        assertFalse(result.isPresent());
    }
}
