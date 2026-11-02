package service;

import model.Portfolio;
import model.Stock;
import model.Transaction;
import model.User;
import util.LoggerUtil;


import java.util.List;
import java.util.logging.Logger;

/**
 * Handles all buy and sell operations.
 *
 * RULE: This class NEVER touches cash or shares directly.
 * It only validates, then calls Portfolio methods to do the actual work.
 * This keeps responsibilities clean and easy to test.
 */
public class TradingService {

    private static final Logger logger = LoggerUtil.getLogger();
    private final MarketServiceImpl market;
    private final DatabaseService db;           

    public TradingService(MarketServiceImpl market, DatabaseService db) {
        this.market = market;
        this.db = db;

    }

    // ----------------------------------------------------------------
    // BUY
    // ----------------------------------------------------------------

    /**
     * Attempts to buy `quantity` shares of `symbol` for `user`.
     *
     * Validation order (fail fast — stop at first problem):
     *   1. Does the symbol exist in the market?
     *   2. Is quantity a positive number?
     *   3. Does the user have enough cash?
     *
     * @return true if the trade succeeded, false if any check failed
     */
    public boolean buy(User user, String symbol, int quantity) {

        // --- Step 1: Find the stock ---
        Stock stock = market.findBySymbol(symbol);
        if (stock == null) {
            System.out.println("[ERROR] Stock symbol '" + symbol + "' not found in market.");
            return false;
        }

        // --- Step 2: Validate quantity ---
        if (quantity <= 0) {
            System.out.println("[ERROR] Quantity must be greater than 0.");
            return false;
        }

        // --- Step 3: Check affordability ---
        // Use stock.getPrice() at this exact moment (live price from the background thread)
        double totalCost = stock.getPrice() * quantity;
        Portfolio portfolio = user.getPortfolio();

        if (portfolio.getCashBalance() < totalCost) {
            System.out.printf("[ERROR] Insufficient funds.%n" +
                            "        Required : ₹%.2f%n" +
                            "        Available: ₹%.2f%n",
                    totalCost, portfolio.getCashBalance());
            return false;
        }

        // --- All checks passed — execute the trade ---
        portfolio.deductCash(totalCost);
        portfolio.addShares(symbol, quantity, stock.getPrice());

        // Record the transaction
        Transaction t = new Transaction("BUY", symbol, quantity, stock.getPrice());
        user.addTransaction(t);
        db.saveTransaction(user.getUserId(), t);
        db.updateCashBalance(user);  


        // Log and confirm
        logger.info("BUY executed: " + t);
        System.out.printf("[SUCCESS] Bought %d shares of %s at ₹%.2f | Total: ₹%.2f%n",
                quantity, symbol.toUpperCase(), stock.getPrice(), totalCost);
        System.out.printf("          Remaining Cash: ₹%.2f%n%n", portfolio.getCashBalance());

        return true;
    }

    // ----------------------------------------------------------------
    // SELL
    // ----------------------------------------------------------------

    /**
     * Attempts to sell `quantity` shares of `symbol` for `user`.
     *
     * Validation order:
     *   1. Does the symbol exist in the market?
     *   2. Is quantity a positive number?
     *   3. Does the user own enough shares to sell?
     *
     * @return true if trade succeeded, false if any check failed
     */
    public boolean sell(User user, String symbol, int quantity) {

        // --- Step 1: Find the stock ---
        Stock stock = market.findBySymbol(symbol);
        if (stock == null) {
            System.out.println("[ERROR] Stock symbol '" + symbol + "' not found in market.");
            return false;
        }

        // --- Step 2: Validate quantity ---
        if (quantity <= 0) {
            System.out.println("[ERROR] Quantity must be greater than 0.");
            return false;
        }

        // --- Step 3: Check ownership ---
        Portfolio portfolio = user.getPortfolio();
        int owned = portfolio.getQuantity(symbol);

        if (owned < quantity) {
            System.out.printf("[ERROR] Not enough shares to sell.%n" +
                            "        Trying to sell: %d%n" +
                            "        You own       : %d%n",
                    quantity, owned);
            return false;
        }

        // --- All checks passed — execute the trade ---
        double totalEarned = stock.getPrice() * quantity;
        portfolio.removeShares(symbol, quantity);
        portfolio.addCash(totalEarned);

        // Record the transaction
        Transaction t = new Transaction("SELL", symbol, quantity, stock.getPrice());
        user.addTransaction(t);
        db.saveTransaction(user.getUserId(), t);
        db.updateCashBalance(user);

        // Log and confirm
        logger.info("SELL executed: " + t);
        System.out.printf("[SUCCESS] Sold %d shares of %s at ₹%.2f | Earned: ₹%.2f%n",
                quantity, symbol.toUpperCase(), stock.getPrice(), totalEarned);
        System.out.printf("          New Cash Balance: ₹%.2f%n%n", portfolio.getCashBalance());

        return true;
    }

    // ----------------------------------------------------------------
    // View transaction history
    // ----------------------------------------------------------------

    /**
     * Prints all trades the user has made in this session.
     */
    public void printHistory(User user) {
        List<Transaction> history = user.getTransactionHistory();

        System.out.println("\n====== Trade History for " + user.getUsername() + " ======");
        if (history.isEmpty()) {
            System.out.println("  No trades yet.");
        } else {
            for (Transaction t : history) {
                System.out.println("  " + t);
            }
        }
        System.out.println("===========================================\n");
    }
}