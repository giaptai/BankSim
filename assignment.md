# BÀI TẬP LỚN: ỨNG DỤNG CONSOLE CHAT CLIENT-SERVER (JAVA CORE)

---

## 1. Mục tiêu đề tài

Xây dựng hệ thống chat Console hoạt động theo mô hình Client - Server bằng **Java Core (JDK 21+)** và **TCP Socket**, tận dụng **Virtual Threads** để xử lý tải cao (hỗ trợ tối đa 5.000 kết nối đồng thời).

Hệ thống không sử dụng database hay thư viện bên ngoài; toàn bộ trạng thái phiên làm việc, danh sách phòng và luồng truyền nhận file được quản lý trực tiếp trong bộ nhớ (in-memory).

---

## 2. Yêu cầu nghiệp vụ

1. **Vào phòng chat đơn giản:**
* Người dùng chỉ cần nhập Hostname, Port và Tên hiển thị (Display Name).
* Tên hợp lệ: dài từ 2 đến 20 ký tự, chấp nhận chữ cái (có dấu), số, khoảng trắng và gạch dưới (`^[\p{L}0-9_ ]{2,20}$`).
* **Cho phép trùng tên:** Server phân biệt người dùng bằng định danh Thread ID.


2. **Cơ chế định danh phiên (Thread ID):**
* Định dạng: `thread{số_thứ_tự}-client-{tên}` (Ví dụ: `thread1-client-long`, `thread2-client-long`).
* Số thứ tự tăng dần (`AtomicInteger`).
* Danh sách online hiển thị Thread ID để người dùng chỉ định chính xác người nhận khi chat 1:1 hoặc gửi file.


3. **Chat 1:1 (Private Chat):**
* Xem danh sách người đang online.
* Gửi/nhận tin nhắn trực tiếp với một người dùng theo Thread ID.


4. **Chat nhóm (Group Chat):**
* Tạo nhóm mới (người tạo tự động là thành viên).
* Thêm thành viên đang online vào nhóm.
* Gửi tin nhắn đến toàn bộ thành viên trong nhóm (người gửi không nhận lại tin của chính mình).


5. **Truyền file (File Transfer):**
* Cho phép gửi file trực tiếp giữa 2 client hoặc gửi file vào nhóm chat.
* Dung lượng tối đa: **150 MB**.
* Cơ chế: Đọc - ghi theo từng khối dữ liệu (Chunk), Server chuyển tiếp (stream) trực tiếp, không lưu đệm toàn bộ file lên ổ cứng hay RAM của Server.


6. **Giới hạn kết nối (Concurrency Control):**
* Server giới hạn tối đa **5.000 kết nối đồng thời** bằng `Semaphore`.
* Khi đã đủ 5.000 kết nối, kết nối thứ 5.001 bị từ chối ngay lập tức và đóng socket.
* Khi một client ngắt kết nối, slot kết nối được giải phóng an toàn đúng một lần.



---

## 3. Thiết kế giao thức truyền thông (Protocol)

Mọi gói tin (Frame) qua TCP Socket đều có cấu trúc nhị phân tiền tố độ dài:

```
+---------------+----------------+-----------------------+
| Type (1 byte) | Length (4 byte)| Payload (N bytes)     |
+---------------+----------------+-----------------------+

```

* `Type`:
* `0x01` (`TEXT`): Gói tin văn bản (lệnh điều khiển, chat). Tối đa 64 KB.
* `0x02` (`FILE`): Gói tin nhị phân truyền file. Tối đa 512 KB/chunk. 4 byte đầu của payload là `uploadId`, phần còn lại là raw byte của file.



### Bảng tập lệnh (TEXT Frame)

Các trường ngăn cách bằng ký tự `|`:

| Chiều | Lệnh / Cú pháp | Ý nghĩa |
| --- | --- | --- |
| Client $\rightarrow$ Server | `NAME|{tên}` | Đăng ký tên, bắt buộc là lệnh đầu tiên |
| Client $\rightarrow$ Server | `LIST` | Lấy danh sách người dùng online |
| Client $\rightarrow$ Server | `PM|{thread_nhận}|{nội dung}` | Chat 1:1 |
| Client $\rightarrow$ Server | `GCREATE|{tên nhóm}` | Tạo nhóm |
| Client $\rightarrow$ Server | `GADD|{group_id}|{thread_thành_viên}` | Thêm người vào nhóm |
| Client $\rightarrow$ Server | `GLIST` | Lấy danh sách nhóm mình tham gia |
| Client $\rightarrow$ Server | `GMSG|{group_id}|{nội dung}` | Gửi tin nhắn vào nhóm |
| Client $\rightarrow$ Server | `FILE_START|{thread_nhận}|{tên_file}|{kích_thước}` | Yêu cầu gửi file 1:1 |
| Client $\rightarrow$ Server | `FILE_END` | Báo kết thúc gửi file |
| Server $\rightarrow$ Client | `OK|NAME|{thread_id}` | Xác nhận tên thành công, trả về Thread ID |
| Server $\rightarrow$ Client | `LIST|{thread1},{thread2}...` | Danh sách online |
| Server $\rightarrow$ Client | `MSG|{thread_gửi}|{nội dung}` | Tin nhắn 1:1 chuyển tiếp |
| Server $\rightarrow$ Client | `GMSG|{group_id}|{thread_gửi}|{nội dung}` | Tin nhắn nhóm chuyển tiếp |
| Server $\rightarrow$ Client | `NOTICE|{nội dung}` | Thông báo hệ thống (vào nhóm, nhận file,...) |
| Server $\rightarrow$ Client | `FILE_START|{uploadId}|{thread_gửi}|{tên_file}|{size}` | Báo cho người nhận chuẩn bị đón file |
| Server $\rightarrow$ Client | `FILE_END|{uploadId}` | Báo truyền file hoàn tất |
| Server $\rightarrow$ Client | `ERR|{mã_lỗi}` | Báo lỗi (`INVALID_NAME`, `USER_OFFLINE`, `FILE_TOO_LARGE`,...) |

---

Dưới đây là đặc tả đầy đủ cho **Mục 4: Chi tiết các Class cần triển khai**, bao gồm đầy đủ **Thuộc tính**, **Phương thức** và **Cách cài đặt chi tiết từng bước** cho toàn bộ 9 class của ứng dụng.

---

