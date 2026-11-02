package service;

import model.Portfolio;
import model.Stock;
import model.User;

import java.util.List;
import java.util.Map;

/**
 * PortfolioService handles all portfolio-related queries.
 *
 * WHY THIS CLASS EXISTS:
 * TradingService handles buy/sell execution.
 * PortfolioService handles viewing and reporting.
 * Each class has ONE job — Single Responsibility Principle.
 */
public class PortfolioService {

    private final MarketServiceImpl market;

    public PortfolioService(MarketServiceImpl market) {
        this.market = market;
    }

    // ----------------------------------------------------------------
    // Summary
    // ----------------------------------------------------------------

    /**
     * Prints full portfolio summary to console.
     * Shows cash, each holding with P&L, and net worth.
     */
    public void printSummary(User user) {
        List<Stock> liveStocks = market.getStocks();
        Portfolio portfolio = user.getPortfolio();

        System.out.println("\n======= Portfolio: " + user.getUsername() + " =======");
        System.out.printf("  Cash Balance : ₹%.2f%n", portfolio.getCashBalance());

        Map<String, Integer> holdings = portfolio.getHoldings();

        if (holdings.isEmpty()) {
            System.out.println("  Holdings     : (none)");
        } else {
            System.out.printf("%n  %-10s %-6s %-12s %-12s %-12s%n",
                    "Symbol", "Qty", "Avg Cost", "Now", "P&L");
            System.out.println("  " + "-".repeat(56));

            for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
                String symbol = entry.getKey();
                int qty       = entry.getValue();
                double avg    = portfolio.getAvgBuyPrice(symbol);
                double live   = getLivePrice(symbol, liveStocks);
                double pl     = (live - avg) * qty;
                String sign   = pl >= 0 ? "+" : "";

                System.out.printf("  %-10s %-6d ₹%-11.2f ₹%-11.2f %s₹%.2f%n",
                        symbol, qty, avg, live, sign, pl);
            }
        }

        System.out.printf("%n  Net Worth : ₹%.2f%n",
                portfolio.getTotalValue(liveStocks));
        System.out.println("==============================\n");
    }

    // ----------------------------------------------------------------
    // Individual stock P&L
    // ----------------------------------------------------------------

    /**
     * Returns profit or loss for one specific stock holding.
     * Positive = profit. Negative = loss.
     */
    public double getProfitLoss(User user, String symbol) {
        Portfolio portfolio = user.getPortfolio();
        int qty      = portfolio.getQuantity(symbol);
        double avg   = portfolio.getAvgBuyPrice(symbol);
        double live  = getLivePrice(symbol, market.getStocks());
        return (live - avg) * qty;
    }

    // ----------------------------------------------------------------
    // Net worth
    // ----------------------------------------------------------------

    public double getNetWorth(User user) {
        return user.getPortfolio().getTotalValue(market.getStocks());
    }

    // ----------------------------------------------------------------
    // Helper
    // ----------------------------------------------------------------

    private double getLivePrice(String symbol, List<Stock> stocks) {
        for (Stock s : stocks) {
            if (s.getSymbol().equalsIgnoreCase(symbol)) {
                return s.getPrice();
            }
        }
        return 0.0;
    }
}