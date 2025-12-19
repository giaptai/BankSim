# BankSim: A Concurrent Banking Simulation System

## 🚀 Project Overview

**BankSim** is a Java application designed to simulate fundamental banking operations. This project was developed to explore and implement robust solutions for common technical challenges in concurrent and database-driven systems. It demonstrates the practical application of Object-Oriented Programming (OOP) principles, advanced Multithreading techniques, JDBC for database interaction, and various Design Patterns. The goal was to build a system that is resilient, efficient, and maintainable, featuring both a console interface and a Swing-based Graphical User Interface (GUI).

## 🎯 The Problem Statement

When building a banking system, even a simulated one, we encountered several key technical challenges that needed careful consideration:

1.  **Concurrency & Data Integrity:** How can we process many transactions at the same time (like deposits, withdrawals, and transfers) without accidentally corrupting account balances (known as Race Conditions) or causing the system to freeze (Deadlocks)? We needed to ensure that all account data remains accurate and consistent.
2.  **Database Performance & Resource Management:** How can the system talk to the database quickly and efficiently? We aimed to reduce the overhead of creating and closing database connections, especially when many transactions happen at once.
3.  **Security Vulnerabilities:** How can we protect the system from common database attacks, such as SQL Injection, which could compromise data?
4.  **Maintainability & Extensibility:** How can we design the system so that adding new types of transactions, supporting different databases, or changing parts of the user interface is straightforward, without breaking existing, stable code?
5.  **Real-time User Feedback:** In a system with many ongoing transactions, how can we show users what's happening in real-time, giving them clear visual updates?

## ✨ Solutions & Applied Techniques

To address the challenges outlined above, BankSim was built using a layered architecture, applying specific concurrency controls, and integrating various Design Patterns.

### 1. **Robust Concurrency Management**

To keep account balances correct and the system stable when many transactions happen at once, we implemented:

*   **`ExecutorService` (Thread Pool):** We used an `ExecutorService` to manage a pool of threads. This helps process many banking transactions (like deposits or transfers) at the same time without creating a new thread for each one. This approach aims to make the system more efficient and responsive.
*   **Per-Account Locking:** To prevent different transactions from changing the same account balance incorrectly (Race Conditions), we used `ReentrantLock` objects. These locks are stored in a `ConcurrentHashMap` within our `BankService`. This means each account has its own lock, ensuring that only one transaction can modify an account at a time.
*   **Deadlock Prevention:** For transfers between two accounts, we made sure to acquire locks for both accounts in a consistent order (e.g., always locking the account with the smaller ID first). This strategy was applied to prevent Deadlocks, where transactions might get stuck waiting for each other indefinitely.

### 2. **Efficient & Secure Database Interaction**

For reliable, fast, and secure data storage, we focused on:

*   **`IDatabaseManager` Abstraction:** We created an `IDatabaseManager` interface. This helps separate the business logic from the specific details of how we talk to the database. This design aims to make it easier to switch between different database systems if needed.
*   **HikariCP Connection Pooling:** Our database manager implementations use HikariCP, a fast connection pooling library. This helps to significantly improve database performance by reusing existing database connections instead of opening and closing new ones for every transaction. This approach aims to reduce overhead and speed up operations, especially under heavy load.

    **Performance Benchmarks (Impact of HikariCP):**
    To evaluate the effect of connection pooling, we conducted performance tests for deposit transactions with varying numbers, comparing performance without HikariCP (opening/closing connections for each transaction) versus with HikariCP.

    **Test Configuration:**
    *   `ExecutorService` with 100 threads (`MAX_THREADS = 100`).
    *   HikariCP `maximumPoolSize` configured to 100.

    **Experimental Results:**

    | Number of Transactions | Before HikariCP (Total Time) | After HikariCP (Total Time) | Improvement (Total Time) | Before HikariCP (ms/transaction) | After HikariCP (ms/transaction) | Improvement (ms/transaction) |
    | :------------------- | :--------------------------- | :-------------------------- | :----------------------- | :----------------------------- | :---------------------------- | :--------------------------- |
    | **1,000**            | ~10 seconds                  | **2 seconds**               | **5x**                   | ~10 ms                         | **2 ms**                      | **5x**                       |
    | **10,000**           | ~89 seconds                  | **9 seconds**               | **~9.9x**                | ~8.9 ms                        | **0.9 ms**                    | **~9.9x**                    |
    | **100,000**          | ~849 seconds (14.15 minutes) | **107 seconds (1.78 minutes)** | **~7.9x**                | ~8.5 ms                        | **1.07 ms**                   | **~7.9x**                    |

    **Conclusion:** The integration of HikariCP led to a notable performance improvement, reducing average transaction processing time from approximately 8.5-10ms to under 2ms. This was particularly effective with a large number of transactions, demonstrating that connection pooling is an effective solution for mitigating the bottleneck caused by database connection overhead in multithreaded applications.

