package common;

public record FileTransfer(
                String transferId,
                String senderId,
                String receiverId,
                String fileName,
                long fileSize) {
        public static Builder builder() {
                return new Builder();
        }

        public static class Builder {
                String transferId;
                String senderId;
                String receiverId;
                String fileName;
                long fileSize;

                public Builder transferId(String transferId) {
                        this.transferId = transferId;
                        return this;
                }

                public Builder senderId(String senderId) {
                        this.senderId = senderId;
                        return this;
                }

                public Builder receiverId(String receiverId) {
                        this.receiverId = receiverId;
                        return this;
                }

                public Builder fileName(String fileName) {
                        this.fileName = fileName;
                        return this;
                }

                public Builder fileSize(long fileSize) {
                        this.fileSize = fileSize;
                        return this;
                }

                public FileTransfer build() {
                        return new FileTransfer(
                                        transferId,
                                        senderId,
                                        receiverId,
                                        fileName,
                                        fileSize);
                }
        }

}
