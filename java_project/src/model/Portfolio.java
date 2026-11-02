package model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks a user's cash balance and stock holdings.
 *
 * holdings is a HashMap where:
 *   KEY   = stock symbol (String)  e.g. "TCS"
 *   VALUE = number of shares owned (Integer) e.g. 10
 *
 * Example after buying 10 TCS and 5 INFY:
 *   holdings = { "TCS" -> 10, "INFY" -> 5 }
 */
public class Portfolio {

    private double cashBalance;
    private Map<String, Integer> holdings;   // symbol -> quantity
    private Map<String, Double> avgBuyPrice;

    public Portfolio(double startingCash) {
        this.cashBalance = startingCash;
        this.holdings = new HashMap<>();
        this.avgBuyPrice = new HashMap<>();
    }

    // ----------------------------------------------------------------
    // Cash operations
    // ----------------------------------------------------------------

    public double getCashBalance() {
        return cashBalance;
    }

    /**
     * Deducts cash when user buys shares.
     * TradingService checks affordability BEFORE calling this.
     */
    public void deductCash(double amount) {
        this.cashBalance -= amount;
    }

    /**
     * Adds cash when user sells shares.
     */
    public void addCash(double amount) {
        this.cashBalance += amount;
    }

    // ----------------------------------------------------------------
    // Holdings operations
    // ----------------------------------------------------------------

    public Map<String, Integer> getHoldings() {
        return holdings;
    }

    /**
     * Returns how many shares of a symbol the user owns.
     * Returns 0 if they own none (getOrDefault avoids null checks).
     */
    public int getQuantity(String symbol) {
        return holdings.getOrDefault(symbol.toUpperCase(), 0);
    }

    /**
     * Adds shares after a successful BUY.
     * If the user already owns some, this adds to the existing quantity.
     */
    public void addShares(String symbol, int quantity, double buyPrice) {
        symbol = symbol.toUpperCase();
        int currentQty = holdings.getOrDefault(symbol, 0);
        double currentAvg = avgBuyPrice.getOrDefault(symbol, 0.0);
    
        // Weighted average formula:
        // newAvg = (currentQty × currentAvg + newQty × newPrice) ÷ totalQty
        // Example: owned 5 @ ₹3800, buying 5 more @ ₹4000
        // newAvg = (5×3800 + 5×4000) ÷ 10 = ₹3900
        int newQty = currentQty + quantity;
        double newAvg = ((currentQty * currentAvg) + (quantity * buyPrice)) / newQty;
    
        holdings.put(symbol, newQty);
        avgBuyPrice.put(symbol, newAvg);
    }

    public double getAvgBuyPrice(String symbol) {
        return avgBuyPrice.getOrDefault(symbol.toUpperCase(), 0.0);
    }

    /**
     * Removes shares after a successful SELL.
     * If quantity drops to 0, removes the entry from the map entirely
     * so the portfolio stays clean.
     */
public void removeShares(String symbol, int quantity) {
    symbol = symbol.toUpperCase();
    int current = holdings.getOrDefault(symbol, 0);
    int remaining = current - quantity;

    if (remaining <= 0) {
        holdings.remove(symbol);
        avgBuyPrice.remove(symbol);    // ← clean up avg too
    } else {
        holdings.put(symbol, remaining);
        // avg buy price doesn't change when selling
    }
}

    // ----------------------------------------------------------------
    // Portfolio valuation
    // ----------------------------------------------------------------

    /**
     * Calculates the total market value of all stocks held.
     * Loops through holdings, looks up each stock's live price.
     *
     * @param liveStocks the current list from MarketServiceImpl
     * @return total value in rupees (does NOT include cash)
     */
    public double getStockValue(List<Stock> liveStocks) {
        double total = 0.0;

        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            String symbol = entry.getKey();
            int qty = entry.getValue();

            // Find the matching stock in the live list
            for (Stock stock : liveStocks) {
                if (stock.getSymbol().equalsIgnoreCase(symbol)) {
                    total += stock.getPrice() * qty;
                    break;
                }
            }
        }

        return total;
    }

    /**
     * Net worth = cash on hand + market value of all stocks.
     */
    public double getTotalValue(List<Stock> liveStocks) {
        return cashBalance + getStockValue(liveStocks);
    }

    public void setCashBalance(double amount) {
        this.cashBalance = amount;
    }

    // ----------------------------------------------------------------
    // Display
    // ----------------------------------------------------------------

    /**
     * Prints a formatted portfolio summary to the console.
     * Pass liveStocks so we can show current price per holding.
     */
    public void printSummary(List<Stock> liveStocks) {
        System.out.println("\n====== Portfolio Summary ======");
        System.out.printf("  Cash Balance : ₹%.2f%n", cashBalance);
        System.out.println("  Holdings:");
    
        if (holdings.isEmpty()) {
            System.out.println("    (no stocks owned)");
        } else {
            System.out.printf("  %-10s %-8s %-12s %-12s %-12s %-10s%n",
                    "Symbol", "Qty", "Avg Cost", "Price Now", "P&L/Share", "Total P&L");
            System.out.println("  " + "-".repeat(68));
    
            for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
                String symbol = entry.getKey();
                int qty = entry.getValue();
                double avgCost = avgBuyPrice.getOrDefault(symbol, 0.0);
                double livePrice = 0.0;
    
                for (Stock s : liveStocks) {
                    if (s.getSymbol().equalsIgnoreCase(symbol)) {
                        livePrice = s.getPrice();
                        break;
                    }
                }
    
                double plPerShare = livePrice - avgCost;
                double totalPL = plPerShare * qty;
                String plSign = totalPL >= 0 ? "+" : "";   // adds + for profit
    
                System.out.printf("  %-10s %-8d ₹%-11.2f ₹%-11.2f %s₹%-10.2f %s₹%.2f%n",
                        symbol, qty, avgCost, livePrice,
                        plSign, plPerShare, plSign, totalPL);
            }
        }
    
        System.out.printf("%n  Total Net Worth : ₹%.2f%n", getTotalValue(liveStocks));
        System.out.println("================================\n");
    }
}