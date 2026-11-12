package server;

import java.net.Socket;

public class SoaptaTextServer extends SoaptaServer {

    private static class Holder {
        private static final SoaptaTextServer INSTANCE = new SoaptaTextServer();
    }

    public static SoaptaTextServer getInstance() {
        return Holder.INSTANCE;
    }

    private SoaptaTextServer() {
        super(8080);
    }

    @Override
    protected ClientHandler createHandler(Socket socket) {
        return new ClientHandler(socket, this);
    }
}
