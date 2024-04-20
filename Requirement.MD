# Topic
Xây dựng ứng dụng chat

# Features
Ứng dụng dụ tính có các chức năng sau:
- Khi 1  user đăng nhập thành công, cung cấp danh sách các bạn bè khác đang online
- User gửi và nhận thông điệp
- Khi có 1 trong 2 user ngắt kết nối hoặc yêu cầu dừng cuộc trò chuyện, thông báo cho bên còn lại biết.
- Tạo nhóm chat:
	- User tạo nhóm chat
	- User gửi yêu cầu mời tham gia nhóm tới các user khác
	- Tham gia và rời nhóm chat
	- Gửi thông điệp tới các nhóm


Thực tế các tính năng khi bắt đầu triển khai thành lập các sơ đồ:
  - Có 2 loại account: user và admin
    + Với một user:
	- Chỉnh sửa hồ sơ
	- Đổi mật khẩu
	- Nhắn tin cho người khác
	- Kết bạn
	- Gia nhập/ Rời nhóm
	- Hiển thị trạng thái bạn bè
	- Cập nhật trạng thái (để bạn bè biết)
	- Tìm kiếm lịch sử nhắn tin
    + Với một admin: 
	- Có toàn bộ tính năng của user
	- Tùy biến group (?) 
	- Có quyền chỉnh sửa user thuộc quyền kiểm soát
	- Thay đổi lịch sử đoạn chat (cụ thể là xóa)
- Về cơ bản thì một nhóm trong này tương tự với nhóm trong messenger, chỉ gồm các tính năng như sau: 
    - Thêm/ bớt người 
    - Tin nhắn mọi người đều có quyền đọc và tìm kiếm lại
    - Xóa tin nhắn ở phía bản thân (tính năng đang được cân nhắc)
