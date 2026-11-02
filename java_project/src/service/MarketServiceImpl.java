package service;

import model.Stock;
import thread.PriceUpdaterThread;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The Market Engine.
 *
 * Uses ScheduledExecutorService to fire PriceUpdaterThread every 2 seconds.
 * The stock list is a CopyOnWriteArrayList — this means:
 *   - Multiple threads can READ it simultaneously without locking.
 *   - When a thread WRITES to it, Java internally copies the whole array,
 *     modifies the copy, then replaces the original. This is safe but
 *     slightly slow — fine for a small list of ~10 stocks.
 */
public class MarketServiceImpl implements MarketService {

    // Thread-safe list shared between the main thread and the updater thread
    private final List<Stock> stocks = new CopyOnWriteArrayList<>();

    // The scheduler — uses 1 background thread
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    /**
     * Seeds the market with some default stocks.
     * Call this once at startup.
     */
    public void initializeMarket() {
        stocks.add(new Stock("RELIANCE", "Reliance Industries",  2800.00));
        stocks.add(new Stock("TCS",      "Tata Consultancy Svc", 3900.00));
        stocks.add(new Stock("INFY",     "Infosys Ltd.",         1600.00));
        stocks.add(new Stock("HDFC",     "HDFC Bank Ltd.",       1700.00));
        stocks.add(new Stock("WIPRO",    "Wipro Ltd.",            560.00));
    }

    /**
     * Starts the background price updater.
     * initialDelay = 0  → first run immediately
     * period       = 2  → then every 2 seconds after that
     */
    private DatabaseService db;   
    
    public void setDatabaseService(DatabaseService db) {
        this.db = db;
    }

    public void startMarket() {
        
        PriceUpdaterThread updater = new PriceUpdaterThread(stocks, db);
        scheduler.scheduleAtFixedRate(updater, 0, 2, TimeUnit.SECONDS);
        System.out.println("[Market] Price engine started. Updates every 2 seconds.\n");
    }

    /**
     * Gracefully shuts down the background thread.
     * Call this when the user exits the app.
     */
    public void stopMarket() {
        scheduler.shutdown();
        System.out.println("[Market] Price engine stopped.");
    }

    /**
     * Returns the live stock list.
     * Safe to read from the main thread at any time.
     */
    public List<Stock> getStocks() {
        return stocks;
    }

    /**
     * Find a stock by its symbol (case-insensitive).
     * Returns null if not found.
     */
    public Stock findBySymbol(String symbol) {
        for (Stock s : stocks) {
            if (s.getSymbol().equalsIgnoreCase(symbol)) {
                return s;
            }
        }
        return null;
    }
}