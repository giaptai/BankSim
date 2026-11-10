package server;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manage user status, chat groups, and message coordination.
 */
public class ChatHub {
    private static final Logger LOGGER = Logger.getLogger(ChatHub.class.getName());
    // private static final ChatHub INSTANCE;
    private final AtomicLong idCounter;
    private final ConcurrentMap<String, ClientHandler> clients;
    private final ConcurrentMap<String, Set<String>> groups;

    private ChatHub() {
        idCounter = new AtomicLong(1000);
        clients = new ConcurrentHashMap<>();
        groups = new ConcurrentHashMap<>();
    }

    private static class Holder {
        private final static ChatHub INSTANCE = new ChatHub();
    }

    public static ChatHub getInstance() {
        return Holder.INSTANCE;
    }

    public String registerClient(String displayName, ClientHandler handler) {
        String clientId = "u-" + idCounter.incrementAndGet();
        clients.put(clientId, handler);
        ClientHandler clientHandler = clients.get(clientId);
        clientHandler.notifyMessage("[SYSTEM] Welcome " + displayName + "! Your assigned ID is: " + clientId);
        clientHandler.notifyMessage("[SYSTEM] Type /help to see available commands.");
        return clientId;
    }

    public void unresigterClient(String clientId) {
        ClientHandler clientHandler = clients.get(clientId);
        // remove client
        clients.remove(clientId);
        // remove client in group
        for (Map.Entry<String, Set<String>> group : groups.entrySet()) {
            group.getValue().remove(clientId);
        }
        clientHandler.notifyMessage("[SYSTEM] Disconnected from server. Goodbye!");
    }

    public void sendPrivateMsg(String fromClientId, String toClientId, String msg) {
        ClientHandler clientHandler = clients.getOrDefault(toClientId, null);
        if (clientHandler == null) {
            // send notify client is not exist
            return;
        }
        String formatMsg = String.format("[PM from %s (%s)]: %s", clientHandler.getClientId(), clientHandler.getDisplayName(), msg);
        // send msg if client exists
        clientHandler.notifyMessage(formatMsg);
    }

    public void createGroup(String groupName, String creatorId) {
        ClientHandler clientHandler = clients.get(creatorId);
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

    public void joinGroup(String groupName, String clientId) {
        ClientHandler clientHandler = clients.get(clientId);

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

    public void sendGroupMsg(String fromClientId, String groupName, String content) {
        ClientHandler sender = clients.get(fromClientId);
        if (!groups.containsKey(groupName)) {
            LOGGER.warning("This group is not exist");
            return;
        }
        Set<String> groupClient = groups.get(groupName);
        if (!groupClient.contains(fromClientId)) {
            LOGGER.log(Level.WARNING, "This client {0} is not a member", fromClientId);
        }
        // if valid then send message
        String formatMsg = String.format("[Group #%s][%s (%s)]: %s", groupName, sender.getClientId(),
                sender.getDisplayName(), content);
        for (String clientId : groupClient) {
            ClientHandler client = clients.get(clientId);
            if (client != null) {
                client.notifyMessage(formatMsg);
            }
        }
    }

    public List<String> getOnlineClient() {
        return clients.values().stream().map(client -> {
            String clientId = client.getClientId();
            String displayName = client.getDisplayName();
            return clientId + ": " + displayName;
        }).toList();
    }
}
