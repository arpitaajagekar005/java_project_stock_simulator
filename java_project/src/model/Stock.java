package model;

/**
 * Represents a single stock in the market.
 * This is a POJO (Plain Old Java Object) — just data + getters/setters.
 */
public class Stock {

    private String symbol;       // e.g. "AAPL"
    private String companyName;  // e.g. "Apple Inc."
    private double price;        // current market price

    public Stock(String symbol, String companyName, double initialPrice) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.price = initialPrice;
    }

    // --- Getters ---

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public double getPrice() {
        return price;
    }

    // --- Setter (only price changes; symbol/name are fixed) ---

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return String.format("%-6s | %-20s | ₹%.2f", symbol, companyName, price);
    }
}