*   **`PreparedStatement`:** We consistently used `PreparedStatement` for all SQL queries. This practice is a standard way to **prevent SQL Injection attacks**, helping to make the system more secure.
*   **Database Transactions:** For complex operations, like money transfers, we grouped multiple database actions into a single database transaction. By managing `auto-commit` and using explicit `commit()` or `rollback()`, we aimed to ensure that these operations are **atomic** (either all succeed or all fail), maintaining data integrity.

### 3. **Architectural Design & Design Patterns**

The project follows a layered architecture (Presentation, Business Service, Data Access) and applies several Design Patterns. This approach was chosen to make the system more flexible, easier to maintain, and simpler to extend in the future.

*   **Singleton Pattern:**
    *   **`BankService`:** We ensured that only one instance of `BankService` exists throughout the application. This helps to centrally manage the `ExecutorService` (thread pool) and the `accountLocks` (our global concurrency control mechanism), aiming for consistent behavior and efficient resource use.
    *   **`IDatabaseManager` Implementations (e.g., `PostgreSQLDatabaseManage`):** Similarly, we maintain a single instance for our database manager implementations. This helps to manage the HikariCP connection pool efficiently, preventing the creation of multiple, potentially wasteful, connection pools.
*   **Observer Pattern:**
    *   The **`BankService`** acts as the `Subject`, notifying registered `Observer`s about `TransactionEvent`s.
    *   The **`ThreadTrackerGUI`** is a `Concrete Observer` that updates the user interface in real-time based on these events.
    *   **Benefit:** This pattern helps to separate the logic for updating the UI from the core business logic. This means different parts of the application can react to events without the `BankService` needing to know their specific details, making the system more flexible.
*   **Template Method Pattern:**
    *   The **`SingleAccTxTemplate`** defines a common structure for transactions involving a single account (like deposits or withdrawals). Specific steps within this structure are then implemented by subclasses (`DepositProcessor`, `WithdrawProcessor`).
    *   **Benefit:** This approach helps to reduce repeated code, standardize how transactions are processed, and makes it easier to add new types of single-account transactions.
*   **Factory Method Pattern (with Registry Pattern):**
    *   The **`DatabaseManagerFactory`** is responsible for creating different `IDatabaseManager` implementations (e.g., for PostgreSQL or MySQL).
    *   We integrated a **Registry Pattern** to allow new database types to register themselves with the factory. This means we can add support for new databases without changing the factory's main code, following the Open/Closed Principle (OCP).
    *   **Benefit:** This helps to keep the client code separate from the specific database classes, making the system more flexible and easier to extend with new database technologies.
*   **Builder Pattern:**
    *   This pattern is used for creating complex objects like `Account` and `TransactionEvent`. These objects often have many properties, some optional and some required.
    *   **Benefit:** The Builder Pattern aims to make object creation clearer, step-by-step, and easier to read and maintain, especially when dealing with many parameters.

### 4. **Adherence to SOLID Principles**

The design of this project was guided by the SOLID principles. We aimed to apply these principles to achieve better maintainability, flexibility, and extensibility:

*   **Single Responsibility Principle (SRP):** We tried to ensure that each class has one clear job.
*   **Open/Closed Principle (OCP):** The system is designed so that new features can be added without changing existing, working code.
*   **Liskov Substitution Principle (LSP):** We aimed for subclasses to be usable in place of their parent classes without causing issues.
*   **Interface Segregation Principle (ISP):** Interfaces were kept small and focused, so classes only depend on the methods they actually need.
*   **Dependency Inversion Principle (DIP):** High-level parts of the system (like the UI or `BankService`) depend on general ideas (interfaces) rather than specific implementations. This helps keep parts of the system loosely connected and easier to change.

### 5. **User Interface**

*   The application offers both a **Console-based** and a **Swing GUI** for user interaction.
*   **`ThreadTrackerGUI`:** A special GUI component was developed to visually track the real-time status of concurrent transactions. This aims to provide immediate feedback on how the system's multithreaded operations are progressing.

## 🧪 Proof & Testing

To help ensure that our concurrent solutions and business logic work correctly and reliably, the project includes:

