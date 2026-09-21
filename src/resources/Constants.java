package resources;

import java.time.format.DateTimeFormatter;

public interface Constants {
    public static final int MAX_THREADS = 100;
    public static final long MIN_UPDATE_INTERVAL_MS = 50;
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final String DB_PROPERTIES_PATH = "resources/dbpostgres.properties";
    public static final String LOGGING_PROPERTIES_PATH = "resources/logging.properties";
    public static final String DDL_SQL_PATH = "resources/ddl.sql";
    public static final String DML_SQL_PATH = "resources/dml.sql";

    public static final String DB_TYPE_POSTGRES = "postgres";
    public static final String DB_TYPE_MYSQL = "mysql";
}