## 4. Chi tiết các Class cần triển khai (Fields, Methods & Implementation)

---

### 4.1 `MessageType` (Enum)

Định nghĩa mã byte định danh loại frame dữ liệu qua TCP socket.

**Thuộc tính**

* `private final byte code` — Mã byte định danh (`0x01` cho TEXT, `0x02` cho FILE).

**Hằng số enum**

* `TEXT((byte) 0x01)`
* `FILE((byte) 0x02)`

**Phương thức**

* `public byte getCode()` — Trả về mã byte của frame.
* `public static MessageType fromCode(int code)` — Chuyển từ mã byte sang enum.

**Cách triển khai (Implementation)**

* Trong `fromCode(int code)`: Dùng `switch` hoặc duyệt qua `values()`. Nếu `code == 1` trả về `TEXT`, `code == 2` trả về `FILE`; nếu khác thì ném ngoại lệ `IllegalArgumentException("Invalid message type code: " + code)`.

---

### 4.2 `Message`

Đại diện cho một gói tin văn bản hoàn chỉnh trao đổi giữa Client và Server.

**Thuộc tính**

* `private final MessageType type` — Luôn là `MessageType.TEXT`.
* `private final String payload` — Chuỗi nội dung lệnh hoặc tin nhắn (UTF-8), không được `null`.

**Phương thức**

* `public Message(MessageType type, String payload)` — Hàm khởi tạo. Ném `NullPointerException` nếu `payload` là null.
* `public MessageType getType()` — Lấy loại frame.
* `public String getPayload()` — Lấy nội dung chuỗi.

**Cách triển khai (Implementation)**

* Class bất biến (immutable), gán trực tiếp dữ liệu ở constructor và chỉ cung cấp getter.

---

### 4.3 `MessageCodec`

Tiện ích đọc/ghi dữ liệu socket ở mức nhị phân theo giao thức tiền tố độ dài (length-prefixed framing).

**Thuộc tính**

* `public static final int MAX_TEXT_SIZE = 65536` — 64 KB (kích thước tối đa cho frame text).
* `public static final int MAX_CHUNK_SIZE = 524288` — 512 KB (kích thước tối đa của một khối nhị phân file).

**Phương thức**

* `private MessageCodec()` — Constructor private, ngăn việc tạo instance.
* `public static void write(DataOutputStream out, Message message) throws IOException` — Ghi một frame TEXT ra socket.
* `public static Message read(DataInputStream in) throws IOException` — Đọc một frame TEXT từ socket.
* `public static void writeChunk(DataOutputStream out, byte[] data, int length) throws IOException` — Ghi một frame FILE ra socket.
* `public static byte[] readChunk(DataInputStream in) throws IOException` — Đọc một frame FILE từ socket.

**Cách triển khai (Implementation)**

* **`write`:**
1. Lấy mảng byte UTF-8 từ `message.getPayload()`.
2. Ghi 1 byte loại frame: `out.writeByte(MessageType.TEXT.getCode())`.
3. Ghi 4 byte độ dài: `out.writeInt(bytes.length)`.
4. Ghi toàn bộ dữ liệu byte: `out.write(bytes)`.
5. Gọi `out.flush()`.


* **`read`:**
1. Đọc 1 byte loại frame bằng `in.readByte()`. Kiểm tra nếu khác `MessageType.TEXT.getCode()` thì ném `IOException`.
2. Đọc 4 byte độ dài: `int length = in.readInt()`.
3. Kiểm tra nếu `length < 0` hoặc `length > MAX_TEXT_SIZE` thì ném `IOException("Text frame too large")`.
4. Đọc đủ số byte bằng `byte[] data = in.readNBytes(length)`.
5. Trả về `new Message(MessageType.TEXT, new String(data, StandardCharsets.UTF_8))`.


* **`writeChunk`:**
1. Ghi 1 byte: `out.writeByte(MessageType.FILE.getCode())`.
2. Ghi 4 byte độ dài: `out.writeInt(length)`.
3. Ghi dữ liệu: `out.write(data, 0, length)`.
4. Gọi `out.flush()`.


* **`readChunk`:**
1. Đọc 4 byte độ dài `int length = in.readInt()` (sau khi bộ đọc ngoài đã đọc 1 byte type = 0x02).
2. Kiểm tra nếu `length < 0` hoặc `length > MAX_CHUNK_SIZE` thì ném `IOException`.
3. Trả về `in.readNBytes(length)`.



---

### 4.4 `Group`

Mô hình hóa một phòng chat nhóm.

**Thuộc tính**

* `private final String id` — Mã nhóm duy nhất (ví dụ: `G1`, `G2`).
* `private final String name` — Tên hiển thị của nhóm.
* `private final String ownerThread` — Thread ID của người tạo nhóm.
* `private final Set<String> members` — Tập hợp Thread ID của các thành viên.

**Phương thức**

* `public Group(String id, String name, String ownerThread)` — Khởi tạo nhóm.
* `public String getId()` — Lấy mã nhóm.
* `public String getName()` — Lấy tên nhóm.
* `public String getOwnerThread()` — Lấy Thread ID của chủ phòng.
* `public boolean addMember(String threadName)` — Thêm thành viên vào nhóm.
* `public boolean isMember(String threadName)` — Kiểm tra một người có trong nhóm hay không.
* `public Set<String> getMembers()` — Trả về danh sách thành viên hiện tại.

**Cách triển khai (Implementation)**

* Trong constructor: Khởi tạo `this.members = ConcurrentHashMap.newKeySet()` để an toàn khi nhiều luồng truy cập; sau đó thêm ngay `ownerThread` vào `members`.
* `getMembers()`: Trả về một bản sao bảo vệ `new HashSet<>(members)` hoặc `Set.copyOf(members)` để tránh rò rỉ tham chiếu ra ngoài.

---

### 4.5 `GroupManager`

Quản lý toàn bộ vòng đời và danh sách nhóm chat trên Server.

**Thuộc tính**

* `private final ConcurrentHashMap<String, Group> groups` — Bảng băm lưu trữ các nhóm, key là `groupId`.
* `private final AtomicInteger groupCounter` — Bộ đếm tự tăng đánh số nhóm.

**Phương thức**

