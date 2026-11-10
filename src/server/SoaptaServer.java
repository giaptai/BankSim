package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * SoaptaServer
 * Manage server lifecycle, allocate VVirtual thread and limit connection number
 */
public class SoaptaServer {
    private static final Logger LOGGER = Logger.getLogger(SoaptaServer.class.getName());
    // private static SoaptaServer instance;
    private final int port;
    private final Semaphore connLimit;
    private final ExecutorService executor;
    private volatile boolean running;

    private SoaptaServer() {
        this.port = 8080;
        this.connLimit = new Semaphore(5000);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.running = true;
    }

    private static class Holder {
        private static final SoaptaServer INSTANCE = new SoaptaServer();
    }

    public static SoaptaServer getInstance() {
        return Holder.INSTANCE;
    }

    public void start() {
        LOGGER.log(Level.INFO, "Running Soapta server on port {0}", String.valueOf(this.port));
        try (ServerSocket server = new ServerSocket(port)) {
            while (this.running) {
                acceptLoop(server);
            }
        } catch (IOException e) {
            LOGGER.warning("Server failed: " + e.getMessage());
        }
    }

    private void acceptLoop(ServerSocket serverSocket) {
        try  {
            Socket socket = serverSocket.accept();
            if (connLimit.tryAcquire()) {
                ClientHandler clientHandler = new ClientHandler(socket);
                executor.submit(clientHandler);
            } else{
                
            }
        } catch (IOException e) {
            LOGGER.warning("Server only handle 5000 connections at the same time");
        }
    }

    public void releaseConn() {
        connLimit.release();
    }

    public static void main(String[] args) {
        SoaptaServer.getInstance().start();
    }
}
