package common;

public final class Constants {

    private Constants() {}

    public static final String SERVER_HOST = "127.0.0.1";
    public static final int SERVER_PORT = 8888;

    public static final String USER_FILE_PATH = "resources/users.txt";
    public static final String LOG_DIR = "logs";

    public static final String CHARSET = "UTF-8";

    public static final String PRIVATE_PREFIX = "@";
    public static final String COMMAND_PREFIX = "@@";

    public static final String PROTOCOL_SEPARATOR = "\\|";
    public static final int PROTOCOL_FIELDS = 5;
}
