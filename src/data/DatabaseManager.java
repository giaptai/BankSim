package data;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import data.models.Account;
import data.models.Transaction;
import resources.Type;
import resources.annotations.Overloading;
import resources.annotations.Repository;

@Repository
public class DatabaseManager {
    private static Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private Properties props;
    private HikariDataSource hikariDataSource;

    {
        try {
            loadProperties();
            initConnectionPool();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "Failed to initialize properties: " + e.getMessage(), e);
        }
    }

    public DatabaseManager() {
        try (Connection tempConn = createHikariConnection()) {
            initializeDatabase(tempConn);
        } catch (IOException | ClassNotFoundException | SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public Connection createConnection() throws IOException, ClassNotFoundException, SQLException {
        Class.forName(props.getProperty("db.driver"));
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");
        return DriverManager.getConnection(url, user, password);
    }

    public Connection createHikariConnection() throws IOException, ClassNotFoundException, SQLException {
        return hikariDataSource.getConnection();
    }

    private void initConnectionPool() throws ClassNotFoundException {
        Class.forName(props.getProperty("db.driver"));
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(props.getProperty("db.url"));
        hikariConfig.setUsername(props.getProperty("db.user"));
        hikariConfig.setPassword(props.getProperty("db.password"));

        // Cấu hình thêm cho HikariCP (tùy chọn, nhưng được khuyến nghị)
        hikariConfig.addDataSourceProperty("cachePrepStmts", true); // Cache PreparedStatement
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", 250); // Kích thước cache
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048"); // Giới hạn độ dài SQL cho cache
        hikariConfig.setMaximumPoolSize(100); // Số lượng kết nối tối đa trong pool
        hikariConfig.setMinimumIdle(25); // Số lượng kết nối tối thiểu luôn sẵn sàng
        hikariConfig.setConnectionTimeout(30000); // 30 giây
        hikariConfig.setIdleTimeout(600000); // 10 phút
        hikariConfig.setMaxLifetime(1800000); // 30 phút

        hikariDataSource = new HikariDataSource(hikariConfig);
        LOGGER.info("HikariCP connection pool initialized.");
    }

    private void loadProperties() throws IOException {
        props = new Properties();
        try (InputStream inp = this.getClass().getClassLoader()
                .getResourceAsStream("resources/dbpostgres.properties")) {
            if (inp == null) {
                LOGGER.log(Level.WARNING, "dbpostgres.properties not found !");
                throw new IOException("dbpostgres.properties not found !");
            }
            props.load(inp);
        }
    }

    private void initializeDatabase(Connection conn) throws SQLException, IOException, ClassNotFoundException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("resources/ddl.sql")) {
            if (is == null) {
                LOGGER.log(Level.WARNING, "ddl.sql not found !");
                throw new IOException("ddl.sql not found !");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line = "";
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
        }
        String ddlSql = sb.toString();
        if (ddlSql == null || ddlSql.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "ddl.sql is empty or null. Skipping database initialization.");
            return;
        }
        try (Statement stmt = conn.createStatement()) {
            String[] statements = ddlSql.split(";");
            for (String statement : statements) {
                String trimmedStatement = statement.trim();
                if (!trimmedStatement.isEmpty()) {
                    LOGGER.info("Executing DDL: "
                            + trimmedStatement.substring(0, Math.min(trimmedStatement.length(), 100)));
                    stmt.execute(trimmedStatement);
                }
            }
            LOGGER.info("Database schema checked/created successfully from ddl.sql.");
        }
    }

    public Account saveAccount(Connection conn, Account account) throws SQLException {
        String sql = "INSERT INTO account(owner_name, balance) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, account.getOwnerName());
            ps.setDouble(2, account.getBalance());
            if (ps.executeUpdate() == 1) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        return new Account(id, account.getOwnerName(), account.getBalance());
                    }
                }
            }
        }
        return null;
    }

    public void updateAccount(Connection conn, Account account) throws SQLException {
        String sql = "UPDATE account SET owner_name=?, balance = ?  WHERE account_id =?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getOwnerName());
            ps.setDouble(2, account.getBalance());
            ps.setInt(3, account.getAccountId());
            ps.executeUpdate();
        }
    }

    public void adjustAccountBalance(Connection conn, int accountId, double amount) throws SQLException {
        String sql = "UPDATE account SET balance = balance + ?  WHERE account_id =?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        }
    }

    public void saveTransaction(Connection conn, Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transactions(account_id, type, amount, timestamp) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transaction.getAccountId());
            ps.setString(2, transaction.getType().name());
            ps.setDouble(3, transaction.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(transaction.getTimestamp()));
            ps.executeUpdate();
        }
    }

    /**
     * this is overloading
     * 
     * @param fromAccount
     * @param toAccount
     * @param amount
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    @Overloading
    public void saveTransaction(Connection conn, Account fromAccount, Account toAccount, double amount)
            throws SQLException {
        String sqlacc = "UPDATE account SET balance= balance + ? WHERE account_id = ?";
        String sqltrans = "INSERT INTO transactions(account_id, type, amount, timestamp) VALUES (?, ?, ?, ?)";
        try (PreparedStatement psAcc = conn.prepareStatement(sqlacc);
                PreparedStatement psTrans = conn.prepareStatement(sqltrans)) {
            psAcc.setDouble(1, amount * -1);
            psAcc.setInt(2, fromAccount.getAccountId());
            psAcc.addBatch();

            psAcc.setDouble(1, amount);
            psAcc.setInt(2, toAccount.getAccountId());
            psAcc.addBatch();
            psAcc.executeBatch();

            Timestamp timeNow = Timestamp.valueOf(LocalDateTime.now());
            psTrans.setInt(1, fromAccount.getAccountId());
            psTrans.setString(2, Type.TRANSFER.name());
            psTrans.setDouble(3, amount);
            psTrans.setTimestamp(4, timeNow);
            psTrans.addBatch();

            psTrans.setInt(1, toAccount.getAccountId());
            psTrans.setString(2, Type.TRANSFER.name());
            psTrans.setDouble(3, amount);
            psTrans.setTimestamp(4, timeNow);
            psTrans.addBatch();

            psTrans.executeBatch();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            throw e;
        }
    }

    public Account getAccountById(Connection conn, int accountId) throws SQLException {
        String sql = "SELECT account_id, owner_name, balance FROM account WHERE account_id = ?";
        Account account = null;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("account_id");
                    String ownername = rs.getString("owner_name");
                    Double balance = rs.getDouble("balance");
                    account = new Account(id, ownername, balance);
                }
            }
        }
        return account;
    }

    @Overloading
    public Account getAccountById(Connection conn, int accountId, boolean forUpdate) throws SQLException {
        String sql = "SELECT account_id, owner_name, balance FROM account WHERE account_id = ?";
        if (forUpdate) {
            sql += " FOR UPDATE";
        }
        Account account = null;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("account_id");
                    String ownername = rs.getString("owner_name");
                    Double balance = rs.getDouble("balance");
                    account = new Account(id, ownername, balance);
                }
            }
        }
        return account;
    }

    public List<Transaction> getTransactionsByAccountId(Connection conn, int accountId) throws SQLException {
        String sql = "SELECT transaction_id, account_id, type, amount, timestamp FROM transactions WHERE account_id = ?";
        List<Transaction> transactions = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String transactionId = rs.getString("transaction_id");
                    int accid = rs.getInt("account_id");
                    Type type = Type.valueOf(rs.getString("type"));
                    Double amount = rs.getDouble("amount");
                    LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
                    Transaction transaction = new Transaction(transactionId, accid, type, amount, timestamp);
                    transactions.add(transaction);
                }
            }
        }
        return transactions;
    }

    public void close() {
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            hikariDataSource.close();
            LOGGER.info("HikariCP connection pool closed.");
        }
    }
}
