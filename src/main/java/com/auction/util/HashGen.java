package com.auction.util;

import org.mindrot.jbcrypt.BCrypt;

public class HashGen {
    public static void main(String[] args) {
        String pw = "admin123";
        String hashed = BCrypt.hashpw(pw, BCrypt.gensalt());
        System.out.println("Hash for admin123: " + hashed);
    }
}
