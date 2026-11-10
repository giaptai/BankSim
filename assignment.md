# SOAPTA – Hệ thống chat nội bộ (Java 21 Core)

## I. Mục tiêu và yêu cầu đề bài

### 1. Mục tiêu học tập

- Nắm vững lập trình mạng với TCP Sockets (`ServerSocket`, `Socket`, `BufferedReader`, `PrintWriter`).
- Ứng dụng Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`) trên Java 21 để xử lý I/O với chi phí tài nguyên thấp.
- Rèn luyện tư duy lập trình hướng đối tượng thông qua 3 Design Patterns:
  - Singleton: Quản lý các instance trung tâm.
  - Observer: Quản lý và thông báo tin nhắn đến các client.
  - Strategy: Tách biệt cách xử lý từng loại lệnh.
- Xử lý đồng bộ dữ liệu và đa luồng an toàn bằng `ConcurrentHashMap`, `Semaphore` và `AtomicLong`.

### 2. Yêu cầu kỹ thuật

- Nền tảng: Java 21 Core, chỉ sử dụng JDK chuẩn, không dùng thư viện thứ ba.
- Định danh người dùng:
  - `clientId`: Chuỗi định danh duy nhất do Server sinh tự động, ví dụ `u-1001`, `u-1002`. Toàn bộ logic định tuyến tin nhắn và quản lý nhóm dựa trên `clientId`.
  - `displayName`: Tên người dùng tự nhập khi kết nối. Cho phép trùng nhau và chỉ dùng để hiển thị.
- Mục tiêu tải và cơ chế bảo vệ:
  - Benchmark Target: Hệ thống đặt mục tiêu xử lý 5.000 kết nối đồng thời. Khả năng thực tế phụ thuộc vào cấu hình OS như `ulimit -n`, socket buffer và RAM máy chủ.
  - Điều tiết kết nối: Dùng `Semaphore(5000)` tại Server để kiểm soát số kết nối hoạt động. Từ chối và đóng các kết nối vượt ngưỡng.
- Mô hình luồng:
  - Server: Vòng lặp `accept()` đẩy mỗi kết nối socket vào một Virtual Thread riêng biệt.
  - Client: Sử dụng 2 platform thread:
    - Main thread đọc dữ liệu từ `Scanner`.
    - Daemon receiver thread nhận tin nhắn từ socket.
- Giao diện: Console CLI bằng tiếng Anh, phong cách tối giản.

## II. Cấu trúc thư mục

```
soapta/
├── common/
│   └── Message.java
│
├── server/
│   ├── SoaptaServer.java
│   ├── ChatHub.java
│   ├── ClientHandler.java
│   └── command/
│       ├── CommandStrategy.java
│       ├── CommandDispatcher.java
│       ├── PrivateMsgStrategy.java
│       ├── GroupMsgStrategy.java
│       ├── CreateGroupStrategy.java
│       ├── JoinGroupStrategy.java
│       ├── ListUsersStrategy.java
│       ├── HelpStrategy.java
│       └── ExitStrategy.java
│
└── client/
    └── SoaptaClient.java
