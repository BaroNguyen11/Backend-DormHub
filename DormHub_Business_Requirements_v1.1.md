# DormHub

## Đặc tả nghiệp vụ hệ thống quản lý đăng ký và lưu trú ký túc xá sinh viên

**Tên dự án:** DormHub --- Hệ thống quản lý đăng ký và lưu trú ký túc xá
sinh viên\
**Loại tài liệu:** Business Requirements / Software Requirements\
**Phiên bản:** 1.1 --- Điều chỉnh theo phạm vi dự án đã chốt\
**Ngày cập nhật:** 29/10/2026\
**Mục đích:** Làm tài liệu gốc cho thiết kế nghiệp vụ, database, Spring
Boot API, React frontend và kiểm thử.

------------------------------------------------------------------------

# 1. Mục tiêu và phạm vi

## 1.1. Mô tả hệ thống

DormHub hỗ trợ quản lý quy trình **đăng ký và lưu trú của sinh viên tại
ký túc xá**, từ lúc sinh viên có nhu cầu đăng ký chỗ ở cho đến khi được
phân phòng, ký hợp đồng, nhận phòng, thực hiện nghĩa vụ thanh toán, gửi
yêu cầu bảo trì và kết thúc hoặc gia hạn thời gian lưu trú.

Hệ thống tập trung vào mối quan hệ giữa sinh viên, yêu cầu đăng ký lưu
trú, phòng/chỗ ở, hợp đồng và các nghĩa vụ phát sinh trong thời gian lưu
trú. DormHub không được định nghĩa là một nền tảng quản trị toàn bộ hoạt
động của ký túc xá.

## 1.2. Vấn đề cần giải quyết

-   Đăng ký lưu trú được tiếp nhận qua giấy tờ, bảng tính hoặc nhiều
    kênh khác nhau, gây khó theo dõi.
-   Manager khó kiểm soát hồ sơ đang chờ duyệt, hồ sơ được chấp thuận và
    hồ sơ bị từ chối.
-   Việc phân phòng có thể thiếu nhất quán nếu không kiểm tra sức chứa
    và tình trạng phòng.
-   Thông tin hợp đồng, tiền đặt cọc, thời hạn lưu trú và gia hạn dễ bị
    phân tán.
-   Sinh viên khó theo dõi hóa đơn, hạn thanh toán và lịch sử thanh
    toán.
-   Yêu cầu bảo trì trong thời gian lưu trú có thể bị bỏ sót nếu không
    có trạng thái xử lý.
-   Sinh viên cần được thông báo khi đăng ký, hợp đồng, thanh toán hoặc
    yêu cầu bảo trì thay đổi trạng thái.
-   Khi sinh viên chuyển phòng, gia hạn hoặc trả phòng, lịch sử và trạng
    thái lưu trú cần được cập nhật nhất quán.

## 1.3. Mục tiêu nghiệp vụ

1.  Quản lý hồ sơ đăng ký lưu trú của sinh viên.
2.  Hỗ trợ Manager xét duyệt, từ chối và theo dõi kết quả đăng ký.
3.  Quản lý tình trạng phòng và phân phòng cho sinh viên trong phạm vi
    lưu trú.
4.  Quản lý hợp đồng, tiền đặt cọc, thời hạn lưu trú và gia hạn.
5.  Ghi nhận việc nhận phòng, chuyển phòng và trả phòng.
6.  Lập và theo dõi hóa đơn, khoản phí lưu trú và thanh toán.
7.  Cho phép sinh viên gửi và theo dõi yêu cầu bảo trì trong thời gian
    lưu trú.
8.  Gửi thông báo theo các sự kiện quan trọng.
9.  Bảo vệ dữ liệu cá nhân và đảm bảo người dùng chỉ thao tác trên dữ
    liệu thuộc phạm vi quyền của mình.

## 1.4. Phạm vi bao gồm

-   Authentication và phân quyền.
-   Hồ sơ sinh viên phục vụ nghiệp vụ lưu trú.
-   Tra cứu thông tin phòng/chỗ còn khả năng tiếp nhận.
-   Đăng ký lưu trú.
-   Xét duyệt đăng ký.
-   Phân phòng.
-   Hợp đồng lưu trú và tiền đặt cọc.
-   Nhận phòng/check-in.
-   Hóa đơn và thanh toán trong thời gian lưu trú.
-   Yêu cầu bảo trì.
-   Thông báo.
-   Gia hạn hợp đồng/lưu trú.
-   Chuyển phòng.
-   Trả phòng/check-out và kết thúc lưu trú.
-   Tra cứu trạng thái hồ sơ, hợp đồng, hóa đơn và yêu cầu của sinh
    viên.

## 1.5. Ngoài phạm vi

Không tự mở rộng dự án thành hệ thống quản trị toàn bộ ký túc xá. Các
nội dung sau không mặc nhiên nằm trong scope:

-   Quản trị tài khoản và phân quyền toàn hệ thống bằng vai trò Admin.
-   Quản lý nhân sự, chấm công hoặc tiền lương.
-   Quản lý kho, tài sản, mua sắm hoặc nhà cung cấp.
-   Quản lý an ninh, camera, kiểm soát cổng hoặc điểm danh.
-   Quản lý toàn bộ cơ sở vật chất ngoài các thông tin phòng cần cho
    đăng ký và lưu trú.
-   Module công tơ điện/nước, nhập chỉ số định kỳ và tự động tính mức
    tiêu thụ.
-   Chat realtime, ứng dụng mobile riêng, QR check-in, email/push nâng
    cao nếu chưa được chốt.
-   Các báo cáo quản trị cấp tổ chức không phục vụ trực tiếp quy trình
    đăng ký và lưu trú.

Dashboard đơn giản cho Manager có thể hỗ trợ theo dõi hồ sơ đăng ký,
tình trạng phân phòng, hợp đồng sắp hết hạn, hóa đơn và yêu cầu bảo trì;
không biến dashboard thành module BI/quản trị tổng thể.

------------------------------------------------------------------------

# 2. Vai trò người dùng

DormHub sử dụng ba vai trò nghiệp vụ chính: **Guest, Student và
Manager**.

## 2.1. Guest

Guest là người chưa có phiên đăng nhập hoặc chưa trở thành sinh viên
đang lưu trú trong hệ thống.

Guest có thể: - Xem thông tin giới thiệu và hướng dẫn đăng ký lưu trú
được công khai. - Xem thông tin phòng còn khả năng tiếp nhận nếu chính
sách cho phép công khai. - Đi tới trang đăng nhập. - Sử dụng luồng khôi
phục mật khẩu/kích hoạt tài khoản nếu được cấp tài khoản.

Guest không thể: - Xem hồ sơ sinh viên. - Xem hồ sơ đăng ký riêng tư,
hợp đồng, hóa đơn hoặc thanh toán. - Xem yêu cầu bảo trì của người
khác. - Thực hiện thao tác dành cho Student hoặc Manager.

**Lưu ý quan trọng:** Guest không đồng nghĩa với chức năng tự đăng ký
tài khoản sinh viên. Tài khoản Student được nhà trường cấp/provision
theo quyết định đã thống nhất; đăng ký lưu trú là một nghiệp vụ khác với
đăng ký tài khoản.

## 2.2. Student

Student là sinh viên có tài khoản được cấp và hồ sơ phù hợp trong hệ
thống.

Student có thể: - Đăng nhập, đăng xuất và sử dụng luồng khôi phục tài
khoản được hỗ trợ. - Xem hồ sơ của mình. - Xem thông tin phòng được phép
tra cứu. - Tạo và theo dõi đăng ký lưu trú. - Xem kết quả xét duyệt và
thông tin phân phòng. - Xem hợp đồng, tiền đặt cọc, thời hạn lưu trú và
yêu cầu gia hạn. - Gửi yêu cầu chuyển phòng theo quy trình được hỗ
trợ. - Xem thông tin nhận phòng/trả phòng liên quan đến mình. - Xem hóa
đơn, chi tiết khoản phí và lịch sử thanh toán của mình. - Thanh toán qua
phương thức được hệ thống cấu hình. - Tạo và theo dõi yêu cầu bảo trì
liên quan đến phòng đang lưu trú. - Xem thông báo được gửi cho mình. -
Không được xem hoặc sửa dữ liệu riêng tư của sinh viên khác.

