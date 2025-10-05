package data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import resources.annotations.Service;

@Service
public abstract class DatabaseManagerFactory {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManagerFactory.class.getName());
    public static Map<String, Supplier<IDatabaseManager>> factory = new ConcurrentHashMap<>();

    static {
        try {
            Class.forName("data.PostgreSQLDatabaseManage");
            Class.forName("data.MySQLDatabaseManage");
            LOGGER.info("DatabaseManager implementations pre-loaded and registered.");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to pre-load database manager classes for registration: " + e.getMessage(),
                    e);
        }
    }

    public static void register(String dbType, Supplier<IDatabaseManager> supplier) {
        factory.put(dbType.toLowerCase(), supplier);
    }

    public static IDatabaseManager create(String dbType) {
        Supplier<IDatabaseManager> supplier = factory.get(dbType.toLowerCase());
        if (supplier == null) {
            throw new IllegalArgumentException("Unsupported DB type: " + dbType);
        }
        return supplier.get();
    }
}
