package com.auction.service;

import com.auction.entity.*;

public class ItemFactory {
    public static Item createItem(String type, String id, String name, double price, Object... extraAttrs) {
        switch (type.toLowerCase()) {
            case "electronics":
                return new Electronics(id, name, price, (Integer) extraAttrs[0]);
            case "art":
                return new Art(id, name, price, (String) extraAttrs[0]);
            default:
                throw new IllegalArgumentException("Unknown item type");
        }
    }
}