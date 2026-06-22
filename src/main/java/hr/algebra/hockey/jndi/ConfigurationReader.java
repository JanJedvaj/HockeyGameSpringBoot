package hr.algebra.hockey.jndi;

import javax.naming.NamingException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.Properties;

public final class ConfigurationReader {
    private static final Path CONFIGURATION_PATH = Path.of("conf", "app.conf");

    private ConfigurationReader() {
    }

    public static String getString(ConfigurationKey configurationKey) throws IOException {
        try (InitialDirContextCloseable context = createContext()) {
            Object value = context.getEnvironment().get(configurationKey.getKeyName());
            if (value == null) {
                throw new IllegalStateException("Missing configuration value: " + configurationKey.getKeyName());
            }
            return value.toString();
        }
    }

    public static int getInteger(ConfigurationKey configurationKey) throws IOException {
        return Integer.parseInt(getString(configurationKey));
    }

    public static String describeConfiguration() throws IOException {
        return "Host " + getString(ConfigurationKey.HOST_NAME)
                + ", P1 socket " + getInteger(ConfigurationKey.PLAYER_ONE_SERVER_PORT)
                + ", P2 socket " + getInteger(ConfigurationKey.PLAYER_TWO_SERVER_PORT)
                + ", RMI " + getInteger(ConfigurationKey.RMI_SERVER_PORT);
    }

    private static InitialDirContextCloseable createContext() throws IOException {
        Properties properties = loadProperties();
        Hashtable<String, String> environment = new Hashtable<>();
        for (String propertyName : properties.stringPropertyNames()) {
            environment.put(propertyName, properties.getProperty(propertyName));
        }
        return new InitialDirContextCloseable(environment);
    }

    private static Properties loadProperties() throws IOException {
        if (!Files.exists(CONFIGURATION_PATH)) {
            throw new IOException("Configuration file not found: " + CONFIGURATION_PATH);
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(CONFIGURATION_PATH)) {
            properties.load(inputStream);
        }
        return properties;
    }
}
