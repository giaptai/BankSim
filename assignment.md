# Đề Bài Dự Án CoreChat: Hệ Thống Chat Socket Đa Người Dùng

Bạn sẽ xây dựng một ứng dụng chat theo mô hình client-server, dùng TCP socket và giao thức frame tự định nghĩa. Hệ thống hỗ trợ chat 1-1, chat nhóm, truyền file lớn và giám sát quản trị. Dự án áp dụng lập trình hướng đối tượng (OOP), xử lý đa luồng (Multithreading), kết nối cơ sở dữ liệu (JDBC) và các Design Pattern, bao gồm cả giao diện console và GUI.

Chỉ dùng Java core (JDK 21 trở lên). Không dùng thư viện ngoài, trừ JDBC driver của PostgreSQL.

---

## 1. Yêu Cầu Về Kiến Trúc và OOP

*   **Lớp `Message`** ✉️
    *   **Mô tả**: Đại diện cho một tin nhắn hoặc lệnh được truyền qua mạng.
    *   **Thuộc tính**:
        *   `private final MessageType type`: Loại tin nhắn.
        *   `private final String payload`: Nội dung, không được `null`.
    *   **Phương thức**:
        *   `public Message(MessageType type, String payload)`: Constructor, kiểm tra tham số không `null`.
        *   `public static Builder builder()`: Trả về `Builder` (xem mục Design Pattern).
        *   `public MessageType getType()`, `public String getPayload()`.
        *   `public String toString()`: Trả về chuỗi biểu diễn để ghi log.
    *   **Điểm**: **1 điểm** (dễ).

*   **Lớp `MessageSerializer`** 📦
    *   **Mô tả**: Mã hóa và giải mã frame theo định dạng `[4 byte độ dài][loại\npayload]`, dùng UTF-8.
    *   **Phương thức**:
        *   `public static void write(OutputStream out, Message message)`: Ghi một frame. Ném lỗi nếu frame vượt 512 KB.
        *   `public static Message read(DataInputStream in)`: Đọc đúng một frame. Ném lỗi nếu độ dài âm, vượt giới hạn, hoặc loại không hợp lệ.
    *   **Yêu cầu**: Không đọc quá số byte của frame. Dùng `readFully`, không dùng `read`.
    *   **Điểm**: **Gộp trong mục `Message`** (1 điểm).

*   **Lớp `ClientSession`** 🔌
    *   **Mô tả**: Đại diện cho một người dùng đã đăng nhập, gồm tên và luồng ghi đến client.
    *   **Thuộc tính**:
        *   `private final String name`: Tên hiển thị, duy nhất trong server.
        *   `private final DataOutputStream out`: Luồng ghi đến client.
    *   **Phương thức**:
        *   `public synchronized void send(Message message)`: Gửi một frame. Từ khóa `synchronized` bảo vệ frame khỏi bị trộn khi nhiều luồng cùng gửi cho một người.
        *   `public String getName()`.
    *   **Điểm**: **1 điểm** (dễ).

*   **Lớp `ChatServer`** 🖥️
    *   **Mô tả**: Mở `ServerSocket`, nhận nhiều kết nối TCP đồng thời và giao mỗi kết nối cho một luồng xử lý riêng.
    *   **Thuộc tính**:
        *   `private final int port`
        *   `private final ExecutorService connectionExecutor`
        *   `private final Semaphore connectionLimit`
        *   `private final SessionRegistry sessionRegistry`
        *   `private final GroupRegistry groupRegistry`
    *   **Phương thức**:
        *   `public ChatServer(int port)`
        *   `public void start()`: Vòng lặp `accept()`. Kết nối vượt giới hạn phải bị từ chối có log.
        *   `public void stop()`: Đóng `ServerSocket` và tắt executor.
    *   **Điểm**: **1 điểm** (trung bình).

