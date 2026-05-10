package com.auction.service;

import java.util.*;
import java.util.concurrent.*;

import com.auction.entity.*;

// Thêm realtime mà không ảnh hưởng code cũ
public class RealtimeNotifier {
    private static RealtimeNotifier instance;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);  // newScheduledThreadPool là 1 thread chạy nền để thực hiện task theo thời gian
    private Map<String, List<Bidder>> watchers = new ConcurrentHashMap<>();
    private RealtimeNotifier() {}

    public static RealtimeNotifier getInstance() {
        if (instance == null) {
            synchronized (RealtimeNotifier.class) {
                if (instance == null) instance = new RealtimeNotifier();
            }
        }
        return instance;
    }

    // Đăng ký theo dõi item
    public void watchItem(String itemId, Bidder bidder) {
        watchers.computeIfAbsent(itemId, k -> new CopyOnWriteArrayList<>()).add(bidder);  // computeIfAbsent ở đây nghĩa là nếu itemId chưa tồn tại thì tạo list mới,
                                                                                                    // nếu tồn tại thì sử dụng list cũ, add là thêm người theo dõi vào list.
        // CopyOnWriteArrayList là một List thread-safe trong Java, hoạt động bằng cách “copy toàn bộ mảng” mỗi khi có thay đổi (add/remove)
    }

    // Gửi thông báo realtime (tự động chạy nền)
    public void notifyRealtime(String itemId, double price, String bidderName) {
        List<Bidder> watching = watchers.get(itemId);
        if (watching != null) {
            for (Bidder b : watching) {
                // Gửi thông báo ngay lập tức
                String msg = String.format("[REALTIME] %s: $%.2f by %s", itemId, price, bidderName);
                System.out.println("📡 -> " + b.getUsername() + ": " + msg);
            }
        }
    }

    // Advanced: Đếm ngược thời gian cho item (chạy nền)
    public void startCountdown(String itemId, int seconds, Runnable onEnd) {
        scheduler.schedule(() -> {
            System.out.println("⏰ TIME'S UP for " + itemId);
            onEnd.run();
        }, seconds, TimeUnit.SECONDS);

        // Đếm ngược hiển thị mỗi 5 giây
        for (int i = seconds; i >= 0; i -= 5) {
            final int remaining = i;
            scheduler.schedule(() -> {
                System.out.println("⏳ " + itemId + " ends in " + remaining + "s");
            }, seconds - i, TimeUnit.SECONDS);
        }
    }
}
