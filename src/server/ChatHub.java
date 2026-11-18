package server;

import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import common.FileTransfer;
import common.constants.AppConstants;

/**
 * Manage user status, chat groups, and message coordination.
 */
public class ChatHub {
    private static final Logger LOGGER = Logger.getLogger(ChatHub.class.getName());
    // private static final ChatHub INSTANCE;
    private final AtomicLong idCounter;
    private final ConcurrentMap<String, TextHandler> clients;
    private final ConcurrentMap<String, Set<String>> groups;

    // file handler
    private final AtomicLong fileIdCouunter;
    private final ConcurrentMap<String, FileTransfer> pendingFiles;
    private final ConcurrentMap<String, CompletableFuture<Socket>> receiverSockets;
    private final ConcurrentMap<String, CompletableFuture<Void>> transferDoneMap;

    private ChatHub() {
        idCounter = new AtomicLong(1000);
        clients = new ConcurrentHashMap<>();
        groups = new ConcurrentHashMap<>();
        // file
        fileIdCouunter = new AtomicLong(100);
        pendingFiles = new ConcurrentHashMap<>();
        receiverSockets = new ConcurrentHashMap<>();
        transferDoneMap = new ConcurrentHashMap<>();
    }

    private static class Holder {
        private final static ChatHub INSTANCE = new ChatHub();
    }

    public static ChatHub getInstance() {
        return Holder.INSTANCE;
    }

    public String registerClient(String displayName, TextHandler handler) {
        String clientId = "u-" + idCounter.incrementAndGet();
        clients.put(clientId, handler);
        TextHandler clientHandler = clients.get(clientId);
        String welcome = """
                ========================================
                           WELCOME TO SOAPTA
                ========================================
                Display name : %s
                Client ID    : %s
                ----------------------------------------
                Type /help to view available commands.
                Type /exit to disconnect.
                ----------------------------------------
                """.formatted(displayName, clientId);
        clientHandler.notifyMessage(welcome);
        return clientId;
    }

    public void unregisterClient(String clientId) {
        // remove client
        TextHandler handler = clients.remove(clientId);

        // remove client in group
        for (Map.Entry<String, Set<String>> group : groups.entrySet()) {
            group.getValue().remove(clientId);
        }
        pendingFiles.entrySet().removeIf(e -> {
            if (e.getValue().senderId().equals(clientId) || e.getValue().receiverId().equals(clientId)) {
                return true;
            }
            return false;
        });
    }

    public void sendPrivateMsg(String fromClientId, String toClientId, String msg) {
        TextHandler clientHandler = clients.getOrDefault(toClientId, null);
        if (clientHandler == null) {
            // send notify client is not exist
            return;
        }
        if (fromClientId.equals(toClientId)) {
            clientHandler.notifyMessage("[You]: " + msg);
            return;
        }
        String formatMsg = String.format("[PM from %s (%s)]: %s", clientHandler.getClientId(),
                clientHandler.getDisplayName(), msg);
        // send msg if client exists
        // clientHandler.notifyMessage("The receipent " + toClientId + " is not exist");

        clientHandler.notifyMessage(formatMsg);
    }

    public void createGroup(String groupName, String creatorId) {
        TextHandler clientHandler = clients.get(creatorId);
        if (!groupName.matches("^[\\p{L}\\d_ ]{2,23}")) {
            LOGGER.warning("Group name must only contain chracter, number, space and length from 2 to 23");
            clientHandler.notifyMessage(
                    "[SYSTEM] Group name must only contain chracter, number, space and length from 2 to 23");
            return;
        }
        Set<String> newGroup = new HashSet<>();
        newGroup.add(creatorId);

        Set<String> group = groups.putIfAbsent(groupName, newGroup);
        if (group != null) {
            LOGGER.warning("This group name already exists!");
            clientHandler.notifyMessage("[SYSTEM] This group name already exists!");
            return;
        }
        clientHandler.notifyMessage("[SYSTEM] Group '" + groupName + "' created successfully.");
        clientHandler.notifyMessage("[SYSTEM] You have joined group '" + groupName + "'.");
    }

    public void createGroupNew(TextHandler client, String groupName) {
        if (!groupName.matches("^[a-zA-Z0-9_-]{2,20}$")) {
            client.notifyMessage(
                    "[SYSTEM] Group name must only contain chracter, number, space and length from 2 to 20\n");
            return;
        }
        if (groups.containsKey(groupName)) {
            client.notifyMessage("[SYSTEM] This group name already exists!\n");
            return;
        }
        Set<String> members = new HashSet<>();
        members.add(client.getClientId());
        groups.put(groupName, members);
        client.notifyMessage("[SYSTEM] Group '" + groupName + "' created successfully.\n");
        client.notifyMessage("[SYSTEM] You have joined group '" + groupName + "'.\n");
        return;
    }

