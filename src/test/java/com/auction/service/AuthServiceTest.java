package com.auction.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import src.main.java.com.auction.service.AuthService;
import src.main.java.com.auction.entity.Bidder;

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
        src.main.java.com.auction.entity.User result = authService.login("testuser", "password");
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }
}