## 2.3. Manager

Manager là người phụ trách xét duyệt đăng ký và xử lý các nghiệp vụ lưu
trú trong phạm vi được giao.

Manager có thể: - Xem danh sách đăng ký lưu trú cần xử lý. - Duyệt hoặc
từ chối đăng ký và ghi lý do khi cần. - Kiểm tra tình trạng/sức chứa
phòng và phân phòng. - Tạo hoặc quản lý hợp đồng lưu trú theo quy trình
được cho phép. - Ghi nhận tiền đặt cọc theo chính sách. - Xác nhận nhận
phòng và trả phòng. - Xem hồ sơ lưu trú trong phạm vi quản lý. - Xử lý
yêu cầu gia hạn hoặc chuyển phòng. - Tạo hóa đơn, quản lý chi tiết phí
và theo dõi tình trạng thanh toán. - Xem yêu cầu bảo trì và cập nhật
trạng thái xử lý. - Gửi hoặc theo dõi thông báo liên quan đến nghiệp
vụ. - Xem các chỉ số vận hành trực tiếp liên quan đến đăng ký và lưu
trú.

Manager không mặc nhiên có quyền quản trị mọi tài khoản, role hoặc thiết
lập toàn hệ thống. Không tạo vai trò Admin trong scope nghiệp vụ hiện
tại.

## 2.4. Ma trận quyền

  --------------------------------------------------------------------------
  Nghiệp vụ            Guest             Student           Manager
  -------------------- ----------------- ----------------- -----------------
  Xem hướng dẫn/thông  Có                Có                Có
  tin công khai                                            

  Đăng nhập/khôi phục  Có                Có                Có
  tài khoản                                                

  Tự đăng ký tài khoản Không             Không             Không
  Student công khai                                        

  Xem hồ sơ cá nhân    Không             Của mình          Hồ sơ trong phạm
                                                           vi quản lý

  Tạo đăng ký lưu trú  Không nếu chưa    Có                Hỗ trợ/xử lý theo
                       được cấp quyền                      quyền

  Xem đăng ký          Không             Của mình          Trong phạm vi
                                                           quản lý

  Duyệt/từ chối đăng   Không             Không             Có
  ký                                                       

  Phân phòng           Không             Không             Có

  Xem hợp đồng         Không             Của mình          Trong phạm vi
                                                           quản lý

  Tạo/quản lý hợp đồng Không             Không             Có

  Gửi yêu cầu gia      Không             Có                Xét duyệt/xử lý
  hạn/chuyển phòng                                         

  Xác nhận             Không             Xem trạng thái    Có
  check-in/check-out                     của mình          

  Xem hóa đơn          Không             Của mình          Trong phạm vi
                                                           quản lý

  Tạo hóa đơn          Không             Không             Có

  Thanh toán hóa đơn   Không             Hóa đơn của mình  Theo dõi/đối
                                                           soát, không mặc
                                                           nhiên thanh toán
                                                           thay

  Tạo yêu cầu bảo trì  Không             Theo quyền lưu    Có thể tiếp
                                         trú của mình      nhận/tạo theo
                                                           chính sách

  Cập nhật trạng thái  Không             Không             Có
  bảo trì                                                  

  Xem thông báo        Không             Thông báo của     Thông báo liên
                                         mình              quan

  Quản trị toàn bộ     Không             Không             Không mặc nhiên
  role/tài khoản                                           
  --------------------------------------------------------------------------

------------------------------------------------------------------------

# 3. Các khái niệm nghiệp vụ cốt lõi

## 3.1. User và Student

-   `User` lưu thông tin xác thực, role, trạng thái tài khoản và thông
    tin đăng nhập.
-   `Student` lưu hồ sơ nghiệp vụ của sinh viên.
-   Một User có role Student gắn với tối đa một Student profile; Student
    profile phải thuộc đúng một User.
-   User của Manager không cần Student profile.
-   Việc cấp tài khoản Student do nhà trường/quy trình provision thực
    hiện; không dùng đăng ký lưu trú để tự động mở tài khoản đăng nhập
    công khai.

## 3.2. Registration --- Đăng ký lưu trú

Registration là hồ sơ đề nghị được lưu trú tại ký túc xá. Đây là **đăng
ký chỗ ở**, không phải đăng ký tài khoản.

Một Registration tối thiểu cần xác định: - Sinh viên gửi đăng ký. - Thời
điểm gửi. - Nhu cầu/thời gian lưu trú. - Phòng hoặc loại phòng mong muốn
nếu quy trình cho phép chọn. - Thông tin cần thiết cho xét duyệt. -
Trạng thái xử lý. - Manager xử lý và lý do từ chối nếu có.

## 3.3. Room Assignment --- Phân phòng

Phân phòng là việc gán sinh viên được duyệt vào phòng/chỗ lưu trú cụ
thể. Đăng ký được duyệt không tự động có nghĩa là sinh viên đã nhận
phòng, trừ khi quy trình được thiết kế để thực hiện hai bước đó trong
cùng giao dịch.

## 3.4. Contract --- Hợp đồng lưu trú

Hợp đồng ghi nhận thời hạn và điều kiện lưu trú của sinh viên, bao gồm
phòng/chỗ ở, ngày bắt đầu/kết thúc, mức phí và tiền đặt cọc nếu áp dụng.

## 3.5. Check-in và Check-out

-   **Check-in:** ghi nhận sinh viên thực tế nhận phòng theo phân bổ/hợp
    đồng hợp lệ.
-   **Check-out:** ghi nhận sinh viên trả phòng và kết thúc hoặc chấm
    dứt giai đoạn lưu trú.
-   Các thao tác này cần cập nhật trạng thái lưu trú/phân bổ phòng nhất
    quán.

## 3.6. Invoice và Payment

-   Invoice là khoản phải thu cho một kỳ hoặc một nghĩa vụ lưu trú.
-   InvoiceDetail là một dòng phí thuộc hóa đơn.
-   Payment/PaymentTransaction ghi nhận lần giao dịch thanh toán và kết
    quả đã xác minh.
-   Hóa đơn đã phát hành, giao dịch thanh toán và trạng thái hợp đồng
    phải được liên kết để tra cứu lịch sử.

## 3.7. MaintenanceRequest

MaintenanceRequest là yêu cầu sửa chữa/bảo trì phát sinh trong thời gian
lưu trú, gắn với sinh viên và phòng liên quan.

## 3.8. Notification

Notification là thông báo phát sinh từ sự kiện nghiệp vụ; mỗi người nhận
có trạng thái đọc riêng.

------------------------------------------------------------------------

# 4. Cấp tài khoản và xác thực

## 4.1. Cấp tài khoản

-   Nhà trường hoặc quy trình quản lý được phê duyệt provision tài khoản
    Student.
-   Không triển khai public self-registration cho Student.
-   Manager được cấp tài khoản bằng quy trình có kiểm soát.
-   Nếu có chức năng kích hoạt tài khoản, chức năng đó không được nhầm
    với việc nộp đơn đăng ký lưu trú.
-   OTP/email có thể dùng cho khôi phục mật khẩu hoặc kích hoạt tài
    khoản nếu được triển khai.

## 4.2. Đăng nhập

1.  Người dùng nhập thông tin đăng nhập.
2.  Backend kiểm tra thông tin xác thực và trạng thái tài khoản.
3.  Nếu hợp lệ, hệ thống cấp token theo thiết kế hiện tại.
4.  Access token được gửi/lưu trong cookie `access_token` theo cách
    triển khai của dự án.
5.  JWT filter xác minh token và nạp User cùng authority vào Security
    Context.
6.  Spring Security kiểm tra role trước khi cho phép endpoint.
7.  Frontend điều hướng tới khu vực tương ứng.