*   **Lớp `SessionRegistry`** 🗂️
    *   **Mô tả**: Lưu danh sách người dùng đang online.
    *   **Phương thức**:
        *   `public boolean join(ClientSession session)`: Trả về `false` nếu tên đã tồn tại.
        *   `public void leave(String name)`
        *   `public ClientSession find(String name)`
        *   `public Set<String> onlineNames()`: Trả về bản sao an toàn để duyệt.
    *   **Điểm**: **Gộp trong mục đồng bộ hóa** (xem mục 2).

*   **Lớp `GroupRegistry` và `Group`** 👥
    *   **Mô tả**: Quản lý nhóm chat. Mỗi nhóm có `groupId` (khóa định danh) và tên hiển thị. Tên nhóm có thể trùng, `groupId` không được trùng.
    *   **Phương thức**:
        *   `public String create(String name, String owner)`: Trả về `groupId`.
        *   `public boolean addMember(String groupId, String name)`
        *   `public Set<String> members(String groupId)`: Trả về bản sao.
    *   **Điểm**: **1 điểm** (trung bình).

*   **Lớp `FileTransfer`** 📁
    *   **Mô tả**: Quản lý một phiên gửi file theo chunk, gồm kích thước, số byte đã nhận và SHA-256.
    *   **Thuộc tính**: `uploadId`, `sender`, `recipient`, `fileName`, `totalSize`, `receivedBytes`, `expectedSha256`.
    *   **Yêu cầu**: Kích thước tối đa 150 MB. Mỗi chunk tối đa 512 KB. Khi nhận đủ, kiểm tra SHA-256 trước khi thông báo cho người nhận.
    *   **Điểm**: **Gộp trong mục truyền file** (xem mục 2).

---

## 2. Yêu Cầu Về Giao Thức và Chức Năng

### 2.1 Giao thức lệnh

| Hướng | Payload | Ý nghĩa |
|---|---|---|
| Client → Server | Tên người dùng (lần đầu) | Đăng ký tên |
| Server → Client | `OK\|JOIN` hoặc `ERROR\|NAME_TAKEN` | Kết quả đăng ký |
| Client → Server | `LIST_USERS` | Lấy danh sách online |
| Server → Client | Loại `USER_LIST`, payload `a, b, c` | Danh sách online |
| Client → Server | `recipient: nội dung` | Chat 1-1 |
| Server → Client | `sender: nội dung` | Tin nhắn đến |
| Client → Server | `GROUP_MSG\|groupId\|nội dung` | Chat nhóm |
| Client → Server | `FILE_START`, `FILE_CHUNK`, `FILE_END` | Truyền file |

Sinh viên có thể thiết kế lại giao thức, nhưng phải ghi rõ trong README.

### 2.2 Chức năng nghiệp vụ

*   **Đăng nhập theo tên**: Người dùng nhập tên, không cần mật khẩu. Tên trùng với người đang online thì bị từ chối và được hỏi lại.
*   **Chat 1-1**: Chọn người đang online, gửi tin. Tin chỉ hiển thị cho người đang chat, còn tin từ người khác không làm gián đoạn màn hình hiện tại.
*   **Chat nhóm**: Tạo nhóm, thêm thành viên, gửi tin cho nhóm. Mỗi thành viên online nhận tin.
*   **Thông báo**: Người dùng nhận thông báo khi được thêm vào nhóm hoặc khi có file gửi đến.
*   **Truyền file**: Gửi file tối đa 150 MB theo chunk, hiển thị tiến trình `[####----] 65%`, có kiểm tra SHA-256.
*   **Lịch sử hội thoại**: Lưu tin nhắn vào CSDL. Người dùng xem được lịch sử 1-1 và nhóm.
*   **Quản trị (Admin Console trên server)**: Nhập lệnh trên console của server để xem số kết nối, người online, số nhóm, số tin/giây, tiến trình truyền file, thread pool và lỗi gần đây.

    > Admin không nằm trong client, vì client không có đăng nhập, nên người dùng thường không được xem thông tin quản trị.

*   **Chấm điểm phần quản trị**: Lệnh `stats`, `groups`, `sessions`, `transfers`, `errors` phải in dữ liệu thật, không chỉ in mẫu cố định.

