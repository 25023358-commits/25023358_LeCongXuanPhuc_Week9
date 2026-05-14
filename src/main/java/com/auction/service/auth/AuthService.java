package com.auction.service.auth;

import com.auction.dao.UserDAO;
import com.auction.entity.user.Admin;
import com.auction.entity.user.Bidder;
import com.auction.entity.user.Seller;
import com.auction.entity.user.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.UUID;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    /**
     * Xác thực đăng nhập bằng cách kiểm tra mật khẩu đã hash bằng BCrypt.
     */
    public User login(String username, String password) {
        try {
            UserDAO.UserRecord record = userDAO.findByUsername(username);
            
            if (record != null) {
                // BACKDOOR CHO ADMIN (Tạm thời để test)
                if ("admin".equals(username) && "admin123".equals(password)) {
                    return new Admin(record.id, record.username);
                }

                // Kiểm tra mật khẩu người dùng nhập vào với mã hash lưu trong DB
                if (BCrypt.checkpw(password, record.hashedPassword)) {
                    // Trả về Object User tương ứng với role
                    switch (record.role) {
                        case "BIDDER":
                            return new Bidder(record.id, record.username, record.balance);
                        case "SELLER":
                            return new Seller(record.id, record.username);
                        case "ADMIN":
                            return new Admin(record.id, record.username);
                        default:
                            return null;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error during login: " + e.getMessage());
        }
        return null;
    }

    /**
     * Đăng ký người dùng mới. Hash mật khẩu trước khi lưu.
     */
    public boolean register(String username, String email, String password, String role) {
        try {
            // Kiểm tra xem username đã tồn tại chưa
            if (userDAO.findByUsername(username) != null) {
                System.out.println("Username already exists.");
                return false;
            }

            // Tạo ID ngẫu nhiên cho user mới (Sửa lỗi: dùng randomUUID() thay vì randomDefault())
            String id = "USR_" + UUID.randomUUID().toString().substring(0, 8);
            
            // Hash mật khẩu bằng thư viện BCrypt
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            
            // Khởi tạo số dư tài khoản (Tăng vốn khởi điểm lên 10000)
            double balance = "BIDDER".equals(role) ? 10000.0 : 0.0;

            // Lưu vào database thông qua DAO
            userDAO.insert(id, username, email, hashedPassword, role, balance);
            
            return true;
            
        } catch (SQLException e) {
            System.err.println("Database error during registration: " + e.getMessage());
            return false;
        }
    }
}