* `public GroupManager()` — Khởi tạo đối tượng.
* `public String createGroup(String name, String ownerThread)` — Tạo một nhóm mới.
* `public boolean addMember(String groupId, String memberThread)` — Thêm thành viên vào nhóm đã có.
* `public Group get(String groupId)` — Lấy thông tin nhóm theo ID.
* `public List<Group> groupsOf(String threadName)` — Lấy danh sách các nhóm mà một người dùng tham gia.

**Cách triển khai (Implementation)**

* **`createGroup`:**
1. Tăng bộ đếm: `int id = groupCounter.incrementAndGet()`.
2. Sinh mã `String groupId = "G" + id`.
3. Tạo đối tượng: `Group group = new Group(groupId, name, ownerThread)`.
4. Lưu vào bảng: `groups.put(groupId, group)` và trả về `groupId`.


* **`addMember`:**
1. Tìm nhóm: `Group group = groups.get(groupId)`.
2. Nếu `group == null` trả về `false`.
3. Ngược lại gọi `return group.addMember(memberThread)`.


* **`groupsOf`:**
1. Lọc trong `groups.values()` xem nhóm nào có `group.isMember(threadName) == true`.
2. Thu thập và trả về `List<Group>`.



---

### 4.6 `FileTransfer`

Theo dõi trạng thái và tiến độ của một phiên truyền file trên Server.

**Thuộc tính**

* `public static final long MAX_FILE_SIZE = 157286400L` — 150 MB (150 * 1024 * 1024 bytes).
* `private final int uploadId` — Mã định danh phiên truyền file (đánh số tự tăng).
* `private final String senderThread` — Thread ID của người gửi.
* `private final String recipientThread` — Thread ID của người nhận.
* `private final String fileName` — Tên file.
* `private final long totalSize` — Dung lượng tổng cộng (bytes).
* `private long transferredBytes` — Số byte đã truyền qua server thành công.

**Phương thức**

* `public FileTransfer(int uploadId, String senderThread, String recipientThread, String fileName, long totalSize)` — Constructor.
* `public int getUploadId()` — Lấy mã phiên gửi.
* `public String getSenderThread()` — Lấy thread người gửi.
* `public String getRecipientThread()` — Lấy thread người nhận.
* `public String getFileName()` — Lấy tên file.
* `public long getTotalSize()` — Lấy tổng dung lượng.
* `public synchronized void addBytes(int n)` — Cộng dồn số byte của chunk vừa chuyển tiếp.
* `public synchronized boolean isComplete()` — Kiểm tra đã truyền đủ 100% dung lượng chưa.
* `public synchronized int progressPercent()` — Tính phần trăm tiến độ (0 - 100).

**Cách triển khai (Implementation)**

* Trong constructor: Kiểm tra nếu `totalSize > MAX_FILE_SIZE` thì ném `IllegalArgumentException("File exceeds 150 MB limit")`.
* `addBytes(int n)`: `this.transferredBytes += n`.
* `isComplete()`: `return this.transferredBytes >= this.totalSize`.
* `progressPercent()`: `return (int) ((transferredBytes * 100) / totalSize)`.

---

### 4.7 `SoaptaServer`

Quản lý socket lắng nghe, cấp phát luồng, điều phối kết nối, định tuyến tin nhắn và file.

**Thuộc tính**

* `public static final int PORT = 8080` — Cổng lắng nghe TCP.
* `public static final int MAX_CONNECTIONS = 5000` — Giới hạn kết nối đồng thời.
* `private ServerSocket serverSocket` — Socket máy chủ.
* `private final ExecutorService connExecutor` — Pool cấp Virtual Threads: `Executors.newVirtualThreadPerTaskExecutor()`.
* `private final Semaphore connectionLimit` — Bộ đếm giới hạn 5.000 kết nối: `new Semaphore(MAX_CONNECTIONS)`.
* `private final AtomicInteger threadCounter` — Đánh số tự tăng cho Thread ID (`new AtomicInteger(0)`).
* `private final ConcurrentHashMap<String, ClientHandler> onlineClients` — Lưu các client online với key là `threadName`.
* `private final ConcurrentHashMap<Integer, FileTransfer> activeTransfers` — Lưu các phiên gửi file đang hoạt động.
* `private final AtomicInteger uploadCounter` — Đánh số tự tăng cho `uploadId`.
* `private final GroupManager groupManager` — Quản lý nhóm chat.

**Phương thức**

* `public void start() throws IOException` — Khởi động Server, mở port và chạy vòng lặp nhận kết nối.
* `public void stop()` — Đóng ServerSocket, ngắt tất cả kết nối client và tắt executor.
* `public String nextThreadName(String displayName)` — Tạo chuỗi định danh luồng mới.
* `public void register(ClientHandler handler)` — Lưu client vào danh sách online.
* `public void unregister(String threadName)` — Xóa client khỏi danh sách online.
* `public ClientHandler find(String threadName)` — Tìm phiên kết nối của một người theo Thread ID.
* `public List<String> listOnline()` — Lấy danh sách tất cả Thread ID đang online.
* `public boolean route(String toThread, Message msg)` — Định tuyến gửi tin nhắn văn bản đến một client cụ thể.
* `public void sendNotice(String toThread, String text)` — Gửi gói tin hệ thống dạng `NOTICE` tới một client.
* `public FileTransfer startTransfer(String senderThread, String recipientThread, String fileName, long size)` — Tạo phiên truyền file mới.
* `public void finishTransfer(int uploadId, boolean success)` — Đóng phiên truyền file.
* `public Semaphore getConnectionLimit()` — Getter trả về semaphore để client nhả permit khi đóng.
* `public GroupManager getGroupManager()` — Getter trả về đối tượng quản lý nhóm.

**Cách triển khai (Implementation)**

* **`start()`:**
1. `serverSocket = new ServerSocket(PORT)`.
2. Vòng lặp `while (!serverSocket.isClosed())`:
* Nhận kết nối: `Socket socket = serverSocket.accept()`.
* Kiểm tra giới hạn: `if (!connectionLimit.tryAcquire())`: ghi log cảnh báo quá tải, đóng socket ngay bằng `socket.close()` và `continue`.
* Tạo handler: `ClientHandler handler = new ClientHandler(socket, this)`.
* Submit handler: `connExecutor.submit(handler)`. Nếu lỗi ngoại lệ: nhả `connectionLimit.release()`, đóng socket.




* **`nextThreadName(displayName)`:**
* `int id = threadCounter.incrementAndGet();`
* `return "thread" + id + "-client-" + displayName;`


