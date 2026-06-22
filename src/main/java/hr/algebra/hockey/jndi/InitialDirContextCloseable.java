package hr.algebra.hockey.jndi;

import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

public final class InitialDirContextCloseable extends InitialDirContext implements AutoCloseable {
    public InitialDirContextCloseable(Hashtable<?, ?> environment) throws NamingException {
        super(environment);
    }
}