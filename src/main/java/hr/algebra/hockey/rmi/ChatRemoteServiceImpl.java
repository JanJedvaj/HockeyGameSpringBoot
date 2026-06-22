package hr.algebra.hockey.rmi;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatRemoteServiceImpl implements ChatRemoteService {
    private final List<String> chatMessages = new CopyOnWriteArrayList<>();

    @Override
    public void sendChatMessage(String message) throws RemoteException {
        if (message != null && !message.isBlank()) {
            chatMessages.add(message);
        }
    }

    @Override
    public List<String> getAllMessages() throws RemoteException {
        return new ArrayList<>(chatMessages);
    }
}