# BankSim: A Simple Banking Simulation

## Project Overview

BankSim is a console-based Java application designed to simulate fundamental banking operations. It provides a simple command-line interface for users to interact with a banking system, allowing them to manage accounts and perform transactions. The project demonstrates a `3-layered architecture (Presentation, Business Logic, Data Access)` and incorporates concurrent transaction processing using Java's `ExecutorService`.

## Features

*   **Open Account**: Create new bank accounts with an initial balance.
*   **Deposit Funds**: Add money to an existing account.
*   **Withdraw Funds**: Remove money from an existing account, with checks for sufficient balance.
*   **Transfer Funds**: Move money between two accounts, ensuring atomicity and handling concurrency with explicit locking.
*   **View Account Details**: Display the current balance and owner information for a specific account.
*   **View Transaction History**: Show a list of all transactions for a given account.
*   **Concurrent Transactions**: Utilizes `ExecutorService` to handle multiple transactions asynchronously, improving responsiveness and simulating real-world banking scenarios.
*   **Robust Exception Handling**: Implements a structured approach to exception handling, differentiating between business exceptions (e.g., `InvalidAmountException`, `AccountNotFoundException`) and technical exceptions, wrapping technical errors into meaningful business exceptions for the presentation layer.

## Project Structure
```text
The project follows a standard layered architecture:
src/
├── App.java                     # Main entry point of the application

├── business/
│   └── service/
│       └── BankService.java     # Business logic layer, handles core banking operations

├── data/
│   ├── DatabaseManager.java     # Data Access Object (DAO) layer, interacts with the database
│   └── models/
│       ├── Account.java         # Data model for a bank account
│       └── Transaction.java     # Data model for a transaction

├── presentation/                # Presentation Layer, show ui console to the user
│   ├── console/
│   │   └── Menu.java            # Handles user input and output for the console
│   └── controller/
│       └── BankController.java  # Presentation layer, manages user interaction flow

├── resources/
│   ├── application.properties   # Configuration file (e.g., for database connection)
│   ├── MyExceptions.java        # Custom exception classes (business and technical wrappers)
│   ├── Type.java                # Enum for transaction types
│   └── annotations/             # Custom annotations (e.g., @Service, @Repository)
│       ├── Overloading.java
│       ├── Repository.java
│       └── Service.java

└── test/                       # Placeholder for tests or utility code
```

*   **`presentation`**: Responsible for user interaction (console I/O) and translating user actions into calls to the business layer.
*   **`business`**: Contains the core business logic and rules. It orchestrates operations, validates business constraints, and handles transaction management.
*   **`data`**: Manages data persistence. It interacts with the underlying database (simulated or actual) to store and retrieve banking data.
*   **`resources`**: Holds configuration files, custom exceptions, and utility enums/annotations.

## Technologies Used

*   **Java Development Kit (JDK)**: Version 11 or higher.
*   **JDBC**: For database interaction (as implied by `SQLException` and `DatabaseManager`).
*   **`java.util.concurrent`**: `ExecutorService`, `Future`, `Callable` for concurrent transaction processing.

## How to Run

### Prerequisites

1.  **Java Development Kit (JDK)**: Ensure you have JDK 11 or a newer version installed on your system.
    You can check your Java version by running:
    ```bash
    java -version
    ```

### Compilation and Execution

1.  **Navigate to the project root directory**:
    Open your terminal or command prompt and go to the `d:\java-project\BankSim` directory.

2.  **Compile the Java source files**:
    Compile all `.java` files from the `src` directory.
    ```bash
    javac -d out src/**/*.java
    ```
    *(Note: This command assumes a flat output directory `out`. If you prefer to maintain the package structure, you might need a more complex `javac` command or use a build tool like Maven/Gradle.)*

3.  **Run the application**:
    Execute the compiled `App.java` class.
    ```bash
    java -cp out App
    ```

    The application's main menu should now be displayed in your console.

### Database Configuration (if applicable)

The `DatabaseManager` suggests interaction with a database. If `application.properties` contains database connection details, ensure they are correctly configured for your environment. For a simple simulation, it might use an in-memory database or a file-based approach that doesn't require external setup.

## Future Enhancements

*   Implement a more robust database solution (e.g., H2, SQLite, PostgreSQL) with proper connection pooling.
*   Add unit and integration tests for all layers.
*   Improve the console UI or develop a graphical user interface (GUI).
*   Implement user authentication and authorization.
*   Add more complex banking features (e.g., loan management, interest calculation).
*   Utilize a build automation tool like Maven or Gradle for easier dependency management and project building.
