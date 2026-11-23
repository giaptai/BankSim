package common.constants;

public final class AppConstants {
    private AppConstants() {
    }

    public final static long MAX_FILE_SIZE = 150L * 1024 * 1024;
    public static final int MAX_CONNECTIONS = 100;
    public static final int MAX_FILE_CONNECTIONS = 50;
    public static final String VALIDATE_IP = "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$";
    public static final String VALIDATE_DISPLAY_NAME = "^[a-zA-Z0-9_ -]{2,20}$";
}
