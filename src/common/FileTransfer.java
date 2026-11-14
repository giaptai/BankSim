package common;

public record FileTransfer(
                String transferId,
                String senderId,
                String receiverId,
                String fileName,
                long fileSize) {

        private class builder {
                String transferId;
                String senderId;
                String receiverId;
                String fileName;
                long fileSize;

                public builder() {
                        // return new builder();
                }

                public builder senderId(String senderId) {
                        this.senderId = senderId;
                        return this;
                }

                public builder receiverId(String receiverId) {
                        this.receiverId = receiverId;
                        return this;
                }

                public builder fileName(String fileName) {
                        this.fileName = fileName;
                        return this;
                }

                public builder transferId(long fileSize) {
                        this.fileSize = fileSize;
                        return this;
                }
                // public static FileTransfer build(){
                //         return new FileTransfer(this);
                // }
        }

}
