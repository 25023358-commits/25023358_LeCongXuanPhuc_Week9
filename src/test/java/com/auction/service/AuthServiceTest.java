package com.auction.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.auction.service.AuthService;
import com.auction.entity.Bidder;
import com.auction.entity.User;
public class AuthServiceTest {
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
    }

    @Test
    void testRegisterUser() {
        Bidder bidder = new Bidder("1", "testuser", 100.0); // Tạo Bidder với ID, username và balance
        boolean result = authService.register(bidder);
        assertTrue(result);
    }

    @Test
    void testLoginUser() {
        Bidder bidder = new Bidder("1", "testuser", 100.0);
        authService.register(bidder);
        User result = authService.login("testuser", "password");
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }
}