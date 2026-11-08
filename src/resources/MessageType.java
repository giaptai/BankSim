package resources;

public enum MessageType {
    TEXT(0x01),
    FILE(0x02),;

    private int code;

    MessageType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static MessageType fromCode(int code) {
        for (MessageType type : MessageType.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new RuntimeException("Unknow message type: " + code);
    }
}