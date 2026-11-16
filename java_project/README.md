# Stock Trading Simulator

## Overview

This project is a **Java-based stock market simulator** with both a console interface and a Java Swing GUI, backed by MySQL through JDBC. It models core trading platform behaviour: live-updating stock prices, buy/sell order execution, portfolio and profit/loss tracking, and permanent trade history.

The goal of this project is to demonstrate clean object-oriented design and practical Java concurrency by:

- separating the application into self-contained layers (model, service, thread, util, main),
- simulating a live market with a background price engine that runs independently of user input,
- and persisting every user, trade, and price snapshot in a relational database.

It was built as an **Advanced Object-Oriented Concepts (AOOC) mini project** and covers classes and objects, inheritance, interfaces, exception handling, file I/O, multithreading, JDBC, and Swing GUI.

---

## Architecture & Domain Information

- **Architecture Pattern:** Layered (Presentation → Business Logic → Data)
- **Database:** MySQL 8.0 (3 normalized tables)
- **Database Access:** JDBC with `PreparedStatement`
- **Concurrency:** `ScheduledExecutorService` + `CopyOnWriteArrayList`
- **Interfaces:** Console application and Swing GUI sharing one backend
- **Domain:** Fintech / Stock Market Education

### Core Modules & Classes

| Module | Classes | Responsibility |
|---|---|---|
| **Model** | `Stock`, `User`, `Portfolio`, `Transaction` | Plain data objects: stocks, user identity, holdings, trade records |
| **Market Engine** | `MarketService`, `MarketServiceImpl` | Interface and implementation that hold the stock list and run the price engine |
| **Price Thread** | `PriceUpdaterThread` | `Runnable` that updates every stock price every 2 seconds |
| **Trading** | `TradingService` | Validates and executes buy/sell orders |
| **Portfolio** | `PortfolioService` | Portfolio summary, per-stock P&L, net worth |
| **Database** | `DatabaseService` | All JDBC operations against MySQL |
| **Logging** | `LoggerUtil` | File logging to `logs/app.log` |
| **Presentation** | `Main`, `StockSimulatorGUI` | Console loop and Swing GUI |

---

## Core Features

### Live Market Simulation
- Five NSE-listed stocks: **RELIANCE, TCS, INFY, HDFC, WIPRO**
- Prices update every **2 seconds** on a background thread
- Random Walk model: each tick moves a price by a random value between **-2% and +2%**
- Price floor of ₹1.00 so a stock can never go negative

### Trading
- Buy and sell shares at the current live price
- Fail-fast validation: symbol exists, quantity is positive, sufficient cash (buy), sufficient shares (sell)
- Every successful trade is recorded as a `Transaction` and saved to MySQL

### Portfolio Tracking
- Virtual starting balance of ₹50,000
- Holdings stored in a `HashMap<String, Integer>` (symbol → quantity)
- **Weighted average buy price** per stock
- Profit/Loss per share and in total, shown in green or red in the GUI
- Net worth = cash + market value of all holdings

### Persistence
- Users, trades, and price snapshots are saved permanently in MySQL
- Cash balance is restored from the database on restart
- Existence check before insert prevents duplicate users
- `market_history` stores a price snapshot every 2 seconds, ready for charting

### Logging
- Java `FileHandler` writes timestamped events to `logs/app.log` in append mode
- Only SEVERE errors are echoed to the console

### Swing GUI
- Live price table (`JTable`) refreshed every 2 seconds with `javax.swing.Timer`
- BUY and SELL buttons with symbol and quantity inputs
- Portfolio panel with cash, net worth, and per-holding P&L
- Scrollable trade history panel

---

## OOP & Java Concepts Demonstrated

| Concept | Where it is used |
|---|---|
| Classes & Objects | `Stock`, `User`, `Portfolio`, `Transaction` and all service classes |
| Encapsulation | Private fields with getters/setters in all model classes |
| Interfaces | `MarketService` (custom), `Runnable`, `ActionListener` |
| Inheritance | `MarketServiceImpl implements MarketService`, `StockSimulatorGUI extends JFrame` |
| Exception Handling | `SQLException` in `DatabaseService`, `NumberFormatException` in input handling, `IOException` in `LoggerUtil` |
| File I/O | `FileHandler` writing to `logs/app.log` |
| Multithreading | `ScheduledExecutorService` runs `PriceUpdaterThread` while the main/EDT thread handles the user |
| Collections | `HashMap` (holdings, average price), `CopyOnWriteArrayList` (shared stock list) |
| JDBC | `DriverManager`, `PreparedStatement`, `ResultSet` |
| Swing | `JFrame`, `JTable`, `JButton`, `JTextField`, `javax.swing.Timer`, layout managers |

---

## Key Engineering Safeguards

- **Thread Safety:** The stock list is a `CopyOnWriteArrayList`, so the background price thread can write while the console or GUI thread reads.
- **SQL Injection Protection:** All queries use `PreparedStatement` with `?` placeholders instead of string concatenation.
- **Validation Before Execution:** Trades are rejected before any cash or shares change if the symbol, quantity, or balance is invalid.
- **Crash-Proof Database Layer:** Every JDBC call is wrapped in `try/catch`, so a database failure is logged instead of crashing the app.
- **Single Responsibility:** Each class has exactly one job (trading, portfolio reporting, persistence, logging).
- **Swing Thread Rule:** The GUI is created with `SwingUtilities.invokeLater`, and screen refreshes run on the Event Dispatch Thread.

