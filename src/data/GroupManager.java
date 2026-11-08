package data;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GroupManager {
    private ConcurrentHashMap<String, Group> groups;
    private AtomicInteger groupCounter;

    public GroupManager() {
        this.groups = new ConcurrentHashMap<>();
        this.groupCounter = new AtomicInteger(0);
    }

    public String createGroup(String name, String ownerThread) {
        int id = groupCounter.incrementAndGet();
        String groupId = "G" + id;
        Group group = new Group(groupId, name, ownerThread);
        groups.put(groupId, group);
        return groupId;
    }

    public boolean addMember(String groupId, String memberThread) {
        Group group = groups.get(groupId);
        if(group == null){
            return false;
        }
        return group.addMember(memberThread);
    }

    public Group get(String groupId) {
        return groups.getOrDefault(groupId, null);
    }

    public List<Group> groupsOf(String threadName) {
        List<Group> groupList = groups.values().stream().toList();
        return groupList.stream().filter(g -> g.isMember(threadName)).toList();
    }

    public boolean exists(String groupId) {
        return groups.get(groupId) != null;
    }
}
