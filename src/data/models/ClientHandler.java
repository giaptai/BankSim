package data.models;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import client.controller.SoaptaServer;
import common.MessageType;
import data.FileTransfer;
import data.Group;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

public class ClientHandler implements Runnable, Closeable {
    private final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    private final Socket socket;
    private final SoaptaServer server;
    private DataInputStream in;
    private DataOutputStream out;
    private String threadName;
    private String displayName;
    private FileTransfer currentUpload;

    public ClientHandler(Socket socket, SoaptaServer server) throws IOException {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            // enter name
            while (threadName == null) {
                byte f1 = in.readByte(); // read 1 byte
                int len = in.readInt(); // read next 4 bytes
                byte[] data = in.readNBytes(len);
                if (MessageType.TEXT.getCode() != f1) {
                    send(new Message(MessageType.TEXT, "ERR|NOT_JOINED"));
                    continue;
                }
                String line = new String(data, StandardCharsets.UTF_8);
                if (!line.startsWith("NAME|")) {
                    send(new Message(MessageType.TEXT, "ERR|NOT_JOINED"));
                    continue;
                }
                String name = line.substring(5);
                handleName(name);
            }
            // chat
            while (!socket.isClosed()) {
                byte f1 = in.readByte(); // read first 1 byte
                int len = in.readInt(); // read next 4 bytes
                byte[] data = new byte[len];
                in.readFully(data);
                if (MessageType.TEXT.getCode() == f1) {
                    String line = new String(data, StandardCharsets.UTF_8);
                    handleCommand(line);
                } else if (MessageType.FILE.getCode() == f1) {
                    handleFileChunk(data);
                }
            }

        } catch (EOFException e) {
            LOGGER.info("Client disconnected: " + threadName);
        } catch (IOException e) {
            LOGGER.info("Connection error with the client " + threadName + ": " + e.getMessage());
        } finally {
            close();
        }
    }

    public String getThreadName() {
        return this.threadName;
    }

    public String getDisplayName(){
        return this.displayName;
    }

    public synchronized void send(Message msg) throws IOException {
        MessageCodec.write(out, msg);
    }

    public synchronized void sendChunk(byte[] data, int length) throws IOException {

    }

    private void handleName(String name) throws IOException {
        if (!name.matches("^[\\p{L}\\d_]{2,20}$")) {
            send(new Message(MessageType.TEXT, "ERR|INVALID_NAME"));
            return;
        }
        this.displayName = name;
        this.threadName = server.nextThreadName(this.displayName);
        server.register(this);
        send(new Message(MessageType.TEXT, "OK|NAME|" + this.threadName));
    }

    private void handleCommand(String line) throws IOException {
        if (line == null || line.isBlank()) {
            return;
        }
        String[] parts = line.split("\\|", 2);
        String cmd = parts[0];
        String rest = (parts.length > 1) ? parts[1] : "";
        switch (cmd) {
            case "LIST" -> handleList();
            case "GLIST" -> handleGroupList();
            case "GCREATE" -> handleGroupCreate(rest);
            case "PM" -> {
                String[] data = rest.split("\\|", 2);
                if (data.length < 2) {
                    send(new Message(MessageType.TEXT, "ERR|UNKNOWN_COMMAND"));
                    return;
                }
                String toThread = data[0];
                String content = data[1];
                handlePrivateMessage(toThread, content);
            }
            case "GADD" -> {
                String[] data = rest.split("\\|", 2);
                if (data.length < 2) {
                    send(new Message(MessageType.TEXT, "ERR|UNKNOWN_COMMAND"));
                    return;
                }
                String groupId = data[0];
                String memberThread = data[1];
                handleGroupAdd(groupId, memberThread);
            }
            case "GMSG" -> {
                String[] data = rest.split("\\|", 2);
                if (data.length < 2) {
                    send(new Message(MessageType.TEXT, "ERR|UNKNOWN_COMMAND"));
                    return;
                }
                String groupId = data[0];
                String content = data[1];
                handleGroupMessage(groupId, content);
            }
            case "FILE_START" -> {
                String[] data = rest.split("\\|", 3);
                if (data.length < 3) {
                    send(new Message(MessageType.TEXT, "ERR|UNKNOWN_COMMAND"));
                    return;
                }
                String toThread = data[0];
                String fileName = data[1];
                long size = 0L;
                try {
                    size = Long.parseLong(data[2]);
                } catch (NumberFormatException e) {
                    send(new Message(MessageType.TEXT, "ERR|UNKNOWN_COMMAND"));
                    return;
                }
                handleFileStart(toThread, fileName, size);
            }
            case "FILE_END" -> handleFileEnd();
            default -> send(new Message(MessageType.TEXT, "ERR|UNKNOWN_COMMAND"));
        }
    }

    private void handleList() throws IOException {
        List<String> onlineClients = server.listOnline();
        String data = String.join(",", onlineClients);
        send(new Message(MessageType.TEXT, "LIST|" + data));
    }

    private void handlePrivateMessage(String toThread, String content) throws IOException {
        boolean sent = server.route(toThread, new Message(MessageType.TEXT, "MSG|" + this.threadName + "|" + content));
        if (!sent) {
            send(new Message(MessageType.TEXT, "ERR|USER_OFFLINE|" + toThread));
        }
    }

    private void handleGroupCreate(String groupName) throws IOException {
        if (!groupName.matches("^[\\p{L}\\h0-9_]{2,20}$")) {
            send(new Message(MessageType.TEXT, "ERR|INVALID_NAME"));
            return;
        }
        String groupId = server.getGroupManager().createGroup(groupName, this.threadName);
        send(new Message(MessageType.TEXT, "OK|GCREATE|" + groupId + "|" + groupName));
    }

    private void handleGroupAdd(String groupId, String memberThread) throws IOException {
        boolean isGroupExist = server.getGroupManager().exists(groupId);
        if (!isGroupExist) {
            send(new Message(MessageType.TEXT, "ERR|GROUP_NOT_FOUND"));
            return;
        }
        Group group = server.getGroupManager().get(groupId);
        if (!group.getOwnerThread().equals(this.threadName)) {
            send(new Message(MessageType.TEXT, "ERR|NOT_OWNER"));
            return;
        }
        ClientHandler handler = server.find(memberThread);
        if (handler == null) {
            send(new Message(MessageType.TEXT, "ERR|USER_OFFLINE"));
            return;
        }
        boolean status = server.getGroupManager().addMember(groupId, memberThread);
        if (status) {
            server.sendNotice(memberThread, "You were added to group \"" + group.getName() + "\" (" + groupId + ")");
            for (String mem : group.getMembers()) {
                if (!mem.equals(memberThread) && !mem.equals(this.threadName)) {
                    server.sendNotice(mem, memberThread + " joined group \"" + group.getName() + "\"");
                }
            }
        }
        send(new Message(MessageType.TEXT, "OK|GADD|" + groupId));
    }

    private void handleGroupList() throws IOException {
        List<Group> groups = server.getGroupManager().groupsOf(threadName);
        List<String> groupName = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (Group group : groups) {
            String groupTemp = String.join(":", group.getId(), group.getName(),
                    String.valueOf(group.getMembers().size()));
            groupName.add(groupTemp);
        }
        String groupsFormat = groupName.stream().collect(Collectors.joining(","));
        sb.append("GLIST|").append(groupsFormat);
        // sb.insert(0, "GLIST|"); // cách 2
        send(new Message(MessageType.TEXT, sb.toString()));
    }

    private void handleGroupMessage(String groupId, String content) throws IOException {
        Group group = server.getGroupManager().get(groupId);
        if (group == null) {
            send(new Message(MessageType.TEXT, "ERR|GROUP_NOT_FOUND"));
            return;
        }
        boolean isMem = group.isMember(this.threadName);
        if (!isMem) {
            send(new Message(MessageType.TEXT, "ERR|NOT_MEMBER"));
            return;
        }
        List<String> mems = group.getMembers().stream().filter(m -> !m.equals(this.threadName))
                .collect(Collectors.toList());
        String payload = String.join("|", "GMSG", groupId, this.threadName, content);

        for (String mem : mems) {
            server.route(mem, new Message(MessageType.TEXT, payload));
        }
    }

    private void handleFileStart(String toThread, String fileName, long size) {

    }

    private void handleFileChunk(byte[] data) {
    }

    private void handleFileEnd() {

    }

    private void failTransfer() {
    }

    @Override
    public void close() {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
        }
        if (this.threadName != null) {
            server.unregister(threadName);
        }
        server.getConnLimit().release();
    }
}