* **`route(toThread, msg)`:**
* Lấy `ClientHandler target = find(toThread)`.
* Nếu `target == null` trả về `false`.
* Gọi `target.send(msg)` và trả về `true`. Bắt `IOException` thì ghi log và trả về `false`.


* **`startTransfer(...)`:**
* Kiểm tra nếu `find(recipientThread) == null` trả về `null`.
* Sinh `int uploadId = uploadCounter.incrementAndGet()`.
* Khởi tạo `FileTransfer ft = new FileTransfer(uploadId, senderThread, recipientThread, fileName, size)`.
* Lưu vào `activeTransfers.put(uploadId, ft)` và trả về `ft`.


* **`finishTransfer(uploadId, success)`:**
* Xóa phiên khỏi map: `activeTransfers.remove(uploadId)`.



---

### 4.8 `ClientHandler` (implements Runnable, Closeable)

Quản lý toàn bộ giao tiếp hai chiều với một Client qua TCP Socket: nhận diện phiên, điều phối lệnh chat và chuyển tiếp file.

**Thuộc tính**

* `private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}0-9_ ]{2,20}$")` — Regex kiểm tra tính hợp lệ của tên người dùng và tên nhóm.
* `private final Socket socket` — Socket TCP kết nối tới Client.
* `private final SoaptaServer server` — Tham chiếu tới Server để tương tác với danh sách online, nhóm và bộ đếm.
* `private final DataInputStream in` — Luồng đọc nhị phân từ socket.
* `private final DataOutputStream out` — Luồng ghi nhị phân ra socket.
* `private final AtomicBoolean released = new AtomicBoolean(false)` — Cờ bảo đảm permit của `Semaphore` chỉ được trả đúng một lần.
* `private String threadName` — Định danh luồng (Thread ID) do Server cấp sau khi đăng ký tên thành công.
* `private String displayName` — Tên hiển thị do người dùng nhập.
* `private FileTransfer currentUpload` — Đối tượng theo dõi phiên gửi file hiện tại của Client này (`null` nếu đang rảnh).

**Phương thức**

* `public ClientHandler(Socket socket, SoaptaServer server) throws IOException` — Hàm khởi tạo, mở luồng I/O.
* `public String getThreadName()` — Trả về `threadName`.
* `public synchronized void send(Message msg) throws IOException` — Ghi đồng bộ một frame TEXT ra socket.
* `public synchronized void sendChunk(byte[] data, int length) throws IOException` — Ghi đồng bộ một frame FILE ra socket.
* `public void run()` — Vòng lặp chính tiếp nhận frame từ Client.
* `private void handleName(String name) throws IOException` — Kiểm tra regex và đăng ký tên với Server.
* `private void handleCommand(String line) throws IOException` — Phân giải chuỗi lệnh và chuyển tiếp đến hàm xử lý tương ứng.
* `private void handleList()` — Trả về danh sách Thread ID đang online.
* `private void handlePrivateMessage(String toThread, String content)` — Định tuyến tin nhắn 1:1.
* `private void handleGroupCreate(String groupName)` — Tạo nhóm chat mới.
* `private void handleGroupAdd(String groupId, String memberThread)` — Thêm thành viên vào nhóm (chỉ dành cho Owner).
* `private void handleGroupList()` — Trả về danh sách các nhóm mà Client này đang tham gia.
* `private void handleGroupMessage(String groupId, String content)` — Gửi tin nhắn đến toàn bộ thành viên trong nhóm.
* `private void handleFileStart(String toThread, String fileName, long size)` — Khởi tạo tiến trình truyền file 1:1.
* `private void handleFileChunk(byte[] data)` — Kiểm tra và chuyển tiếp khối dữ liệu nhị phân của file.
* `private void handleFileEnd()` — Kiểm tra kích thước và chốt phiên gửi file thành công.
* `private void failTransfer()` — Hủy phiên gửi file khi xảy ra sự cố hoặc đứt kết nối.
* `public void close()` — Giải phóng tài nguyên, hủy phiên file dở dang và hoàn trả permit.

---

**Cách triển khai (Implementation)**

* **`ClientHandler(socket, server)`:**
* Gán `this.socket = socket`, `this.server = server`.
* Khởi tạo ngay `in = new DataInputStream(socket.getInputStream())` và `out = new DataOutputStream(socket.getOutputStream())` để sẵn sàng đọc/ghi.


* **`getThreadName()`:**
* Trả về biến `this.threadName`. Không sinh tên mới ở đây.


* **Đồng bộ ghi (`send` & `sendChunk`):**
* Cả hai hàm đều dùng từ khóa `synchronized` ở cấp method.
* `send(msg)`: Gọi `MessageCodec.write(out, msg)`.
* `sendChunk(data, length)`: Gọi `MessageCodec.writeChunk(out, data, length)`.
* *Mục đích:* Ngăn các luồng khác ghi xen kẽ frame văn bản vào giữa các khối dữ liệu của file.


* **Vòng lặp `run()`:**
1. *Giai đoạn 1 (Chờ đăng ký tên):* Lặp đọc frame đầu tiên. Nếu loại frame $\ne$ `TEXT` hoặc chuỗi nhận được không bắt đầu bằng `NAME|` $\rightarrow$ gửi `ERR|NOT_JOINED` và chờ tiếp. Khi nhận đúng tiền tố `NAME|` $\rightarrow$ cắt lấy tên và gọi `handleName(tên)`.
2. *Giai đoạn 2 (Nhận lệnh và dữ liệu):* Khi `threadName != null`, lặp đọc liên tục:
* Đọc byte loại frame và mảng byte payload theo độ dài.
* Frame `TEXT` $\rightarrow$ chuyển payload thành `String` UTF-8 và gọi `handleCommand(line)`.
* Frame `FILE` $\rightarrow$ nếu `currentUpload != null` thì gọi `handleFileChunk(payload)`, ngược lại gửi `ERR|UNKNOWN_COMMAND`.


3. *Dọn dẹp:* Bắt `EOFException` / `IOException` (khi Client ngắt kết nối) để thoát vòng lặp; khối `finally` luôn gọi `close()`.


* **`handleName(name)`:**
* So khớp `name` với `NAME_PATTERN`. Không khớp $\rightarrow$ gửi `ERR|INVALID_NAME`.
* Hợp lệ $\rightarrow$ gán `displayName = name`, gọi `server.nextThreadName(displayName)` để lấy `threadName`, gọi `server.register(this)`, gửi lại `OK|NAME|` + `threadName`.


