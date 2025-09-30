### **📝 Đề Bài Dự Án BankSim: Hệ Thống Điểm Số & Phân Loại Độ Khó**

Bạn sẽ xây dựng một ứng dụng mô phỏng hệ thống ngân hàng, áp dụng Lập trình Hướng đối tượng (OOP), xử lý đa luồng (Multithreading) và kết nối Cơ sở dữ liệu (JDBC), bao gồm cả giao diện console và GUI.

---

### **1\. Yêu Cầu Về Kiến Trúc và OOP**

Đây là các lớp cốt lõi của hệ thống, tuân thủ các nguyên tắc OOP.

*   **Lớp Account** 💳
    *   **Mô tả**: Đại diện cho một tài khoản ngân hàng.
    *   **Thuộc tính**:
        *   `private int accountId`: ID duy nhất, được tạo tự động bởi CSDL.
        *   `private String ownerName`: Tên chủ sở hữu.
        *   `private double balance`: Số dư tài khoản.
    *   **Phương thức**:
        *   `public Account(int accountId, String ownerName, double initialBalance)`: Constructor đầy đủ.
        *   `public Account(String ownerName, double initialBalance)`: Constructor khi tạo tài khoản mới.
        *   `public int getAccountId()`: Trả về accountId.
        *   `public String getOwnerName()`: Trả về ownerName.
        *   `public double getBalance()`: Trả về balance.
        *   `public String toString()`: Trả về chuỗi biểu diễn thông tin tài khoản.
    *   **Điểm**: **1 điểm** (dễ).
*   **Lớp Transaction** 💸
    *   **Mô tả**: Đại diện cho một giao dịch đã được thực hiện.
    *   **Thuộc tính**:
        *   `private String transactionId`: ID duy nhất của giao dịch (UUID).
        *   `private int accountId`: ID tài khoản liên quan.
        *   `private Type type`: Loại giao dịch (`DEPOSIT`, `WITHDRAW`, `TRANSFER`).
        *   `private double amount`: Số tiền giao dịch.
        *   `private LocalDateTime timestamp`: Thời gian giao dịch.
    *   **Phương thức**:
        *   `public Transaction(int accountId, Type type, double amount)`: Constructor cho giao dịch mới.
        *   `public Transaction(String transactionId, int accountId, Type type, double amount, LocalDateTime timestamp)`: Constructor đầy đủ.
        *   Các phương thức getter cho tất cả các thuộc tính.
        *   `public String toString()`: Trả về chuỗi biểu diễn thông tin giao dịch.
    *   **Điểm**: **1 điểm** (dễ).
*   **Lớp BankService** 🏦
    *   **Mô tả**: Lớp quản lý chính của hệ thống, chứa logic nghiệp vụ.
    *   **Thuộc tính**:
        *   `private ExecutorService transactionExecutor`: Một `ExecutorService` để quản lý các luồng xử lý giao dịch.
        *   `private DatabaseManager databaseManager`: Instance của lớp `DatabaseManager`.
        *   `private final Map<Integer, Lock> accountLocks`: Một `ConcurrentHashMap` để quản lý các `ReentrantLock` cho từng `accountId` một cách duy nhất, đảm bảo đồng bộ hóa giữa các luồng.
    *   **Phương thức**:
        *   `public BankService(DatabaseManager databaseManager)`: Constructor.
        *   `public Account openAccount(String ownerName, double initialBalance)`: Tạo tài khoản mới, lưu vào CSDL.
        *   `public Future<?> deposit(Integer accountId, double amount)`: Xử lý giao dịch gửi tiền.
        *   `public Future<?> withdraw(Integer accountId, double amount)`: Xử lý giao dịch rút tiền.
        *   `public void transfer(int fromAccountId, int toAccountId, double amount)`: Xử lý giao dịch chuyển khoản.
        *   `public String getAccountDetails(int accountId)`: Trả về thông tin chi tiết tài khoản.
        *   `public List<Transaction> getTransactionHistory(int accountId)`: Lấy lịch sử giao dịch.
        *   `public void close()`: Tắt `ExecutorService` khi ứng dụng kết thúc.
    *   **Điểm**: **1.5 điểm** (trung bình).

