package hr.algebra.hockey.rmi;

import hr.algebra.hockey.jndi.ConfigurationKey;
import hr.algebra.hockey.jndi.ConfigurationReader;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public final class RmiServer {
    private static final int RANDOM_PORT_HINT = 0;

    private RmiServer() {
    }

    public static void main(String[] args) {
        try {
            int registryPort = ConfigurationReader.getInteger(ConfigurationKey.RMI_SERVER_PORT);
            Registry registry = LocateRegistry.createRegistry(registryPort);
            ChatRemoteService service = new ChatRemoteServiceImpl();
            ChatRemoteService stub = (ChatRemoteService) UnicastRemoteObject.exportObject(service, RANDOM_PORT_HINT);
            registry.rebind(ChatRemoteService.REMOTE_OBJECT_NAME, stub);
            System.out.println("RMI chat server started on port " + registryPort + ".");
        } catch (Exception exception) {
            System.err.println("Unable to start RMI chat server: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
}