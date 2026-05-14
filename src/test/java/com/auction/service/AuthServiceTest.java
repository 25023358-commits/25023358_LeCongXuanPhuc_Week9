package com.auction.service;

import com.auction.service.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.auction.entity.user.User;
public class AuthServiceTest {
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
    }

    @Test
    void testRegisterUser() {
        String uniqueUser = "testuser_" + System.currentTimeMillis();
        boolean result = authService.register(uniqueUser, "test@email.com", "password", "BIDDER");
        assertTrue(result);
    }

    @Test
    void testLoginUser() {
        String uniqueUser = "loginuser_" + System.currentTimeMillis();
        authService.register(uniqueUser, "test@email.com", "password", "BIDDER");
        User result = authService.login(uniqueUser, "password");
        assertNotNull(result);
        assertEquals(uniqueUser, result.getUsername());
    }
}