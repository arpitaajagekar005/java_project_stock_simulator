package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a registered user of the simulator.
 * Each user owns exactly one Portfolio and has a transaction history.
 */
public class User {

    private int userId;
    private String username;
    private String passwordHash;   // never store plain-text passwords
    private Portfolio portfolio;
    private List<Transaction> transactionHistory;

    public User(int userId, String username, String passwordHash, double startingCash) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.portfolio = new Portfolio(startingCash);       // every user starts with cash
        this.transactionHistory = new ArrayList<>();
    }

    // --- Getters ---

    public int getUserId() { return userId; }

    public String getUsername() { return username; }

    public String getPasswordHash() { return passwordHash; }

    public Portfolio getPortfolio() { return portfolio; }

    public List<Transaction> getTransactionHistory() { return transactionHistory; }

    // --- Utility ---

    /**
     * Adds a completed transaction to this user's history.
     * Called by TradingService after every successful buy/sell.
     */
    public void addTransaction(Transaction t) {
        transactionHistory.add(t);
    }

    @Override
    public String toString() {
        return String.format("User[%d] %s | Cash: ₹%.2f",
                userId, username, portfolio.getCashBalance());
    }
}