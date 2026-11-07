package data.protocol;

public class Message {
    private MessageType type;
    private String payload;

    public Message(MessageType type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() {
        return this.type;
    }

    public String getPayload() {
        return this.payload;
    }
}
