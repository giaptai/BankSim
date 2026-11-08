package data;

public class FileTransfer {
    public static final long MAX_FILE_SIZE = 1 << 19;
    private final String uploadId;
    private final String senderThread;
    private final String recipientThread;
    private final String fileName;
    private final long totalSize;
    private long transferredBytes;

    public FileTransfer(String uploadId, String senderThread, String recipientThread, String fileName, long totalSize) {
        this.uploadId = uploadId;
        this.senderThread = senderThread;
        this.recipientThread = recipientThread;
        this.fileName = fileName;
        this.totalSize = totalSize;
    }

    public void addBytes(int n) {

    }

    public boolean isComplete() {
        return transferredBytes == totalSize;
    }

    public int progressPercent() {
        return 0;
    }
}