Đăng nhập thành công không có nghĩa là được phép gọi tất cả API. Student
không thể gọi endpoint chỉ dành cho Manager.

## 4.3. Đăng xuất và token

-   Xóa cookie xác thực theo đúng cấu hình.
-   Nếu có refresh token, xử lý hết hiệu lực/thu hồi theo thiết kế.
-   Không ghi JWT hoặc secret vào log.
-   Cookie production cần cấu hình an toàn phù hợp HTTPS, `HttpOnly`,
    `Secure`, `SameSite`.
-   Vì JWT dùng cookie, cần duy trì cơ chế CSRF phù hợp cho request thay
    đổi dữ liệu.

## 4.4. Khôi phục mật khẩu

1.  Người dùng gửi yêu cầu.
2.  Backend phản hồi không làm lộ tài khoản/email có tồn tại hay không.
3.  Gửi OTP/liên kết xác minh nếu chức năng đã được cấu hình.
4.  Xác minh mã còn hiệu lực.
5.  Đặt mật khẩu mới.
6.  Giới hạn số lần thử và tần suất yêu cầu.

------------------------------------------------------------------------

# 5. Hồ sơ sinh viên

## 5.1. Dữ liệu hồ sơ

Các trường cụ thể theo schema đã triển khai; thông thường có thể gồm: -
ID nội bộ. - Mã sinh viên. - Họ tên. - Thông tin liên hệ cần thiết. -
Thông tin học tập/đơn vị nếu nghiệp vụ cần. - Trạng thái hồ sơ. - User
liên kết. - Thời điểm tạo/cập nhật.

Không thêm dữ liệu nhạy cảm không cần thiết.

## 5.2. Quy tắc

-   Student chỉ xem/cập nhật các trường được cho phép của mình.
-   Mã sinh viên và dữ liệu do nhà trường quản lý không được tự sửa nếu
    không có quy trình xác minh.
-   Manager chỉ xem dữ liệu trong phạm vi công việc.
-   Khóa tài khoản không đồng nghĩa với xóa lịch sử đăng ký, hợp đồng,
    hóa đơn hay lưu trú.
-   Các thay đổi quan trọng cần lưu người thực hiện và thời điểm nếu hệ
    thống hỗ trợ audit.

------------------------------------------------------------------------

# 6. Tra cứu phòng và tình trạng tiếp nhận

## 6.1. Mục tiêu

Cho sinh viên biết các lựa chọn lưu trú và cho Manager kiểm tra phòng
còn khả năng tiếp nhận trước khi duyệt/phân phòng.

## 6.2. Thông tin phòng

Thông tin phục vụ nghiệp vụ có thể gồm: - ID/mã phòng. - Tòa nhà/khu và
tầng nếu cần. - Loại phòng. - Sức chứa. - Số chỗ đang được phân bổ. - Số
chỗ còn khả dụng. - Giá/phí tham chiếu. - Tiện ích/thông tin mô tả. -
Trạng thái có thể tiếp nhận.

Không coi đây là yêu cầu phải triển khai module quản lý toàn bộ cơ sở
vật chất.

## 6.3. Trạng thái phòng đề xuất

-   `AVAILABLE`: có khả năng tiếp nhận.
-   `FULL`: đã đủ sức chứa.
-   `RESERVED`: có chỗ đang được giữ theo một đăng ký/đợt phân bổ.
-   `OCCUPIED`: đang có người lưu trú; nếu quản lý theo giường thì phòng
    vẫn có thể còn chỗ.
-   `MAINTENANCE`: tạm ngừng tiếp nhận vì bảo trì.
-   `UNAVAILABLE`: không thể phân bổ vì lý do khác.

Nếu hệ thống quản lý theo số chỗ/giường thay vì chỉ theo phòng, trạng
thái khả dụng phải tính dựa trên số chỗ còn lại; không đánh đồng
`OCCUPIED` với `FULL`.

## 6.4. Quy tắc

-   Không phân bổ vượt sức chứa.
-   Không phân phòng không khả dụng.
-   Kiểm tra sức chứa khi gửi đăng ký (nếu chọn phòng), khi duyệt và tại
    thời điểm phân bổ cuối cùng.
-   Phòng/chỗ phải có trạng thái nhất quán với hợp đồng và
    check-in/check-out.
-   Cần quyết định dự án quản lý theo phòng hay theo giường/chỗ. Nếu một
    phòng chứa nhiều sinh viên và mỗi sinh viên có chỗ riêng, mô hình
    `Bed`/`RoomSlot` có thể cần thiết; không được âm thầm giả định mỗi
    phòng chỉ có một sinh viên.

------------------------------------------------------------------------

# 7. Đăng ký lưu trú

## 7.1. Luồng Student

1.  Student đăng nhập bằng tài khoản được cấp.
2.  Student xem thông tin và điều kiện đăng ký lưu trú.
3.  Student điền hồ sơ đăng ký và thời gian lưu trú dự kiến.
4.  Student chọn phòng/loại phòng nếu quy trình cho phép.
5.  Backend xác định Student từ người dùng đang đăng nhập; không tin
    `studentId` tự gửi từ client.
6.  Backend validate thông tin và kiểm tra điều kiện.
7.  Hệ thống lưu Registration ở trạng thái chờ xét duyệt.
8.  Student theo dõi trạng thái hồ sơ.
9.  Khi Manager xử lý, Student nhận thông báo kết quả.

## 7.2. Luồng Manager

1.  Manager xem danh sách hồ sơ đang chờ.
2.  Manager mở chi tiết hồ sơ.
3.  Manager kiểm tra thông tin và điều kiện lưu trú.
4.  Manager duyệt hoặc từ chối.
5.  Nếu từ chối, lưu lý do khi cần.
6.  Nếu duyệt, chuyển sang bước phân phòng/hợp đồng theo quy trình.
7.  Hệ thống ghi người xử lý và thời điểm xử lý nếu schema hỗ trợ.
8.  Hệ thống thông báo kết quả cho sinh viên.

## 7.3. Trạng thái Registration

-   `PENDING`: chờ xét duyệt.
-   `APPROVED`: đã được chấp thuận.
-   `REJECTED`: bị từ chối.
-   `CANCELLED`: bị hủy theo quy trình hợp lệ.

Có thể bổ sung trạng thái trung gian nếu quy trình cần, nhưng phải thống
nhất giữa backend và frontend.

## 7.4. Quy tắc

-   Chỉ Student đã có tài khoản và hồ sơ hợp lệ mới nộp đăng ký trực
    tuyến.
-   Registration không tạo User mới.
-   Backend xác định chủ sở hữu từ Security Context.
-   Mỗi hồ sơ phải có lịch sử trạng thái rõ ràng.
-   Duyệt đăng ký không đồng nghĩa với check-in.
-   Duyệt đăng ký không được làm vượt sức chứa.
-   Quy định số đơn đang chờ của mỗi sinh viên và quy trình hủy đơn phải
    được thống nhất trước khi triển khai.

------------------------------------------------------------------------

# 8. Phân phòng

## 8.1. Mục tiêu

Gán sinh viên đã được chấp thuận vào phòng/chỗ cụ thể để chuẩn bị hợp
đồng và nhận phòng.

## 8.2. Luồng

1.  Manager mở hồ sơ đã được duyệt.
2.  Manager kiểm tra các phòng/chỗ còn khả dụng.
3.  Manager chọn phòng/chỗ phù hợp.
4.  Backend kiểm tra sức chứa, trạng thái phòng, thời gian dự kiến và
    các phân bổ đang tồn tại.
5.  Backend lưu phân bổ.
6.  Hệ thống cập nhật khả năng tiếp nhận.
7.  Student nhận thông báo về kết quả phân phòng.
8.  Quy trình tiếp tục tới hợp đồng/đặt cọc và check-in.

## 8.3. Quy tắc

-   Không cho hai thao tác đồng thời phân cùng một chỗ cho hai sinh
    viên.
