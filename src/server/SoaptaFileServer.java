package server;

import java.net.Socket;

public class SoaptaFileServer extends SoaptaServer {

    private SoaptaFileServer() {
        super(8081);
    }

    private static class Holder {
        private static final SoaptaFileServer INSTANCE = new SoaptaFileServer();
    }

    public static SoaptaFileServer getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    protected Runnable createHandler(Socket socket) {
        return new FileHandler(socket, this);
    }
}
