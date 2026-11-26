package com.dormhub.service;

import com.dormhub.config.ContractLandlordProperties;
import com.dormhub.dto.response.ContractDraftResponse;
import com.dormhub.dto.response.ContractLandlordResponse;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.entity.User;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractDraftService {
    private final StudentService studentService;
    private final RoomService roomService;
    private final ContractLandlordProperties landlordProperties;

    public ContractDraftResponse getDraft(User user, Long roomId) {
        RoomResponse room = roomService.getRoomByIdForStudent(roomId, user);
        BigDecimal price = room.getPricePerMonth();
        if (price == null || price.signum() <= 0) {
            throw new AppException(ErrorCode.CONTRACT_RENT_INVALID);
        }
        // Match ContractService's current monthly rent and one-month deposit calculation.
        BigDecimal monthlyRent = price.setScale(0, RoundingMode.HALF_UP);
        return ContractDraftResponse.builder()
                .title("HỢP ĐỒNG THUÊ CHỖ Ở KÝ TÚC XÁ")
                .organizationNote("Bên A: " + landlordProperties.getName()
                        + ", đại diện " + landlordProperties.getRepresentative() + ".")
                .landlord(new ContractLandlordResponse(landlordProperties))
                .introduction("Hai bên thống nhất giao kết hợp đồng thuê chỗ ở ký túc xá với các điều khoản sau:")
                .student(studentService.getMyProfile(user.getId()))
                .room(room)
                .durationMonths(ContractService.RENTAL_DURATION_MONTHS)
                .monthlyRent(monthlyRent)
                .depositAmount(monthlyRent)
                .sections(SECTIONS)
                .signatureNote("BÊN B (Ký và ghi rõ họ tên)                         ĐẠI DIỆN BÊN A (Ký và ghi rõ họ tên)")
                .build();
    }

    // Nội dung Điều 1-6 theo mẫu Hop_dong_KTX_DormHub.pdf; ngày dự thảo chỉ là dự kiến.
    private static final List<ContractDraftResponse.Section> SECTIONS = List.of(
            new ContractDraftResponse.Section("Điều 1. Nội dung hợp đồng", List.of(
                    "1.1. Bên A bố trí cho Bên B một chỗ ở tại ký túc xá theo hồ sơ đăng ký đã được phê duyệt trên hệ thống DormHub.",
                    "1.2. Thông tin tòa nhà, phòng, tầng, loại phòng và sức chứa được thể hiện tại phần thông tin phòng của bản dự thảo này.",
                    "1.3. Bên B sử dụng chỗ ở đúng mục đích, không tự ý chuyển phòng hoặc chuyển giao quyền sử dụng chỗ ở cho người khác. Việc thay đổi chỗ ở phải được Bên A chấp thuận.")),
            new ContractDraftResponse.Section("Điều 2. Thời hạn, giá thuê và phương thức thanh toán", List.of(
                    "2.1. Ngày bắt đầu thuê là ngày nhận phòng do Bên B chọn và xác nhận sau khi đóng cọc. Thời hạn hiển thị trên hợp đồng PENDING chỉ là dự kiến và sẽ được cập nhật khi Bên B xác nhận. Nếu bắt đầu từ ngày 1 đến 15, hợp đồng kết thúc cuối tháng thứ 12; nếu bắt đầu từ ngày 16 trở đi, tháng đầu là kỳ lẻ, sau đó là 12 tháng trọn vẹn.",
                    "2.2. Tiền thuê theo tháng được thể hiện tại phần thông tin thuê của bản dự thảo này.",
                    "2.3. Tiền đặt cọc được thể hiện tại phần thông tin thuê của bản dự thảo này.",
                    "2.4. Bên B thanh toán tiền đặt cọc trong 48 giờ kể từ khi đơn được duyệt. Tiền đặt cọc được đối soát khi trả phòng, sau khi xác định các khoản phải thanh toán và bồi thường (nếu có) theo thỏa thuận của hai bên.",
                    "2.5. Tiền thuê tháng đầu được tính từ ngày bắt đầu thuê ghi trên hợp đồng, bao gồm ngày đó: tiền thuê tháng × số ngày thuê trong tháng / số ngày thực tế của tháng, làm tròn đến đồng Việt Nam. Các tháng trọn vẹn tính đủ tiền thuê tháng. Tiền điện và nước được tính riêng theo mức tiêu thụ thực tế và đơn giá áp dụng.",
                    "2.6. Bên B thanh toán theo số tiền, nội dung chuyển khoản và hạn thanh toán ghi trên từng hóa đơn. Hệ thống hỗ trợ thanh toán chuyển khoản qua mã VietQR và ghi nhận kết quả sau khi giao dịch được xác nhận. Bên B có thể tra cứu hóa đơn và lịch sử thanh toán trên DormHub.",
                    "2.7. Việc điều chỉnh giá thuê, xử lý công nợ, phân bổ chi phí sử dụng chung và hoàn trả tiền đặt cọc thực hiện theo thỏa thuận và quy định được Bên A công bố, không tự động thay đổi đơn giá đã cam kết.",
                    "2.8. Sau khi đóng cọc, Bên B chọn và xác nhận ngày nhận phòng trong vòng 7 ngày tính từ ngày thanh toán. Nếu quá hạn do Bên B, Bên A xem xét hủy suất và xử lý tiền cọc sau khi kiểm tra nguyên nhân. Lỗi bàn giao của ký túc xá hoặc lỗi hệ thống không được coi là lỗi của Bên B.")),
            new ContractDraftResponse.Section("Điều 3. Gia hạn, chuyển phòng và chấm dứt hợp đồng", List.of(
                    "3.1. Khi có nhu cầu tiếp tục lưu trú, Bên B gửi yêu cầu gia hạn trên DormHub để Bên A xem xét. Việc gửi yêu cầu không đồng nghĩa với việc gia hạn đã được chấp thuận.",
                    "3.2. Khi có nhu cầu chuyển phòng, Bên B gửi yêu cầu chuyển phòng. Việc chuyển phòng chỉ được thực hiện sau khi được phê duyệt và hoàn thành thủ tục bàn giao theo hướng dẫn của Bên A.",
                    "3.3. Hợp đồng chấm dứt khi hết thời hạn mà không được gia hạn, khi hai bên thống nhất chấm dứt trước hạn, hoặc trong trường hợp khác theo nội quy, thỏa thuận và pháp luật áp dụng.",
                    "3.4. Bên B gửi yêu cầu trả phòng trên DormHub, phối hợp kiểm tra hiện trạng tài sản, bàn giao chỗ ở và đối soát các hóa đơn còn phải thanh toán. Bên A xác nhận kết quả trả phòng và xử lý tiền đặt cọc theo quy định đã thỏa thuận.",
                    "3.5. Các khoản hoàn trả, khấu trừ và thời hạn giải quyết khi chấm dứt hợp đồng được xác định theo chính sách của đơn vị quản lý và thỏa thuận hợp pháp giữa hai bên.")),
            new ContractDraftResponse.Section("Điều 4. Trách nhiệm của Bên B", List.of(
                    "4.1. Ở đúng phòng được bố trí; tuân thủ nội quy ký túc xá, quy định về an ninh, an toàn, phòng cháy chữa cháy và vệ sinh chung.",
                    "4.2. Giữ gìn tài sản được bàn giao, tự bảo quản tài sản cá nhân; không tự ý thay đổi kết cấu hoặc thiết bị của phòng.",
                    "4.3. Thông báo hư hỏng thông qua chức năng báo hỏng trên DormHub hoặc trực tiếp cho Bên A khi cần xử lý khẩn cấp. Chịu trách nhiệm đối với thiệt hại do mình gây ra theo quy định và kết quả xác minh.",
                    "4.4. Thanh toán đầy đủ, đúng hạn các hóa đơn; kiểm tra thông báo và lịch sử thanh toán để kịp thời phản ánh sai lệch.",
                    "4.5. Cung cấp thông tin chính xác, bảo mật tài khoản DormHub và phối hợp thực hiện thủ tục nhận phòng, gia hạn, chuyển phòng, trả phòng.")),
            new ContractDraftResponse.Section("Điều 5. Trách nhiệm của Bên A", List.of(
                    "5.1. Xem xét hồ sơ đăng ký, bố trí và bàn giao chỗ ở theo hợp đồng; cung cấp các trang thiết bị, dịch vụ theo điều kiện thực tế đã công bố.",
                    "5.2. Ghi nhận chỉ số điện, nước; lập hóa đơn, thông báo hạn thanh toán và lưu lịch sử giao dịch để phục vụ đối soát.",
                    "5.3. Tiếp nhận và xử lý yêu cầu báo hỏng, gia hạn, chuyển phòng, trả phòng; thông báo kết quả xử lý cho Bên B.",
                    "5.4. Hướng dẫn thủ tục lưu trú theo quy định; phối hợp bảo đảm an ninh, vệ sinh và bảo trì cơ sở vật chất.",
                    "5.5. Thực hiện đối soát công nợ và tiền đặt cọc khi Bên B hoàn tất thủ tục trả phòng; thông báo rõ các khoản khấu trừ, nếu có.")),
            new ContractDraftResponse.Section("Điều 6. Điều khoản chung", List.of(
                    "6.1. Hai bên cam kết thực hiện các nội dung đã thỏa thuận. Nội quy ký túc xá và phụ lục được hai bên chấp thuận là bộ phận của hợp đồng.",
                    "6.2. Mọi sửa đổi, bổ sung phải được hai bên thống nhất. Dữ liệu trên DormHub phục vụ quản lý, tra cứu và đối soát; không mặc nhiên thay thế chữ ký hợp lệ của các bên.",
                    "6.3. Hợp đồng được lập thành 02 bản có giá trị như nhau, mỗi bên giữ 01 bản.")));

    public List<ContractDraftResponse.Section> getOfficialSections() {
        return SECTIONS.stream()
                .map(section -> new ContractDraftResponse.Section(section.getHeading(),
                        section.getParagraphs().stream().map(this::officialParagraph).toList()))
                .toList();
    }

    private String officialParagraph(String paragraph) {
        if (paragraph.startsWith("1.2.")) {
            return "1.2. Thông tin tòa nhà, phòng, tầng, loại phòng và sức chứa được thể hiện tại phần thông tin chỗ ở của hợp đồng này.";
        }
        if (paragraph.startsWith("2.1.")) {
            return "2.1. Ngày bắt đầu và ngày kết thúc thuê được thể hiện tại phần thông tin thuê của hợp đồng này. Nếu bắt đầu từ ngày 1 đến 15, tháng đầu là tháng thứ nhất và hợp đồng kết thúc cuối tháng thứ 12. Nếu bắt đầu từ ngày 16 trở đi, tháng đầu là kỳ lẻ bổ sung, sau đó là 12 tháng trọn vẹn.";
        }
        if (paragraph.startsWith("2.2.")) {
            return "2.2. Tiền thuê theo tháng được thể hiện tại phần thông tin thuê của hợp đồng này.";
        }
        if (paragraph.startsWith("2.3.")) {
            return "2.3. Tiền đặt cọc được thể hiện tại phần thông tin thuê của hợp đồng này.";
        }
        return paragraph;
    }
}
