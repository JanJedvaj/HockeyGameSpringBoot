package hr.algebra.hockey.network;

import hr.algebra.hockey.model.GameState;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SocketMultiplayerService implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(SocketMultiplayerService.class.getName());
    private static final int RETRY_DELAY_MILLISECONDS = 1_000;
    private static final int OUTBOUND_QUEUE_CAPACITY = 4;

    private final boolean host;
    private final String hostName;
    private final int port;
    private final Consumer<GameState> gameStateConsumer;
    private final Runnable launchRequestHandler;
    private final Consumer<String> statusConsumer;
    private final BlockingQueue<MultiplayerMessage> outboundMessages =
            new LinkedBlockingQueue<>(OUTBOUND_QUEUE_CAPACITY);

    private volatile boolean running;
    private volatile boolean connected;
    private ServerSocket serverSocket;
    private Socket socket;

    private SocketMultiplayerService(
            boolean host,
            String hostName,
            int port,
            Consumer<GameState> gameStateConsumer,
            Runnable launchRequestHandler,
            Consumer<String> statusConsumer) {
        this.host = host;
        this.hostName = hostName;
        this.port = port;
        this.gameStateConsumer = gameStateConsumer;
        this.launchRequestHandler = launchRequestHandler;
        this.statusConsumer = statusConsumer;
    }

    public static SocketMultiplayerService createHost(
            int port,
            Runnable launchRequestHandler,
            Consumer<String> statusConsumer) {
        return new SocketMultiplayerService(true, null, port, null, launchRequestHandler, statusConsumer);
    }

    public static SocketMultiplayerService createClient(
            String hostName,
            int port,
            Consumer<GameState> gameStateConsumer,
            Consumer<String> statusConsumer) {
        return new SocketMultiplayerService(false, hostName, port, gameStateConsumer, null, statusConsumer);
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        Thread connectionThread = new Thread(host ? this::runHost : this::runClient,
                host ? "hockey-socket-host" : "hockey-socket-client");
        connectionThread.setDaemon(true);
        connectionThread.start();
        LOGGER.info("Started " + connectionThread.getName() + " on port " + port);
    }

    public boolean isConnected() {
        return connected;
    }

    public void sendGameState(GameState gameState) {
        if (host && connected) {
            enqueueLatest(MultiplayerMessage.gameState(gameState));
        }
    }

    public void sendLaunchRequest() {
        if (!host && connected) {
            enqueueLatest(MultiplayerMessage.launchRequest());
        }
    }

    private void runHost() {
        try (ServerSocket listeningSocket = new ServerSocket(port)) {
            serverSocket = listeningSocket;
            LOGGER.info("Socket host listening on port " + port);
            publishStatus("PLAYER_1 waiting for PLAYER_2 on port " + port + ".");
            while (running) {
                try {
                    Socket acceptedSocket = listeningSocket.accept();
                    LOGGER.info("Accepted multiplayer connection from " + acceptedSocket.getRemoteSocketAddress());
                    handleConnection(acceptedSocket);
                } catch (SocketException exception) {
                    if (running) {
                        publishStatus("Multiplayer host socket error: " + exception.getMessage());
                    }
                }
            }
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Multiplayer host failed", exception);
            if (running) {
                publishStatus("Unable to start multiplayer host: " + exception.getMessage());
            }
        }
    }

    private void runClient() {
        while (running) {
            try {
                publishStatus("PLAYER_2 connecting to " + hostName + ":" + port + "...");
                handleConnection(new Socket(hostName, port));
            } catch (ConnectException exception) {
                LOGGER.fine("PLAYER_2 connection retry: " + exception.getMessage());
                sleepBeforeRetry();
            } catch (IOException exception) {
                LOGGER.log(Level.WARNING, "Multiplayer client connection failed", exception);
                if (running) {
                    publishStatus("Multiplayer client error: " + exception.getMessage());
                    sleepBeforeRetry();
                }
            }
        }
    }

    private void handleConnection(Socket connectedSocket) throws IOException {
        socket = connectedSocket;
        connected = true;
        LOGGER.info("Multiplayer socket connected: " + connectedSocket.getRemoteSocketAddress());
        outboundMessages.clear();
        publishStatus(host ? "PLAYER_2 connected." : "Connected to PLAYER_1.");

        try (Socket currentSocket = connectedSocket;
             ObjectOutputStream outputStream = new ObjectOutputStream(currentSocket.getOutputStream());
             ObjectInputStream inputStream = new ObjectInputStream(currentSocket.getInputStream())) {
            Thread writerThread = createWriterThread(currentSocket, outputStream);
            writerThread.start();
            readMessages(inputStream);
        } finally {
            LOGGER.info("Multiplayer socket connection closed.");
            connected = false;
            socket = null;
            outboundMessages.clear();
            if (running) {
                publishStatus("Multiplayer connection closed.");
            }
        }
    }

    private Thread createWriterThread(Socket connectionSocket, ObjectOutputStream outputStream) {
        Thread writerThread = new Thread(() -> {
            while (running && connected && socket == connectionSocket) {
                try {
                    MultiplayerMessage message = outboundMessages.poll(250, TimeUnit.MILLISECONDS);
                    if (message == null) {
                        continue;
                    }
                    outputStream.writeObject(message);
                    outputStream.flush();
                    outputStream.reset();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (IOException exception) {
                    LOGGER.log(Level.WARNING, "Socket writer stopped after an I/O error", exception);
                    closeSocketQuietly();
                    return;
                }
            }
        }, host ? "hockey-host-writer" : "hockey-client-writer");
        writerThread.setDaemon(true);
        return writerThread;
    }

    private void readMessages(ObjectInputStream inputStream) throws IOException {
        while (running && connected) {
            try {
                Object object = inputStream.readObject();
                if (object instanceof MultiplayerMessage message) {
                    handleMessage(message);
                }
            } catch (EOFException | SocketException exception) {
                return;
            } catch (ClassNotFoundException exception) {
                LOGGER.log(Level.WARNING, "Unknown multiplayer message class", exception);
                publishStatus("Unknown multiplayer message received.");
            }
        }
    }

    private void handleMessage(MultiplayerMessage message) {
        if (message.getType() == MultiplayerMessage.Type.GAME_STATE
                && !host
                && message.getGameState() != null
                && gameStateConsumer != null) {
            gameStateConsumer.accept(message.getGameState());
        } else if (message.getType() == MultiplayerMessage.Type.LAUNCH_REQUEST
                && host
                && launchRequestHandler != null) {
            launchRequestHandler.run();
        }
    }

    private void enqueueLatest(MultiplayerMessage message) {
        if (!outboundMessages.offer(message)) {
            outboundMessages.poll();
            outboundMessages.offer(message);
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    private void publishStatus(String status) {
        if (statusConsumer != null) {
            statusConsumer.accept(status);
        }
    }

    private void closeSocketQuietly() {
        Socket currentSocket = socket;
        if (currentSocket != null) {
            try {
                currentSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public void close() {
        running = false;
        connected = false;
        closeSocketQuietly();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        LOGGER.info("Socket multiplayer service shut down.");
    }
}