*   **`test/` directory:** This directory contains various test cases.
*   **`SimRunner.java`:** A dedicated class was created to simulate a high volume of concurrent transactions from multiple threads. This class is specifically designed to stress-test and verify that our per-account locking mechanism and deadlock prevention strategies work as intended under heavy load. These tests serve as evidence that the system can handle concurrency without data corruption.

## Video Demo: Concurrent Transactions (1000 transactions with 100 threads)

[Video demo](/media/20250924_175258.mp4)

## Live Demo
You can experience the BankSim application developed on Render here:
[https://banksim-webswing-0-1.onrender.com/banksim/](https://banksim-webswing-0-1.onrender.com/banksim/)

**Deployment Configuration on Render:**
*   **PostgreSQL DB:** 0.1 CPU, 256 MB RAM
*   **Web Service (Webswing App):** 0.1 CPU, 512 MB RAM

## 🐳 Docker Deployment with Webswing

### Tổng quan
BankSim có thể được deploy dưới dạng Docker container sử dụng Webswing để chạy ứng dụng Swing trong trình duyệt web.

### Cấu trúc Project
```
webswing/
├── Dockerfile              # Docker image configuration
├── jetty.properties        # Jetty web server config
├── webswing.config         # Webswing application config
├── apps/
│   └── BankSim/
│       ├── BankSim.jar     # Main application JAR
│       └── lib/            # Dependencies (PostgreSQL, HikariCP, SLF4J)
├── server/                 # Webswing server files
└── admin/                  # Webswing admin interface
```

### Cấu hình Database
Trước khi build Docker image, cần cấu hình kết nối database trong `src/resources/dbpostgres.properties`:

```properties
db.type=postgres
db.driver=org.postgresql.Driver
db.url=jdbc:postgresql://YOUR_HOST:5432/YOUR_DATABASE
db.user=YOUR_USERNAME
db.password=YOUR_PASSWORD
```

**Lưu ý quan trọng:**
- File `dbpostgres.properties` sẽ được đóng gói vào `BankSim.jar` khi build
- Để deploy lên production (Render, AWS, etc.), thay thế các giá trị bằng database credentials thực tế
- Có thể sử dụng environment variables trong Dockerfile để bảo mật thông tin nhạy cảm

### Các bước Build và Deploy

#### 1. Chuẩn bị BankSim.jar
Đầu tiên, build BankSim.jar với database configuration phù hợp:

**PowerShell:**
```powershell
# Dọn dẹp và build
Remove-Item -Recurse -Force bin -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path bin

# Compile
Get-ChildItem src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName | Out-File sources.txt -Encoding UTF8
javac -d bin -cp "lib/*" -encoding UTF-8 (Get-Content sources.txt)

# Copy resources (bao gồm dbpostgres.properties)
Copy-Item -Path src\resources -Destination bin -Recurse -Force

# Tạo JAR
jar cvfm BankSim.jar MANIFEST.MF -C bin .

# Verify resources được đóng gói
jar tf BankSim.jar | Select-String "resources/dbpostgres.properties"
```

#### 2. Copy JAR và dependencies vào webswing
```powershell
# Copy BankSim.jar và lib vào webswing/apps/BankSim/
Copy-Item BankSim.jar webswing\apps\BankSim\
Copy-Item -Recurse lib webswing\apps\BankSim\
```

#### 3. Build Docker Image
```bash
cd webswing
docker build -t banksim-webswing:latest .
```

**Docker Image Details:**
- **Base Image:** `eclipse-temurin:21-jre-jammy` (Java 21 JRE)
- **Display Server:** Xvfb (X Virtual FrameBuffer for headless Swing apps)
- **Web Server:** Jetty (embedded in Webswing)
- **Port:** 8080
- **Image Size:** ~350-400 MB (lightweight JRE-based)

#### 4. Test Local
```bash
docker run -p 8080:8080 banksim-webswing:latest
```

Truy cập: `http://localhost:8080/banksim`

**Thông tin đăng nhập Webswing (áp dụng cho cả Local và Render):**
- **Username:** `admin`
- **Password:** `pwd`

#### 5. Push to DockerHub
```bash
# Tag image
docker tag banksim-webswing:latest YOUR_DOCKERHUB_USERNAME/banksim-webswing:latest

# Login
docker login

# Push
docker push YOUR_DOCKERHUB_USERNAME/banksim-webswing:latest
```

### Dockerfile Configuration

**Các thành phần chính:**

```dockerfile
FROM eclipse-temurin:21-jre-jammy

# Install Xvfb và X11 libraries
RUN apt-get update && apt-get install --no-install-recommends -y \
    xvfb libxext6 libxi6 libxtst6 libxrender1 libpangoft2-1.0-0

# Copy toàn bộ webswing folder
COPY . /opt/webswing/

# Environment variables
ENV WEBSWING_HOME=/opt/webswing
ENV DISPLAY=:99
ENV WEBSWING_JAVA_OPTS="-Xmx256M"

# Start Xvfb (background) và Webswing server
CMD ["./start.sh"]
```

### Webswing Configuration

File `webswing/webswing.config` cần cấu hình đúng classpath:

```json
{
  "/banksim": {
    "name": "BankSim",
    "swingConfig": {
      "homeDir": "${user.dir}/apps/BankSim",
      "launcherConfig": {
        "mainClass": "App"
      },
      "classPathEntries": [
        "BankSim.jar",
        "lib/*.jar"
      ]
    }
  }
}
```

### Các vấn đề thường gặp và giải pháp

| Vấn đề | Nguyên nhân | Giải pháp |
|--------|-------------|-----------|
| Container stopped immediately | Dockerfile CMD không dùng `exec` | Sử dụng `exec` trong start script |
| Main class not found | `classPathEntries` không đúng | Kiểm tra `webswing.config` có `BankSim.jar` và `lib/*.jar` |
| UnsupportedClassVersionError | Java version không khớp | Đảm bảo Dockerfile dùng Java 21 (match với compilation) |
| Database connection failed | Properties file không được đóng gói | Verify `jar tf BankSim.jar` có `resources/dbpostgres.properties` |

### Environment Variables (Optional - Advanced)

Để bảo mật hơn, có thể dùng environment variables thay vì hardcode trong properties:

**Trong Dockerfile:**
```dockerfile
ENV DB_URL=jdbc:postgresql://host:5432/db \
    DB_USER=username \
    DB_PASSWORD=password
```

**Trong code Java:** Đọc từ `System.getenv()` thay vì properties file.

### Render Deployment

Để deploy lên Render:
1. Push Docker image lên DockerHub
2. Tạo Web Service mới trên Render
3. Chọn "Deploy from Docker image"
4. Nhập image URL: `YOUR_USERNAME/banksim-webswing:latest`
5. Set port: `8080`
6. Configure resources: 0.1 CPU, 512 MB RAM

**Lưu ý:** Đảm bảo database PostgreSQL trên Render đã được tạo và connection string được cập nhật trong `dbpostgres.properties` trước khi build JAR.

## 🛠️ How to Run the Project

### Prerequisites:

*   Java Development Kit (JDK) 17 or higher.
*   Maven (for dependency management and building).
*   A running PostgreSQL or MySQL database instance.

### Configuration:

1.  **Database Configuration:**
    *   Create an empty database (e.g., `banksim_db`).
    *   Copy `resources/application-sample.properties` to `resources/application.properties`.
    *   Edit `resources/dbpostgres.properties` or `resources/dbmysql.properties` (depending on your chosen database) with your database connection details (URL, username, password).
    *   In `App.java`, ensure `DatabaseManagerFactory.create()` is configured to use your desired DB type (e.g., `Constants.DB_TYPE_POSTGRES`).

2.  **Logging Configuration:**
    *   The `resources/logging.properties` file contains the configuration for `java.util.logging`.

### Build and Run:
## How to build jar ?

Sử dụng PowerShell (recommended) hoặc Command Prompt (cmd). Thực hiện tại thư mục gốc dự án: d:\java-project\BankSim

PowerShell (dễ nhất):
```powershell
# 1. Dọn bin
Remove-Item -Recurse -Force bin -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path bin

# 2. Tạo danh sách nguồn và biên dịch
Get-ChildItem src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName | Out-File sources.txt -Encoding UTF8
javac -d bin -cp "lib/*" -encoding UTF-8 (Get-Content sources.txt)

# 3. Sao chép resources (dbpostgres.properties, logging.properties, ...)
Copy-Item -Path src\resources -Destination bin -Recurse -Force

# 4. (Nếu cần) Tạo MANIFEST.MF — hoặc dùng MANIFEST.MF đã có sẵn
@"
Manifest-Version: 1.0
Main-Class: App
Class-Path: lib/postgresql-42.7.7.jar lib/HikariCP-7.0.2.jar lib/slf4j-api-2.0.17.jar lib/slf4j-jdk14-2.0.17.jar
"@ | Out-File -FilePath MANIFEST.MF -Encoding ASCII

# 5. Tạo JAR
jar cvfm BankSim.jar MANIFEST.MF -C bin .

# 6. Kiểm tra file resource đã có trong JAR chưa
jar tf BankSim.jar | Select-String "resources/dbpostgres.properties"

# 7. Chạy JAR
java -jar BankSim.jar
```

Command Prompt (cmd.exe):
```cmd
REM 1. Dọn bin
rmdir /s /q bin
mkdir bin

REM 2. Biên dịch (biên dịch tất cả .java; nếu cmd không hỗ trợ glob đầy đủ, dùng for)
for /R src %f in (*.java) do @echo %f >> sources.txt
javac -d bin -cp "lib\*" @sources.txt

REM 3. Sao chép resources
xcopy /E /I src\resources bin\resources

REM 4. Tạo/chuẩn bị MANIFEST.MF (tạo bằng notepad hoặc copy file có sẵn)

REM 5. Tạo JAR
jar cvfm BankSim.jar MANIFEST.MF -C bin .

REM 6. Kiểm tra
jar tf BankSim.jar | findstr /I "resources\\dbpostgres.properties"

REM 7. Chạy
java -jar BankSim.jar
```

Ghi chú ngắn:
- Hãy chắc chắn thư mục `lib` (các JAR phụ thuộc) nằm cùng cấp với `BankSim.jar`.
- Kiểm tra `bin\resources\dbpostgres.properties` trước khi tạo JAR; nếu không có, ứng dụng sẽ báo lỗi khi chạy.
- Nếu gặp lỗi thiếu class khi javac, đảm bảo các JAR phụ thuộc trong `lib` đúng phiên bản và đường dẫn chính xác trong `MANIFEST.MF`. -> nghĩa là copy thư mục lib vào theo đúng MENIFEST thể hiện ở Class-Path

## 📂 Project Structure

```text
src/
├── App.java                      # Main entry point of the application
├── business/
│   ├── service/
│   │   ├── BankService.java      # Core business logic, Subject, Singleton instance
│   │   ├── IBankService.java     # Interface for BankService (DIP)
│   │   └── transaction/
│   │       ├── observer/         # Observer Pattern components
│   │       │   ├── Observer.java
│   │       │   ├── Subject.java
│   │       │   └── TransactionEvent.java
│   │       └── template/         # Template Method Pattern components
│   │           ├── DepositProcessor.java
│   │           ├── SingleAccTxTemplate.java
│   │           └── WithdrawProcessor.java
├── data/
│   ├── DatabaseManagerFactory.java # Factory Method (with Registry Pattern)
│   ├── IDatabaseManager.java       # Database abstraction interface
│   ├── MySQLDatabaseManage.java    # MySQL implementation (Singleton instance)
│   ├── PostgreSQLDatabaseManage.java # PostgreSQL implementation (Singleton instance)
│   └── models/
│       ├── Account.java            # Account data model (Builder Pattern)
│       └── Transaction.java        # Transaction data model
├── presentation/
│   ├── console/
│   │   └── Menu.java             # Console user interface
│   ├── controller/
│   │   ├── BankController.java
│   │   └── SwingBankController.java
│   └── ui/
│       ├── BankSwingGUI.java     # Main Swing GUI frame
│       ├── ThreadTrackerGUI.java # Real-time transaction tracker GUI (Observer)
│       └── panels/               # Individual Swing panels for different operations
│           ├── DepositPanel.java
│           ├── MainMenuPanel.java
│           ├── OpenAccountPanel.java
│           ├── TransferPanel.java
│           ├── ViewBalancePanel.java
│           ├── ViewTransactionHistoryPanel.java
│           └── WithdrawPanel.java
├── resources/
│   ├── application-sample.properties
│   ├── Constants.java            # Global constants
│   ├── dbmysql.properties
│   ├── dbpostgres.properties
│   ├── ddl.sql                   # Database schema definition (DDL)
│   ├── dml.sql                   # Sample data script
│   ├── logging.properties        # Logging configuration
│   ├── MyExceptions.java         # Custom exception classes
│   ├── TransactionStatus.java    # Enum for transaction statuses
│   ├── Type.java                 # Enum for transaction types
│   └── annotations/              # Custom annotations
│       ├── Builder.java
│       ├── Overloading.java
│       ├── Repository.java
│       ├── Service.java
│       └── Test.java
└── test/
    ├── GUIWithdrawTest.java
    └── SimRunner.java              # Multithreaded simulation and testing
```