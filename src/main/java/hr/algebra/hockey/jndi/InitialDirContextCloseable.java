package hr.algebra.hockey.jndi;

import java.util.Hashtable;

public final class InitialDirContextCloseable implements AutoCloseable {
    private final Hashtable<String, String> environment;

    public InitialDirContextCloseable(Hashtable<String, String> environment) {
        this.environment = environment;
    }

    public Hashtable<String, String> getEnvironment() {
        return environment;
    }

    @Override
    public void close() {
        environment.clear();
    }
}