---

## 3. Yêu Cầu Về Đa Luồng (Multithreading)

*   **Xử lý kết nối bằng ExecutorService**: Mỗi kết nối được xử lý trên một luồng riêng, dùng `Executors.newVirtualThreadPerTaskExecutor()`. Không dùng `new Thread` cho mỗi kết nối.
    *   **Điểm**: **1.5 điểm** (trung bình).

*   **Giới hạn 5000 kết nối đồng thời**: Dùng `Semaphore`. Dùng `tryAcquire()` để không chặn vòng lặp `accept()`. Kết nối vượt giới hạn phải bị đóng và được ghi log. Nếu `submit` thất bại, phải trả lại permit.
    *   **Điểm**: **1 điểm** (trung bình).

*   **Fan-out tin nhắn nhóm bằng thread pool có giới hạn**: Khi gửi tin nhóm, server không duyệt toàn bộ thành viên trên luồng nhận. Việc gửi phải đi qua một `ThreadPoolExecutor` cố định, dùng `LinkedBlockingQueue` có giới hạn.
    *   **Điểm**: **Gộp trong mục ExecutorService** (1.5 điểm).

*   **Đồng bộ hóa không bị race condition**: Các cấu trúc dùng chung (`SessionRegistry`, `GroupRegistry`, danh sách thành viên nhóm) phải an toàn khi nhiều luồng cùng truy cập. Dùng `ConcurrentHashMap` và `putIfAbsent` cho việc đăng ký tên. Tuyệt đối không dùng `containsKey` rồi `put` vì có thể bị race condition.
    *   **Điểm**: **3 điểm** (khó).

*   **Ngăn chặn deadlock**: Khi một thao tác cần khóa hai đối tượng (ví dụ: chuyển thành viên từ nhóm này sang nhóm khác, hoặc cập nhật hai `ClientSession` cùng lúc), các khóa phải được lấy theo một thứ tự nhất quán, ví dụ theo `groupId` tăng dần hoặc theo tên tăng dần. Sinh viên phải viết ít nhất một test hoặc kịch bản chứng minh không bị deadlock.
    *   **Điểm**: **3 điểm** (khó).

*   **Không dùng `synchronized` quanh thao tác I/O chậm**: Vì virtual thread bị pinned khi giữ monitor trong thao tác blocking trên JDK 21. Dùng `ReentrantLock` nếu cần khóa trong đoạn có I/O.
    *   **Điểm**: **Không tính điểm riêng**, nhưng bị trừ 1 điểm nếu vi phạm.

---

## 4. Yêu Cầu Về Cơ Sở Dữ Liệu (JDBC)

*   **Lớp `DatabaseManager`** 🗄️
    *   **Mô tả**: Chịu trách nhiệm mọi tương tác với PostgreSQL. Là cầu nối duy nhất giữa server và CSDL.
    *   **Thuộc tính**:
        *   `private final Properties props`: Cấu hình kết nối từ `application.properties`.
    *   **Phương thức**:
        *   `public DatabaseManager()`: Tải cấu hình và tạo schema từ `schema.sql` nếu chưa có.
        *   `private Connection getConnection()`
        *   `public void saveMessage(String sender, String recipient, String content)`: Lưu tin 1-1.
        *   `public void saveGroupMessage(String groupId, String sender, String content)`: Lưu tin nhóm.
        *   `public void saveGroup(String groupId, String name, String owner)`
        *   `public void addGroupMember(String groupId, String name)`
        *   `public List<ChatMessage> getConversation(String userA, String userB, int limit)`: Lấy lịch sử 1-1.
        *   `public List<ChatMessage> getGroupHistory(String groupId, int limit)`: Lấy lịch sử nhóm.
    *   **Điểm**: **1.5 điểm** (trung bình).