---

### **2\. Yêu Cầu Về Đa Luồng (Multithreading)**

Đây là các yêu cầu cốt lõi để đảm bảo hệ thống hoạt động ổn định trong môi trường đa luồng.

*   **Sử dụng ExecutorService**: Mọi giao dịch (gửi, rút, chuyển khoản) phải được gửi đến một **thread pool** (`ExecutorService`) để xử lý bất đồng bộ.
    *   **Điểm**: **1.5 điểm** (trung bình).
*   **Đồng bộ hóa với Per-Account Locking**: Sử dụng `ReentrantLock` instances được quản lý bởi một `ConcurrentHashMap` trong lớp `BankService` để tránh **race condition** khi nhiều luồng cùng truy cập và thay đổi số dư của cùng một tài khoản.
    *   **Điểm**: **3 điểm** (khó).
*   **Ngăn chặn Deadlock**: Khi chuyển khoản, phải khóa cả hai tài khoản (tài khoản nguồn và tài khoản đích) một cách tuần tự (ví dụ: theo thứ tự ID tài khoản tăng dần) để tránh **deadlock**.
    *   **Điểm**: **3 điểm** (khó).

---

### **3\. Yêu Cầu Về Cơ Sở Dữ Liệu (JDBC)**

*   **Lớp DatabaseManager** 🗄️
    *   **Mô tả**: Lớp này chịu trách nhiệm về mọi tương tác với cơ sở dữ liệu PostgreSQL. Nó đóng vai trò là cầu nối giữa ứng dụng và CSDL.
    *   **Thuộc tính**:
        *   `private Properties props`: Để tải cấu hình kết nối CSDL từ `application.properties`.
    *   **Phương thức**:
        *   `public DatabaseManager()`: Constructor để tải cấu hình và khởi tạo schema CSDL (nếu chưa có) từ `ddl.sql`.
        *   `private Connection getConnection()`: Phương thức nội bộ để lấy một kết nối CSDL mới.
        *   `public Account saveAccount(Account account)`: Lưu một tài khoản mới vào bảng `account`, trả về đối tượng `Account` đã có `accountId` được tạo bởi CSDL.
        *   `public void updateAccount(Account account)`: Cập nhật thông tin (tên chủ sở hữu, số dư) của tài khoản trong CSDL.
        *   `public void adjustAccountBalance(int accountId, double amountChange)`: Cập nhật số dư tài khoản bằng cách cộng/trừ một lượng nhất định (`amountChange`), sử dụng cập nhật nguyên tử (`balance = balance + ?`) trực tiếp trong CSDL.
        *   `public void saveTransaction(Transaction transaction)`: Lưu một giao dịch đơn lẻ vào bảng `transactions`.
        *   `public void saveTransaction(Account fromAccount, Account toAccount, double amount)`: Lưu giao dịch chuyển khoản, bao gồm cập nhật số dư của cả hai tài khoản và chèn hai bản ghi giao dịch vào bảng `transactions` trong cùng một **database transaction** sử dụng **batch processing** để đảm bảo tính nguyên tử.
        *   `public Account getAccountById(int accountId)`: Lấy thông tin tài khoản từ CSDL dựa trên `accountId`. Trả về một đối tượng `Account` hoặc `null` nếu không tìm thấy.
        *   `public List<Transaction> getTransactionsByAccountId(int accountId)`: Lấy lịch sử giao dịch từ bảng `transactions`. Trả về một `List<Transaction>`.
    *   **Điểm**: **1.5 điểm** (trung bình).
*   **Sử dụng PreparedStatement**: Sử dụng `PreparedStatement` thay vì `Statement` trong mọi câu lệnh SQL để phòng chống **SQL Injection**.
    *   **Điểm**: **3 điểm** (khó).

---

### **4\. Yêu Cầu Về Giao Diện & Xử Lý Lỗi**

*   **Giao diện Console**: Xây dựng menu tương tác cho người dùng để thực hiện các thao tác ngân hàng.
    *   **Điểm**: **1 điểm** (dễ).
