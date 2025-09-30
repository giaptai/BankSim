package data;

import resources.annotations.Service;

@Service
public abstract class DatabaseManagerFactory {
    public static IDatabaseManager create(String dbType) {
        return switch (dbType.toLowerCase()) {
            case "postgres" -> new PostgreSQLDatabaseManage();
            case "mysql" -> new MySQLDatabaseManage();
            default -> throw new IllegalArgumentException("Unsupported DB type: " + dbType);
        };
    }
}
