package data.models;

import resources.MessageType;
import resources.annotations.Builder;

@Builder
public class Message {
    private MessageType type;
    private String payload;

    Message(MessageType type, String payload) {
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