*   **Giao diện Đồ họa (GUI)**: Cung cấp một giao diện người dùng đồ họa (GUI) sử dụng Swing cho các thao tác ngân hàng cơ bản, bao gồm các panel riêng biệt cho từng chức năng và một numpad ảo.
    *   **Điểm**: **2 điểm** (trung bình).
*   **Xử lý Lỗi**: Sử dụng `try-catch` để xử lý các ngoại lệ (ví dụ: số dư không đủ, tài khoản không tồn tại, số tiền không hợp lệ) và hiển thị thông báo lỗi rõ ràng cho người dùng.
    *   **Điểm**: **1.5 điểm** (trung bình).
*   **Kiểm tra và Ghi log tên tài khoản trùng lặp/không hợp lệ**:
    *   Khi mở tài khoản mới, tên chủ sở hữu (`ownerName`) phải được kiểm tra tính hợp lệ bằng một **biểu thức chính quy (regex)** (ví dụ: chỉ chứa chữ cái, khoảng trắng, không quá dài).
    *   Nếu tên tài khoản đã tồn tại trong CSDL, ghi lại sự kiện này vào **log file (hoặc console)** cùng với ngày tháng cụ thể.
    *   **Điểm**: **2 điểm** (trung bình).


```
dự án này hiện tại có thể áp dụng design patterns nào - ngoài factory method mới triển khai xong, chỉ nêu tên thôi (chưa có đưa code) và nêu lý do tại sao dùng (lợi ích)

Dưới đây là các design patterns có thể áp dụng cho dự án của bạn (ngoài Factory Method):

Singleton

Lý do: Đảm bảo chỉ có một instance cho các class như DatabaseManager, Logger, hoặc cấu hình hệ thống.
Lợi ích: Tiết kiệm tài nguyên, quản lý trạng thái dùng chung dễ dàng.
Strategy

Lý do: Cho phép thay đổi thuật toán xử lý giao dịch, kiểm tra bảo mật, hoặc tính phí giao dịch mà không sửa code lớp sử dụng.
Lợi ích: Dễ mở rộng, thay đổi logic runtime linh hoạt.
Observer

Lý do: Theo dõi thay đổi trạng thái tài khoản, giao dịch để cập nhật GUI hoặc log.
Lợi ích: Tách biệt logic cập nhật giao diện/log khỏi logic nghiệp vụ, dễ bảo trì.
Command

Lý do: Đóng gói các thao tác như deposit, withdraw, transfer thành các đối tượng lệnh.
Lợi ích: Dễ undo/redo, xếp hàng (queue) giao dịch, log lịch sử thao tác.
Template Method

Lý do: Định nghĩa khung xử lý giao dịch, cho phép các bước cụ thể (kiểm tra, ghi log, commit) được override ở subclass.
Lợi ích: Tái sử dụng code, chuẩn hóa quy trình nghiệp vụ.
Decorator

Lý do: Thêm chức năng cho Account hoặc Transaction (ví dụ: logging, kiểm tra bảo mật) mà không sửa code gốc.
Lợi ích: Mở rộng chức năng linh hoạt, không ảnh hưởng class chính.
Adapter

Lý do: Kết nối với các hệ thống bên ngoài hoặc API khác mà interface không tương thích (ví dụ: chuyển đổi giữa các loại database hoặc hệ thống thanh toán).
Lợi ích: Tăng khả năng tích hợp, giảm phụ thuộc vào code bên ngoài.
Builder

Lý do: Tạo các đối tượng phức tạp như Account, Transaction với nhiều thuộc tính tuỳ chọn.
Lợi ích: Code rõ ràng, dễ đọc, tránh constructor dài dòng.
```


### **Tổng Kết và Điểm Vượt Qua**

*   **Tổng điểm tối đa**: 22 điểm.
*   **Điểm vượt qua**: **15 điểm**.

Bạn phải đạt được ít nhất 15 điểm để được coi là đã hoàn thành bài tập này một cách xuất sắc. Hãy tập trung vào các yêu cầu "khó" (3 điểm) để đảm bảo bạn đạt được mục tiêu. Chúc bạn thành công! 😊