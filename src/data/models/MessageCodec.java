package data.models;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import resources.MessageType;

public final class MessageCodec {

    public static final int MAX_TEXT_SIZE = 1 << 16;
    public static final int MAX_CHUNK_SIZE = 1 << 19;

    private MessageCodec() {
    }

    public static void write(DataOutputStream out, Message message) throws IOException {
        if (MessageType.TEXT != message.getType()) {
            throw new IOException("Must TEXT type");
        }
        byte[] bs = message.getPayload().getBytes(StandardCharsets.UTF_8);
        if (bs.length > MAX_TEXT_SIZE) {
            throw new IOException("Do not exceed maximum value");
        }
        out.writeByte(MessageType.TEXT.getCode());
        out.writeInt(bs.length);
        out.write(bs);
        out.flush();
    }

    public static Message read(DataInputStream in) throws IOException {
        int typeByte = in.readUnsignedByte();

        if (MessageType.TEXT.getCode() != typeByte) {
            throw new IOException("Must TEXT type");
        }

        int len = in.readInt(); // read next 4 bytes
        if (len < 0) {
            throw new IOException("must >= 0");
        } else if (len > MAX_TEXT_SIZE) {
            throw new IOException("Do not exceed maximum value");
        }
        byte[] bs = new byte[len];
        in.readFully(bs);
        String message = new String(bs, StandardCharsets.UTF_8);
        return new Message(MessageType.TEXT, message);
    }

    public static void writeChunk(DataOutputStream out, byte[] data, int length) throws IOException {
        if (length > MAX_CHUNK_SIZE) {
            throw new IOException("Do not exceed maximum value");
        } else if (length < 0) {
            throw new NegativeArraySizeException("must >= 0");
        } else if (length > data.length) {
            throw new IOException("Do not exceed maximum value");
        }
        out.writeByte(MessageType.FILE.getCode());
        out.writeInt(length);
        out.write(data, 0, length);
        out.flush();
    }

    public static byte[] readChunk(DataInputStream in) throws IOException {
        int typeByte = in.readUnsignedByte();
        if (MessageType.FILE.getCode() != typeByte) {
            throw new IOException("Must FILE type");
        }
        int len = in.readInt();
        if (len < 0) {
            throw new NegativeArraySizeException("Must >= 0");
        } else if (len > MAX_CHUNK_SIZE) {
            throw new IOException("Do not exceed maximum value");
        }
        byte[] dataBytes = new byte[len];
        in.readFully(dataBytes);
        return dataBytes;
    }
}
