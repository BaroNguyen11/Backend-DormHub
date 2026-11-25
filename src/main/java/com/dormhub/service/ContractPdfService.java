package com.dormhub.service;

import com.dormhub.config.ContractLandlordProperties;
import com.dormhub.dto.response.ContractDraftResponse;
import com.dormhub.entity.Building;
import com.dormhub.entity.Contract;
import com.dormhub.entity.Room;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DepositStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractPdfService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    private final ContractService contractService;
    private final ContractDraftService draftService;
    private final ContractLandlordProperties landlord;

    public byte[] renderForStudent(Long userId, Long contractId) throws IOException {
        Contract contract = contractService.getMyContractEntity(userId, contractId);
        if (contract.getStatus() == ContractStatus.DRAFT) {
            throw new AppException(ErrorCode.CONTRACT_NOT_FOUND);
        }
        Student student = contract.getStudent();
        User user = student.getUser();
        Room room = contract.getRoom();
        Building building = room.getBuilding();

        try (PDDocument document = new PDDocument();
             InputStream regularStream = font("NotoSans-Regular.ttf");
             InputStream boldStream = font("NotoSans-Bold.ttf")) {
            PDType0Font regular = PDType0Font.load(document, regularStream, true);
            PDType0Font bold = PDType0Font.load(document, boldStream, true);
            document.getDocumentInformation().setTitle("Hợp đồng thuê chỗ ở ký túc xá " + contractId);
            document.getDocumentInformation().setAuthor(landlord.getName());

            try (Layout layout = new Layout(document, regular, bold)) {
                layout.center("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM", 10, true);
                layout.center("Độc lập - Tự do - Hạnh phúc", 10, true);
                layout.space(18);
                layout.center("HỢP ĐỒNG THUÊ CHỖ Ở KÝ TÚC XÁ", 15, true);
                layout.center("Số: " + contract.getContractId(), 10, false);
                layout.center(contract.getStatus() == ContractStatus.PENDING
                        ? "HỢP ĐỒNG CHỜ XÁC NHẬN THỜI HẠN THUÊ"
                        : "HỢP ĐỒNG ĐÃ XÁC NHẬN THỜI HẠN THUÊ", 9, true);
                layout.space(16);

                layout.heading("BÊN A - ĐƠN VỊ QUẢN LÝ KÝ TÚC XÁ (BÊN CHO THUÊ)");
                layout.field("Tên đơn vị", landlord.getName());
                layout.field("Người đại diện", landlord.getRepresentative());
                layout.field("Chức vụ", landlord.getPosition());
                layout.field("Địa chỉ", landlord.getAddress());
                layout.field("Số điện thoại", landlord.getPhone());
                layout.field("Email", landlord.getEmail());

                layout.heading("BÊN B - SINH VIÊN (BÊN THUÊ)");
                layout.field("Họ và tên", first(student.getFullName(), user == null ? null : user.getFullName()));
                layout.field("Mã sinh viên", student.getStudentCode());
                layout.field("CCCD", student.getCic());
                layout.field("Giới tính", student.getGender() == null ? null : switch (student.getGender()) {
                    case MALE -> "Nam";
                    case FEMALE -> "Nữ";
                    default -> student.getGender().name();
                });
                layout.field("Ngày sinh", date(student.getDateOfBirth()));
                layout.field("Số điện thoại", user == null ? null : user.getPhone());
                layout.field("Địa chỉ", student.getAddress());
                layout.field("Email", first(user == null ? null : user.getEmail(), student.getEmail()));

                layout.heading("THÔNG TIN CHỖ Ở VÀ GIÁ THUÊ");
                layout.field("Tòa nhà", building.getName() + " (" + building.getBuildingCode() + ")");
                layout.field("Địa chỉ tòa nhà", building.getAddress());
                layout.field("Phòng", room.getRoomNumber());
                layout.field("Tầng", String.valueOf(room.getFloor()));
                layout.field("Loại phòng", room.getRoomType() == null ? null : switch (room.getRoomType()) {
                    case SINGLE -> "Phòng đơn";
                    case DOUBLE -> "Phòng đôi";
                    case QUAD -> "Phòng 4 người";
                    case SIX_BED -> "Phòng 6 người";
                    case EIGHT_BED -> "Phòng 8 người";
                });
                layout.field("Sức chứa", room.getCapacity() + " sinh viên");
                layout.field(contract.getStatus() == ContractStatus.PENDING
                        ? "Ngày bắt đầu dự kiến" : "Ngày bắt đầu", date(contract.getStartDate()));
                layout.field(contract.getStatus() == ContractStatus.PENDING
                        ? "Ngày kết thúc dự kiến" : "Ngày kết thúc", date(contract.getEndDate()));
                layout.field("Tiền thuê mỗi tháng", money(contract.getMonthlyRent()));
                layout.field("Tiền đặt cọc", money(contract.getDepositAmount()));
                layout.field("Trạng thái hợp đồng", switch (contract.getStatus()) {
                    case PENDING -> contract.getDepositStatus() == DepositStatus.HELD
                            ? "Đã đóng cọc, chờ xác nhận ngày nhận phòng"
                            : "Chờ thanh toán tiền cọc";
                    case ACTIVE -> "Đang hiệu lực";
                    default -> contract.getStatus().name();
                });
                layout.space(10);
                layout.paragraph("Hai bên thống nhất giao kết hợp đồng thuê chỗ ở ký túc xá với các điều khoản sau:");

                List<ContractDraftResponse.Section> sections = draftService.getOfficialSections();
                for (ContractDraftResponse.Section section : sections) {
                    layout.heading(section.getHeading());
                    for (String paragraph : section.getParagraphs()) {
                        layout.paragraph(paragraph);
                    }
                }

                layout.space(24);
                layout.signatures(landlord.getRepresentative());
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    private static InputStream font(String name) throws IOException {
        InputStream stream = ContractPdfService.class.getResourceAsStream("/fonts/" + name);
        if (stream == null) throw new IOException("Missing bundled PDF font: " + name);
        return stream;
    }

    private static String first(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private static String date(LocalDate value) {
        return value == null ? null : value.format(DATE);
    }

    private static String date(LocalDateTime value) {
        return value == null ? null : value.format(DATE);
    }

    private static String money(BigDecimal amount) {
        return amount == null ? null : NumberFormat.getNumberInstance(VIETNAMESE).format(amount) + " đồng";
    }

    private static final class Layout implements AutoCloseable {
        private static final float MARGIN = 54;
        private static final float BOTTOM = 65;
        private static final float WIDTH = PDRectangle.A4.getWidth() - 2 * MARGIN;
        private final PDDocument document;
        private final PDType0Font regular;
        private final PDType0Font bold;
        private PDPageContentStream stream;
        private float y;

        private Layout(PDDocument document, PDType0Font regular, PDType0Font bold) throws IOException {
            this.document = document;
            this.regular = regular;
            this.bold = bold;
            nextPage();
        }

        private void nextPage() throws IOException {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private void ensure(float height) throws IOException {
            if (y - height < BOTTOM) nextPage();
        }

        private void space(float height) throws IOException {
            ensure(height);
            y -= height;
        }

        private void center(String text, float size, boolean strong) throws IOException {
            PDType0Font font = strong ? bold : regular;
            ensure(size + 8);
            float width = font.getStringWidth(text) * size / 1000;
            draw(text, font, size, Math.max(MARGIN, (PDRectangle.A4.getWidth() - width) / 2));
            y -= size + 6;
        }

        private void heading(String text) throws IOException {
            ensure(60);
            space(8);
            for (String line : wrap(text, bold, 10.5f)) {
                ensure(17);
                draw(line, bold, 10.5f, MARGIN);
                y -= 17;
            }
            space(3);
        }

        private void field(String label, String value) throws IOException {
            paragraph(label + ": " + (value == null || value.isBlank() ? "Chưa cập nhật" : value));
        }

        private void signatures(String representative) throws IOException {
            ensure(90);
            draw("BÊN B", bold, 10, MARGIN + 70);
            draw("ĐẠI DIỆN BÊN A", bold, 10, MARGIN + 320);
            y -= 15;
            draw("(Ký và ghi rõ họ tên)", regular, 9, MARGIN + 44);
            draw("(Ký và ghi rõ họ tên)", regular, 9, MARGIN + 300);
            y -= 60;
            draw(representative, bold, 10, MARGIN + 320);
            y -= 15;
        }

        private void paragraph(String text) throws IOException {
            for (String line : wrap(text, regular, 10)) {
                ensure(15);
                draw(line, regular, 10, MARGIN);
                y -= 15;
            }
            space(4);
        }

        private List<String> wrap(String text, PDType0Font font, float size) throws IOException {
            java.util.ArrayList<String> lines = new java.util.ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : text.split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && font.getStringWidth(candidate) * size / 1000 > WIDTH) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            if (!line.isEmpty()) lines.add(line.toString());
            return lines;
        }

        private void draw(String text, PDType0Font font, float size, float x) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, y);
            stream.showText(text);
            stream.endText();
        }

        @Override
        public void close() throws IOException {
            if (stream != null) stream.close();
        }
    }
}
