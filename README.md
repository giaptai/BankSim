# BankSim: A Simple Banking Simulation

## Project Overview

BankSim is a console-based and Swing GUI Java application designed to simulate fundamental banking operations. It provides interfaces for users to interact with a banking system, allowing them to manage accounts and perform transactions. The project demonstrates a `3-layered architecture (Presentation, Business Logic, Data Access)` and incorporates concurrent transaction processing using Java's `ExecutorService` combined with explicit per-account locking for critical operations.

## Features

*   **Open Account**: Create new bank accounts with an initial balance.
*   **Deposit Funds**: Add money to an existing account.
*   **Withdraw Funds**: Remove money from an existing account, with checks for sufficient balance.
*   **Transfer Funds**: Move money between two accounts, ensuring atomicity and handling concurrency with explicit per-account locking to prevent race conditions and deadlocks.
*   **View Account Details**: Display the current balance and owner information for a specific account.
*   **View Transaction History**: Show a list of all transactions for a given account.
*   **Concurrent Transactions**: Utilizes `ExecutorService` to handle multiple transactions asynchronously. Critical sections (like balance checks and updates during transfers) are protected using `ReentrantLock` instances managed by a `ConcurrentHashMap` in the `BankService` layer, ensuring data consistency.
*   **Robust Exception Handling**: Implements a structured approach to exception handling, differentiating between business exceptions (e.g., `InvalidAmountException`, `AccountNotFoundException`, `InsufficientFundsException`) and technical exceptions, wrapping technical errors into meaningful business exceptions for the presentation layer.
*   **Database Persistence**: All account and transaction data is stored persistently in a PostgreSQL database using JDBC.

## Project Structure
```text
The project follows a standard layered architecture:
src/
├── App.java                     # Main entry point of the application

├── business/
│   └── service/
│       └── BankService.java     # Business logic layer, handles core banking operations, manages per-account locks

├── data/
│   ├── DatabaseManager.java     # Data Access Object (DAO) layer, interacts with the database using atomic SQL updates and batch processing
│   └── models/
│       ├── Account.java         # Data model for a bank account (no internal locking)
│       └── Transaction.java     # Data model for a transaction

├── presentation/                # Presentation Layer
│   ├── console/
│   │   └── Menu.java            # Handles user input and output for the console
│   └── controller/
│       ├── BankController.java  # Console presentation layer, manages user interaction flow
│       └── SwingBankController.java # Swing GUI presentation layer controller
│   └── ui/
│       ├── BankSwingGUI.java    # Main Swing GUI frame
│       └── panels/              # Individual Swing panels for different operations
│           ├── DepositPanel.java
│           ├── MainMenuPanel.java
│           ├── OpenAccountPanel.java
│           ├── TransferPanel.java
│           ├── ViewBalancePanel.java
│           ├── ViewTransactionHistoryPanel.java
│           └── WithdrawPanel.java

├── resources/
│   ├── application.properties   # Configuration file (e.g., for database connection)
│   ├── ddl.sql                  # Database schema definition (DDL)
│   ├── MyExceptions.java        # Custom exception classes (business and technical wrappers)
│   ├── Type.java                # Enum for transaction types
│   └── annotations/             # Custom annotations (e.g., @Service, @Repository, @Test)
│       ├── Overloading.java
│       ├── Repository.java
│       ├── Service.java
│       └── Test.java

└── test/                       # Unit and integration tests for banking operations
    ├── DepositTest.java        # Test for concurrent deposit operations
    ├── TestMultithread.java    # General multithreaded deposit test using ExecutorService
    ├── TranferTest.java        # Test for concurrent transfer operations
    └── WithdrawTest.java       # Test for concurrent withdraw operations
```

*   **`presentation`**: Responsible for user interaction (console I/O or GUI) and translating user actions into calls to the business layer.
*   **`business`**: Contains the core business logic and rules. It orchestrates operations, validates business constraints, and handles transaction management, including concurrency control for shared resources.
*   **`data`**: Manages data persistence. It interacts with the underlying database to store and retrieve banking data, ensuring atomic updates for balance changes.
*   **`resources`**: Holds configuration files, custom exceptions, and utility enums/annotations.
*   **`test`**: Contains various test cases, including multithreaded scenarios, to verify the correctness and robustness of the banking operations.

## Technologies Used

*   **Java Development Kit (JDK)**: Version 11 or higher.
*   **JDBC**: For robust database interaction with PostgreSQL.
*   **`java.util.concurrent`**: `ExecutorService`, `Future`, `Callable`, `ConcurrentHashMap`, `ReentrantLock` for concurrent transaction processing and explicit lock management.
*   **PostgreSQL**: As the relational database for data persistence.
*   **Swing**: For the optional graphical user interface.

## Demo Images tests
### Deposit (30 times deposit)
- ![](/images/deposit.png)
### Withdraw (30 times withdraw)
- ![](/images/withdraw.png)
### Tranfer (10 times tranfer)
- ![](/images/tranfer.png)