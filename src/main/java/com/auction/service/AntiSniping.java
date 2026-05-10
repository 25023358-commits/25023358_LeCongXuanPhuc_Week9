package src.main.java.com.auction.service;

import java.util.concurrent.*;

// Advanced: Chống thầu chụp giờ cuối
public class AntiSniping {
    private static AntiSniping instance;
    private ConcurrentHashMap<String, Long> auctionEndTime = new ConcurrentHashMap<>();  // mỗi item có 1 thời điểm kết thúc
    private ConcurrentHashMap<String, ScheduledFuture<?>> extendTasks = new ConcurrentHashMap<>();    // dùng để:quản lý task gia hạn cancel task nếu cần, hiện tại: chưa sử dụng
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private AntiSniping() {}

    public static AntiSniping getInstance() {
        if (instance == null) {
            synchronized (AntiSniping.class) {
                if (instance == null) instance = new AntiSniping();
            }
        }
        return instance;
    }

    // Bắt đầu phiên đấu giá với thời gian (giây)
    public void startAuction(String itemId, int durationSeconds) {
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        auctionEndTime.put(itemId, endTime);  // lưu vào map
        System.out.println("🎯 Auction started for " + itemId + " | Ends in " + durationSeconds + "s");
    }

    // Kiểm tra và gia hạn nếu thầu trong 10 giây cuối (chống sniping)
    public boolean checkAndExtend(String itemId) {
        Long endTime = auctionEndTime.get(itemId);
        if (endTime == null) return true;

        long remaining = endTime - System.currentTimeMillis();
        long remainingSeconds = remaining / 1000;

        // Nếu còn ít hơn 10 giây và có thầu mới -> gia hạn thêm 20 giây
        if (remainingSeconds <= 10 && remainingSeconds > 0) {
            long newEndTime = System.currentTimeMillis() + 20000; // +20 giây
            auctionEndTime.put(itemId, newEndTime);
            System.out.println("🛡️ [Anti-Sniping] " + itemId + " extended by 20s!");
            return true;
        }

        // Hết giờ
        if (remaining <= 0) {
            auctionEndTime.remove(itemId);
            System.out.println("🔚 Auction ended for " + itemId);
            return false;
        }

        return true; // đấu giá tiếp tục
    }

    public long getRemainingSeconds(String itemId) {
        Long endTime = auctionEndTime.get(itemId);
        if (endTime == null) return 0;
        return Math.max(0, (endTime - System.currentTimeMillis()) / 1000);
    }

    // === THÊM METHOD NÀY === với mục đích kiểm tra xem còn đấu giá không
    public boolean isAuctionActive(String itemId) {
        Long endTime = auctionEndTime.get(itemId);
        if (endTime == null) return false;
        return getRemainingSeconds(itemId) > 0;
    }
}