    public void joinGroup(String groupName, String clientId) {
        TextHandler clientHandler = clients.get(clientId);

        if (!groups.containsKey(groupName)) {
            LOGGER.warning("This group is not exist");
            clientHandler.notifyMessage("[ERROR] Group '" + groupName + "' does not exist.");
            return;
        }
        Set<String> groupClient = groups.get(groupName);
        if (groupClient.contains(clientId)) {
            LOGGER.log(Level.INFO, "This client {0} is already member", clientId);
            clientHandler.notifyMessage("[SYSTEM] You are already a member of group '" + groupName + "'.");
            return;
        }
        groupClient.add(clientId);
        LOGGER.log(Level.INFO, "Client {0} joins group", clientId);
        clientHandler.notifyMessage("[SYSTEM] You have joined group '" + groupName + "'.");
    }

    public void joinGroupNew(TextHandler client, String groupName) {
        if (!groupName.matches("^[\\p{Lower}\\p{Upper}\\d_-]{2,20}$")) {
            client.notifyMessage(
                    "[SYSTEM] Group name must only contain chracter, number, space and length from 2 to 20\n");
            return;
        }
        if (!groups.containsKey(groupName)) {
            client.notifyMessage("[ERROR] Group '" + groupName + "' does not exist.\n");
            return;
        }
        Set<String> mems = groups.get(groupName);
        if (mems.contains(client.getClientId())) {
            LOGGER.log(Level.INFO, "This client {0} is already member", client.getClientId());
            client.notifyMessage("[SYSTEM] You are already a member of group '" + groupName + "'.\n");
            return;
        }
        mems.add(client.getClientId());
        LOGGER.log(Level.INFO, "Client {0} joins group", client.getClientId());
        client.notifyMessage("[SYSTEM] You have joined group '" + groupName + "'.\n");
    }

    public void sendGroupMsg(String fromClientId, String groupName, String content) {
        TextHandler sender = clients.get(fromClientId);
        if (!groups.containsKey(groupName)) {
            LOGGER.warning("This group is not exist");
            return;
        }
        Set<String> groupClient = groups.get(groupName);
        if (!groupClient.contains(fromClientId)) {
            LOGGER.log(Level.WARNING, "This client {0} is not a member", fromClientId);
        }
        // if it is valid then send message
        String formatMsg = String.format("[Group #%s][%s (%s)]: %s", groupName, sender.getClientId(),
                sender.getDisplayName(), content);
        for (String clientId : groupClient) {
            TextHandler client = clients.get(clientId);
            if (client != null) {
                client.notifyMessage(formatMsg);
            }
        }
    }

    public void sendGroupMsgNew(TextHandler client, String groupAndMsg) {
        String[] data = groupAndMsg.split("\\s+", 2);
        if (!groups.containsKey(data[0])) {
            LOGGER.warning("This group is not exist");
            client.notifyMessage("[ERROR] Group '" + data[0] + "' does not exist.");
            return;
        }
        Set<String> mems = groups.get(data[0]);
        if (!mems.contains(client.getClientId())) {
            LOGGER.log(Level.WARNING, "This client {0} is not a member", client.getClientId());
            client.notifyMessage(groupAndMsg);
            return;
        }
        String formatMsg = String.format("[Group #%s][%s (%s)]: %s", data[0], client.getClientId(),
                client.getDisplayName(), data[1]);
        for (String mem : mems) {
            TextHandler receiver = clients.get(mem);
            receiver.notifyMessage(formatMsg);
        }
    }

    // public List<String> getGroupsSummary() {
    // return groups.entrySet().stream()
    // .map(group -> String.format("- %s (%d members)\n", group.getKey(),
    // group.getValue().size())).toList();
    // }