* **`handleCommand(line)`:**
* Cắt lệnh tại dấu `|` đầu tiên bằng `line.split("\\|", 2)` để lấy `cmd` (tên lệnh) và `rest` (tham số phía sau).
* Điều hướng bằng `switch (cmd)`:
* `"LIST"` $\rightarrow$ gọi `handleList()`.
* `"GLIST"` $\rightarrow$ gọi `handleGroupList()`.
* `"GCREATE"` $\rightarrow$ gọi `handleGroupCreate(rest)`.
* `"PM"` $\rightarrow$ tách `rest.split("\\|", 2)` lấy `[toThread, content]`, gọi `handlePrivateMessage(...)`.
* `"GADD"` $\rightarrow$ tách `rest.split("\\|", 2)` lấy `[groupId, memberThread]`, gọi `handleGroupAdd(...)`.
* `"GMSG"` $\rightarrow$ tách `rest.split("\\|", 2)` lấy `[groupId, content]`, gọi `handleGroupMessage(...)`.
* `"FILE_START"` $\rightarrow$ tách `rest.split("\\|", 3)` lấy `[toThread, fileName, size]`, parse dung lượng và gọi `handleFileStart(...)`.
* `"FILE_END"` $\rightarrow$ gọi `handleFileEnd()`.
* Lệnh lạ hoặc thiếu tham số $\rightarrow$ gửi `ERR|UNKNOWN_COMMAND`.




* **`handleList()`:**
* Lấy `server.listOnline()`, ghép thành chuỗi phân tách bởi dấu phẩy $\rightarrow$ gửi `LIST|` + `danh_sách`.


* **`handlePrivateMessage(toThread, content)`:**
* Gọi `server.route(toThread, new Message(MessageType.TEXT, "MSG|" + threadName + "|" + content))`.
* Nếu trả về `false` (người nhận không online) $\rightarrow$ gửi phản hồi `ERR|USER_OFFLINE|` + `toThread`.


* **`handleGroupCreate(groupName)`:**
* Kiểm tra regex tên nhóm với `NAME_PATTERN`. Không khớp $\rightarrow$ gửi `ERR|INVALID_NAME`.
* Hợp lệ $\rightarrow$ gọi `server.getGroupManager().createGroup(groupName, threadName)`, gửi lại `OK|GCREATE|` + `groupId` + `|` + `groupName`.


* **`handleGroupAdd(groupId, memberThread)`:**
* Kiểm tra 3 điều kiện: Nhóm có tồn tại? Bạn có phải là `owner`? Người muốn thêm có đang online (`server.find`)? Nếu sai, gửi lỗi tương ứng: `ERR|GROUP_NOT_FOUND`, `ERR|NOT_OWNER`, hoặc `ERR|USER_OFFLINE`.
* Nếu thỏa mãn $\rightarrow$ gọi `groupManager.addMember(...)`. Nếu thành công $\rightarrow$ gửi `server.sendNotice(...)` cho người vừa được thêm, và gửi `OK|GADD|` + `groupId` cho người gọi lệnh.


* **`handleGroupList()`:**
* Lấy danh sách nhóm từ `server.getGroupManager().groupsOf(threadName)`.
* Ghép thành chuỗi dạng `GLIST|G1:TênNhóm1:SốThànhViên,G2:TênNhóm2:SốThànhViên` và gửi về Client.


* **`handleGroupMessage(groupId, content)`:**
* Kiểm tra nhóm tồn tại và người gửi có trong nhóm (`isMember`). Nếu sai $\rightarrow$ gửi `ERR|GROUP_NOT_FOUND` hoặc `ERR|NOT_MEMBER`.
* Lấy danh sách thành viên nhóm: với mỗi người $\ne$ `threadName`, gọi `server.route(...)` để chuyển tiếp gói `GMSG|groupId|threadName|content` (người offline tự động bỏ qua).


* **`handleFileStart(toThread, fileName, size)`:**
* Kiểm tra: `currentUpload == null` (nếu có file dở $\rightarrow$ gửi `ERR|BUSY`), `size <= 150MB` (nếu vượt $\rightarrow$ gửi `ERR|FILE_TOO_LARGE`), người nhận đang online (`server.find`).
* Hợp lệ $\rightarrow$ gọi `server.startTransfer(...)` để nhận `FileTransfer`, gán vào `currentUpload`.
* Gửi `OK|FILE_START|` + `uploadId` cho người gửi và gửi thông báo `FILE_START|uploadId|threadName|fileName|size` cho người nhận.


* **`handleFileChunk(data)`:**
* Bóc 4 byte đầu của mảng `data` để đọc số nguyên `uploadId`.
* Kiểm tra nếu `uploadId != currentUpload.getUploadId()` $\rightarrow$ gọi `failTransfer()`.
* Cộng dồn số byte thực (`data.length - 4`) vào `currentUpload.addBytes(...)`.
* Lấy handler người nhận (`server.find`). Nếu tìm thấy $\rightarrow$ gọi `recipient.sendChunk(data, data.length)`. Nếu đứt kết nối $\rightarrow$ gọi `failTransfer()`.


* **`handleFileEnd()`:**
* Kiểm tra `currentUpload != null` và `currentUpload.isComplete()`.
* Gửi frame `FILE_END|` + `uploadId` cho cả người gửi và người nhận.
* Gọi `server.finishTransfer(uploadId, true)` rồi gán `currentUpload = null`.


* **`failTransfer()`:**
* Nếu `currentUpload == null` $\rightarrow$ kết thúc.
* Gửi frame lỗi `ERR|TRANSFER_FAILED|` + `uploadId` cho cả hai bên (bỏ qua lỗi nếu socket một bên đã ngắt).
* Gọi `server.finishTransfer(uploadId, false)` và gán `currentUpload = null`.


* **`close()`:**
1. Gọi `failTransfer()` để dọn dẹp phiên gửi file đang dở nếu có.
2. Đóng `socket` nếu chưa đóng.
3. Nếu `threadName != null` $\rightarrow$ gọi `server.unregister(threadName)`.
4. Trả permit cho `Semaphore` an toàn đúng một lần:
```java
if (released.compareAndSet(false, true)) {
    server.getConnectionLimit().release();
}

```

---