-   Kiểm tra lại sức chứa ngay khi ghi nhận phân bổ, không chỉ dựa vào
    dữ liệu frontend đã xem trước đó.
-   Phân phòng cần liên kết với Registration hoặc hồ sơ lưu trú để truy
    vết.
-   Có thể cần trạng thái phân bổ như `RESERVED`, `ASSIGNED`,
    `CHECKED_IN`, `RELEASED`; phải phân biệt trạng thái phân bổ với
    trạng thái phòng.
-   Hủy phân bổ phải giải phóng chỗ và cập nhật trạng thái liên quan.
-   Nếu hệ thống chỉ quản lý phòng mà không quản lý giường/chỗ riêng,
    phải ghi rõ giới hạn đó trong nghiệp vụ.

------------------------------------------------------------------------

# 9. Hợp đồng và tiền đặt cọc

## 9.1. Mục tiêu

Ghi nhận thỏa thuận lưu trú và các nghĩa vụ tài chính ban đầu giữa sinh
viên và đơn vị quản lý.

## 9.2. Dữ liệu hợp đồng đề xuất

-   Mã hợp đồng.
-   Student.
-   Phòng/chỗ được phân bổ.
-   Registration liên quan nếu cần.
-   Ngày bắt đầu và kết thúc.
-   Giá/phí lưu trú đã thỏa thuận.
-   Tiền đặt cọc nếu áp dụng.
-   Điều khoản/ghi chú cần thiết.
-   Trạng thái hợp đồng.
-   Thời điểm tạo/cập nhật.
-   Thông tin gia hạn/chấm dứt.

## 9.3. Trạng thái hợp đồng

-   `DRAFT`: đang soạn.
-   `PENDING_DEPOSIT`: chờ hoàn thành nghĩa vụ đặt cọc nếu chính sách
    yêu cầu.
-   `ACTIVE`: hợp đồng có hiệu lực.
-   `PENDING_RENEWAL`: đang xử lý gia hạn nếu chọn mô hình trạng thái
    này.
-   `EXPIRED`: hết hạn.
-   `TERMINATED`: chấm dứt trước hạn.

Không bắt buộc phải dùng toàn bộ enum trên. Nếu gia hạn được quản lý
bằng entity yêu cầu riêng, không cần lặp lại trạng thái tương đương
trong Contract.

## 9.4. Luồng hợp đồng/đặt cọc

1.  Registration được duyệt và sinh viên được phân phòng.
2.  Manager tạo hợp đồng hoặc ghi nhận hợp đồng theo quy trình của đơn
    vị.
3.  Backend kiểm tra sinh viên, phòng, thời hạn và mức phí.
4.  Hệ thống ghi nhận nghĩa vụ đặt cọc nếu có.
5.  Sinh viên xem hợp đồng và số tiền cần thanh toán.
6.  Nếu đặt cọc là điều kiện trước check-in, chỉ cho phép nhận phòng khi
    điều kiện được xác nhận.
7.  Hợp đồng chuyển sang hiệu lực theo chính sách.
8.  Sinh viên nhận thông báo về hợp đồng và các mốc quan trọng.

## 9.5. Quy tắc

-   Ngày bắt đầu phải trước ngày kết thúc.
-   Không phân bổ hiệu lực vượt sức chứa.
-   Không cho check-in nếu các điều kiện bắt buộc của hợp đồng/đặt cọc
    chưa đạt.
-   Nếu đặt cọc được hoàn lại/khấu trừ khi trả phòng, phải có quy trình
    riêng và ghi nhận lịch sử.
-   Không xóa lịch sử hợp đồng khi sinh viên chuyển phòng hoặc kết thúc
    lưu trú.

------------------------------------------------------------------------

# 10. Nhận phòng (Check-in)

## 10.1. Mục tiêu

Xác nhận sinh viên thực tế bắt đầu lưu trú theo phân phòng và hợp đồng
hợp lệ.

## 10.2. Luồng

1.  Manager mở hồ sơ lưu trú/hợp đồng đã đủ điều kiện.
2.  Manager kiểm tra thông tin sinh viên, phòng/chỗ, thời hạn và nghĩa
    vụ bắt buộc.
3.  Manager xác nhận sinh viên nhận phòng.
4.  Backend lưu thời điểm check-in và người thực hiện.
5.  Hệ thống cập nhật trạng thái phân bổ và hồ sơ lưu trú.
6.  Hệ thống gửi thông báo xác nhận cho Student.
7.  Các kỳ hóa đơn tiếp theo được xác định theo chính sách hợp đồng.

## 10.3. Quy tắc

-   Không check-in khi chưa có phân phòng hợp lệ.
-   Không check-in khi hợp đồng/đặt cọc còn điều kiện bắt buộc chưa hoàn
    thành.
-   Không ghi nhận check-in lặp như hai lần lưu trú độc lập.
-   Check-in phải gắn với đúng sinh viên và phòng/chỗ.
-   Ngày check-in thực tế và ngày bắt đầu hợp đồng có thể khác nhau;
    phải lưu/hiển thị theo đúng định nghĩa dữ liệu.

------------------------------------------------------------------------

# 11. Hóa đơn và chi phí lưu trú

## 11.1. Mục tiêu

Quản lý các khoản sinh viên phải trả trong thời gian lưu trú, bao gồm
tiền phòng, điện/nước và các phí dịch vụ được áp dụng.

## 11.2. Thành phần dữ liệu

**Invoice** có thể gồm: - ID/mã hóa đơn. - Student. - Contract hoặc hồ
sơ lưu trú liên quan. - Kỳ hóa đơn. - Ngày phát hành. - Hạn thanh
toán. - Tổng tiền. - Trạng thái. - Thời điểm tạo/cập nhật.

**InvoiceDetail** có thể gồm: - Invoice ID. - Loại phí. - Mô tả. - Số
lượng nếu áp dụng. - Đơn giá. - Thành tiền. - Ghi chú.

Loại phí có thể gồm tiền phòng, điện, nước, Internet/dịch vụ hoặc khoản
phí khác theo chính sách lưu trú.

## 11.3. Điện và nước

-   Điện/nước được biểu diễn dưới dạng dòng phí trong hóa đơn.
-   **Module MeterReading/công tơ điện nước không nằm trong phạm vi hiện
    tại.**
-   Trong phiên bản hiện tại, khoản phí có thể do Manager nhập từ dữ
    liệu bên ngoài hoặc được tạo từ chính sách phí đã thống nhất.
-   Không hard-code đơn giá hoặc tổng tiền ở frontend.
-   Chỉ thêm các dòng phí áp dụng thực tế; không bắt buộc hóa đơn nào
    cũng có mọi loại phí.

## 11.4. Trạng thái hóa đơn

-   `UNPAID`: chưa thanh toán.
-   `PARTIALLY_PAID`: thanh toán một phần, chỉ dùng nếu hỗ trợ.
-   `PAID`: đã thanh toán đủ.
-   `OVERDUE`: quá hạn chưa thanh toán đủ.
-   `CANCELLED`: bị hủy theo quy trình có quyền.

Nếu phiên bản đầu chỉ hỗ trợ thanh toán toàn bộ, không cần bật nghiệp vụ
thanh toán một phần.

## 11.5. Luồng

1.  Manager lập hóa đơn cho sinh viên/kỳ lưu trú.
2.  Manager nhập hoặc xác nhận chi tiết phí.
3.  Backend validate và tính tổng từ các dòng chi phí.
4.  Backend lưu Invoice và InvoiceDetail trong transaction.
5.  Hóa đơn được phát hành theo quy trình.
6.  Student xem hóa đơn và hạn thanh toán.
7.  Student thanh toán qua phương thức hỗ trợ.
8.  Backend xác minh giao dịch và cập nhật trạng thái.
9.  Student và Manager xem trạng thái đã được cập nhật.

## 11.6. Quy tắc

-   Backend tính/kiểm tra tổng tiền; không tin tổng tiền do frontend tự
    tính.
