package hr.algebra.hockey.utils;

import hr.algebra.hockey.exception.ChatActionException;
import hr.algebra.hockey.jndi.ConfigurationKey;
import hr.algebra.hockey.jndi.ConfigurationReader;
import hr.algebra.hockey.model.PlayerType;
import hr.algebra.hockey.rmi.ChatRemoteService;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Optional;

public final class ChatUtils {
    private ChatUtils() {
    }

    public static Optional<ChatRemoteService> initializeChatRemoteService() {
        try {
            Registry registry = LocateRegistry.getRegistry(
                    ConfigurationReader.getString(ConfigurationKey.HOST_NAME),
                    ConfigurationReader.getInteger(ConfigurationKey.RMI_SERVER_PORT));
            return Optional.of((ChatRemoteService) registry.lookup(ChatRemoteService.REMOTE_OBJECT_NAME));
        } catch (NotBoundException | java.io.IOException exception) {
            return Optional.empty();
        }
    }

    public static List<String> getAllMessages(ChatRemoteService chatRemoteService) {
        try {
            return chatRemoteService.getAllMessages();
        } catch (RemoteException exception) {
            throw new ChatActionException("Reading RMI chat messages failed.", exception);
        }
    }

    public static void sendChatMessage(
            ChatRemoteService chatRemoteService,
            PlayerType playerType,
            String message) {
        try {
            chatRemoteService.sendChatMessage(playerType + ": " + message.trim());
        } catch (RemoteException exception) {
            throw new ChatActionException("Sending RMI chat message failed.", exception);
        }
    }
}
