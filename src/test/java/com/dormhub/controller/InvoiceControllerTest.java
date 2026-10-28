package com.dormhub.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dormhub.entity.Invoice;
import com.dormhub.entity.Contract;
import com.dormhub.entity.ContractStatus;
import com.dormhub.entity.InvoiceItemType;
import com.dormhub.entity.MeterReading;
import com.dormhub.entity.PriceType;
import com.dormhub.entity.Prices;
import com.dormhub.entity.ReadingStatus;
import com.dormhub.entity.Room;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.MeterReadingRepository;
import com.dormhub.repository.PricesRepository;
import com.dormhub.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private MeterReadingRepository meterReadingRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private PricesRepository pricesRepository;

    @Test
    void createsUtilityInvoiceWithSnapshotDetailsAndServerTotal() throws Exception {
        Room room = roomRepository.saveAndFlush(new Room());
        pricesRepository.saveAndFlush(new Prices(
                "water-rate-next-month", PriceType.WATER, new BigDecimal("20000"),
                LocalDate.of(2026, 11, 1), LocalDateTime.of(2026, 10, 31, 12, 0),
                "manager", null));
        meterReadingRepository.saveAndFlush(reading(room, ReadingStatus.CONFIRMED));

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(room.getRoomId(), ",\"totalAmount\":1")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/v1/invoices/\\d+")))
                .andExpect(jsonPath("$.invoiceType").value("UTILITY"))
                .andExpect(jsonPath("$.roomId").value(room.getRoomId()))
                .andExpect(jsonPath("$.billingMonth").value("2026-10"))
                .andExpect(jsonPath("$.totalAmount").value(25000))
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.details[0].itemType").value("ELECTRICITY"))
                .andExpect(jsonPath("$.details[0].quantity").value(2.5))
                .andExpect(jsonPath("$.details[0].unitPrice").value(4000))
                .andExpect(jsonPath("$.details[0].amount").value(10000))
                .andExpect(jsonPath("$.details[1].itemType").value("WATER"))
                .andExpect(jsonPath("$.details[1].unitPrice").value(10000))
                .andExpect(jsonPath("$.details[1].amount").value(15000));

        Invoice persisted = invoiceRepository.findAll().getFirst();
        BigDecimal detailSum = persisted.getDetails().stream()
                .map(detail -> detail.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, persisted.getTotalAmount().compareTo(detailSum));
    }

    @Test
    void listAndDetailReadPersistedInvoice() throws Exception {
        Room room = roomRepository.saveAndFlush(new Room());
        meterReadingRepository.saveAndFlush(reading(room, ReadingStatus.CONFIRMED));
        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(room.getRoomId(), "")))
                .andExpect(status().isCreated());
        Long invoiceId = invoiceRepository.findAll().getFirst().getInvoiceId();

        mockMvc.perform(get("/api/v1/invoices")
                        .param("month", "2026-10").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].invoiceId").value(invoiceId))
                .andExpect(jsonPath("$.content[0].totalAmount").value(25000));

        mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.details.length()").value(2))
                .andExpect(jsonPath("$.totalAmount").value(25000));
    }

    @Test
    void rejectsDuplicateUtilityInvoiceInService() throws Exception {
        Room room = roomRepository.saveAndFlush(new Room());
        meterReadingRepository.saveAndFlush(reading(room, ReadingStatus.CONFIRMED));
        String body = createBody(room.getRoomId(), "");
        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void databaseConstraintAlsoRejectsDuplicateRoomAndMonth() throws Exception {
        Room room = roomRepository.saveAndFlush(new Room());
        meterReadingRepository.saveAndFlush(reading(room, ReadingStatus.CONFIRMED));
        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(room.getRoomId(), "")))
                .andExpect(status().isCreated());

        assertThrows(DataIntegrityViolationException.class, () -> invoiceRepository.saveAndFlush(
                new Invoice(room, YearMonth.of(2026, 10),
                        LocalDateTime.of(2026, 10, 31, 12, 0), BigDecimal.ZERO)));
    }

    @Test
    void preservesFractionalConsumptionInPersistedDetail() throws Exception {
        Room room = roomRepository.saveAndFlush(new Room());
        meterReadingRepository.saveAndFlush(new MeterReading(
                room, YearMonth.of(2026, 10),
                BigDecimal.ZERO, new BigDecimal("0.0000000000002"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("5000000000000"), new BigDecimal("10000"),
                ReadingStatus.CONFIRMED, LocalDateTime.of(2026, 10, 31, 12, 0)));

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(room.getRoomId(), "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(1));

        Invoice invoice = invoiceRepository.findAll().getFirst();
        var electricity = invoice.getDetails().stream()
                .filter(detail -> detail.getItemType() == InvoiceItemType.ELECTRICITY)
                .findFirst().orElseThrow();
        assertEquals(0, electricity.getQuantity().compareTo(new BigDecimal("0.0000000000002")));
        assertEquals(new BigDecimal("1"), electricity.getAmount());
    }

    @Test
    void rejectsMissingReadingAndUnconfirmedReading() throws Exception {
        Room room = roomRepository.saveAndFlush(new Room());
        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(room.getRoomId(), "")))
                .andExpect(status().isNotFound());

        meterReadingRepository.saveAndFlush(reading(room, ReadingStatus.DRAFT));
        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(room.getRoomId(), "")))
                .andExpect(status().isConflict());
    }

    @Test
    void roomFeeRequiresContractId() throws Exception {
        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceType\":\"ROOM_FEE\",\"billingMonth\":\"2026-10\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsRoomFeeFromContractRentAndPreventsDuplicate() throws Exception {
        Room room = roomRepository.saveAndFlush(new Room());
        Contract contract = contractRepository.saveAndFlush(new Contract(room,
                LocalDateTime.of(2026, 10, 15, 0, 0), LocalDateTime.of(2027, 6, 30, 23, 59),
                new BigDecimal("1234567"), ContractStatus.ACTIVE));
        String body = "{\"invoiceType\":\"ROOM_FEE\",\"contractId\":" + contract.getContractId()
                + ",\"billingMonth\":\"2026-10\"}";
        mockMvc.perform(post("/api/v1/invoices").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contractId").value(contract.getContractId()))
                .andExpect(jsonPath("$.roomId").value(room.getRoomId()))
                .andExpect(jsonPath("$.totalAmount").value(1234567))
                .andExpect(jsonPath("$.status").value("UNPAID"))
                .andExpect(jsonPath("$.details[0].itemType").value("ROOM"))
                .andExpect(jsonPath("$.details[0].amount").value(1234567));
        mockMvc.perform(post("/api/v1/invoices").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void databaseConstraintAlsoRejectsDuplicateContractAndMonth() throws Exception {
        Room room = roomRepository.saveAndFlush(new Room());
        Contract contract = contractRepository.saveAndFlush(new Contract(room,
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 12, 31, 23, 59),
                new BigDecimal("1000000"), ContractStatus.ACTIVE));
        invoiceRepository.saveAndFlush(new Invoice(contract, YearMonth.of(2026, 10),
                LocalDateTime.of(2026, 10, 31, 12, 0), new BigDecimal("1000000")));
        assertThrows(DataIntegrityViolationException.class, () -> invoiceRepository.saveAndFlush(
                new Invoice(contract, YearMonth.of(2026, 10),
                        LocalDateTime.of(2026, 10, 31, 12, 0), new BigDecimal("1000000"))));
    }

    @Test
    void rejectsInactiveContractAndMissingContract() throws Exception {
        Room room = roomRepository.saveAndFlush(new Room());
        Contract contract = contractRepository.saveAndFlush(new Contract(room,
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 31, 23, 59),
                new BigDecimal("1000000"), ContractStatus.CANCELLED));
        mockMvc.perform(post("/api/v1/invoices").contentType(MediaType.APPLICATION_JSON)
                .content("{\"invoiceType\":\"ROOM_FEE\",\"contractId\":" + contract.getContractId()
                        + ",\"billingMonth\":\"2026-10\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/invoices").contentType(MediaType.APPLICATION_JSON)
                .content("{\"invoiceType\":\"ROOM_FEE\",\"contractId\":999999,\"billingMonth\":\"2026-10\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void validatesCreateAndReadRequests() throws Exception {
        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceType\":\"\",\"roomId\":1,\"billingMonth\":\"2026-10\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceType\":\"UTILITY\",\"roomId\":1,\"contractId\":2,\"billingMonth\":\"2026-10\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceType\":\"UTILITY\",\"roomId\":0,\"billingMonth\":\"2026-10\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceType\":\"UTILITY\",\"roomId\":1,\"billingMonth\":\"2026-13\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/invoices").param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/invoices/0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/invoices/999999"))
                .andExpect(status().isNotFound());
    }

    private String createBody(Long roomId, String extraField) {
        return "{\"invoiceType\":\"UTILITY\",\"roomId\":" + roomId
                + ",\"billingMonth\":\"2026-10\"" + extraField + "}";
    }

    private MeterReading reading(Room room, ReadingStatus status) {
        return new MeterReading(
                room, YearMonth.of(2026, 10),
                new BigDecimal("10"), new BigDecimal("12.5"),
                new BigDecimal("3"), new BigDecimal("4.5"),
                new BigDecimal("4000"), new BigDecimal("10000"),
                status, LocalDateTime.of(2026, 10, 31, 12, 0));
    }
}