### 4.9 `SoaptaClient`

Ứng dụng Client chạy trên Console, cung cấp menu và tương tác với người dùng.

**Thuộc tính**

* `private Socket socket` — Socket kết nối tới Server.
* `private DataInputStream in` — Luồng đọc từ Server.
* `private DataOutputStream out` — Luồng ghi tới Server.
* `private final Scanner scanner` — Nhập liệu bàn phím (`new Scanner(System.in)`).
* `private String myThread` — Thread ID do Server cấp sau khi đăng nhập.
* `private String displayName` — Tên người dùng nhập lúc khởi động.
* `private volatile String activeChatTarget` — Thread ID người đang chat 1:1 (hoặc `groupId` đang chat nhóm); là `null` nếu đang ở màn hình Menu.
* `private final BlockingQueue<String> responseQueue` — Hàng đợi trung chuyển kết quả đồng bộ (`LIST`, `GLIST`) từ luồng nhận về luồng menu chính (`new LinkedBlockingQueue<>()`).
* `private final Map<Integer, FileOutputStream> incomingFiles` — Bảng lưu luồng ghi các file đang nhận về thư mục `./downloads/` theo từng `uploadId`.

**Phương thức**

* `public static void main(String[] args)` — Hàm `main` khởi động ứng dụng.
* `public void start()` — Khởi động luồng nhập liệu và kết nối máy chủ.
* `private void login()` — Giao diện nhập tên, gửi lệnh `NAME` và chờ xác nhận.
* `private void startReceiver()` — Tạo và chạy luồng ngầm (Virtual Thread hoặc Platform Thread) chuyên đọc frame từ Server.
* `private void handleIncomingText(String line)` — Phân tích gói tin văn bản từ Server gửi tới.
* `private void handleIncomingChunk(byte[] data)` — Ghi chunk nhị phân nhận được vào file trên đĩa cứng.
* `private void showMainMenu()` — Hiển thị Menu chính và xử lý điều hướng chức năng 1-6.
* `private void privateChat()` — Chức năng Chat 1:1.
* `private void groupChat()` — Chức năng Chat nhóm.
* `private void createGroup()` — Chức năng Tạo nhóm mới.
* `private void showMyGroups()` — Chức năng Xem danh sách các nhóm đã vào.
* `private void sendFileScreen()` — Chức năng Gửi file (đọc file theo chunk 512 KB và gửi đi).
* `private void sendText(String command) throws IOException` — Đóng gói và gửi frame TEXT.
* `private void exit()` — Đóng ứng dụng an toàn.

**Cách triển khai (Implementation)**

* **Khởi tạo và Login:**
1. Yêu cầu nhập Hostname và Port. Tạo `socket = new Socket(host, port)`.
2. Tạo `in = new DataInputStream(socket.getInputStream())` và `out = new DataOutputStream(socket.getOutputStream())`.
3. Lặp nhập tên `displayName` cho đến khi khớp regex: Gửi gói tin `NAME|{displayName}` qua `MessageCodec.write(out, ...)`.
4. Đọc phản hồi đầu tiên: Nếu nhận được `OK|NAME|{threadName}` thì gán `this.myThread = threadName`, in ra màn hình đăng nhập thành công và thoát vòng lặp login.


* **Cơ chế 2 luồng độc lập:**
* *Luồng nhận (`startReceiver`):* Chạy vòng lặp vô tận lắng nghe socket:
* Nếu đọc được `0x01` (`TEXT`): gọi `handleIncomingText(line)`.
* Nếu `line` bắt đầu bằng `LIST|` hoặc `GLIST|`: Đẩy vào `responseQueue.offer(line)` để hàm ở menu lấy dữ liệu.
* Nếu `line` bắt đầu bằng `MSG|sender|content`: Nếu `sender.equals(activeChatTarget)` thì in ngay `sender > content`. Nếu đang ở chỗ khác thì in dòng thông báo nhẹ: `[New message from sender]`.
* Nếu `line` bắt đầu bằng `GMSG|groupId|sender|content`: Nếu `groupId.equals(activeChatTarget)` thì in `[GroupName] sender > content`.
* Nếu `line` bắt đầu bằng `NOTICE|`: In ngay `[NOTICE] nội dung`.
* Nếu `line` bắt đầu bằng `FILE_START|uploadId|sender|fileName|size`: Tạo thư mục `./downloads/` (nếu chưa có), mở `new FileOutputStream("./downloads/" + fileName)` và lưu vào `incomingFiles.put(uploadId, fos)`. In thông báo đang nhận file.
* Nếu `line` bắt đầu bằng `FILE_END|uploadId`: Lấy `FileOutputStream` tương ứng, đóng luồng qua `fos.close()`, xóa khỏi map và in thông báo nhận file hoàn tất.


* Nếu đọc được `0x02` (`FILE`): Gọi `handleIncomingChunk(data)`:
* Đọc 4 byte đầu mảng để lấy `uploadId`.
* Ghi phần dữ liệu còn lại `(offset = 4, len = data.length - 4)` vào luồng `FileOutputStream` của `uploadId` đó.




* *Luồng chính (`Main Thread`):*
* Chạy menu điều hướng `showMainMenu()`.
* Khi người dùng chọn Chat 1:1 (`privateChat()`): Gửi lệnh `LIST`, gọi `responseQueue.poll(3, TimeUnit.SECONDS)` để lấy danh sách người online hiển thị lên màn hình. Người dùng chọn một người nhận -> Gán `activeChatTarget = targetThread` -> Vào vòng lặp đọc chuỗi từ bàn phím. Mỗi câu chat gõ vào được gửi bằng lệnh `PM|targetThread|nội dung`. Khi người dùng gõ `/exit` thì gán `activeChatTarget = null` và quay về Menu.




* **Gửi File theo Chunk (`sendFileScreen`):**
1. Nhập đường dẫn file từ bàn phím. Kiểm tra `File file = new File(path)`.
2. Nếu không tồn tại hoặc `file.length() > 150 * 1024 * 1024`: Báo lỗi và dừng.
3. Gửi lệnh `FILE_START|target|file.getName()|file.length()`.
4. Chờ phản hồi `OK|FILE_START|{uploadId}` từ server.
5. Mở `FileInputStream fis = new FileInputStream(file)`:
* Tạo bộ đệm chunk: `byte[] buffer = new byte[MessageCodec.MAX_CHUNK_SIZE]`. 4 byte đầu luôn dùng để ghi `uploadId`.
* Đọc tuần tự từng đợt dữ liệu tối đa `524284` bytes (để cộng 4 byte ID là đủ 512 KB), sau đó gọi `MessageCodec.writeChunk(...)` gửi sang socket.
* Tính toán tỷ lệ phần trăm đã đọc để vẽ thanh tiến trình `[####------] 40%`.


