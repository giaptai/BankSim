package server;

import java.net.Socket;

import common.constants.AppConstants;

public class TextServer extends SoaptaServer {

    private static class Holder {
        private static final TextServer INSTANCE = new TextServer();
    }

    public static TextServer getInstance() {
        return Holder.INSTANCE;
    }

    private TextServer() {
        super(8080, AppConstants.MAX_CONNECTIONS);
    }

    @Override
    protected Runnable createHandler(Socket socket) {
        return new TextHandler(socket, this);
    }
}