-   Dùng `BigDecimal` hoặc kiểu tiền tệ chính xác tương đương.
-   Hóa đơn đã phát hành cần quy trình điều chỉnh/hủy có kiểm soát.
-   Không xóa lịch sử hóa đơn/thanh toán khi hợp đồng hết hạn.
-   Trạng thái quá hạn phải dựa vào hạn thanh toán và số tiền còn nợ.
-   Phí và kỳ hóa đơn phải phù hợp với thời hạn hợp đồng/lưu trú.

------------------------------------------------------------------------

# 12. Thanh toán và đặt cọc --- SePay/VNPay

## 12.1. Mục tiêu

Ghi nhận việc thanh toán tiền đặt cọc hoặc hóa đơn lưu trú bằng phương
thức được cấu hình.

## 12.2. Phương thức

SePay và VNPay là các phương thức đang được xem xét cho tích hợp. Chỉ
phương thức đã được cấu hình và kiểm thử mới được bật cho người dùng.

## 12.3. Dữ liệu giao dịch đề xuất

-   ID nội bộ.
-   Invoice hoặc nghĩa vụ đặt cọc liên quan.
-   Student/User thực hiện.
-   Phương thức thanh toán.
-   Số tiền.
-   Trạng thái giao dịch.
-   Mã giao dịch nội bộ.
-   Mã giao dịch nhà cung cấp.
-   Thời điểm khởi tạo.
-   Thời điểm xác nhận kết quả.

## 12.4. Trạng thái giao dịch

-   `PENDING`.
-   `SUCCESS`.
-   `FAILED`.
-   `CANCELLED`.
-   `EXPIRED` nếu phương thức hỗ trợ.

## 12.5. Luồng thanh toán

1.  Student mở khoản phí/đặt cọc cần thanh toán.
2.  Backend kiểm tra quyền sở hữu và số tiền còn phải trả.
3.  Backend tạo giao dịch `PENDING`.
4.  Backend khởi tạo giao dịch theo hướng dẫn chính thức của nhà cung
    cấp.
5.  Student thực hiện thanh toán.
6.  Backend nhận callback/webhook hoặc xác minh trạng thái qua cơ chế
    chính thức.
7.  Backend kiểm tra nguồn, chữ ký, mã giao dịch, số tiền và khoản phải
    thu.
8.  Backend cập nhật giao dịch theo cách idempotent.
9.  Khi thành công được xác minh, backend cập nhật nghĩa vụ tài chính.
10. Hệ thống gửi thông báo kết quả.

## 12.6. Quy tắc bắt buộc

-   Không xác nhận thanh toán chỉ vì frontend quay về trang success.
-   Không tin số tiền/invoice ID từ client nếu chưa kiểm tra ở backend.
-   Callback gửi lặp không được ghi nhận thanh toán trùng.
-   Mã giao dịch nhà cung cấp phải được đối chiếu.
-   Nếu số tiền không khớp, không tự đánh dấu đã thanh toán; cần ghi
    nhận và xử lý đối soát.
-   Giao dịch thất bại không được làm nghĩa vụ tài chính chuyển thành đã
    thanh toán.
-   Thay đổi Payment và Invoice phải nhất quán.
-   Không log secret, JWT hoặc thông tin thanh toán nhạy cảm.
-   Cần chốt phương thức nào được triển khai trước, quy tắc partial
    payment và hoàn tiền trước khi làm các trường hợp tương ứng.

------------------------------------------------------------------------

# 13. Gia hạn lưu trú/hợp đồng

## 13.1. Mục tiêu

Cho Student đề nghị kéo dài thời gian lưu trú và cho Manager xét duyệt
dựa trên tình trạng hợp đồng, phòng và chính sách.

## 13.2. Luồng

1.  Student xem ngày hết hạn hợp đồng.
2.  Student gửi yêu cầu gia hạn trong thời gian được phép.
3.  Backend kiểm tra hợp đồng còn phù hợp để gia hạn.
4.  Manager xem xét yêu cầu.
5.  Manager chấp thuận hoặc từ chối và ghi lý do khi cần.
6.  Nếu chấp thuận, cập nhật hợp đồng hoặc tạo phiên bản/giai đoạn gia
    hạn theo mô hình dữ liệu đã chọn.
7.  Hệ thống cập nhật ngày hết hạn và các khoản phí liên quan.
8.  Student nhận thông báo kết quả.

## 13.3. Quy tắc

-   Không cho gia hạn hợp đồng đã kết thúc theo cách bỏ qua kiểm tra;
    trường hợp ngoại lệ cần quy trình riêng.
-   Phải ghi nhận thời hạn mới và bảo toàn lịch sử theo chính sách.
-   Kiểm tra phòng/chỗ có tiếp tục khả dụng cho thời gian gia hạn hay
    không.
-   Hóa đơn và phí sau gia hạn phải dựa trên thời hạn/giá đã được xác
    nhận.
-   Gia hạn không tự động đồng nghĩa với việc đổi phòng.

------------------------------------------------------------------------

# 14. Chuyển phòng

## 14.1. Mục tiêu

Cho sinh viên đang lưu trú gửi yêu cầu chuyển sang phòng/chỗ khác và cho
Manager xét duyệt.

## 14.2. Luồng

1.  Student gửi yêu cầu chuyển phòng, lý do và lựa chọn mong muốn nếu
    có.
2.  Backend xác định hồ sơ lưu trú hiện tại.
3.  Manager kiểm tra lý do, khả năng tiếp nhận và điều kiện hợp đồng.
4.  Manager duyệt hoặc từ chối.
5.  Nếu duyệt, hệ thống phân bổ phòng/chỗ mới theo quy trình.
6.  Xác định thời điểm hiệu lực chuyển phòng.
7.  Cập nhật phân bổ cũ và mới nhất quán.
8.  Cập nhật hợp đồng/phí nếu chính sách yêu cầu.
9.  Student nhận thông báo.

## 14.3. Quy tắc

-   Không cho chuyển sang phòng không khả dụng hoặc vượt sức chứa.
-   Không giải phóng phòng cũ trước khi phương án chuyển phòng được xác
    nhận hợp lệ.
-   Phải giữ lịch sử phòng cũ và phòng mới.
-   Nếu chuyển phòng làm thay đổi giá/phí, cần xác nhận ngày hiệu lực và
    cách tính khoản chênh lệch.
-   Không coi chuyển phòng là một đăng ký lưu trú hoàn toàn mới nếu
    chính sách không yêu cầu.

------------------------------------------------------------------------

# 15. Trả phòng (Check-out) và kết thúc lưu trú

## 15.1. Mục tiêu

Ghi nhận sinh viên kết thúc thời gian lưu trú, cập nhật phòng/chỗ và xử
lý các nghĩa vụ liên quan.

## 15.2. Luồng

1.  Student gửi yêu cầu trả phòng hoặc Manager khởi tạo quy trình theo
    chính sách.
2.  Manager kiểm tra hợp đồng, thời hạn và tình trạng lưu trú.
3.  Kiểm tra hóa đơn/công nợ và các nghĩa vụ còn lại.
4.  Ghi nhận thời điểm check-out thực tế.
5.  Xử lý tiền đặt cọc theo chính sách nếu có.
6.  Kết thúc hợp đồng hoặc cập nhật trạng thái phù hợp.
7.  Giải phóng phân bổ phòng/chỗ sau khi check-out được xác nhận.
8.  Cập nhật khả năng tiếp nhận của phòng/chỗ.
9.  Gửi thông báo xác nhận cho Student.
10. Giữ lại lịch sử lưu trú, hợp đồng, hóa đơn và thanh toán.

## 15.3. Trạng thái đề xuất

Yêu cầu trả phòng có thể dùng: - `PENDING`. - `APPROVED`. -
`REJECTED`. - `COMPLETED` hoặc trạng thái tương đương khi check-out được
xác nhận.

Hợp đồng có thể chuyển sang `EXPIRED` hoặc `TERMINATED` tùy lý do kết
thúc. Không dùng một trạng thái duy nhất để biểu diễn đồng thời trạng
thái hợp đồng, yêu cầu trả phòng và tình trạng phòng.