*   **Batch processing và transaction**: Khi lưu tin nhắn nhóm cho nhiều thành viên (ví dụ bản ghi trạng thái "đã nhận" cho từng người), dùng batch insert trong một database transaction (`setAutoCommit(false)`, `commit`, `rollback` khi lỗi). Hai bản ghi của một lần chuyển khoản hoặc một tin nhắn phải được ghi cùng lúc.
    *   **Điểm**: **Gộp trong mục DatabaseManager** (1.5 điểm).

*   **Sử dụng `PreparedStatement`**: Mọi câu lệnh SQL có tham số phải dùng `PreparedStatement`, không dùng `Statement` và không ghép chuỗi. Điều này phòng chống SQL Injection, kể cả khi người dùng nhập tên có ký tự đặc biệt như `'` hoặc `;`.
    *   **Điểm**: **3 điểm** (khó).

---

## 5. Yêu Cầu Về Giao Diện & Xử Lý Lỗi

*   **Giao diện console (client)**: Menu gồm các mục: Private Chat, Group Chat, Create Group, My Conversations, Notifications, File Transfer, Exit. Chọn `/exit` trong chat 1-1 phải quay lại menu chính, không đóng chương trình.
    *   **Điểm**: **1 điểm** (dễ).

*   **Giao diện đồ họa (GUI) bằng Swing**: Cung cấp giao diện cho chat 1-1 và nhóm, gồm vùng hiển thị tin nhắn, ô nhập và danh sách người online. Tin nhắn đến phải cập nhật trên luồng Swing bằng `SwingUtilities.invokeLater`.
    *   **Điểm**: **2 điểm** (trung bình).

*   **Xử lý lỗi**: Dùng `try-catch` cho các trường hợp: tên không hợp lệ, tên đã tồn tại, người nhận offline, nhóm không tồn tại, frame vượt giới hạn, file vượt 150 MB, checksum sai, mất kết nối. Mỗi lỗi phải có thông báo rõ ràng cho người dùng.
    *   **Điểm**: **1.5 điểm** (trung bình).

*   **Kiểm tra tên bằng regex và ghi log trùng tên**:
    *   Tên người dùng phải khớp biểu thức chính quy, ví dụ `^[\\p{L}0-9_ ]{2,20}$`: chỉ chữ cái, số, dấu gạch dưới hoặc khoảng trắng, dài từ 2 đến 20 ký tự. Tên không hợp lệ bị từ chối.
    *   Khi tên bị từ chối vì đã online, ghi log kèm ngày giờ đầy đủ, ví dụ `2026-11-07 18:37:00 WARNING: Name already taken: alice`.
    *   Log phải dùng tiếng Anh.
    *   **Điểm**: **1.5 điểm** (trung bình).

*   **Lưu ý về log và console**: Log dùng `java.util.logging.Logger` với định dạng gọn. Giao diện người dùng dùng `System.out` hoặc Swing, không dùng `Logger`, để console không hiện thời gian và tên class.

---

## 6. Các Design Patterns Đã Áp Dụng

Mỗi pattern phải có class cụ thể trong code, và README phải giải thích lý do chọn pattern đó.

*   **Factory Method (kết hợp Registry Pattern)** 🏭
    *   **Điểm**: **2 điểm**
    *   **Mô tả**: `CommandHandlerFactory.create(String command)` trả về `CommandHandler` phù hợp (ví dụ `LIST_USERS`, `PM`, `GROUP_MSG`, `FILE_START`). Các handler được đăng ký vào một `Map<String, Supplier<CommandHandler>>`, để thêm lệnh mới không cần sửa factory.
    *   **Lợi ích**: Tách `ChatServer` khỏi từng loại lệnh cụ thể, dễ mở rộng theo nguyên tắc Open/Closed.

*   **Singleton** 🌟
    *   **Điểm**: **2 điểm**
    *   **Mô tả**: Đảm bảo một instance duy nhất cho:
        *   `SessionRegistry` và `GroupRegistry`: nguồn sự thật duy nhất về người online và nhóm.
        *   `DatabaseManager`: quản lý một pool kết nối duy nhất.
    *   **Lợi ích**: Không tạo trùng trạng thái dùng chung giữa các luồng.

