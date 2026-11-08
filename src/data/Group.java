package data;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Group {
    private final String id;
    private final String name;
    private final String ownerThread;
    private final Set<String> members = ConcurrentHashMap.newKeySet();

    public Group(String id, String name, String ownerThread) {
        this.id = id;
        this.name = name;
        this.ownerThread = ownerThread;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getOwnerThread() {
        return this.ownerThread;
    }

    public boolean addMember(String threadName) {
        return members.add(threadName);
    }

    public boolean isMember(String threadName) {
        return members.contains(threadName);
    }

    public Set<String> getMembers() {
        return members.stream().collect(Collectors.toSet());
    }
}
