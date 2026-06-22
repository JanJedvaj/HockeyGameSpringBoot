package hr.algebra.hockey.jndi;

import javax.naming.Context;
import javax.naming.NamingException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

public final class ConfigurationReader {
    private static final String INITIAL_CONTEXT_FACTORY = "com.sun.jndi.fscontext.RefFSContextFactory";
    private static final String PROVIDER_URL = "file:./conf/";
    private static final String CONFIGURATION_FILE = "app.conf";

    private static Properties properties;

    private ConfigurationReader() {
    }

    public static String getString(ConfigurationKey configurationKey) throws IOException, NamingException {
        String value = getProperties().getProperty(configurationKey.getKeyName());
        if (value == null) {
            throw new IllegalStateException("Missing configuration value: " + configurationKey.getKeyName());
        }
        return value.trim();
    }

    public static int getInteger(ConfigurationKey configurationKey) throws IOException, NamingException {
        return Integer.parseInt(getString(configurationKey));
    }

    public static String describeConfiguration() throws IOException, NamingException {
        return "Host " + getString(ConfigurationKey.HOST_NAME)
                + ", P1 socket " + getInteger(ConfigurationKey.PLAYER_ONE_SERVER_PORT)
                + ", P2 socket " + getInteger(ConfigurationKey.PLAYER_TWO_SERVER_PORT)
                + ", RMI " + getInteger(ConfigurationKey.RMI_SERVER_PORT);
    }

    private static synchronized Properties getProperties() throws IOException, NamingException {
        if (properties == null) {
            properties = loadPropertiesThroughJndi();
        }
        return properties;
    }

    private static Properties loadPropertiesThroughJndi() throws IOException, NamingException {
        Hashtable<String, String> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
        environment.put(Context.PROVIDER_URL, PROVIDER_URL);

        Properties loadedProperties = new Properties();
        try (InitialDirContextCloseable context = new InitialDirContextCloseable(environment)) {
            Object configurationObject = context.lookup(CONFIGURATION_FILE);
            try (FileReader reader = new FileReader(configurationObject.toString())) {
                loadedProperties.load(reader);
            }
        }
        return loadedProperties;
    }
}