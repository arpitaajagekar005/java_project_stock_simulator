package service;

import model.Stock;
import java.util.List;

/**
 * Interface defining the contract for the Market Engine.
 * MarketServiceImpl implements this interface.
 *
 * WHY AN INTERFACE HERE:
 * Tomorrow if you want a MockMarketService for testing,
 * you just implement this same interface — Main.java never changes.
 */
public interface MarketService {

    void initializeMarket();       // load stocks into the list

    void startMarket();            // start background price updater

    void stopMarket();             // shut down the thread cleanly

    List<Stock> getStocks();       // return the live stock list

    Stock findBySymbol(String symbol);  // search by symbol e.g. "TCS"
}