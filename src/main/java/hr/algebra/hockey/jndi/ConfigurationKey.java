package hr.algebra.hockey.jndi;

public enum ConfigurationKey {
    PLAYER_ONE_SERVER_PORT("player.one.server.port"),
    PLAYER_TWO_SERVER_PORT("player.two.server.port"),
    HOST_NAME("host.name"),
    RMI_SERVER_PORT("rmi.server.port");

    private final String keyName;

    ConfigurationKey(String keyName) {
        this.keyName = keyName;
    }

    public String getKeyName() {
        return keyName;
    }
}