---

## Tools & Tech Stack

### Language & Libraries
- Java (Core Java, `java.util.concurrent`, `java.util.logging`, `java.time`)
- Java Swing / AWT
- JDBC

### Database & Drivers
- MySQL 8.0
- MySQL Connector/J 9.7.0

### Developer Tools
- Visual Studio Code
- MySQL Command Line Client
- Git & GitHub
- Trello (task tracking)

---

## Console Commands

| Command | Description |
|---|---|
| `LIST` | Show live prices of all stocks |
| `BUY <SYMBOL> <QTY>` | Buy shares, e.g. `BUY TCS 10` |
| `SELL <SYMBOL> <QTY>` | Sell shares, e.g. `SELL TCS 5` |
| `PORTFOLIO` | Show cash, holdings, average cost, P&L, and net worth |
| `HISTORY` | Show trades made in the current session |
| `DBHISTORY` | Show all trades read directly from MySQL |
| `EXIT` | Stop the market thread, close the database connection, and quit |

---

## Database Schema

```sql
CREATE DATABASE stock_simulator;
USE stock_simulator;

CREATE TABLE users (
    user_id       INT PRIMARY KEY,
    username      VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    cash_balance  DOUBLE NOT NULL
);

CREATE TABLE transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NOT NULL,
    type           VARCHAR(4) NOT NULL,
    symbol         VARCHAR(10) NOT NULL,
    quantity       INT NOT NULL,
    price_at_time  DOUBLE NOT NULL,
    total_amount   DOUBLE NOT NULL,
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE market_history (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    symbol      VARCHAR(10) NOT NULL,
    price       DOUBLE NOT NULL,
    recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

| Table | Purpose |
|---|---|
| `users` | User identity and persisted cash balance |
| `transactions` | Every buy and sell, linked to a user by foreign key |
| `market_history` | Price snapshot per stock every 2 seconds |

---

## Project Structure

```
StockTradingSimulator/
│
├── lib/                              # MySQL Connector/J jar
│
├── src/
│   ├── model/
│   │   ├── Stock.java
│   │   ├── User.java
│   │   ├── Portfolio.java
│   │   └── Transaction.java
│   │
│   ├── service/
│   │   ├── MarketService.java        # Interface
│   │   ├── MarketServiceImpl.java    # Price engine, stock list
│   │   ├── TradingService.java       # Buy / sell logic
│   │   ├── PortfolioService.java     # Portfolio summary & P&L
│   │   └── DatabaseService.java      # JDBC operations
│   │
│   ├── thread/
│   │   └── PriceUpdaterThread.java   # Runnable, updates prices every 2s
│   │
│   ├── util/
│   │   └── LoggerUtil.java           # File logging
│   │
│   └── main/
│       ├── Main.java                 # Console application
│       └── StockSimulatorGUI.java    # Swing GUI
│
├── logs/
│   └── app.log
│
└── README.md
```

---

## Getting Started

### Prerequisites
- JDK 17 or later
- MySQL 8.0
- MySQL Connector/J jar placed in `lib/`

### 1. Set up the database
Run the schema above in MySQL.

### 2. Configure the connection
In `src/service/DatabaseService.java`, set your MySQL credentials:

```java
private static final String URL     = "jdbc:mysql://localhost:3306/stock_simulator";
private static final String DB_USER = "root";
private static final String DB_PASS = "your_mysql_password";
```

> Avoid committing your real password to GitHub. Keep a placeholder in the pushed code.

### 3. Compile (Windows PowerShell, single line)

```powershell
javac -cp "lib/mysql-connector-j-9.7.0.jar" -d out src/model/Stock.java src/model/Transaction.java src/model/User.java src/model/Portfolio.java src/thread/PriceUpdaterThread.java src/service/MarketService.java src/service/MarketServiceImpl.java src/service/TradingService.java src/service/PortfolioService.java src/service/DatabaseService.java src/util/LoggerUtil.java src/main/Main.java src/main/StockSimulatorGUI.java
```

### 4. Run

**Console version**
```powershell
java -cp "out;lib/mysql-connector-j-9.7.0.jar" main.Main
```

**GUI version**
```powershell
java -cp "out;lib/mysql-connector-j-9.7.0.jar" main.StockSimulatorGUI
```

> On Linux or macOS, use `:` instead of `;` in the classpath. On Windows, run `chcp 65001` first so the ₹ symbol displays correctly in the console.

---

## How Prices Change

Every 2 seconds, `PriceUpdaterThread` applies this formula to each stock:

```
changePercent = (random.nextDouble() * 2 - 1) * 2.0     // between -2.0% and +2.0%
newPrice      = oldPrice + (oldPrice * changePercent / 100)
```

For example, if TCS is ₹3800 and `changePercent` is +1.5%, the new price is ₹3857. This is a **Random Walk** model: each price depends on the previous one plus a small random step.

---

## Future Enhancements

- Multi-user registration and login
- Candlestick / line charts drawn from `market_history`
- Limit orders and stop-loss orders
- Pause and resume market control
- More stocks across sectors (banking, auto, IT)
- Real-time price feed from a market data API

---

## Academic Details

Developed as an **AOOC Mini Project** at D.Y. Patil College of Engineering & Technology, Kasaba Bawada, Kolhapur, Department of Computer Science and Engineering (2025-26).
