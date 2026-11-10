package common;

public record Message(
        String type,
        String fromClientId,
        String fromDisplayName,
        String target,
        String content,
        long timestamp) {
}