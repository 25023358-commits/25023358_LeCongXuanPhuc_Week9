package com.auction.service;

import com.auction.entity.*;

public class ItemFactory {
    public static Item createItem(String type, String id, String name, double price, int extraAttr) {
        switch (type.toLowerCase()) {    // chuyển chữ cái in hoa về chữ cái thường
            case "electronics":
                return new Electronics(id, name, price, extraAttr);
            case "art":
                return new Art(id, name, price, String.valueOf(extraAttr));  // vì extraAttr là int mà Art cần string nên phải chuyển về String.
            default:
                throw new IllegalArgumentException("Unknown item type");
        }
    }
}
