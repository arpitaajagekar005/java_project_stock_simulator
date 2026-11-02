package main;

import model.User;
import model.Stock;
import model.Transaction;
import service.DatabaseService;
import service.MarketServiceImpl;
import service.TradingService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Main GUI window for the Stock Trading Simulator.
 *
 * Swing hierarchy in this file:
 *   JFrame (the window)
 *     └── JPanel mainPanel (BorderLayout)
 *           ├── NORTH  → marketPanel   (live stock prices table)
 *           ├── CENTER → middlePanel   (buy/sell + portfolio side by side)
 *           └── SOUTH  → historyPanel  (trade history log)
 */
public class StockSimulatorGUI extends JFrame {

    // ----------------------------------------------------------------
    // Backend services (same objects your console app used)
    // ----------------------------------------------------------------
    private final MarketServiceImpl market;
    private final TradingService tradingService;
    private final DatabaseService db;
    private final User user;

    // ----------------------------------------------------------------
    // GUI components we need to update from the timer
    // ----------------------------------------------------------------
    private DefaultTableModel marketTableModel;   // data behind the stock table
    private JLabel cashLabel;                     // shows current cash
    private JLabel netWorthLabel;                 // shows total net worth
    private JTextArea historyArea;                // scrollable trade log
    private JPanel holdingsPanel;                 // shows each stock holding

    // ----------------------------------------------------------------
    // Constructor — builds the entire window
    // ----------------------------------------------------------------
    public StockSimulatorGUI() {

        // --- Start backend (same as Main.java) ---
        market = new MarketServiceImpl();
        market.initializeMarket();

        db = new DatabaseService();
        db.connect();

        user = new User(1, "Rahul", "hashed_pw", 50000.00);
        db.saveUser(user);

        market.setDatabaseService(db);
        market.startMarket();

        tradingService = new TradingService(market, db);

        // --- Configure the JFrame (the window) ---
        setTitle("Stock Trading Simulator");
        setSize(900, 650);                          // width x height in pixels
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // X button closes app
        setLocationRelativeTo(null);                // centre on screen

        // --- Build the layout ---
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        mainPanel.add(buildMarketPanel(),  BorderLayout.NORTH);
        mainPanel.add(buildMiddlePanel(),  BorderLayout.CENTER);
        mainPanel.add(buildHistoryPanel(), BorderLayout.SOUTH);

        add(mainPanel);

        // --- Start the GUI refresh timer ---
        // Every 2000ms (2 seconds) refresh stock prices and portfolio
        // This runs on the EDT — safe to update GUI here
        Timer guiTimer = new Timer(2000, e -> refreshAll());
        guiTimer.start();

        setVisible(true);   // show the window
    }

    // ----------------------------------------------------------------
    // PANEL 1 — Live Market Prices (JTable)
    // ----------------------------------------------------------------
    private JPanel buildMarketPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Live Market Prices"));

        // Column headers for the table
        String[] columns = {"Symbol", "Company", "Price (₹)", "Change"};

