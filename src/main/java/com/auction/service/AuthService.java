package src.main.java.com.auction.service;

import src.main.java.com.auction.dao.UserDAO;
import src.main.java.com.auction.entity.User;

public class AuthService {
    private UserDAO userDAO = new UserDAO();

    public User login(String username, String password) {
        User user = userDAO.getUserByUsername(username);
        if (user != null && "password".equals(password)) { // Simple check, in real hash
            return user;
        }
        return null;
    }

    public boolean register(User user) {
        userDAO.saveUser(user);
        return true;
    }
}
