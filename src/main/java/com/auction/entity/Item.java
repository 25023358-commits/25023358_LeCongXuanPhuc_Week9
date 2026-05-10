package src.main.java.com.auction.entity;
import java.util.ArrayList;
import java.util.List;

public abstract class Item extends Entity {
    // 1. Giữ đúng chuẩn của Nhật: Dùng private để đảm bảo tính đóng gói (Encapsulation)
    private String name;
    private double startingPrice;
    private double currentHighestBid;
    private String highestBidderId; // Lưu ID người trả giá cao nhất

    // 2. Tích hợp Observer của Tùng: Dùng transient để tránh lỗi khi tuần hóa dữ liệu (nếu có)
    private transient List<AuctionObserver> observers = new ArrayList<>();

    public Item(String id, String name, double startingPrice) {
        super(id);
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice;
    }

    // --- GETTER/SETTER (Giữ chuẩn của Nhật) ---
    public String getName() { return name; }
    public double getStartingPrice() { return startingPrice; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public String getHighestBidderId() { return highestBidderId; }

    public abstract void printInfo();

    // --- LOGIC OBSERVER (Từ bản của Tùng) ---
    public void addObserver(AuctionObserver observer) {
        if (observers == null) observers = new ArrayList<>(); // Đề phòng lỗi null
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    protected void notifyObservers(String message) {
        if (observers != null) {
            for (AuctionObserver obs : observers) {
                obs.update(message);
            }
        }
    }

    /**
     * TỐI ƯU: Kết hợp cả tính an toàn luồng và logic thông báo
     * Trả về boolean để AuctionManager biết thầu có thành công không
     */
    public synchronized boolean updateHighestBid(double newBid, String bidderId) {
        if (newBid > currentHighestBid) {
            this.currentHighestBid = newBid;
            this.highestBidderId = bidderId;

            // Thông báo cho những người đang theo dõi sản phẩm này
            notifyObservers("New highest bid for " + name + ": $" + newBid + " by " + bidderId);
            return true;
        }
        return false;
    }
}