6. Sau khi đọc hết file: Gửi lệnh `FILE_END` và in thông báo hoàn tất.


---

### Tóm tắt sơ đồ tương tác giữa các Class

```
                +--------------------------------------------------+
                |                   SoaptaServer                   |
                |  - connectionLimit (Semaphore: 5000)             |
                |  - onlineClients (ConcurrentHashMap)             |
                |  - groupManager (GroupManager)                   |
                |  - activeTransfers (ConcurrentHashMap)           |
                +--------------------------------------------------+
                                         |
                       +-----------------+-----------------+
                       |                                   |
              [manages / spawns]                  [manages / spawns]
                       |                                   |
                       v                                   v
             +--------------------+               +--------------------+
             |   ClientHandler    |               |   ClientHandler    |
             | (thread1-client-a) |               | (thread2-client-b) |
             +--------------------+               +--------------------+
                       ^                                   ^
                       | (TCP Socket / MessageCodec)       | (TCP Socket / MessageCodec)
                       v                                   v
             +--------------------+               +--------------------+
             |    SoaptaClient    |               |    SoaptaClient    |
             | (Console Client 1) |               | (Console Client 2) |
             +--------------------+               +--------------------+

```

---

## 5. Thang điểm đánh giá (Tổng: 100 điểm)

| STT | Hạng mục đánh giá | Yêu cầu chi tiết | Điểm |
| --- | --- | --- | --- |
| **I** | **Kiến trúc & Đóng gói Giao thức** | **20đ** |  |
| 1 | Framing & Codec | Đọc/ghi đúng format tiền tố byte loại + 4 byte length, không bị dính gói hay tràn dữ liệu. | 10đ |
| 2 | Virtual Threads & Concurrency Setup | Khởi tạo Virtual Threads qua ExecutorService; Semaphore giới hạn đúng 5.000 kết nối. | 10đ |
| **II** | **Xử lý Kết nối & Định danh** | **20đ** |  |
| 3 | Validate tên & Cho phép trùng tên | Regex hợp lệ tiếng Việt, tự sinh ID `thread{id}-client-{name}` chính xác, không xung đột. | 10đ |
| 4 | Vòng đời kết nối & Giải phóng tài nguyên | Khi client thoát (bình thường hoặc cưỡng bức), server tự hủy phiên, nhả Semaphore permit đúng 1 lần. | 10đ |
| **III** | **Chức năng Chat (1:1 & Nhóm)** | **30đ** |  |
| 5 | Lấy danh sách Online & Chat 1:1 | Lệnh `LIST` hiển thị đủ Thread ID; tin nhắn gửi đúng người nhận, báo lỗi nếu người nhận offline. | 15đ |
| 6 | Quản lý & Chat Nhóm | Tạo nhóm, thêm thành viên online, gửi tin tới toàn bộ thành viên (trừ người gửi), thông báo NOTICE khi được thêm. | 15đ |
| **IV** | **Truyền File Tối đa 150 MB** | **20đ** |  |
| 7 | Truyền file theo Chunk (Streaming) | Cắt nhỏ file thành các chunk $\le 512$ KB kèm `uploadId`; Server chuyển tiếp trực tiếp, không tràn bộ nhớ heap. | 10đ |
| 8 | Bắt lỗi gián đoạn truyền file | Xử lý file quá 150 MB; nếu người gửi hoặc nhận ngắt mạng giữa chừng thì hủy phiên và báo lỗi cho bên còn lại. | 10đ |
| **V** | **Giao diện Console & Trải nghiệm** | **10đ** |  |
| 9 | Console UI & Menu điều hướng | Menu rõ ràng, chuyển đổi mượt mà giữa các chế độ chat và menu chính bằng lệnh `/exit`. | 10đ |
| **TỔNG** |  |  | **100đ** |

---

## 6. Đặc tả chi tiết giao diện Console (UI / UX Flows)

Ứng dụng chạy hoàn toàn trên terminal/dòng lệnh, sử dụng text và ký tự ASCII tiêu chuẩn. Mỗi màn hình chức năng cần có tiêu đề phân cách bằng khung `=` rõ ràng.

### 6.1. Màn hình Đăng nhập (Login)

Khi khởi động ứng dụng `SoaptaClient`, người dùng nhập thông tin kết nối và tên hiển thị:

```text
========================================
           SOAPTA CHAT LOGIN
========================================
Hostname: localhost
Port: 8080

Enter your display name: long
Connecting to server...
Login successful!
Your ID: thread3-client-long

```

* **Xử lý lỗi tên:** Nếu tên không khớp regex `^[\p{L}0-9_ ]{2,20}$`:
```text
Invalid name! Use 2-20 characters (letters, numbers, space, underscore).
Enter your display name: _

```


* **Cho phép trùng tên:** Người dùng vẫn được đăng nhập bình thường, server tự động cấp `thread{id}-client-{tên}` để phân biệt.

---

### 6.2. Menu chính (Main Menu)

Sau khi đăng nhập thành công, client hiển thị menu điều hướng:

```text
========================================
               MAIN MENU
========================================
1. Private Chat (1:1)
2. Group Chat
3. Create Group
4. My Groups
5. Send File
6. Exit

Choose an option (1-6): _

```

* Nhập ký tự không hợp lệ hoặc nằm ngoài `1-6`: in thông báo `Invalid option. Please choose 1-6.` và cho chọn lại.

---

### 6.3. Màn hình Chat 1:1 (Private Chat)

**Bước 1: Chọn người nhận từ danh sách online**

```text
========================================
             PRIVATE CHAT
========================================
Online Users (3):
1. thread1-client-an
2. thread2-client-binh
3. thread3-client-long (You)

Select recipient number (0 to back): 2

```

* Nếu chọn `0`: quay lại Menu chính.
* Nếu chọn số của chính mình hoặc số không tồn tại: báo lỗi và yêu cầu nhập lại.

**Bước 2: Giao diện phòng chat**

