package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import server.command.CommandDispatcher;

/**
 * 
 * ClientHandler
 * each instance represent of client who is connecting and process I/O on a
 * virtual thread
 */

public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    private final Socket socket;
    private String clientId;
    private String displayName;
    private BufferedReader reader;
    private PrintWriter writer;
    private final CommandDispatcher dispatcher;

    protected ClientHandler(Socket socket) {
        this.socket = socket;
        this.dispatcher = new CommandDispatcher();
    }

    public String getClientId() {
        return clientId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void notifyMessage(String message) {
        writer.println(message);
    }

    public void close() {
        try {
            ChatHub chatHub = ChatHub.getInstance();
            SoaptaServer soaptaServer = SoaptaServer.getInstance();
            chatHub.unresigterClient(this.clientId);
            displayName = null;
            clientId = null;
            socket.close();
            soaptaServer.releaseConn();
        } catch (IOException e) {
            // later
        }
    }

    @Override
    public void run() {
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            this.displayName = reader.readLine();
            if (this.displayName == null) {
                LOGGER.warning("Client disconnected before sending display name");
                return;
            }

            ChatHub chatHub = ChatHub.getInstance();
            String clientId = chatHub.registerClient(displayName, this);
            this.clientId = clientId;
            
            String line;
            while ((line = reader.readLine()) != null) {
                dispatcher.dispatch(this, line);
            }
        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        } finally {
            close();
        }
    }
}
