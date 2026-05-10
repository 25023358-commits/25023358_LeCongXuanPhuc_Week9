package com.auction.app;

import com.auction.server.AuctionServer;
import com.auction.client.AuctionClient;

import java.util.logging.Logger;
import java.util.logging.Level;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        if (args.length > 0 && "server".equals(args[0])) {
            AuctionServer.main(args);
        } else {
            AuctionClient.main(args);
        }
    }
}