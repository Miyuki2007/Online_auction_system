package dao;
import java.io.Serializable;
public class AdminStats implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int totalUsers;
    private final int totalBidders;
    private final int totalSellers;
    private final int activeUsers;
    private final int lockedUsers;

    private final int totalAuctions;
    private final int runningAuctions;
    private final int finishedAuctions;
    private final int canceledAuctions;
    private final int paidAuctions;

    private final double totalBidVolume;
    private final int totalBidCount;

    public AdminStats(int totalUsers, int totalBidders, int totalSellers, int activeUsers, int lockedUsers, int totalAuctions, int runningAuctions, int finishedAuctions, int canceledAuctions, int paidAuctions, double totalBidVolume, int totalBidCount) {
        this.totalUsers = totalUsers;
        this.totalBidders = totalBidders;
        this.totalSellers = totalSellers;
        this.activeUsers = activeUsers;
        this.lockedUsers = lockedUsers;
        this.totalAuctions = totalAuctions;
        this.runningAuctions = runningAuctions;
        this.finishedAuctions = finishedAuctions;
        this.canceledAuctions = canceledAuctions;
        this.paidAuctions = paidAuctions;
        this.totalBidVolume = totalBidVolume;
        this.totalBidCount = totalBidCount;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public int getTotalBidders() {
        return totalBidders;
    }

    public int getTotalSellers() {
        return totalSellers;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public int getLockedUsers() {
        return lockedUsers;
    }

    public int getTotalAuctions() {
        return totalAuctions;
    }

    public int getRunningAuctions() {
        return runningAuctions;
    }

    public int getFinishedAuctions() {
        return finishedAuctions;
    }

    public int getCanceledAuctions() {
        return canceledAuctions;
    }

    public int getPaidAuctions() {
        return paidAuctions;
    }

    public double getTotalBidVolume() {
        return totalBidVolume;
    }

    public int getTotalBidCount() {
        return totalBidCount;
    }
}