        // DefaultTableModel holds the data — false = cells not editable
        marketTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;   // user cannot click and edit table cells
            }
        };

        JTable table = new JTable(marketTableModel);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);  // can't drag columns

        // Populate with initial data
        refreshMarketTable();

        // JScrollPane makes the table scrollable if there are many rows
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(860, 160));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ----------------------------------------------------------------
    // PANEL 2 — Middle (Buy/Sell LEFT + Portfolio RIGHT)
    // ----------------------------------------------------------------
    private JPanel buildMiddlePanel() {
        // GridLayout(1, 2) = 1 row, 2 columns — side by side
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.add(buildBuySellPanel());
        panel.add(buildPortfolioPanel());
        return panel;
    }

    // ----------------------------------------------------------------
    // PANEL 2a — Buy / Sell
    // ----------------------------------------------------------------
    private JPanel buildBuySellPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Trade"));

        // GridBagConstraints controls WHERE each component sits in the grid
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);  // padding around each component
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Symbol label + field ---
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Symbol:"), gbc);

        JTextField symbolField = new JTextField(10);
        symbolField.setToolTipText("e.g. TCS, INFY, RELIANCE");
        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(symbolField, gbc);

        // --- Quantity label + field ---
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Quantity:"), gbc);

        JTextField qtyField = new JTextField(10);
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(qtyField, gbc);

        // --- Status label (shows success/error messages) ---
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(statusLabel, gbc);

        // --- BUY button ---
        JButton buyBtn = new JButton("BUY");
        buyBtn.setBackground(new Color(29, 158, 117));  // green
        buyBtn.setForeground(Color.WHITE);
        buyBtn.setFocusPainted(false);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        panel.add(buyBtn, gbc);

        // --- SELL button ---
        JButton sellBtn = new JButton("SELL");
        sellBtn.setBackground(new Color(226, 75, 74));  // red
        sellBtn.setForeground(Color.WHITE);
        sellBtn.setFocusPainted(false);
        gbc.gridx = 1; gbc.gridy = 3;
        panel.add(sellBtn, gbc);

        // --- Button Actions ---
        // ActionListener is like an event handler — fires when button clicked
        buyBtn.addActionListener(e -> {
            String symbol = symbolField.getText().trim().toUpperCase();
            String qtyText = qtyField.getText().trim();

            try {
                int qty = Integer.parseInt(qtyText);
                boolean success = tradingService.buy(user, symbol, qty);

                if (success) {
                    statusLabel.setForeground(new Color(29, 158, 117));
                    statusLabel.setText("Bought " + qty + " × " + symbol);
                    refreshAll();   // update all panels immediately
                } else {
                    statusLabel.setForeground(new Color(226, 75, 74));
                    statusLabel.setText("Buy failed — check funds/symbol");
                }
            } catch (NumberFormatException ex) {
                statusLabel.setForeground(new Color(226, 75, 74));
                statusLabel.setText("Quantity must be a number");
            }
        });

        sellBtn.addActionListener(e -> {
            String symbol = symbolField.getText().trim().toUpperCase();
            String qtyText = qtyField.getText().trim();

            try {
                int qty = Integer.parseInt(qtyText);
                boolean success = tradingService.sell(user, symbol, qty);

                if (success) {
                    statusLabel.setForeground(new Color(29, 158, 117));
                    statusLabel.setText("Sold " + qty + " × " + symbol);
                    refreshAll();
                } else {
                    statusLabel.setForeground(new Color(226, 75, 74));
                    statusLabel.setText("Sell failed — check shares/symbol");
                }
            } catch (NumberFormatException ex) {
                statusLabel.setForeground(new Color(226, 75, 74));
                statusLabel.setText("Quantity must be a number");
            }
        });

        return panel;
    }

    // ----------------------------------------------------------------
    // PANEL 2b — Portfolio Summary
    // ----------------------------------------------------------------
    private JPanel buildPortfolioPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBorder(BorderFactory.createTitledBorder("Portfolio"));

        // Top: cash + net worth
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        statsPanel.add(new JLabel("Cash Balance:"));
        cashLabel = new JLabel("₹" + String.format("%.2f",
                user.getPortfolio().getCashBalance()));
        cashLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statsPanel.add(cashLabel);

        statsPanel.add(new JLabel("Net Worth:"));
        netWorthLabel = new JLabel("calculating...");
        netWorthLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statsPanel.add(netWorthLabel);

        outer.add(statsPanel, BorderLayout.NORTH);

        // Holdings list (refreshed every 2s)
        holdingsPanel = new JPanel();
        holdingsPanel.setLayout(new BoxLayout(holdingsPanel, BoxLayout.Y_AXIS));
        outer.add(new JScrollPane(holdingsPanel), BorderLayout.CENTER);

        return outer;
    }

    // ----------------------------------------------------------------
    // PANEL 3 — Trade History (scrollable log)
    // ----------------------------------------------------------------
    private JPanel buildHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Trade History"));

        historyArea = new JTextArea(6, 40);
        historyArea.setEditable(false);   // read only
        historyArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scroll = new JScrollPane(historyArea);
        scroll.setPreferredSize(new Dimension(860, 130));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ----------------------------------------------------------------
    // REFRESH — called every 2 seconds by the Timer
    // ----------------------------------------------------------------

    /**
     * Updates all 4 panels with the latest data.
     * Safe to call on EDT (called by javax.swing.Timer).
     */
    private void refreshAll() {
        refreshMarketTable();
        refreshPortfolio();
        refreshHistory();
    }

    private void refreshMarketTable() {
        List<Stock> stocks = market.getStocks();

        // Clear all rows, then re-add with latest prices
        marketTableModel.setRowCount(0);

        for (Stock s : stocks) {
            double price = s.getPrice();

            // Simple change indicator (random walk — no prev price stored)
            String change = "~";

            marketTableModel.addRow(new Object[]{
                s.getSymbol(),
                s.getCompanyName(),
                String.format("₹%.2f", price),
                change
            });
        }
    }

    private void refreshPortfolio() {
        List<Stock> liveStocks = market.getStocks();

        // Update cash and net worth labels
        cashLabel.setText("₹" + String.format("%.2f",
                user.getPortfolio().getCashBalance()));
        netWorthLabel.setText("₹" + String.format("%.2f",
                user.getPortfolio().getTotalValue(liveStocks)));

        // Rebuild holdings list
        holdingsPanel.removeAll();

        if (user.getPortfolio().getHoldings().isEmpty()) {
            holdingsPanel.add(new JLabel("  No holdings yet."));
        } else {
            for (var entry : user.getPortfolio().getHoldings().entrySet()) {
                String symbol = entry.getKey();
                int qty = entry.getValue();
                double avg = user.getPortfolio().getAvgBuyPrice(symbol);
                double live = 0;

                for (Stock s : liveStocks) {
                    if (s.getSymbol().equalsIgnoreCase(symbol)) {
                        live = s.getPrice();
                        break;
                    }
                }

                double pl = (live - avg) * qty;
                String plText = (pl >= 0 ? "+" : "") + String.format("%.2f", pl);
                Color plColor = pl >= 0 ?
                        new Color(29, 158, 117) : new Color(226, 75, 74);

                JLabel lbl = new JLabel(String.format(
                        "  %s × %d  |  avg ₹%.2f  |  now ₹%.2f  |  P&L: %s",
                        symbol, qty, avg, live, plText));
                lbl.setForeground(plColor);
                holdingsPanel.add(lbl);
            }
        }

        holdingsPanel.revalidate();   // redraws the panel after changes
        holdingsPanel.repaint();
    }

    private void refreshHistory() {
        List<Transaction> history = user.getTransactionHistory();

        if (history.isEmpty()) {
            historyArea.setText("  No trades yet.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        // Show newest first
        for (int i = history.size() - 1; i >= 0; i--) {
            sb.append(history.get(i).toString()).append("\n");
        }
        historyArea.setText(sb.toString());
    }

    // ----------------------------------------------------------------
    // Clean shutdown when window closes
    // ----------------------------------------------------------------
    private void onClose() {
        market.stopMarket();
        db.disconnect();
    }

    // ----------------------------------------------------------------
    // Entry point
    // ----------------------------------------------------------------
    public static void main(String[] args) {
        // SwingUtilities.invokeLater — always start Swing on the EDT
        SwingUtilities.invokeLater(() -> {
            StockSimulatorGUI gui = new StockSimulatorGUI();
            // Hook into window close event for clean shutdown
            gui.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    gui.onClose();
                }
            });
        });
    }
}