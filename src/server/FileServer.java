package server;

import java.net.Socket;

import common.constants.Constants;

public class FileServer extends SoaptaServer {

    private FileServer() {
        super(8081, Constants.MAX_FILE_CONNECTIONS);
    }

    private static class Holder {
        private static final FileServer INSTANCE = new FileServer();
    }

    public static FileServer getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    protected Runnable createHandler(Socket socket) {
        return new FileHandler(socket, this);
    }
}
