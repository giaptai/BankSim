package data.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MessageSerializer {
    private static final int MAX_FRAME = 1 << 19;

    public static void write(OutputStream out, Message message) throws IOException {
        String data = message.getType().name() + "\n" + message.getPayload();
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_FRAME) {
            throw new IOException("Frame size exceeds maximum allowed");
        }
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeInt(bytes.length);
        dos.write(bytes);
        dos.flush();
    }

    public static Message read(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0 || len > MAX_FRAME) {
            throw new IOException("Frame size exceeds maximum allowed");
        }
        byte[] body = new byte[len];
        in.readFully(body);

        String text = new String(body, StandardCharsets.UTF_8);
        int newline = text.indexOf('\n');
        if (newline < 0) {
            throw new IOException("Missing message type separator");
        }
        MessageType type = MessageType.valueOf(text.substring(0, newline));
        return new Message(type, text.substring(newline + 1));
    }
}
