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
import java.util.logging.Logger;

import data.models.Account;
import data.models.Transaction;
import resources.Type;
import resources.annotations.Overloading;
import resources.annotations.Repository;

@Repository
public class DatabaseManager {
    private static Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private Properties props;
    private Connection conn;

    public DatabaseManager() {
        try {
            loadProperties();
            initializeDatabase();
        } catch (IOException | ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void loadProperties() throws IOException {
        props = new Properties();
        try (InputStream inp = this.getClass().getClassLoader()
                .getResourceAsStream("resources/application.properties")) {
            if (inp == null) {
                throw new IOException("application.properties not found !");
            }
            props.load(inp);
        }
    }

    // public void openConnection() throws IOException, ClassNotFoundException, SQLException {
    //     if (conn == null || conn.isClosed()) {
    //         Class.forName(props.getProperty("db.driver"));
    //         String url = props.getProperty("db.url");
    //         String user = props.getProperty("db.user");
    //         String password = props.getProperty("db.password");
    //         conn = DriverManager.getConnection(url, user, password);
    //         conn.setAutoCommit(false); // nếu muốn quản lý transaction thủ công
    //     }
    // }

    private Connection getConnection() throws IOException, ClassNotFoundException, SQLException {
        Class.forName(props.getProperty("db.driver")); // optional
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");
        return DriverManager.getConnection(url, user, password);
    }

    private void initializeDatabase() throws SQLException, IOException, ClassNotFoundException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("resources/ddl.sql")) {
            if (is == null) {
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
            LOGGER.warning("ddl.sql is empty or null. Skipping database initialization.");
            return;
        }
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
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

    public Account saveAccount(Account account) throws IOException, ClassNotFoundException, SQLException {
        String sql = "INSERT INTO account(owner_name, balance) VALUES (?, ?)";
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

    public void updateAccount(Account account) throws IOException, ClassNotFoundException, SQLException {
        String sql = "UPDATE account SET owner_name=?, balance = ?  WHERE account_id =?";
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getOwnerName());
            ps.setDouble(2, account.getBalance());
            ps.setInt(3, account.getAccountId());
            ps.executeUpdate();
        }
    }

    public void adjustAccountBalance(int accountId, double amount)
            throws IOException, ClassNotFoundException, SQLException {
        String sql = "UPDATE account SET balance = balance + ?  WHERE account_id =?";
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            try {
                conn.setAutoCommit(false);
                ps.setDouble(1, amount);
                ps.setInt(2, accountId);
                ps.addBatch();
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void saveTransaction(Transaction transaction) throws IOException, ClassNotFoundException, SQLException {
        String sql = "INSERT INTO transactions(account_id, type, amount, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            try {
                conn.setAutoCommit(false);
                ps.setInt(1, transaction.getAccountId());
                ps.setString(2, transaction.getType().name());
                ps.setDouble(3, transaction.getAmount());
                ps.setTimestamp(4, Timestamp.valueOf(transaction.getTimestamp()));
                ps.addBatch();
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            } finally {
                conn.setAutoCommit(true);
            }
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
    public void saveTransaction(Account fromAccount, Account toAccount, double amount)
            throws IOException, ClassNotFoundException, SQLException {
        String sqlacc = "UPDATE account SET balance= balance + ? WHERE account_id = ?";
        String sqltrans = "INSERT INTO transactions(account_id, type, amount, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
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
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Account getAccountById(int accountId) throws IOException, ClassNotFoundException, SQLException {
        String sql = "SELECT account_id, owner_name, balance FROM account WHERE account_id = ?";
        Account account = null;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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

    public List<Transaction> getTransactionsByAccountId(int accountId)
            throws IOException, ClassNotFoundException, SQLException {
        String sql = "SELECT transaction_id, account_id, type, amount, timestamp FROM transactions WHERE account_id = ?";
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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

    public void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
