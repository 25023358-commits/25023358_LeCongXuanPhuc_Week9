package src.test.java.com.auction.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import src.main.java.com.auction.service.DefaultBiddingStrategy;

public class DefaultBiddingStrategyTest {

    private DefaultBiddingStrategy strategy;

    @BeforeEach
    public void setUp() {
        // Khởi tạo đối tượng trước mỗi hàm test
        strategy = new DefaultBiddingStrategy();
    }

    @Test
    public void testValidBid_NewBidIsHigher() {
        // Kịch bản: Giá mới (150) cao hơn giá hiện tại (100) -> Hợp lệ
        boolean result = strategy.isValidBid(150.0, 100.0);
        assertTrue(result, "Giá 150 phải hợp lệ khi giá hiện tại là 100");
    }

    @Test
    public void testInvalidBid_NewBidIsEqual() {
        // Kịch bản: Giá mới bằng giá hiện tại -> Không hợp lệ
        boolean result = strategy.isValidBid(100.0, 100.0);
        assertFalse(result, "Giá thầu bằng giá hiện tại không được phép hợp lệ");
    }

    @Test
    public void testInvalidBid_NewBidIsLower() {
        // Kịch bản: Giá mới thấp hơn giá hiện tại -> Không hợp lệ
        boolean result = strategy.isValidBid(90.0, 100.0);
        assertFalse(result, "Giá thầu thấp hơn giá hiện tại không được phép hợp lệ");
    }
}