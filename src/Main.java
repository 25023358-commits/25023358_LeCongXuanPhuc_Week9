package src;

public class Main {
    public static void main(String[] args) {
        AuctionManager manager = AuctionManager.getInstance();

        // Khởi tạo AutoBidder (truyền manager vào constructor)
        AutoBidder autoBidder = new AutoBidder(manager);

        // Tạo sản phẩm
        Item laptop = new Electronics("L01", "MacBook Pro M3", 1500.0, 12);
        manager.addItem(laptop);

        // Tạo bidder
        Bidder nhat = new Bidder("U01", "Nhat_Leader", 5000.0);
        Bidder tung = new Bidder("U02", "Tung_Design", 4000.0);
        Bidder phuc = new Bidder("U03", "Phuc_Thread", 3000.0);
        Bidder quang = new Bidder("U04", "Quang_New", 4500.0);

        // Đăng ký observer
        manager.addObserver(nhat);
        manager.addObserver(tung);
        manager.addObserver(phuc);
        manager.addObserver(quang);

        // === ĐĂNG KÝ AUTO-BID (theo format của bạn) ===
        System.out.println("\n========== ĐĂNG KÝ AUTO-BID ==========");
        autoBidder.register("Tung_Design", "L01", 2800.0, 80.0);
        autoBidder.register("Phuc_Thread", "L01", 2500.0, 50.0);
        autoBidder.register("Quang_New", "L01", 4000.0, 100.0);
        System.out.println("=======================================\n");

        // Bắt đầu đếm ngược và theo dõi realtime
        manager.startCountdown("L01", 60);
        manager.watchItem("L01", nhat);
        manager.watchItem("L01", tung);
        manager.watchItem("L01", phuc);
        manager.watchItem("L01", quang);

        System.out.println("--- BẮT ĐẦU PHIÊN ĐẤU GIÁ ---");
        laptop.printInfo();
        System.out.println("📌 Auto-bid đã đăng ký:");
        System.out.println("   • Tung_Design: max=$2800, step=$80");
        System.out.println("   • Phuc_Thread: max=$2500, step=$50");
        System.out.println("   • Quang_New: max=$4000, step=$100\n");

        // === ĐẤU GIÁ ĐA LUỒNG ===
        // Thread 1: Tung đặt giá thủ công
        Thread thread1 = new Thread(() -> {
            System.out.println("👤 [Manual] Tung_Design đang đặt giá...");
            manager.placeBid("L01", "Tung_Design", 1600.0);
        });

        // Thread 2: Phúc đặt giá thủ công
        Thread thread2 = new Thread(() -> {
            System.out.println("👤 [Manual] Phuc_Thread đang đặt giá...");
            manager.placeBid("L01", "Phuc_Thread", 1700.0);
        });

        // Thread 3: Nhật đặt giá thủ công (không auto-bid)
        Thread thread3 = new Thread(() -> {
            System.out.println("👤 [Manual] Nhat_Leader đang đặt giá...");
            manager.placeBid("L01", "Nhat_Leader", 1800.0);
        });

        // Thread 4: Quang đặt giá thủ công
        Thread thread4 = new Thread(() -> {
            System.out.println("👤 [Manual] Quang_New đang đặt giá...");
            manager.placeBid("L01", "Quang_New", 1900.0);
        });

        // Chạy tất cả các luồng
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        // Đợi tất cả luồng chạy xong
        try {
            thread1.join();
            thread2.join();
            thread3.join();
            thread4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Đợi thêm 1 giây để auto-bid kịp kích hoạt (nếu có)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // === KẾT QUẢ ===
        System.out.println("\n--- KẾT THÚC PHIÊN ĐẤU GIÁ ---");
        System.out.println("💰 Giá cuối cùng: $" + laptop.getCurrentHighestBid());
        System.out.println("🏆 Người thắng: " + laptop.getHighestBidderId());

        // === THỐNG KÊ ===
        System.out.println("\n========== THỐNG KÊ ==========");
        manager.showAnalytics("L01");
        System.out.println("⏰ Time remaining: " + manager.getRemainingTime("L01") + "s");

        // In lịch sử giao dịch
        manager.printTransactionHistory("L01");
    }
}