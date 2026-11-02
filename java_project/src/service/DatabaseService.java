package service;

import model.Transaction;
import model.User;
import util.LoggerUtil;

import java.sql.*;
import java.util.logging.Logger;

/**
 * Handles all communication between Java and MySQL.
 *
 * Responsibilities:
 *   1. Open and close the database connection
 *   2. Save a new user to the users table
 *   3. Save a transaction every time a trade is executed
 *   4. Load a user's cash balance back from the DB on login
 *
 * JDBC flow every time:
 *   Connection → PreparedStatement → execute → close
 */
public class DatabaseService {

    /**
 * Records one price snapshot per stock per tick.
 * Called by PriceUpdaterThread every 2 seconds.
 * GUI will later query this to draw line charts.
 */
public void saveMarketPrice(String symbol, double price) {
    if (connection == null) return;   // safety check

    String sql = "INSERT INTO market_history (symbol, price) VALUES (?, ?)";

    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
        stmt.setString(1, symbol.toUpperCase());
        stmt.setDouble(2, price);
        stmt.executeUpdate();
    } catch (SQLException e) {
        logger.severe("saveMarketPrice failed: " + e.getMessage());
    }
}

    private static final Logger logger = LoggerUtil.getLogger();

    // --- Connection details — change password to your MySQL root password ---
    private static final String URL      = "jdbc:mysql://localhost:3306/stock_simulator";
    private static final String DB_USER  = "root";
    private static final String DB_PASS  = "Ajagekar2005";   // ← change this

    private Connection connection;

    // ----------------------------------------------------------------
    // Connect / Disconnect
    // ----------------------------------------------------------------

    /**
     * Opens a connection to MySQL.
     * Call this once at app startup.
     */
    public void connect() {
        try {
            // DriverManager uses the JDBC URL to find and open the connection
            connection = DriverManager.getConnection(URL, DB_USER, DB_PASS);
            System.out.println("[DB] Connected to MySQL successfully.");
            logger.info("Database connection established.");
        } catch (SQLException e) {
            System.out.println("[DB ERROR] Could not connect to MySQL: " + e.getMessage());
            logger.severe("DB connection failed: " + e.getMessage());
        }
    }

    /**
     * Closes the connection cleanly.
     * Call this when the user types EXIT.
     */
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
                logger.info("Database connection closed.");
            }
        } catch (SQLException e) {
            logger.severe("Error closing DB: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Save User
    // ----------------------------------------------------------------

    /**
     * Inserts a new user into the users table.
     * Call this once when creating a new user for the first time.
     *
     * PreparedStatement (?) is used instead of String concatenation
     * to prevent SQL Injection — IMPORTANT for viva.
     */
    public void saveUser(User user) {
    
        // STEP 1 — check if user already exists
        String checkSql = "SELECT COUNT(*) FROM users WHERE user_id = ?";
    
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setInt(1, user.getUserId());
            ResultSet rs = checkStmt.executeQuery();
    
            if (rs.next() && rs.getInt(1) > 0) {
                // User already exists — load their cash balance from DB
                System.out.println("[DB] Welcome back, " + user.getUsername() + "!");
                loadCashBalance(user);
                return;   // skip insert
            }
    
        } catch (SQLException e) {
            logger.severe("User check failed: " + e.getMessage());
            return;
        }
    
        // STEP 2 — user is new, insert them
        String sql = "INSERT INTO users (user_id, username, password_hash, cash_balance) " +
                     "VALUES (?, ?, ?, ?)";
    
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt   (1, user.getUserId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getPasswordHash());
            stmt.setDouble(4, user.getPortfolio().getCashBalance());
            stmt.executeUpdate();
            System.out.println("[DB] New user '" + user.getUsername() + "' saved.");
            logger.info("New user saved: " + user.getUsername());
    
        } catch (SQLException e) {
            logger.severe("saveUser failed: " + e.getMessage());
        }
    }
    
    /**
     * Loads the saved cash balance from DB back into the user's portfolio.
     * So if they had ₹32,000 last session, they start with ₹32,000 this session.
     */
    private void loadCashBalance(User user) {
        String sql = "SELECT cash_balance FROM users WHERE user_id = ?";
    
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user.getUserId());
            ResultSet rs = stmt.executeQuery();
    
            if (rs.next()) {
                double savedBalance = rs.getDouble("cash_balance");
                user.getPortfolio().setCashBalance(savedBalance);
                System.out.println("[DB] Cash balance restored: ₹" +
                        String.format("%.2f", savedBalance));
            }
        } catch (SQLException e) {
            logger.severe("loadCashBalance failed: " + e.getMessage());
        }
    }
    // ----------------------------------------------------------------
    // Save Transaction
    // ----------------------------------------------------------------

    /**
     * Inserts one trade record into the transactions table.
     * Called by TradingService after every successful buy or sell.
     *
     * @param userId the ID of the user who made the trade
     * @param t      the Transaction object created by TradingService
     */
    public void saveTransaction(int userId, Transaction t) {
        String sql = "INSERT INTO transactions " +
                     "(user_id, type, symbol, quantity, price_at_time, total_amount) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt   (1, userId);
            stmt.setString(2, t.getType());
            stmt.setString(3, t.getSymbol());
            stmt.setInt   (4, t.getQuantity());
            stmt.setDouble(5, t.getPriceAtTime());
            stmt.setDouble(6, t.getTotalAmount());

            stmt.executeUpdate();
            logger.info("Transaction saved to DB: " + t);

        } catch (SQLException e) {
            System.out.println("[DB ERROR] Could not save transaction: " + e.getMessage());
            logger.severe("saveTransaction failed: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Update cash balance
    // ----------------------------------------------------------------

    /**
     * Syncs the user's current cash balance back to the database.
     * Call this after every trade so the DB stays up to date.
     */
    public void updateCashBalance(User user) {
        String sql = "UPDATE users SET cash_balance = ? WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setDouble(1, user.getPortfolio().getCashBalance());
            stmt.setInt   (2, user.getUserId());

            stmt.executeUpdate();
            logger.info("Cash balance updated for user: " + user.getUsername());

        } catch (SQLException e) {
            System.out.println("[DB ERROR] Could not update cash: " + e.getMessage());
            logger.severe("updateCashBalance failed: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Print all transactions from DB (bonus command)
    // ----------------------------------------------------------------

    /**
     * Reads ALL transactions for a user directly from the database
     * and prints them. This proves data is actually being persisted.
     */
    public void printTransactionsFromDB(int userId) {
        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY created_at";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            System.out.println("\n====== Transactions from Database ======");
            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.printf("  [%s] %s | %s × %d @ ₹%.2f = ₹%.2f%n",
                        rs.getString("created_at"),
                        rs.getString("type"),
                        rs.getString("symbol"),
                        rs.getInt("quantity"),
                        rs.getDouble("price_at_time"),
                        rs.getDouble("total_amount"));
            }

            if (!found) System.out.println("  No transactions found in database.");
            System.out.println("========================================\n");

        } catch (SQLException e) {
            System.out.println("[DB ERROR] Could not read transactions: " + e.getMessage());
            logger.severe("printTransactionsFromDB failed: " + e.getMessage());
        }
    }
}