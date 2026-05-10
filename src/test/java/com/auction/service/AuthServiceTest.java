package src.test.java.com.auction.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import src.main.java.com.auction.service.AuthService;
public class AuthServiceTest {
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
    }

    @Test
    void testRegisterUser() {
        boolean result = authService.register("testuser", "password", "BIDDER");
        assertTrue(result);
    }

    @Test
    void testLoginUser() {
        authService.register("testuser", "password", "BIDDER");
        boolean result = authService.login("testuser", "password");
        assertTrue(result);
    }
}
