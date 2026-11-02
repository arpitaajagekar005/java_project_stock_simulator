package thread;

import model.Stock;
import service.DatabaseService;

import java.util.List;
import java.util.Random;

/**
 * This Runnable is handed to ScheduledExecutorService.
 * It runs every 2 seconds ON A BACKGROUND THREAD — completely separate
 * from the main thread where the user types commands.
 *
 * KEY CONCEPT: The user's Scanner (console input) blocks the main thread
 * while waiting for input. That's fine — the background thread runs
 * independently and keeps updating prices while the user is typing.
 */
public class PriceUpdaterThread implements Runnable {

    private final List<Stock> stocks;
    private final Random random = new Random();
    private final DatabaseService db; 

    // Max percentage the price can swing up or down each tick
    private static final double MAX_CHANGE_PERCENT = 2.0;

    public PriceUpdaterThread(List<Stock> stocks, DatabaseService db) {
        this.stocks = stocks;
        this.db = db;
    }

@Override
    public void run() {
        for (Stock stock : stocks) {
            double changePercent = (random.nextDouble() * 2 - 1) * MAX_CHANGE_PERCENT;
            double oldPrice = stock.getPrice();
            double newPrice = oldPrice + (oldPrice * changePercent / 100.0);
            newPrice = Math.max(newPrice, 1.00);
            newPrice = Math.round(newPrice * 100.0) / 100.0;
            stock.setPrice(newPrice);

            db.saveMarketPrice(stock.getSymbol(), newPrice);   // ← ADD THIS
        }
    }
}