```

## III. Thiết kế chi tiết các thành phần

### 1. Phía Server

#### 1.1. `SoaptaServer` – Singleton

Quản lý vòng đời khởi động Server, cấp phát Virtual Thread và giới hạn số lượng kết nối.

Thuộc tính:

- `private static SoaptaServer instance`: Instance duy nhất.
- `private final int port`: Cổng Server, mặc định `8888`.
- `private final Semaphore connectionLimiter`: Giới hạn 5.000 kết nối (`new Semaphore(5000)`).
- `private final ExecutorService executor`: Bộ cấp phát Virtual Thread (`Executors.newVirtualThreadPerTaskExecutor()`).
- `private volatile boolean running`: Trạng thái hoạt động của Server.

Phương thức:

- `public static synchronized SoaptaServer getInstance(int port)`
  - Khởi tạo hoặc trả về Singleton instance.
- `public void start()`
  - Mở `ServerSocket`.
  - Bắt đầu vòng lặp tiếp nhận kết nối.
- `private void acceptLoop(ServerSocket serverSocket)`
  - Gọi `serverSocket.accept()` để tiếp nhận kết nối.
  - Kiểm tra `connectionLimiter.tryAcquire()`.
  - Nếu thành công:
    - Tạo `ClientHandler`.
    - Gửi ClientHandler vào `executor` để xử lý trên Virtual Thread.
  - Nếu thất bại:
    - Từ chối kết nối.
    - Gửi thông báo Server đã đạt giới hạn.
    - Đóng socket.
- `public void releaseConnection()`
  - Giải phóng một permit khi client ngắt kết nối.
- `public static void main(String[] args)`
  - Điểm khởi chạy Server.

#### 1.2. `ChatHub` – Singleton & Observer Subject

Quản lý trạng thái người dùng, nhóm chat và điều phối tin nhắn.

Thuộc tính:

- `private static final ChatHub INSTANCE = new ChatHub()`: Singleton instance theo kiểu Eager Initialization.
- `private final AtomicLong idCounter`: Bộ đếm ID an toàn đa luồng, bắt đầu từ `1000`.
- `private final ConcurrentMap<String, ClientHandler> clients`: Danh sách người dùng đang online.
  - Key: `clientId`.
  - Value: `ClientHandler`.
- `private final ConcurrentMap<String, Set<String>> groups`: Danh sách nhóm.
  - Key: `groupName`.
  - Value: Tập hợp `clientId` của các thành viên.

Phương thức:

- `public static ChatHub getInstance()`
  - Trả về instance duy nhất.
- `public String registerClient(String displayName, ClientHandler handler)`
  - Sinh ID mới dạng `u-` + `idCounter.incrementAndGet()`.
  - Lưu `(clientId, handler)` vào `clients`.
  - Trả về `clientId`.
- `public void unregisterClient(String clientId)`
  - Xóa người dùng khỏi `clients`.
  - Duyệt qua toàn bộ nhóm để xóa `clientId` khỏi danh sách thành viên.
- `public void sendPrivateMessage(String fromId, String toId, String content)`
  - Tìm người nhận trong `clients`.
  - Nếu tồn tại, gửi tin nhắn riêng đến người nhận.
  - Nếu không tồn tại, thông báo lỗi cho người gửi.
- `public void createGroup(String groupName, String creatorId)`
  - Kiểm tra tên nhóm.
  - Tạo nhóm mới bằng `putIfAbsent`.
  - Nếu nhóm đã tồn tại, thông báo lỗi.
  - Nếu tạo thành công, tự động thêm `creatorId` vào danh sách thành viên.
- `public void joinGroup(String groupName, String clientId)`
  - Kiểm tra nhóm có tồn tại hay không.
  - Nếu không tồn tại, thông báo lỗi cho client.
  - Nếu đã là thành viên, thông báo người dùng đã tham gia nhóm.
  - Nếu chưa là thành viên, thêm `clientId` vào danh sách thành viên.
  - Thông báo kết quả tham gia nhóm cho client.
- `public void sendGroupMessage(String fromId, String groupName, String content)`
  - Kiểm tra nhóm có tồn tại hay không.
  - Kiểm tra người gửi có phải thành viên nhóm hay không.
  - Nếu hợp lệ, gửi tin nhắn đến các thành viên đang online trong nhóm.
  - Nếu không hợp lệ, gửi thông báo lỗi cho người gửi.
- `public List<String> getOnlineUsers()`
  - Trả về danh sách người dùng online theo định dạng:
    - `u-1001 (Alex)`
    - `u-1002 (Bob)`

Quy tắc quản lý nhóm:

- Người tạo nhóm tự động trở thành thành viên đầu tiên.
- Người dùng khác phải sử dụng `/join <group>` để tham gia.
- Một người dùng không thể tham gia cùng một nhóm nhiều lần.
- Chỉ thành viên mới được gửi tin nhắn vào nhóm.
- Khi người dùng ngắt kết nối, `clientId` được xóa khỏi danh sách thành viên các nhóm.

#### 1.3. `ClientHandler` – Observer & Runnable

Mỗi instance đại diện cho một client đang kết nối và xử lý I/O trên một Virtual Thread.

Thuộc tính:

- `private final Socket socket`: Socket TCP của client.
- `private String clientId`: ID do ChatHub cấp phát.
- `private String displayName`: Tên hiển thị.
- `private BufferedReader reader`: Luồng đọc dữ liệu từ socket.
- `private PrintWriter writer`: Luồng ghi dữ liệu xuống socket, bật `autoFlush`.
- `private final CommandDispatcher dispatcher`: Bộ phân phối lệnh.

Phương thức:

- `public void run()`
  1. Đọc dòng đầu tiên từ client làm `displayName`.
  2. Gọi `ChatHub.getInstance().registerClient(displayName, this)`.
  3. Nhận `clientId` được cấp phát.
  4. Gửi thông báo chào mừng kèm `clientId` về client.
  5. Tiếp tục đọc dữ liệu bằng `reader.readLine()`.
  6. Chuyển từng dòng nhận được đến `dispatcher.dispatch(this, line)`.
  7. Khi kết nối kết thúc, gọi `close()` trong `finally`.
- `public void notifyMessage(String message)`
  - Gửi thông báo hoặc tin nhắn đến client thông qua `writer`.
- `public void close()`
  - Đóng socket và các stream.
  - Hủy đăng ký client khỏi ChatHub.
  - Giải phóng permit của Semaphore thông qua Server.
- Getter:
  - `getClientId()`
  - `getDisplayName()`

#### 1.4. `Message` – Record Data Model

Mô hình dữ liệu đại diện cho một tin nhắn.

```
public record Message(
    String type,            // "SYSTEM", "PRIVATE", "GROUP"
    String fromClientId,    // ID người gửi
    String fromDisplayName, // Tên người gửi
    String target,          // ID người nhận hoặc tên group
    String content,         // Nội dung tin nhắn
    long timestamp          // System.currentTimeMillis()
) {}
```

### 2. Hệ thống xử lý lệnh – Strategy Pattern

#### 2.1. `CommandStrategy` – Interface

Định nghĩa phương thức chung cho tất cả lệnh:

```
public interface CommandStrategy {
    void execute(ClientHandler sender, String args);
}
```

#### 2.2. Các Strategy cụ thể

`PrivateMsgStrategy`

- Nhận tham số theo định dạng `<targetClientId> <content>`.
- Tách ID người nhận và nội dung tin nhắn.
- Gọi `ChatHub.getInstance().sendPrivateMessage(...)`.

`GroupMsgStrategy`

- Nhận tham số theo định dạng `<groupName> <content>`.
- Tách tên nhóm và nội dung.
- Gọi `ChatHub.getInstance().sendGroupMessage(...)`.

`CreateGroupStrategy`

- Nhận tên nhóm từ `args`.
- Gọi `ChatHub.getInstance().createGroup(...)`.
- Người tạo tự động được thêm vào nhóm.

`JoinGroupStrategy`

- Nhận tên nhóm từ `args`.
- Kiểm tra tham số không được để trống.
- Gọi `ChatHub.getInstance().joinGroup(...)`.
- ChatHub chịu trách nhiệm kiểm tra nhóm và cập nhật thành viên.

`ListUsersStrategy`

- Lấy danh sách người dùng online từ `ChatHub.getInstance().getOnlineUsers()`.
- Gửi danh sách về client bằng `sender.notifyMessage(...)`.

`HelpStrategy`

- Gửi danh sách lệnh được hỗ trợ về client.
- Không cần truy cập hoặc thay đổi trạng thái trong ChatHub.

`ExitStrategy`

- Gửi thông báo ngắt kết nối.
- Gọi `sender.close()`.

#### 2.3. `CommandDispatcher` – Registry & Dispatcher

Thuộc tính:

```
private final Map<String, CommandStrategy> commands = new HashMap<>();
```

Constructor:

Đăng ký các lệnh và Strategy tương ứng:

```
public CommandDispatcher() {
    commands.put("/msg", new PrivateMsgStrategy());
    commands.put("/group", new GroupMsgStrategy());
    commands.put("/create", new CreateGroupStrategy());
    commands.put("/join", new JoinGroupStrategy());
    commands.put("/list", new ListUsersStrategy());
    commands.put("/help", new HelpStrategy());
    commands.put("/exit", new ExitStrategy());
}
```

Phương thức `dispatch(ClientHandler sender, String rawInput)`:

1. Bỏ qua nếu chuỗi rỗng.
2. Tách từ đầu tiên làm tên lệnh.
3. Phần còn lại được xem là `args`.
4. Tìm Strategy tương ứng trong `commands`.
5. Nếu tìm thấy, gọi `execute(sender, args)`.
6. Nếu không tìm thấy, gửi thông báo:
   - `[ERROR] Unknown command. Type /help for assistance.`

### 3. Phía Client

#### `SoaptaClient` – Console Client

Client sử dụng hai platform thread để xử lý nhập và nhận dữ liệu.

Thuộc tính:

- `private final String host`: Địa chỉ Server, mặc định `127.0.0.1`.
- `private final int port`: Cổng Server, mặc định `8888`.
- `private Socket socket`: Socket kết nối đến Server.

Phương thức:

`public static void main(String[] args)`

\- Khởi chạy ứng dụng Client.

`public void start()`

1. Tạo kết nối `new Socket(host, port)`.
2. Khởi tạo `BufferedReader in` và `PrintWriter out`.
3. Nhập `displayName` thông qua `Scanner(System.in)` và gửi lên Server.
4. Khởi chạy Receiver Thread:
   - Là daemon thread.
   - Đọc dữ liệu liên tục từ `in.readLine()`.
   - In tin nhắn nhận được bằng `System.out.println()`.
5. Main thread tiếp tục đọc lệnh từ bàn phím.
6. Gửi lệnh đến Server thông qua `out.println()`.
7. Khi người dùng nhập `/exit`, kết thúc vòng lặp và đóng socket.

## IV. Danh sách lệnh của SOAPTA

| Lệnh    | Cú pháp                  | Chức năng                       |
| ------- | ------------------------ | ------------------------------- |
| Help    | `/help`                  | Hiển thị danh sách lệnh         |
| List    | `/list`                  | Xem danh sách người dùng online |
| Create  | `/create <group>`        | Tạo nhóm chat mới               |
| Join    | `/join <group>`          | Tham gia nhóm chat đã tồn tại   |
| Group   | `/group <group> <text>`  | Gửi tin nhắn vào nhóm           |
| Private | `/msg <clientId> <text>` | Gửi tin nhắn riêng              |
| Exit    | `/exit`                  | Ngắt kết nối                    |

## V. Mô phỏng giao diện Console

### 1. Kết nối và đăng nhập

```
Connecting to SOAPTA Server (127.0.0.1:8888)...
Connected!

