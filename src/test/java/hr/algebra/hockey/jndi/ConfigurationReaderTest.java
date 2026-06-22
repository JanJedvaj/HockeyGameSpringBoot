package hr.algebra.hockey.jndi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigurationReaderTest {
    @Test
    void readsConfigurationThroughJndiFileContext() throws Exception {
        assertEquals("localhost", ConfigurationReader.getString(ConfigurationKey.HOST_NAME));
        assertEquals(8001, ConfigurationReader.getInteger(ConfigurationKey.PLAYER_ONE_SERVER_PORT));
        assertEquals(1099, ConfigurationReader.getInteger(ConfigurationKey.RMI_SERVER_PORT));
    }
}
