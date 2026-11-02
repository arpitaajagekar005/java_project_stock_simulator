package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A record of a single buy or sell trade.
 * Created by TradingService and stored in User's history + database later.
 */
public class Transaction {

    // "BUY" or "SELL" — we use a String to keep it simple
    private String type;
    private String symbol;
    private int quantity;
    private double priceAtTime;    // price per share AT the moment of trade
    private double totalAmount;    // priceAtTime × quantity
    private LocalDateTime timestamp;

    public Transaction(String type, String symbol, int quantity, double priceAtTime) {
        this.type = type;
        this.symbol = symbol.toUpperCase();
        this.quantity = quantity;
        this.priceAtTime = priceAtTime;
        this.totalAmount = priceAtTime * quantity;
        this.timestamp = LocalDateTime.now();   // captured automatically
    }

    // --- Getters ---

    public String getType()          { return type; }
    public String getSymbol()        { return symbol; }
    public int getQuantity()         { return quantity; }
    public double getPriceAtTime()   { return priceAtTime; }
    public double getTotalAmount()   { return totalAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return String.format("[%s] %s | %s × %d @ ₹%.2f = ₹%.2f",
                timestamp.format(fmt), type, symbol,
                quantity, priceAtTime, totalAmount);
    }
}