Enter display name: Alex
[SYSTEM] Welcome Alex! Your assigned ID is: u-1001
[SYSTEM] Type /help to see available commands.

>
```

### 2. Trợ giúp và danh sách người dùng

```
> /help
Commands:
  /list                     List all active users
  /create <group>           Create a new group
  /join <group>             Join an existing group
  /group <group> <text>     Send a message to a group
  /msg <clientId> <text>    Send a private 1:1 message
  /exit                     Disconnect from server

> /list
Active Users (3):
- u-1001: Alex (You)
- u-1002: Bob
- u-1003: Alex

>
```

Người dùng có thể trùng `displayName`, nhưng mỗi người có `clientId` riêng biệt.

### 3. Chat 1:1

```
> /msg u-1002 Hi Bob, can you review the socket pull request?
[PM to u-1002 (Bob)]: Hi Bob, can you review the socket pull request?

[PM from u-1002 (Bob)]: Sure Alex, checking it now.
>
```

### 4. Tạo nhóm chat

```
> /create dev-team
[SYSTEM] Group 'dev-team' created successfully.
[SYSTEM] You have joined group 'dev-team'.

>
```

Người tạo nhóm tự động trở thành thành viên.

### 5. Tham gia nhóm chat

```
> /join dev-team
[SYSTEM] You have joined group 'dev-team'.
```

Nếu nhóm không tồn tại:

```
> /join unknown-group
[ERROR] Group 'unknown-group' does not exist.
```

Nếu người dùng đã tham gia nhóm:

```
> /join dev-team
[SYSTEM] You are already a member of group 'dev-team'.
```

### 6. Gửi tin nhắn nhóm

```
> /group dev-team Hello everyone, virtual threads are working great!
[Group #dev-team][u-1001 (Alex)]: Hello everyone, virtual threads are working great!
[Group #dev-team][u-1002 (Bob)]: Welcome!
>
```

Nếu người dùng chưa tham gia nhóm:

```
> /group dev-team Hello everyone!
[ERROR] You are not a member of group 'dev-team'. Use /join first.
```

### 7. Xử lý khi Server đạt giới hạn kết nối

```
Connecting to SOAPTA Server (127.0.0.1:8888)...
[ERROR] Connection rejected: Server capacity reached (5000/5000 connections).
Connection closed.
```

### 8. Client ngắt kết nối
```
> /exit
[SYSTEM] Disconnected from server. Goodbye!
Connection closed.
```

## VI. Các nguyên tắc xử lý đa luồng

- Server sử dụng Virtual Threads để xử lý từng kết nối độc lập.
- `Semaphore` giới hạn số lượng kết nối đang hoạt động.
- `AtomicLong` cấp phát ID duy nhất trong môi trường đa luồng.
- `ConcurrentHashMap` quản lý danh sách client và group.
- Các tập thành viên bên trong `groups` sử dụng `ConcurrentHashMap.newKeySet()` để hỗ trợ thêm/xóa thành viên an toàn đa luồng.
- Khi ghi dữ liệu xuống cùng một client socket từ nhiều Virtual Thread, cần đồng bộ thao tác ghi để tránh các thông điệp bị xen kẽ.
- Việc đóng kết nối phải bảo đảm chỉ giải phóng Semaphore permit một lần cho mỗi kết nối đã được cấp phép.

## VII. Lộ trình triển khai

Bước 1 – Data & Contract

- Tạo `record Message`.
- Tạo interface `CommandStrategy`.

Bước 2 – State Repository

- Xây dựng `ChatHub` Singleton.
- Quản lý client bằng `ConcurrentHashMap`.
- Quản lý group và danh sách thành viên.
- Tích hợp `AtomicLong` để cấp phát `clientId`.
- Triển khai:
  - `registerClient()`
  - `unregisterClient()`
  - `createGroup()`
  - `joinGroup()`
  - `sendPrivateMessage()`
  - `sendGroupMessage()`
  - `getOnlineUsers()`

Bước 3 – Command Layer

- Viết các lớp triển khai `CommandStrategy`.
- Tạo `CommandDispatcher`.
- Đăng ký các lệnh:
  - `/msg`
  - `/group`
  - `/create`
  - `/join`
  - `/list`
  - `/help`
  - `/exit`

Bước 4 – Client Worker

- Xây dựng `ClientHandler`.
- Khởi tạo stream đọc/ghi.
- Tiếp nhận và xử lý lệnh từ client.
- Đăng ký và hủy đăng ký client trong ChatHub.

Bước 5 – Server Bootstrap

- Xây dựng `SoaptaServer` Singleton.
- Khởi tạo `ServerSocket`.
- Tích hợp `Semaphore(5000)`.
- Sử dụng `Executors.newVirtualThreadPerTaskExecutor()`.

Bước 6 – Client CLI

- Xây dựng `SoaptaClient`.
- Tạo Main thread để nhập lệnh.
- Tạo Daemon receiver thread để nhận tin nhắn.
- Hoàn thiện giao diện Console.

Lưu ý về phạm vi: Bản thiết kế này tập trung vào chat 1:1, chat nhóm, tạo nhóm, tham gia nhóm và quản lý kết nối. Chức năng truyền file tối đa 150 MB và quản trị/giám sát nhóm chưa nằm trong phạm vi triển khai của phiên bản này.

Bản này đã tích hợp `/join`, `JoinGroupStrategy` và `HelpStrategy` vào cấu trúc, danh sách lệnh, mô tả class, mockup và roadmap; không còn tình trạng `/help` xuất hiện trên giao diện nhưng thiếu phần xử lý.