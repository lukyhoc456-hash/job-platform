# Hướng Dẫn Cài Đặt & Chạy Dự Án (Job Platform)

Dự án bao gồm 2 phần chính: **Backend (Java Spring Boot)** và **Frontend (React/Vite)**. Dưới đây là các bước để cài đặt và chạy hoàn chỉnh dự án trên máy của bạn.

---

## 1. Yêu cầu hệ thống (Prerequisites)
- **Java Development Kit (JDK)** phiên bản 17 (hoặc tương thích).
- **Maven** (để cấu hình và chạy backend).
- **Node.js** & **npm** (phiên bản 16 trở lên để chạy Frontend Vite).
- **MySQL Server** (chạy ở cổng mặc định 3306).

---

## 2. Cài đặt Cơ Sở Dữ Liệu (Database)

1. Mở hệ quản trị MySQL (như MySQL Workbench, XAMPP, DBeaver, v.v...).
2. Đăng nhập bằng tài khoản (mặc định trong code là):
   - **Tài khoản:** `root`
   - **Mật khẩu:** `01228923505`
   *(Nếu bạn dùng mật khẩu khác, hãy sửa lại file `application.properties` ở phần Backend).*
3. Tạo một database mới có tên là `job_platform`:
   ```sql
   CREATE DATABASE job_platform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
4. Hệ thống Backend (nhờ cấu hình `hibernate.ddl-auto=update`) sẽ tự động tạo các bảng khi chạy lần đầu.
5. *(Tùy chọn)* Nếu trong thư mục `database/` chứa file `job_platform.sql` có dữ liệu mẫu, bạn có thể thực hiện import file này vào MySQL để có sẵn dữ liệu test.

---

## 3. Cài đặt & Chạy Backend (Spring Boot)

Thư mục chứa Backend: `job_platform_be/`

### 3.1. Cấu hình biến môi trường (`.env`)
Trong thư mục `job_platform_be`, đảm bảo bạn đã tạo file `.env` với các nội dung cấu hình để gửi mail, gọi API AI (Groq) và cổng thanh toán VNPay:

```env
GROQ_API_KEYS=key1:gsk_xxx,key2:gsk_yyy...
MAIL_USERNAME=lukyhoc456@gmail.com
MAIL_PASSWORD=sjacnwmhwlnkgxds
vnp_TmnCode=OMICEIEE
vnp_HashSecret=XNGFOCXNLENKJZHFPURNRESYVVPZUHSG
vnp_Url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
```
*(Lưu ý: Viết đúng hoa/thường cho các key VNPay như trên để không bị lỗi BeanCreationException)*

### 3.2. Chạy Backend
Mở Terminal / Command Prompt, di chuyển vào thư mục Backend và chạy lệnh Maven:

```bash
cd LuanVan-2026\job_platform_be
mvn spring-boot:run
```

**Kết quả mong đợi:** 
Backend sẽ bắt đầu tải dependencies và khởi động. Khi thấy dòng `Started JobPlatformApplication in ... seconds` là thành công. Backend sẽ chạy ở địa chỉ: **http://localhost:8090**

---

## 4. Cài đặt & Chạy Frontend (React Vite)

Thư mục chứa Frontend: `job_platform_fe/`

### 4.1. Cài đặt thư viện
Mở một cửa sổ Terminal / Command Prompt *khác*, di chuyển vào thư mục Frontend:

```bash
cd LuanVan-2026\job_platform_fe
npm install
```

### 4.2. Khởi động Frontend
Sau khi cài đặt xong `node_modules`, bạn chạy lệnh:

```bash
npm run dev
```

**Kết quả mong đợi:**
Vite sẽ khởi động rất nhanh và hiện ra một địa chỉ local (thường là **http://localhost:5173**). Bạn click vào link đó bằng trình duyệt để sử dụng ứng dụng.

---

## 5. Một số lưu ý thêm
- **Upload File:** Khi sử dụng tính năng Upload (Avatar/CV/Logo), hệ thống sẽ lưu vào thư mục `job_platform_be/uploads/`.
- **Lỗi kẹt Port (8090):** Nếu khi chạy backend báo lỗi `Port 8090 was already in use`, hãy chạy PowerShell với quyền Administrator và gõ các lệnh sau để ngắt tiến trình đang kẹt:
  ```powershell
  netstat -ano | findstr :8090
  # Ghi nhớ số ID ở cột cuối cùng (ví dụ: 7520)
  taskkill /F /PID 7520
  ```
- **Lưu ý Git:** Khi sửa code trên 2 máy khác nhau, nhớ lưu dữ liệu và chạy `git pull` để nhận code mới nhất trước khi chạy backend.
