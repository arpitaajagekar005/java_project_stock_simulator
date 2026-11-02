package main;

import model.User;
import service.DatabaseService;
import service.MarketServiceImpl;
import service.PortfolioService;
import service.TradingService;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        // Start the market engine
        MarketServiceImpl market = new MarketServiceImpl();
        market.initializeMarket();
        market.startMarket();

        // Create one user with ₹50,000 starting cash
        User user = new User(1, "Rahul", "hashed_pw", 50000.0);
        
        DatabaseService db = new DatabaseService();   
        db.connect();
        db.saveUser(user);
        market.setDatabaseService(db);   
        market.startMarket();

        // Trading service wired to the market
        TradingService tradingService = new TradingService(market, db);
        PortfolioService portfolioService = new PortfolioService(market);  

        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Stock Trading Simulator ===");
        System.out.println("Commands:");
        System.out.println("  LIST");
        System.out.println("  BUY <SYMBOL> <QTY>       e.g. BUY TCS 10");
        System.out.println("  SELL <SYMBOL> <QTY>      e.g. SELL TCS 5");
        System.out.println("  PORTFOLIO");
        System.out.println("  HISTORY");
        System.out.println("  EXIT");
        System.out.println("================================\n");

        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) break;   // ← ADD THIS LINE
            String input = scanner.nextLine().trim().toUpperCase();
            String[] parts = input.split("\\s+");   // split on any whitespace

            switch (parts[0]) {

                case "LIST":
                    System.out.println("\n--- Live Market Prices ---");
                    for (var s : market.getStocks()) System.out.println(s);
                    System.out.println("--------------------------\n");
                    break;

                case "BUY":
                    // Expects: BUY <SYMBOL> <QTY>
                    if (parts.length < 3) {
                        System.out.println("Usage: BUY <SYMBOL> <QTY>\n");
                        break;
                    }
                    try {
                        String symbol = parts[1];
                        int qty = Integer.parseInt(parts[2]);
                        tradingService.buy(user, symbol, qty);
                    } catch (NumberFormatException e) {
                        System.out.println("[ERROR] Quantity must be a whole number.\n");
                    }
                    break;

                case "SELL":
                    // Expects: SELL <SYMBOL> <QTY>
                    if (parts.length < 3) {
                        System.out.println("Usage: SELL <SYMBOL> <QTY>\n");
                        break;
                    }
                    try {
                        String symbol = parts[1];
                        int qty = Integer.parseInt(parts[2]);
                        tradingService.sell(user, symbol, qty);
                    } catch (NumberFormatException e) {
                        System.out.println("[ERROR] Quantity must be a whole number.\n");
                    }
                    break;

                case "PORTFOLIO":
                    portfolioService.printSummary(user);
                    break;

                case "HISTORY":
                    tradingService.printHistory(user);
                    break;

                case "EXIT":
                    
                    market.stopMarket();
                    System.out.println("Goodbye!");
                    scanner.close();
                    db.disconnect();
                    return;

                case "DBHISTORY":
                    db.printTransactionsFromDB(user.getUserId());
                    break;

                case "ADDBALANCE":
                    // usage: ADDBALANCE 100000
                    if (parts.length < 2) {
                        System.out.println("Usage: ADDBALANCE <amount>\n");
                        break;
                    }
                    try {
                        double amount = Double.parseDouble(parts[1]);
                        user.getPortfolio().addCash(amount);
                        db.updateCashBalance(user);   // saves to MySQL too
                        System.out.printf("[SUCCESS] Added ₹%.2f | New Balance: ₹%.2f%n%n",
                                amount, user.getPortfolio().getCashBalance());
                    } catch (NumberFormatException e) {
                        System.out.println("[ERROR] Amount must be a number.\n");
                    }
                    break;

                default:
                    System.out.println("Unknown command. Type LIST, BUY, SELL, PORTFOLIO, HISTORY, or EXIT.\n");

                scanner.close();
            }
        }
    }
}