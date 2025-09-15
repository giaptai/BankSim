package data;

import java.io.InputStream;
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

import data.models.Account;
import data.models.Transaction;
import resources.Type;
import resources.annotations.Overloading;
import resources.annotations.Repository;

@Repository
public class DatabaseManager {

    public DatabaseManager() {
    }

    private Connection getConnection() throws IOException, ClassNotFoundException, SQLException {
        Properties props = new Properties();
        try (InputStream inp = this.getClass().getClassLoader().getResourceAsStream("resources/application.properties")) {
            if (inp == null) {
                throw new IOException("application.properties not found !");
            }
            props.load(inp);
        }
        Class.forName(props.getProperty("db.driver")); // optional
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");
        return DriverManager.getConnection(url, user, password);
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
        String sql = "UPDATE account SET owner_name=?, balance=?  WHERE account_id =?";
        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getOwnerName());
            ps.setDouble(2, account.getBalance());
            ps.setInt(3, account.getAccountId());
            ps.executeUpdate();
        }
    }

    public void saveTransaction(Transaction transaction) throws IOException, ClassNotFoundException, SQLException {
        String sql = "INSERT INTO transactions(account_id, type, amount, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transaction.getAccountId());
            ps.setString(2, transaction.getType().name());
            ps.setDouble(3, transaction.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(transaction.getTimestamp()));
            ps.executeUpdate();
        }
    }

    // this is overloading
    @Overloading
    public void saveTransaction(Account fromAccount, Account toAccount, double amount)
            throws IOException, ClassNotFoundException, SQLException {
        String sqlacc = "UPDATE account SET balance=? WHERE account_id = ?";
        String sqltrans = "INSERT INTO transactions(account_id, type, amount, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psAcc = conn.prepareStatement(sqlacc);
                    PreparedStatement psTrans = conn.prepareStatement(sqltrans)) {

                // Update fromAccount
                psAcc.setDouble(1, fromAccount.getBalance());
                psAcc.setInt(2, fromAccount.getAccountId());
                psAcc.addBatch();

                // Update toAccount
                psAcc.setDouble(1, toAccount.getBalance());
                psAcc.setInt(2, toAccount.getAccountId());
                psAcc.addBatch();

                psAcc.executeBatch();

                // Insert transaction logs (batch insert)
                // transactions of fromAccount

                Timestamp timeNow = Timestamp.valueOf(LocalDateTime.now());

                psTrans.setInt(1, fromAccount.getAccountId());
                psTrans.setString(2, Type.TRANSFER.name());
                psTrans.setDouble(3, amount);
                psTrans.setTimestamp(4, timeNow);
                psTrans.addBatch();

                // transactions of toAccount
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
        String sql = "SELECT account_id, type, amount, timestamp FROM transactions WHERE account_id = ?";
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // String transactionId = rs.getString("transaction_id");
                    int accid = rs.getInt("account_id");
                    Type type = Type.valueOf(rs.getString("type"));
                    Double amount = rs.getDouble("amount");
                    LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
                    Transaction transaction = new Transaction(accid, type, amount, timestamp);
                    transactions.add(transaction);
                }
            }
        }
        return transactions;
    }

    public void closeConnection() {

    }
}