*   **Observer** 👁️
    *   **Điểm**: **2 điểm**
    *   **Mô tả**: `ChatServer` phát các sự kiện (`UserJoined`, `UserLeft`, `MessageSent`, `FileProgress`, `ErrorOccurred`) cho các `ServerEventListener` đã đăng ký. Một listener cập nhật bộ đếm metric và phần Admin Console; một listener khác ghi log.
    *   **Lợi ích**: Tách logic giám sát khỏi logic chat. Thêm metric mới không cần sửa luồng xử lý tin nhắn.

*   **Template Method** 📝
    *   **Điểm**: **1.5 điểm**
    *   **Mô tả**: `CommandHandler` định nghĩa khung xử lý: `validate()` → `execute()` → `respond()`. Các lớp con như `PrivateMessageHandler`, `GroupMessageHandler`, `FileChunkHandler` chỉ triển khai các bước cụ thể.
    *   **Lợi ích**: Chuẩn hóa cách xử lý lệnh, dễ thêm lệnh mới.

*   **Builder** 🏗️
    *   **Điểm**: **1 điểm**
    *   **Mô tả**: `Message.Builder` tạo đối tượng `Message` với các trường tùy chọn như `type`, `payload`, `timestamp`, `sender`, rõ ràng và dễ đọc.
    *   **Lợi ích**: Tránh constructor nhiều tham số và giảm lỗi khi truyền sai thứ tự.

---

## 7. Yêu Cầu Nộp Bài

*   **Mã nguồn**: Đặt theo cấu trúc package rõ ràng (`common`, `server`, `client`, `db`, `gui`).
*   **README.md** gồm:
    *   Hướng dẫn chạy server và client.
    *   Mô tả giao thức, có bảng lệnh.
    *   Các Design Pattern đã dùng và lý do.
    *   **Giới hạn đã biết**, ví dụ: tin đến lúc đang gõ trên console có thể lẫn hiển thị, tên không có xác thực nên có thể bị giả mạo khi người dùng offline.
*   **Kết quả đo đạc**: Số kết nối đồng thời đạt được, bộ nhớ sử dụng và thời gian gửi tin nhóm 100 người. Đo trước và sau khi dùng virtual thread nếu có thể.
*   **Demo**: Quay hoặc mô tả kịch bản: hai người chat 1-1, một nhóm chat, gửi file 150 MB, admin xem thống kê.

---

## 8. Tổng Kết và Điểm Vượt Qua

*   **Tổng điểm tối đa**: **30.5 điểm** (22 điểm chức năng + 8.5 điểm Design Patterns).
*   **Điểm vượt qua**: **21 điểm**.

| Nhóm | Hạng mục | Điểm |
|---|---|---|
| OOP | Message, MessageSerializer | 1 |
| OOP | ClientSession | 1 |
| OOP | ChatServer | 1 |
| OOP | Giới hạn 5000 kết nối | 1 |
| Đa luồng | ExecutorService và fan-out | 1.5 |
| Đa luồng | Đồng bộ hóa không race condition | 3 |
| Đa luồng | Ngăn deadlock | 3 |
| JDBC | DatabaseManager, batch, transaction | 1.5 |
| JDBC | PreparedStatement | 3 |
| Giao diện | Console client | 1 |
| Giao diện | GUI Swing | 2 |
| Xử lý lỗi | try-catch và thông báo lỗi | 1.5 |
| Xử lý lỗi | Regex tên và log trùng tên có ngày | 1.5 |
| Design Pattern | Factory Method (Registry) | 2 |
| Design Pattern | Singleton | 2 |
| Design Pattern | Observer | 2 |
| Design Pattern | Template Method | 1.5 |
| Design Pattern | Builder | 1 |
| **Tổng** | | **30.5** |

Bạn phải đạt ít nhất **21 điểm** để hoàn thành. Hãy tập trung vào ba mục khó (đồng bộ hóa, ngăn deadlock, PreparedStatement) vì chúng chiếm 9 điểm. Chúc bạn thành công! 😊
