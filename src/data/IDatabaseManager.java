package data;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import data.models.Account;
import data.models.Transaction;
import resources.Type;
import resources.annotations.Overloading;

public interface IDatabaseManager {
    void loadProperties() throws IOException;

    default Connection createConnection() throws IOException, ClassNotFoundException, SQLException {
        return null;
    }

    void initializeDatabase(Connection conn) throws SQLException, IOException, ClassNotFoundException;

    Connection createHikariConnection() throws IOException, ClassNotFoundException, SQLException;

    void initConnectionPool() throws ClassNotFoundException;

    void close();

    default public Account saveAccount(Connection conn, Account account) throws SQLException {
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

    default public void updateAccount(Connection conn, Account account) throws SQLException {
        String sql = "UPDATE account SET owner_name=?, balance = ?  WHERE account_id =?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getOwnerName());
            ps.setDouble(2, account.getBalance());
            ps.setInt(3, account.getAccountId());
            ps.executeUpdate();
        }
    }

    default public void adjustAccountBalance(Connection conn, int accountId, double amount) throws SQLException {
        String sql = "UPDATE account SET balance = balance + ?  WHERE account_id =?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        }
    }

    default public void saveTransaction(Connection conn, Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transactions(account_id, type, amount, timestamp) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transaction.getAccountId());
            ps.setString(2, transaction.getType().name());
            ps.setDouble(3, transaction.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(transaction.getTimestamp()));
            ps.executeUpdate();
        }
    }

    void saveTransaction(Connection conn, Account from, Account to, double amount) throws SQLException;

    default public Account getAccountById(Connection conn, int accountId) throws SQLException {
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
    default public Account getAccountById(Connection conn, int accountId, boolean forUpdate) throws SQLException {
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

    default public List<Transaction> getTransactionsByAccountId(Connection conn, int accountId) throws SQLException {
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
}