## 15.4. Quy tắc

-   Không giải phóng phòng/chỗ trước khi việc check-out được xác nhận.
-   Công nợ và tiền đặt cọc phải được xử lý theo chính sách; không tự
    xóa nghĩa vụ tài chính.
-   Phải giữ lịch sử lưu trú để tra cứu.
-   Check-out không xóa Student hoặc User.
-   Nếu sinh viên trả phòng trước hạn, phải có quy tắc về hợp đồng, phí
    và đặt cọc.

------------------------------------------------------------------------

# 16. Yêu cầu bảo trì (Maintenance)

## 16.1. Mục tiêu

Cho Student gửi yêu cầu sửa chữa liên quan đến nơi đang lưu trú và cho
Manager theo dõi quá trình xử lý.

## 16.2. Dữ liệu chính

Entity `MaintenanceRequest` hiện được thiết kế với: - `id`. - `title`. -
`description`. - `maintenanceStatus`. - `student`. - `room`. -
`createdAt`. - `updatedAt`. - `resolvedAt`.

Response hiện trả: - `id`, `title`, `description`, `status`. -
`studentId`, `roomId`. - `createdAt`, `updatedAt`, `resolvedAt`.

## 16.3. Trạng thái hiện tại

-   `PENDING`: mới gửi, chờ xử lý.
-   `IN_PROGRESS`: đang xử lý.
-   `COMPLETED`: đã hoàn thành.
-   `REJECTED`: bị từ chối.

## 16.4. Luồng Student

1.  Student đăng nhập.
2.  Student tạo yêu cầu với tiêu đề, mô tả và phòng liên quan.
3.  Backend lấy Student từ User đang xác thực.
4.  Backend kiểm tra dữ liệu và phòng.
5.  Lưu yêu cầu ở trạng thái `PENDING`.
6.  Student xem danh sách và chi tiết yêu cầu của mình.
7.  Student nhận thông báo khi trạng thái thay đổi nếu Notification đã
    tích hợp.

## 16.5. Luồng Manager

1.  Manager xem yêu cầu trong phạm vi quản lý.
2.  Manager mở chi tiết.
3.  Chuyển `PENDING` sang `IN_PROGRESS` hoặc `REJECTED`.
4.  Chuyển `IN_PROGRESS` sang `COMPLETED` hoặc `REJECTED`.
5.  Khi chuyển sang `COMPLETED`, backend ghi `resolvedAt`.
6.  `updatedAt` được cập nhật khi bản ghi thay đổi.
7.  Student được thông báo về kết quả nếu Notification đã tích hợp.

## 16.6. Quy tắc chuyển trạng thái

-   `PENDING` → `IN_PROGRESS`.
-   `PENDING` → `REJECTED`.
-   `IN_PROGRESS` → `COMPLETED`.
-   `IN_PROGRESS` → `REJECTED`.
-   `COMPLETED` và `REJECTED` là trạng thái kết thúc trong logic hiện
    tại.
-   Không chuyển trạng thái kết thúc trở lại nếu chưa có quy trình mở
    lại.
-   Gửi cùng trạng thái hiện tại có thể trả lại bản ghi hiện tại mà
    không tạo thay đổi mới.
-   `resolvedAt` được gán khi chuyển sang `COMPLETED`.

## 16.7. Quyền truy cập và điểm cần hoàn thiện

-   Student chỉ xem yêu cầu của mình.
-   Manager cập nhật trạng thái qua endpoint có phân quyền Manager.
-   Student không được tự sửa `status`, `studentId` hoặc `resolvedAt`.
-   **Cần hoàn thiện quy tắc quyền sở hữu phòng:** hiện tại service kiểm
    tra phòng tồn tại; nghiệp vụ đầy đủ nên đảm bảo Student chỉ tạo yêu
    cầu cho phòng đang được phân công/đang lưu trú, trừ trường hợp có
    quyền xử lý đặc biệt.
-   Cần xác định Manager được xem yêu cầu toàn bộ khu lưu trú hay chỉ
    phạm vi được giao.
-   Ảnh đính kèm, mức độ ưu tiên, SLA, trao đổi bình luận và mở lại yêu
    cầu chưa phải yêu cầu bắt buộc đã chốt.

## 16.8. API đã kiểm thử

-   `POST /api/v1/student/maintenance`
-   `GET /api/v1/student/maintenance`
-   `GET /api/v1/student/maintenance/{maintenanceId}`
-   `PATCH /api/v1/manager/maintenance/{maintenanceId}/status`

Người phát triển đã kiểm thử thành công luồng tạo → danh sách → chi tiết
→ `IN_PROGRESS` → `COMPLETED`; response trả `resolvedAt` sau khi hoàn
thành.

------------------------------------------------------------------------

# 17. Notification

## 17.1. Mục tiêu

Thông báo cho đúng người dùng khi có thay đổi liên quan tới đăng ký,
phân phòng, hợp đồng, thanh toán, bảo trì hoặc kết thúc lưu trú.

## 17.2. Các sự kiện cần thông báo

-   Đăng ký lưu trú đã được tiếp nhận.
-   Đăng ký được duyệt/từ chối.
-   Phòng/chỗ đã được phân bổ.
-   Hợp đồng được tạo hoặc kích hoạt.
-   Đặt cọc đã được xác nhận.
-   Sắp tới ngày check-in.
-   Có hóa đơn mới hoặc sắp đến hạn.
-   Thanh toán đã được xác minh thành công/thất bại.
-   Yêu cầu bảo trì đổi trạng thái.
-   Yêu cầu gia hạn/chuyển phòng được xử lý.
-   Nhắc thời hạn hợp đồng.
-   Check-out được xác nhận.

## 17.3. Mô hình dữ liệu đề xuất

**Notification** - ID. - Tiêu đề. - Nội dung. - Loại sự kiện. - Link/ID
tham chiếu nghiệp vụ nếu cần. - Thời điểm tạo.

**NotificationReceiver** hoặc `Notification_Receiver` - Notification
ID. - User nhận. - Trạng thái đã đọc/chưa đọc. - Thời điểm đọc nếu cần.

Một Notification có thể gửi tới nhiều người; trạng thái đọc phải riêng
cho từng người nhận.

## 17.4. Luồng

1.  Sự kiện nghiệp vụ được xác nhận.
2.  Backend xác định người nhận dựa trên quan hệ và quyền.
3.  Tạo Notification và bản ghi người nhận.
4.  User xem danh sách thông báo.
5.  User mở hoặc đánh dấu đã đọc.
6.  Backend cập nhật trạng thái đọc của chính người nhận đó.
7.  Frontend hiển thị số thông báo chưa đọc.

## 17.5. Quy tắc

-   User chỉ xem thông báo của mình hoặc thông báo chung được phép nhận.
-   Đánh dấu đã đọc không ảnh hưởng người nhận khác.
-   Không tạo thông báo trùng vô hạn khi request/callback được retry.
-   Lỗi gửi thông báo không được làm sai trạng thái hợp đồng hoặc thanh
    toán.
-   Phiên bản đầu có thể chỉ cần thông báo trong ứng dụng;
    realtime/email/push là phần mở rộng nếu chưa được chốt.

------------------------------------------------------------------------

# 18. Mô hình dữ liệu nghiệp vụ

Đây là mô hình domain ở mức khái niệm, không phải schema SQL cuối cùng.

## 18.1. Các thực thể chính

-   `User`: thông tin đăng nhập, role, trạng thái tài khoản.
-   `Student`: hồ sơ nghiệp vụ sinh viên.
-   `Room`: thông tin phòng và khả năng tiếp nhận.
-   `Registration`: đơn đăng ký lưu trú.
-   `RoomAssignment` hoặc dữ liệu phân bổ tương đương: sinh viên được
    gán phòng/chỗ.
-   `Contract`: hợp đồng lưu trú và thời hạn.
-   `Deposit` hoặc Payment/Invoice liên quan: nghĩa vụ và giao dịch đặt
    cọc, tùy mô hình đã chọn.
