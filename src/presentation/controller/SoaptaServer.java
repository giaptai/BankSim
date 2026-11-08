package presentation.controller;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import data.models.Message;
import data.GroupManager;
import data.models.ClientHandler;
import resources.Constants;

public class SoaptaServer {
    private static Logger LOGGER = Logger.getLogger(SoaptaServer.class.getName());
    private final Semaphore connLimit;
    private final AtomicInteger threadCounter;
    private final Map<String, ClientHandler> onlineClients;
    private final GroupManager groupManager;
    private final ExecutorService connExecutor;
    private ServerSocket serverSocket;

    public SoaptaServer() {
        this.connLimit = new Semaphore(Constants.MAX_CONNECTIONS);
        this.threadCounter = new AtomicInteger(0);
        this.onlineClients = new ConcurrentHashMap<>();
        this.groupManager = new GroupManager();
        this.connExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void start() throws IOException {
        this.serverSocket = new ServerSocket(Constants.PORT);
        LOGGER.info("Server is listening on port " + Constants.PORT);
        while (!serverSocket.isClosed()) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                if (!connLimit.tryAcquire()) { // get a slot
                    LOGGER.warning("Connection rejected. Max limit reached.");
                    socket.close();
                    continue;
                }

                try {
                    connExecutor.submit(new ClientHandler(socket, this));
                } catch (Exception e) {
                    LOGGER.warning("Failed to submit client handler: " + e.getMessage());
                    connLimit.release(); // return slot
                    socket.close();
                }
            } catch (IOException e) {
                if (serverSocket.isClosed()) {
                    LOGGER.info("Server socket is closed. Stopping server...");
                    break;
                } else {
                    LOGGER.warning("Accept failed: " + e.getMessage());
                }
            }
        }
    }

    public String nextThreadName(String displayName) {
        int id = threadCounter.incrementAndGet();
        return "thread" + id + "-client-" + displayName;
    }

    public void register(ClientHandler handler) {
        onlineClients.put(handler.getThreadName(), handler);
        LOGGER.info("Registered client: " + handler.getThreadName());
    }

    public void unregister(String threadName) {
        if (threadName != null) {
            onlineClients.remove(threadName);
            LOGGER.info("Unregistered client: " + threadName);
        }
    }

    public ClientHandler find(String threadName) {
        return onlineClients.getOrDefault(threadName, null);
    }

    public List<String> listOnline() {
        return onlineClients.keySet().stream().toList();
    }

    public boolean route(String toThread, Message msg) {
        ClientHandler handler = find(toThread);
        if (handler == null) {
            return false;
        }
        try {
            handler.send(msg);
            return true;
        } catch (IOException e) {
            LOGGER.warning("Failed to route message to " + toThread + ": " + e.getMessage());
            return false;
        }
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LOGGER.warning("Error closing server socket: " + e.getMessage());
        }
        for (ClientHandler client : onlineClients.values()) {
            client.close();
        }
        onlineClients.clear();

        connExecutor.shutdown(); // stop receive new task, continue task running
        try {
            if (!connExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                connExecutor.shutdownNow(); // send a interrupt to task's running
            }
        } catch (InterruptedException e) {
            connExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public GroupManager getGroupManager() {
        return this.groupManager;
    }

    public static void main(String[] args) throws IOException {
        new SoaptaServer().start();
    }
}
