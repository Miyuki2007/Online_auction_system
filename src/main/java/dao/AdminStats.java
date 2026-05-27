package dao;

import java.io.Serial;
import java.io.Serializable;

public class AdminStats implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int totalUsers;
    private int totalBidders;
    private int totalSellers;
    private int activeUsers;
    private int lockedUsers;

    private int totalAuctions;
    private int runningAuctions;
    private int finishedAuctions;
    private int canceledAuctions;
    private int paidAuctions;

    private double totalVolume;
    private int totalBids;

    public AdminStats(int totalUsers, int totalBidders, int totalSellers, int activeUsers, int lockedUsers,
                      int totalAuctions, int runningAuctions, int finishedAuctions, int canceledAuctions, int paidAuctions,
                      double totalVolume, int totalBids) {
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
        this.totalVolume = totalVolume;
        this.totalBids = totalBids;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getTotalBidders() {
        return totalBidders;
    }

    public void setTotalBidders(int totalBidders) {
        this.totalBidders = totalBidders;
    }

    public int getTotalSellers() {
        return totalSellers;
    }

    public void setTotalSellers(int totalSellers) {
        this.totalSellers = totalSellers;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(int activeUsers) {
        this.activeUsers = activeUsers;
    }

    public int getLockedUsers() {
        return lockedUsers;
    }

    public void setLockedUsers(int lockedUsers) {
        this.lockedUsers = lockedUsers;
    }

    public int getTotalAuctions() {
        return totalAuctions;
    }

    public void setTotalAuctions(int totalAuctions) {
        this.totalAuctions = totalAuctions;
    }

    public int getRunningAuctions() {
        return runningAuctions;
    }

    public void setRunningAuctions(int runningAuctions) {
        this.runningAuctions = runningAuctions;
    }

    public int getFinishedAuctions() {
        return finishedAuctions;
    }

    public void setFinishedAuctions(int finishedAuctions) {
        this.finishedAuctions = finishedAuctions;
    }

    public int getCanceledAuctions() {
        return canceledAuctions;
    }

    public void setCanceledAuctions(int canceledAuctions) {
        this.canceledAuctions = canceledAuctions;
    }

    public int getPaidAuctions() {
        return paidAuctions;
    }

    public void setPaidAuctions(int paidAuctions) {
        this.paidAuctions = paidAuctions;
    }

    public double getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(double totalVolume) {
        this.totalVolume = totalVolume;
    }

    public int getTotalBids() {
        return totalBids;
    }

    public void setTotalBids(int totalBids) {
        this.totalBids = totalBids;
    }
}