package data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import data.models.Account;
import resources.annotations.Overloading;

import resources.Type;

public class PostgreSQLDatabaseManage implements IDatabaseManager {
    private static Logger LOGGER = Logger.getLogger(PostgreSQLDatabaseManage.class.getName());
    private Properties props;
    private HikariDataSource hikariDataSource;

    private static final String DB_PROPERTIES_PATH = "resources/dbpostgres.properties";
    private static final String DDL_SQL_PATH = "resources/ddl.sql";
    private static final String DML_SQL_PATH = "resources/dml.sql";

    // load properties first
    {
        try {
            loadProperties();
            initConnectionPool();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize properties: " + e.getMessage(), e);
        }
    }

    public PostgreSQLDatabaseManage() {
        try (Connection tempConn = createHikariConnection()) {
            initializeDatabase(tempConn);
            insertDefaultAccount(tempConn);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    @Override
    public void loadProperties() throws IOException {
        props = new Properties();
        try (InputStream ip = this.getClass().getClassLoader()
                .getResourceAsStream(DB_PROPERTIES_PATH)) {
            if (ip == null) {
                LOGGER.log(Level.SEVERE, "dbpostgres.properties not found !");
                throw new IOException("dbpostgres.properties not found !");
            }
            props.load(ip);
            LOGGER.info("DEBUG: db.url read from properties file: " + props.getProperty("db.url"));
        }
    }

    // @Override
    // public Connection createConnection() throws IOException, ClassNotFoundException, SQLException {
    //     Class.forName(props.getProperty("db.driver"));
    //     String url = props.getProperty("db.url");
    //     String user = props.getProperty("db.user");
    //     String password = props.getProperty("db.password");
    //     return DriverManager.getConnection(url, user, password);
    // }

    @Override
    public void initializeDatabase(Connection conn) throws SQLException, IOException, ClassNotFoundException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(DDL_SQL_PATH)) {
            if (is == null) {
                LOGGER.log(Level.SEVERE, "ddl.sql not found !");
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
            LOGGER.log(Level.SEVERE, "ddl.sql is empty or null. Skipping database initialization.");
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

    private void insertDefaultAccount(Connection conn) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(DML_SQL_PATH)) {
            if (is == null) {
                LOGGER.log(Level.SEVERE, "dml.sql not found !");
                throw new IOException("dml.sql not found !");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is,
                    StandardCharsets.UTF_8))) {
                String line = "";
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            String dmlSql = sb.toString();
            if (dmlSql == null || dmlSql.trim().isEmpty()) {
                LOGGER.log(Level.SEVERE, "dml.sql is empty or null. Skipping database initialization.");
                return;
            }
            try (Statement st = conn.createStatement()) {
                String[] statements = dmlSql.split(";");
                for (String statement : statements) {
                    String trimmedStatement = statement.trim();
                    if (!trimmedStatement.isEmpty()) {
                        LOGGER.info("Executing DML: INSERT INTO account");
                        st.execute(trimmedStatement);
                    }
                }
                LOGGER.info("Data inserted successfully from dml.sql.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert default accounts from dml.sql: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public Connection createHikariConnection() throws IOException, ClassNotFoundException, SQLException {
        return hikariDataSource.getConnection();
    }

    @Override
    public void initConnectionPool() throws ClassNotFoundException {
        Class.forName(props.getProperty("db.driver"));
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(props.getProperty("db.url"));
        hikariConfig.setUsername(props.getProperty("db.user"));
        hikariConfig.setPassword(props.getProperty("db.password"));
        // optional
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.setMaximumPoolSize(100);
        hikariConfig.setMinimumIdle(25);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        //
        hikariDataSource = new HikariDataSource(hikariConfig);
        LOGGER.info("HikariCP connection pool initialized.");
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

    @Override
    public void close() {
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            hikariDataSource.close();
            LOGGER.info("HikariCP connection pool closed.");
        }
    }
}