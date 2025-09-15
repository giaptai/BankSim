### **📝 Đề Bài Dự Án BankSim: Hệ Thống Điểm Số & Phân Loại Độ Khó**

Bạn sẽ xây dựng một ứng dụng console mô phỏng hệ thống ngân hàng, áp dụng Lập trình Hướng đối tượng (OOP), xử lý đa luồng (Multithreading) và kết nối Cơ sở dữ liệu (JDBC).

---

### **1\. Yêu Cầu Về Kiến Trúc và OOP**

Đây là các lớp cốt lõi của hệ thống, tuân thủ các nguyên tắc OOP.

* **Lớp Account** 💳  
  * **Mô tả**: Đại diện cho một tài khoản ngân hàng.  
  * **Thuộc tính**:  
    * private final String accountId: ID duy nhất, không thể thay đổi.  
    * private final String ownerName: Tên chủ sở hữu, không thể thay đổi.  
    * private double balance: Số dư tài khoản.  
    * private final Lock accountLock: Một ReentrantLock để đồng bộ hóa.  
  * **Phương thức**:  
    * public Account(String accountId, String ownerName, double initialBalance): Constructor.  
    * public String getAccountId(): Trả về accountId.  
    * public String getOwnerName(): Trả về ownerName.  
    * public double getBalance(): Trả về balance.  
    * public Lock getAccountLock(): Trả về accountLock.  
    * public void deposit(double amount): Tăng số dư.  
    * public void withdraw(double amount): Giảm số dư.  
  * **Điểm**: **1 điểm** (dễ).  
* **Lớp Transaction** 💸  
  * **Mô tả**: Đại diện cho một giao dịch đã được thực hiện.  
  * **Thuộc tính**:  
    * private final long transactionId: ID duy nhất của giao dịch.  
    * private final String accountId: ID tài khoản liên quan.  
    * private final TransactionType type: Loại giao dịch (DEPOSIT, WITHDRAW, TRANSFER).  
    * private final double amount: Số tiền giao dịch.  
    * private final Timestamp timestamp: Thời gian giao dịch.  
  * **Phương thức**:  
    * public Transaction(String accountId, TransactionType type, double amount): Constructor.  
    * public long getTransactionId(): Trả về transactionId.  
    * Các phương thức getter khác cho các thuộc tính còn lại.  
  * **Điểm**: **1 điểm** (dễ).  
* **Lớp Bank** 🏦  
  * **Mô tả**: Lớp quản lý chính của hệ thống.  
  * **Thuộc tính**:  
    * private Map\<String, Account\> accounts: Lưu trữ các đối tượng Account.  
    * private ExecutorService transactionExecutor: Một ExecutorService để quản lý các luồng.  
    * private DatabaseManager dbManager: Instance của lớp DatabaseManager.  
  * **Phương thức**:  
    * public Bank(): Constructor để khởi tạo các thuộc tính.  
    * public void openAccount(String ownerName, double initialBalance): Tạo tài khoản mới, lưu vào accounts map và CSDL.  
    * public void deposit(String accountId, double amount): Xử lý giao dịch gửi tiền.  
    * public void withdraw(String accountId, double amount): Xử lý giao dịch rút tiền.  
    * public void transfer(String fromAccountId, String toAccountId, double amount): Xử lý giao dịch chuyển khoản.  
    * public String getAccountDetails(String accountId): Trả về thông tin tài khoản.  
    * public List\<Transaction\> getTransactionHistory(String accountId): Lấy lịch sử giao dịch.  
  * **Điểm**: **1.5 điểm** (trung bình).

---

### **2\. Yêu Cầu Về Đa Luồng (Multithreading)**

Đây là các yêu cầu cốt lõi để đảm bảo hệ thống hoạt động ổn định trong môi trường đa luồng.

* **Sử dụng ExecutorService**: Mọi giao dịch phải được gửi đến một **thread pool** để xử lý.  
  * **Điểm**: **1.5 điểm** (trung bình).  
* **Đồng bộ hóa hoặc Lock**: Sử dụng cơ chế đồng bộ hóa hoặc **lock** để tránh **race condition** khi nhiều luồng cùng truy cập và thay đổi số dư.  
  * **Điểm**: **3 điểm** (khó).  
* **Ngăn chặn Deadlock**: Khi chuyển khoản, phải khóa cả hai tài khoản (fromAccount và toAccount) một cách tuần tự để tránh **deadlock**.  
  * **Điểm**: **3 điểm** (khó).

---

### **3\. Yêu Cầu Về Cơ Sở Dữ Liệu (JDBC)**

* **Lớp DatabaseManager** 🗄️  
  * **Mô tả**: Lớp này chịu trách nhiệm về mọi tương tác với cơ sở dữ liệu. Nó đóng vai trò là cầu nối giữa ứng dụng và CSDL.  
  * **Thuộc tính**:  
    * private Connection connection: Kết nối tới CSDL.  
  * **Phương thức**:  
    * public DatabaseManager(): Constructor để thiết lập kết nối JDBC (ví dụ: DriverManager.getConnection(...)).  
    * public void saveAccount(Account account):  
      * Lưu một tài khoản mới vào bảng accounts.  
      * Sử dụng PreparedStatement với câu lệnh INSERT.  
    * public void updateAccount(Account account):  
      * Cập nhật số dư của tài khoản trong CSDL.  
      * Sử dụng PreparedStatement với câu lệnh UPDATE.  
    * public void saveTransaction(List<Transaction> transactions):  
      * Lưu một hoặc nhiều giao dịch vào bảng transactions trong cùng một transaction của DB.  
      * Sử dụng PreparedStatement với câu lệnh INSERT để batch insert.  
    * public Account getAccountById(String accountId):  
      * Lấy thông tin tài khoản từ CSDL dựa trên accountId.  
      * Trả về một đối tượng Account hoặc null nếu không tìm thấy.  
    * public List\<Transaction\> getTransactionsByAccountId(String accountId):  
      * Lấy lịch sử giao dịch từ bảng transactions.  
      * Trả về một List\<Transaction\>.  
    * public void closeConnection(): Đóng kết nối CSDL khi ứng dụng kết thúc.  
  * **Điểm**: **1.5 điểm** (trung bình).  
* **Sử dụng PreparedStatement**: Sử dụng PreparedStatement thay vì Statement trong mọi câu lệnh SQL để phòng chống **SQL Injection**.  
  * **Điểm**: **3 điểm** (khó).

---

### **4\. Yêu Cầu Về Giao Diện & Xử Lý Lỗi**

* **Giao diện Console**: Xây dựng menu tương tác cho người dùng.  
  * **Điểm**: **1 điểm** (dễ).  
* **Xử lý Lỗi**: Sử dụng try-catch để xử lý các ngoại lệ (ví dụ: số dư không đủ, tài khoản không tồn tại) và hiển thị thông báo lỗi rõ ràng.  
  * **Điểm**: **1.5 điểm** (trung bình).

### **Tổng Kết và Điểm Vượt Qua**

* **Tổng điểm tối đa**: 17.5 điểm.  
* **Điểm vượt qua**: **12 điểm**.

Bạn phải đạt được ít nhất 12 điểm để được coi là đã hoàn thành bài tập này một cách xuất sắc. Hãy tập trung vào các yêu cầu "khó" (3 điểm) để đảm bảo bạn đạt được mục tiêu. Chúc bạn thành công\! 😊