package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import common.constants.AppConstants;

/**
 * 
 * SoaptaServer
 * Manage server lifecycle, allocate VVirtual thread and limit connection number
 */
public abstract class SoaptaServer {
    private static final Logger LOGGER = Logger.getLogger(SoaptaServer.class.getName());
    // private static SoaptaServer instance;
    private final int port;
    protected final Semaphore connLimit;
    protected final ExecutorService executor;
    protected volatile boolean running;

    protected SoaptaServer(int port) {
        this.port = port;
        this.connLimit = new Semaphore(AppConstants.MAX_CONNECTIONS);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.running = true;
    }

    protected SoaptaServer(int port, int maxConn) {
        this.port = port;
        this.connLimit = new Semaphore(maxConn);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.running = true;
    }
    /**
     * run socket server
     */
    protected final void start() {
        LOGGER.log(Level.INFO, "Running Soapta server on port {0}", String.valueOf(this.port));
        try (ServerSocket server = new ServerSocket(port)) {
            while (this.running) {
                acceptLoop(server);
            }
        } catch (IOException e) {
            LOGGER.warning("Server failed: " + e.getMessage());
        }
    }

    // accept connection from socket client
    private void acceptLoop(ServerSocket serverSocket) {
        boolean permitAcquired = false;
        try {
            Socket socket = serverSocket.accept();
            if (connLimit.tryAcquire()) {
                permitAcquired = true;
                Runnable client = createHandler(socket);
                executor.submit(client);
                permitAcquired = false;
            } else {
                socket.close();
            }
        } catch (Exception e) {
            LOGGER.warning("Server only handle 5000 connections at the same time");
            if (permitAcquired) {
                releaseConn();
            }
        }
    };

    public void releaseConn() {
        connLimit.release();
    }

    protected abstract Runnable createHandler(Socket socket);

    public static void main(String[] args) {
        // close() chờ các task hoàn thành. Vì hai Server chạy liên tục
        try (ExecutorService service = Executors.newVirtualThreadPerTaskExecutor();) {
            service.submit(() -> TextServer.getInstance().start());
            service.submit(() -> FileServer.getInstance().start());
        } catch (Exception e) {
        }
    }
}