-   `CheckIn`/`CheckOut` hoặc trường thời gian/trạng thái tương ứng:
    lịch sử nhận/trả phòng.
-   `Invoice`: hóa đơn.
-   `InvoiceDetail`: dòng phí.
-   `PaymentTransaction`: giao dịch thanh toán.
-   `MaintenanceRequest`: yêu cầu bảo trì.
-   `Notification`: nội dung thông báo.
-   `NotificationReceiver`: người nhận và trạng thái đọc.

Không bắt buộc tạo một entity riêng cho mọi khái niệm nếu codebase hiện
tại đã biểu diễn nghiệp vụ tương đương bằng trạng thái/trường hợp lý.
Tuy nhiên, không được gộp các trạng thái khác bản chất vào một field duy
nhất.

## 18.2. Quan hệ khái niệm

-   User 1 --- 0..1 Student.
-   Student 1 --- N Registration theo lịch sử.
-   Room 1 --- N Registration.
-   Student 1 --- N RoomAssignment theo lịch sử.
-   Room 1 --- N RoomAssignment theo lịch sử.
-   Student 1 --- N Contract.
-   Room/RoomAssignment 1 --- N Contract hoặc liên kết theo mô hình hợp
    đồng được chọn.
-   Contract 1 --- N Invoice theo các kỳ phí nếu nghiệp vụ yêu cầu.
-   Invoice 1 --- N InvoiceDetail.
-   Invoice 1 --- N PaymentTransaction nếu cho phép nhiều lần
    thử/partial payment.
-   Student 1 --- N MaintenanceRequest.
-   Room 1 --- N MaintenanceRequest.
-   Notification N --- N User thông qua NotificationReceiver.

Các quan hệ cụ thể cần phù hợp với entity hiện tại. Trước khi tạo entity
mới, coding agent phải đọc codebase để tránh tạo hai bảng cùng biểu diễn
một khái niệm.

## 18.3. Nguyên tắc dữ liệu

-   Dùng foreign key cho quan hệ quan trọng.
-   Dùng unique constraint cho mã định danh cần duy nhất.
-   Student profile liên kết đúng User.
-   Không tin ID chủ sở hữu do client gửi để cấp quyền.
-   Không xóa cứng lịch sử lưu trú, hợp đồng, hóa đơn và thanh toán chỉ
    vì Student đã trả phòng.
-   Các thao tác phân phòng, check-in, check-out, tạo hóa đơn và xác
    nhận thanh toán cần transaction phù hợp.
-   Cân nhắc locking/constraint khi nhiều Manager có thể phân bổ cùng
    một phòng/chỗ đồng thời.
-   Response dùng DTO, tránh trả entity quan hệ trực tiếp hoặc lộ dữ
    liệu không cần thiết.

------------------------------------------------------------------------

# 19. Quy tắc bảo mật và phân quyền

## 19.1. Authorization

-   Endpoint riêng tư yêu cầu xác thực.
-   Endpoint Student yêu cầu role Student và kiểm tra quyền sở hữu bản
    ghi.
-   Endpoint Manager yêu cầu role Manager và kiểm tra phạm vi nghiệp vụ.
-   Không có role Admin trong phạm vi nghiệp vụ hiện tại.
-   Ẩn nút trên frontend không thay thế kiểm tra quyền tại backend.
-   Backend lấy User hiện tại từ Security Context; không tin
    `studentId`/`userId` do client gửi để xác định chủ sở hữu.

## 19.2. JWT/cookie/CSRF

-   Dự án hiện đọc JWT từ cookie `access_token`.
-   Cookie xác thực cần được bảo vệ đúng cách.
-   Khi CSRF được bật, request POST/PATCH/PUT/DELETE phải gửi CSRF token
    hợp lệ.
-   CORS chỉ cho phép origin cần thiết; cấu hình credentials an toàn.
-   Không log JWT, refresh token hoặc secret.

## 19.3. Quyền sở hữu dữ liệu

-   Student A không thể xem registration, contract, invoice, payment
    hoặc maintenance request của Student B bằng cách thay ID trên URL.
-   Manager chỉ truy cập dữ liệu trong phạm vi được giao.
-   Mọi thay đổi trạng thái quan trọng phải được kiểm tra ở
    service/backend.
-   Lỗi trả cho client không được tiết lộ dữ liệu riêng tư hoặc chi tiết
    hệ thống không cần thiết.

------------------------------------------------------------------------

# 20. Quy tắc API và xử lý lỗi

## 20.1. Kiến trúc backend

Giữ cấu trúc layer hiện có của dự án:

-   `config`
-   `controller`
-   `dto`
-   `entity`
-   `exception`
-   `mapper`
-   `repository`
-   `security`
-   `service`

Không tự chuyển dự án sang cấu trúc feature-based nếu chưa có quyết định
thay đổi kiến trúc.

Vai trò từng layer: - Controller nhận request, gọi service và trả
response. - DTO định nghĩa dữ liệu request/response. - Service thực thi
quy tắc nghiệp vụ. - Repository truy vấn và lưu dữ liệu. - Mapper chuyển
đổi Entity/DTO. - Exception/ErrorCode/GlobalExceptionHandler chuẩn hóa
lỗi. - Security xác thực và phân quyền.

## 20.2. HTTP status gợi ý

-   `200 OK`: truy vấn/cập nhật thành công.
-   `201 Created`: tạo tài nguyên thành công.
-   `400 Bad Request`: dữ liệu đầu vào sai.
-   `401 Unauthorized`: chưa xác thực hoặc token không hợp lệ.
-   `403 Forbidden`: đã xác thực nhưng không đủ quyền hoặc CSRF không
    hợp lệ.
-   `404 Not Found`: không tìm thấy tài nguyên; có thể dùng cách trả lời
    không tiết lộ tài nguyên của người khác.
-   `409 Conflict`: xung đột trạng thái, sức chứa, phân bổ hoặc giao
    dịch trùng.
-   `500 Internal Server Error`: lỗi không dự kiến; không trả stack
    trace hoặc secret cho client.

## 20.3. Quy tắc lỗi

-   Format response lỗi nhất quán.
-   Validate dữ liệu bằng Bean Validation.
-   Dùng ErrorCode có ý nghĩa nghiệp vụ.
-   Không trả SQL/stack trace/secret cho frontend.
-   Log đủ ngữ cảnh để debug nhưng không chứa thông tin xác thực nhạy
    cảm.

------------------------------------------------------------------------

# 21. Yêu cầu phi chức năng

## 21.1. Tính toàn vẹn

-   Không phân phòng vượt sức chứa.
-   Không cho check-in khi chưa đạt điều kiện bắt buộc.
-   Không giải phóng chỗ trước khi check-out hợp lệ.
-   Không xác nhận thanh toán chưa được kiểm chứng.
-   Retry không tạo đăng ký, thanh toán hoặc thông báo trùng ngoài ý
    muốn.

## 21.2. Hiệu năng

-   Phân trang danh sách đăng ký, hóa đơn, bảo trì và lịch sử.
-   Index các foreign key, trạng thái và trường ngày dùng để lọc.
-   Tránh N+1 query.
-   Trả DTO với dữ liệu cần thiết.
-   Không tải toàn bộ dữ liệu rồi tính tổng ở frontend.

## 21.3. Khả năng bảo trì

-   Giữ layer rõ ràng.
-   Chuẩn hóa enum và chuyển trạng thái.
-   Test quy tắc nghiệp vụ và phân quyền.
-   Cấu hình dev/test/production riêng.
-   Không commit secret vào repository.
-   Đọc code hiện tại trước khi thêm entity/service mới.

## 21.4. Trải nghiệm người dùng

-   Responsive cho desktop/mobile.
-   Có loading, empty, success và error states.
-   Hiển thị rõ trạng thái đăng ký, phân phòng, hợp đồng, hóa đơn và bảo
    trì.
-   Không bắt sinh viên nhập lại thông tin backend có thể xác định an
    toàn.
