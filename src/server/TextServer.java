package server;

import java.net.Socket;

import common.constants.Constants;

public class TextServer extends SoaptaServer {

    private static class Holder {
        private static final TextServer INSTANCE = new TextServer();
    }

    public static TextServer getInstance() {
        return Holder.INSTANCE;
    }

    private TextServer() {
        super(8080, Constants.MAX_CONNECTIONS);
    }

    @Override
    protected Runnable createHandler(Socket socket) {
        return new TextHandler(socket, this);
    }
}