```text
Chatting with [thread2-client-binh]. Type /exit to return to menu.
--------------------------------------------------
You > Xin chao ban!
thread2-client-binh > Chao ban, co chuyen gi the?
You > Dang test ung dung chat Java day.
thread2-client-binh > Ok rat muot nhe!
You > /exit
Returning to menu...

```

* Nhận tin nhắn từ người đang chat: in trực tiếp `thread_gửi > nội dung`.
* Nhận tin nhắn từ người khác: in thông báo vắn tắt `[New message from thread1-client-an]`.

---

### 6.4. Màn hình Chat nhóm (Group Chat)

**Bước 1: Chọn nhóm tham gia**

```text
========================================
              GROUP CHAT
========================================
Your Groups:
1. Ban Hoc (ID: G1 | 3 members)
2. Gaming (ID: G2 | 5 members)

Select group number (0 to back): 1

```

* Nếu chưa tham gia nhóm nào: in `You have not joined any groups yet.` và quay lại menu.

**Bước 2: Giao diện phòng chat nhóm**

```text
Joined room [Ban Hoc - G1]. Type /exit to return to menu.
--------------------------------------------------
You > Moi nguoi da nhan file de tai chua?
[Ban Hoc] thread1-client-an > Minh nhan roi nhe.
[Ban Hoc] thread2-client-binh > Minh dang xem.
You > /exit
Returning to menu...

```

* Tin nhắn nhóm được hiển thị theo định dạng: `[Tên nhóm] thread_gửi > nội dung`.
* Người gửi không nhận lại tin phản chiếu của chính mình.

---

### 6.5. Màn hình Tạo nhóm (Create Group)

```text
========================================
             CREATE GROUP
========================================
Group Name: Ban Hoc

Select members to add:
1. thread1-client-an
2. thread2-client-binh

Enter member numbers (separated by comma, e.g. 1,2 or Enter to skip): 1,2

Creating group...
Group "Ban Hoc" created successfully! Group ID: G1
Total members: 3 (including you)

```

* Tên nhóm phải thỏa mãn regex giống tên người dùng; nếu sai báo `Invalid group name.`
* Người tạo tự động trở thành thành viên đầu tiên và giữ vai trò `owner`.
* Các người dùng được thêm vào sẽ nhận được dòng thông báo trên console của họ: `[NOTICE] You were added to group "Ban Hoc" (G1) by thread3-client-long.`

---

### 6.6. Màn hình Xem danh sách nhóm của tôi (My Groups)

Liệt kê các nhóm mà client hiện tại đang tham gia:

```text
========================================
              MY GROUPS
========================================
- [G1] Ban Hoc (Owner: thread3-client-long | 3 members)
- [G2] Gaming Team (Owner: thread1-client-an | 4 members)

Press Enter to continue...

```

---

### 6.7. Màn hình Gửi File (Send File)

Cho phép gửi file 1:1 hoặc gửi vào nhóm:

```text
========================================
             FILE TRANSFER
========================================
1. Send file to user (1:1)
2. Send file to group
0. Back to menu

Choose (0-2): 1
Recipient Thread ID: thread2-client-binh
File path: D:/data/report.zip

Checking file...
File size: 45.2 MB (Valid <= 150 MB)
Sending file: report.zip
[####################] 100%
Upload completed successfully!

```

* **Phía người gửi:**
* Nếu file không tồn tại: báo `File not found! Check your path.`
* Nếu file $> 150\text{ MB}$: báo `File too large! Max allowed size is 150 MB.`
* Thanh tiến trình gồm 20 ký tự `#` và `-`, cập nhật theo phần trăm số byte đã chuyển.


* **Phía người nhận:**
* Tự động nhận frame, ghi dữ liệu vào thư mục `./downloads/` và in thông báo:


```text
[FILE] Receiving "report.zip" (45.2 MB) from thread3-client-long...
[FILE] Saved to ./downloads/report.zip successfully!

```



---

### 6.8. Quy tắc chung cho Console Client

1. **Phục hồi sau lỗi nhập liệu:** Khi người dùng nhập chuỗi sai kiểu, số ngoài danh mục hoặc ký tự lạ, ứng dụng in cảnh báo ngắn gọn và yêu cầu nhập lại, tuyệt đối không quăng ngoại lệ làm crash app.
2. **Cơ chế luồng hiển thị (Async Display):** Luồng nhận tin nhắn chạy ngầm (`Receiver Thread`) in trực tiếp lên `System.out`. Khi có tin nhắn đến trong lúc người dùng đang gõ, text có thể bị đẩy dòng nhẹ — đây là đặc tính chấp nhận được của console không dùng thư viện ngoài.
3. **Thoát an toàn (Graceful Exit):** Chọn `6. Exit` hoặc gõ `/exit` ở ngoài menu sẽ in `Goodbye!` và đóng socket sạch sẽ, giúp server thu hồi slot kết nối ngay lập tức.

---

## 7. Kịch bản kiểm thử (Test Cases)

1. **Trùng tên:**
* Khởi động 2 Client cùng nhập tên `Long`.
* Kết quả mong đợi: Server tạo ra 2 thread riêng biệt (ví dụ `thread1-client-long` và `thread2-client-long`). Cả 2 cùng online và chat được với nhau.


2. **Cách ly tin nhắn 1:1:**
* 3 Client A, B, C cùng online. A gửi tin nhắn riêng cho B.
* Kết quả: B nhận được nội dung tin nhắn, C hoàn toàn không nhận được gì.


3. **Chat nhóm:**
* Client A tạo nhóm `TestGroup`, thêm B và C.
* A gửi tin nhắn nhóm: B và C nhận được, A không nhận lại tin phản hồi lặp lại của chính mình.


4. **Truyền file lớn & Dung lượng giới hạn:**
* Gửi file dung lượng 100 MB: Bên nhận lưu file hoàn chỉnh, mở ra kiểm tra tính toàn vẹn (checksum MD5/SHA256 trùng khớp).
* Gửi file > 150 MB: Client chặn ngay lập tức hoặc Server từ chối với lỗi `FILE_TOO_LARGE`.


5. **Đứt kết nối bất ngờ:**
* Client đang truyền file thì bị tắt ngang (Ctrl + C hoặc tắt cửa sổ console).
* Phía người nhận nhận được thông báo `ERR|TRANSFER_FAILED`, server không bị crash và tự thu hồi slot kết nối.