-   Thông báo nghiệp vụ cần rõ ràng, có thời gian và liên kết tới nội
    dung liên quan.

------------------------------------------------------------------------

# 22. Tiêu chí nghiệm thu

## 22.1. Đăng nhập và hồ sơ

-   Student đăng nhập bằng tài khoản đã được cấp.
-   Không có public self-registration cho tài khoản Student.
-   Student chỉ xem hồ sơ của mình.
-   Endpoint được bảo vệ theo role.
-   Token không hợp lệ bị từ chối.

## 22.2. Đăng ký và phân phòng

-   Student gửi được hồ sơ hợp lệ.
-   Manager xem và duyệt/từ chối được.
-   Student xem được kết quả.
-   Không phân phòng vượt sức chứa.
-   Không phân bổ cùng một chỗ cho nhiều người trong khoảng thời gian
    trùng nếu chính sách không cho phép.
-   Duyệt đăng ký và phân phòng là hai trạng thái/khái niệm rõ ràng.

## 22.3. Hợp đồng, đặt cọc và check-in

-   Hợp đồng liên kết đúng Student và phòng/chỗ.
-   Thời hạn hợp đồng hợp lệ.
-   Đặt cọc được ghi nhận theo chính sách.
-   Check-in chỉ thành công khi đủ điều kiện.
-   Lịch sử hợp đồng và lưu trú được giữ lại.

## 22.4. Hóa đơn và thanh toán

-   Manager tạo hóa đơn có các dòng phí.
-   Backend tính/kiểm tra tổng tiền.
-   Student chỉ xem hóa đơn của mình.
-   Chỉ kết quả thanh toán đã xác minh mới cập nhật trạng thái.
-   Callback lặp không ghi nhận thanh toán trùng.
-   Hóa đơn và giao dịch phản ánh nhất quán kết quả.

## 22.5. Gia hạn, chuyển phòng, check-out

-   Student gửi yêu cầu theo điều kiện.
-   Manager duyệt/từ chối.
-   Phòng/chỗ và hợp đồng cập nhật đúng.
-   Lịch sử phân phòng được giữ lại.
-   Check-out không xóa lịch sử và không làm mất công nợ/biên nhận.

## 22.6. Maintenance

-   Student tạo được yêu cầu hợp lệ.
-   Yêu cầu gắn với Student và phòng.
-   Student chỉ xem yêu cầu của mình.
-   Manager cập nhật trạng thái theo luồng hợp lệ.
-   Trạng thái kết thúc không chuyển ngược nếu chưa có quy trình mở lại.
-   `resolvedAt` được gán khi `COMPLETED`.
-   `updatedAt` cập nhật khi bản ghi thay đổi.

## 22.7. Notification

-   Sự kiện quan trọng tạo thông báo cho đúng người nhận.
-   User chỉ xem thông báo của mình.
-   Trạng thái đọc độc lập theo từng người nhận.
-   Retry không tạo thông báo trùng không kiểm soát.

------------------------------------------------------------------------

# 23. Những chính sách cần chốt khi triển khai module tương ứng

Các điểm dưới đây cần được quyết định cụ thể; không được tự giả định nếu
ảnh hưởng tới quyền, tiền, sức chứa hoặc lịch sử:

1.  **Mô hình chỗ ở:** quản lý theo phòng hay theo từng giường/chỗ.
2.  **Điều kiện đăng ký:** giấy tờ, thời gian đăng ký, số đơn đang chờ
    và điều kiện đủ tư cách.
3.  **Quy trình phân phòng:** duyệt đơn có tự phân phòng hay Manager
    phân thủ công.
4.  **Hợp đồng:** thời điểm tạo/kích hoạt và điều kiện đặt cọc.
5.  **Check-in:** có bắt buộc thanh toán đặt cọc hoặc phí đầu kỳ trước
    khi nhận phòng không.
6.  **Hóa đơn:** chu kỳ thu, ngày phát hành, hạn thanh toán, chính sách
    phí.
7.  **Thanh toán:** SePay hay VNPay triển khai trước; có partial
    payment/hoàn tiền không.
8.  **Gia hạn:** thời hạn gửi yêu cầu, điều kiện duyệt và cách lưu lịch
    sử hợp đồng.
9.  **Chuyển phòng:** lý do được chấp nhận, thời điểm chuyển và cách xử
    lý phí.
10. **Check-out:** quy trình trả phòng, công nợ, hoàn/khấu trừ đặt cọc.
11. **Maintenance:** Student chỉ gửi yêu cầu cho phòng đang được phân
    công hay có trường hợp ngoại lệ; Manager phụ trách theo phạm vi nào.
12. **Notification:** in-app là đủ cho bản đầu hay cần realtime/email.
13. **Audit:** thao tác nào cần lưu lịch sử người thực hiện và thời
    gian.

Khi chưa chốt, nên triển khai tối giản nhưng bảo toàn dữ liệu và để ngỏ
khả năng mở rộng; không tự ý đưa tính năng ngoài phạm vi vào bản đầu.

------------------------------------------------------------------------

# 24. Hướng dẫn sử dụng tài liệu với OpenCode/coding agent

Khi giao việc cho coding agent:

1.  Đọc tài liệu này và kiểm tra codebase hiện tại trước khi sửa.
2.  Chỉ làm đúng module/task được giao; không tự mở rộng sang quản trị
    ký túc xá tổng thể.
3.  Giữ đúng tên và mục tiêu dự án: **hệ thống quản lý đăng ký và lưu
    trú ký túc xá sinh viên**.
4.  Không thêm vai trò Admin vào phạm vi nghiệp vụ nếu chưa có quyết
    định mới.
5.  Không tạo public self-registration cho Student; tài khoản được
    provision theo quy trình nhà trường.
6.  Không triển khai module công tơ điện/nước trong scope hiện tại.
7.  Không hard-code giá/tiền ở frontend.
8.  Bảo vệ quyền sở hữu dữ liệu ở backend.
9.  Giữ kiến trúc layer hiện tại và tận dụng class có sẵn; tránh tạo
    entity/DTO/service trùng.
10. Thêm test cho quy tắc chuyển trạng thái, phân quyền, sức chứa và
    giao dịch khi phù hợp.
11. Báo rõ file đã thay đổi, test đã chạy, kết quả và phần còn thiếu.
12. Không tuyên bố hoàn tất chỉ vì compile thành công; phải kiểm thử
    nghiệp vụ phù hợp.
13. Nếu phát hiện yêu cầu chưa chốt, ghi rõ giả định hoặc báo cần quyết
    định, không âm thầm tự mở rộng scope.

------------------------------------------------------------------------

# 25. Tóm tắt chuẩn của DormHub

DormHub là **hệ thống quản lý đăng ký và lưu trú ký túc xá sinh viên**,
không phải hệ thống quản trị tổng thể ký túc xá.

Vòng đời nghiệp vụ chính:

**Tài khoản được cấp → Student đăng ký lưu trú → Manager xét duyệt →
Phân phòng → Hợp đồng/đặt cọc → Check-in → Hóa đơn/thanh toán →
Maintenance/Notification → Gia hạn hoặc chuyển phòng → Check-out.**

Ba vai trò chính: - Guest. - Student. - Manager.

Các nguyên tắc không được tự ý thay đổi: - Đăng ký lưu trú không đồng
nghĩa với đăng ký tài khoản. - Student chỉ truy cập dữ liệu của chính
mình. - Manager xử lý các hồ sơ lưu trú trong phạm vi quyền. - Phân
phòng phải bảo toàn sức chứa. - Hợp đồng, đặt cọc, hóa đơn và thanh toán
phải nhất quán. - Thanh toán chỉ được xác nhận sau khi backend xác
minh. - Lịch sử lưu trú và tài chính phải được bảo toàn. - Maintenance
có luồng trạng thái rõ ràng. - Notification gắn với người nhận và trạng
thái đọc riêng. - Không thêm Admin hoặc các module quản trị tổng thể khi
chưa có yêu cầu mới.

**Kết thúc tài liệu.**