    public void getGroups(TextHandler client) {
        List<String> listGroup = groups.entrySet().stream()
                .map(group -> String.format("- %s (%d members)", group.getKey(), group.getValue().size())).toList();
        if (listGroup.isEmpty()) {
            client.notifyMessage("Active Groups (0): No groups created yet. Use /group create to start one.\n");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Active Groups (%s):\n", listGroup.size()));
        for (int i = 0; i < listGroup.size(); i++) {
            sb.append(String.format("- %-20s", listGroup.get(i)));
            if (33 == (i + 1)) {
                sb.append("\n");
            }
        }
        sb.append("\n");
        client.notifyMessage(sb.toString());
    }

    public List<String> getOnlineClient() {
        return clients.values().stream().map(client -> {
            String clientId = client.getClientId();
            String displayName = client.getDisplayName();
            return String.format("%s (%s)", displayName, clientId);
        }).toList();
    }

    public List<String> getJoinedGroups(String clientId) {
        TextHandler client = clients.get(clientId);
        List<String> groupJoined = groups.entrySet().stream().filter(
                g -> g.getValue().contains(clientId)).map(groupName -> groupName.getKey()).toList();
        if (groupJoined.isEmpty()) {
            client.notifyMessage("You have not joined any groups yet. Use /group join or /group create.\n");
            return List.of();
        } else {
            return groupJoined;
        }
    }

    public void myGroup(TextHandler client) {
        List<String> groupJoined = groups.entrySet().stream().filter(g -> g.getValue().contains(client.getClientId()))
                .map(g -> String.format("- %s (%d members)", g.getKey(), g.getValue().size()))
                .toList();
        if (groupJoined.isEmpty()) {
            client.notifyMessage("You have not joined any groups yet. Use /group join or /group create.\n");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Joined Groups (%s):\n", groupJoined.size()));
        for (int i = 0; i < groupJoined.size(); i++) {
            sb.append(String.format("- %-20s", groupJoined.get(i)));
            if (33 == (i + 1)) {
                sb.append("\n");
            }
        }
        sb.append("\n");
        client.notifyMessage(sb.toString());
    }

    public void leaveGroup(String groupName, String clientId) {
        TextHandler client = clients.get(clientId);

        boolean isGroupExist = groups.containsKey(groupName);
        if (!isGroupExist) {
            client.notifyMessage("[ERROR] Group '" + groupName + "' does not exist.");
            return;
        }
        Set<String> members = groups.get(groupName);
        if (members.contains(clientId)) {
            members.remove(clientId);
            client.notifyMessage("[SYSTEM] You have left group '" + groupName + "'.");
            // send notify to other members
            for (String member : members) {
                TextHandler mem = clients.get(member);
                mem.notifyMessage(String.format("[Group #%s][SYSTEM]: %s (%s) has left the group.", groupName,
                        client.getClientId(), client.getDisplayName()));
            }
        } else {
            client.notifyMessage("[ERROR] You are not a member of group '" + groupName + "'.");
        }
        if (members.isEmpty()) {
            groups.remove(groupName);
            LOGGER.info("Group '" + groupName + "' was automatically removed (0 members remaining).");
        }
    }

    public void leaveGroupNew(TextHandler client, String groupName) {
        if (!groupName.matches("^[\\p{Lower}\\p{Upper}\\d_-]{2,20}$")) {
            client.notifyMessage(
                    "[SYSTEM] Group name must only contain chracter, number, space and length from 2 to 20");
            return;
        }
        if (!groups.containsKey(groupName)) {
            client.notifyMessage("[ERROR] Group '" + groupName + "' does not exist.");
            return;
        }
        Set<String> mems = groups.get(groupName);
        if (mems.contains(client.getClientId())) {
            client.notifyMessage("[SYSTEM] You have left group '" + groupName + "'.");
            mems.remove(client.getClientId());
            if (mems.isEmpty()) {
                groups.remove(groupName);
                LOGGER.info("Group '" + groupName + "' was automatically removed (0 members remaining).");
            }
        } else {
            client.notifyMessage("[ERROR] You are not a member of group '" + groupName + "'.");
        }
    }

    public void fileSend(TextHandler sender, String subArgs) {
        if (subArgs.isBlank()) {
            return;
        }
        String[] parts = subArgs.split("\\s+", 3);
        if (parts.length < 3) {
            sender.notifyMessage("[ERROR] Invalid format (Web Console does not support local file path).");
            return;
        }
        String receiverId = parts[0];
        long fileSize;
        try {
            fileSize = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            sender.notifyMessage("[ERROR] Invalid file size.");
            return;
        }
        String fileName = parts[2];
        try {
            if (fileSize > AppConstants.MAX_FILE_SIZE) {
                sender.notifyMessage("[ERROR] File size exceeds the 150 MB limit.\n");
                return;
            } else if (fileName.isEmpty()) {
                sender.notifyMessage("[ERROR] Cannot send an empty file.\n");
                return;
            }
            TextHandler receiver = clients.get(receiverId);
            if (receiver == null) {
                sender.notifyMessage("[ERROR] Receiver is not online.\n");
                return;
            } else if (receiverId.equals(sender.getClientId())) {
                sender.notifyMessage("[ERROR] You cannot send a file to yourself.\n");
                return;
            }

            String transderId = "ft-" + fileIdCouunter.incrementAndGet();
            FileTransfer fileTransfer = FileTransfer.builder()
                    .transferId(transderId)
                    .senderId(sender.getClientId())
                    .receiverId(receiverId)
                    .fileName(fileName)
                    .fileSize(fileSize)
                    .build();
            // danh sach file
            pendingFiles.put(transderId, fileTransfer);
            sender.notifyMessage(
                    String.format("[FILE] File request sent to %s for '%s'. [ID: %s]\n", receiverId,
                            fileName, transderId));
            receiver.notifyMessage(
                    String.format("[FILE] Incoming file request: %s (%d bytes) | From: %s | Transfer Id: %s\n",
                            fileName, fileSize, sender.getClientId(), transderId));
        } catch (Exception e) {
            LOGGER.warning("send file: " + e.getMessage());
        }
    }

    public void fileAccept(TextHandler client, String subArgs) {
        String transferId = subArgs;
        FileTransfer transfer = pendingFiles.get(transferId);
        if (Objects.isNull(transfer)) {
            client.notifyMessage("[ERROR] File transfer request not found.\n");
            return;
        }
        TextHandler sender = clients.get(transfer.senderId());
        if (Objects.isNull(sender)) {
            client.notifyMessage("[ERROR] Sender `" + transfer.senderId() + "` is not online.\n");
            return;
        }
        if (!client.getClientId().equals(transfer.receiverId())) {
            client.notifyMessage("[ERROR] You are not the receiver of this file transfer.\n");
            return;
        }
        client.notifyMessage(String.format(
                "[FILE] You accepted the file transfer request from %s for '%s' (%d bytes). [ID: %s]\n",
                transfer.senderId(), transfer.fileName(), transfer.fileSize(), transfer.transferId()));
        sender.notifyMessage(String.format("[FILE] '%s' accepted your file transfer request for '%s'. [ID: %s]\n",
                client.getClientId(), transfer.fileName(), transfer.transferId()));
    }

    public void fileDecline(TextHandler client, String subArgs) {
        String transferId = subArgs;
        FileTransfer transfer = pendingFiles.get(transferId);
        if (Objects.isNull(transfer)) {
            client.notifyMessage("[ERROR] File transfer request not found.\n");
            return;
        }
        TextHandler sender = clients.get(transfer.senderId());
        if (!client.getClientId().equals(transfer.receiverId())) {
            client.notifyMessage("[ERROR] You are not the receiver of this file transfer.\n");
            return;
        }
        pendingFiles.remove(transferId);
        client.notifyMessage(String.format(
                "[FILE] You declined the file transfer request from %s for '%s'. [ID: %s]\n",
                transfer.senderId(), transfer.fileName(), transfer.transferId()));
        sender.notifyMessage(String.format("[FILE] '%s' declined your file transfer request for '%s'. [ID: %s]\n",
                client.getClientId(), transfer.fileName(), transfer.transferId()));
    }

    public void fileRequest(TextHandler client, String subArgs) {
        List<String> l = pendingFiles.entrySet().stream()
                .filter(i -> i.getValue().receiverId().equals(client.getClientId())).map(
                        f -> {
                            FileTransfer fileTransfer = f.getValue();
                            String str = String.format("- [%s] '%s' (%.1f KB) from %s\n", fileTransfer.transferId(),
                                    fileTransfer.fileName(), fileTransfer.fileSize() / 1024.0,
                                    fileTransfer.senderId());
                            return str;
                        })
                .toList();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Pending File Requests: (%d)\n", l.size()));
        l.forEach(sb::append);
        client.notifyMessage(sb.toString());
    }

    public FileTransfer getFileTransfer(String transferId) {
        return pendingFiles.get(transferId);
    }

    public CompletableFuture<Socket> getReceiverSocketFuture(String transferId) {
        return receiverSockets.computeIfAbsent(transferId, k -> new CompletableFuture<>());
    }

    public CompletableFuture<Void> getTransferDoneFuture(String transferId) {
        return transferDoneMap.computeIfAbsent(transferId, k -> new CompletableFuture<>());
    }

    public void cleanupFileFutures(String transferId) {
        receiverSockets.remove(transferId);
        transferDoneMap.remove(transferId);
        pendingFiles.remove(transferId);
    }
}
