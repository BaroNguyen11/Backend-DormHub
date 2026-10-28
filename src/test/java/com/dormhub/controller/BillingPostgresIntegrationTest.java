package com.dormhub.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dormhub.entity.Contract;
import com.dormhub.entity.ContractStatus;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.InvoiceDetail;
import com.dormhub.entity.InvoiceItemType;
import com.dormhub.entity.InvoiceType;
import com.dormhub.entity.MeterReading;
import com.dormhub.entity.ReadingStatus;
import com.dormhub.entity.Room;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.MeterReadingRepository;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Runs only when an isolated PostgreSQL test database is configured. */
@EnabledIfEnvironmentVariable(named = "DORMHUB_TEST_DB_URL", matches = ".*/[^/]*(dev|test)[^/]*")
@EnabledIfEnvironmentVariable(named = "DORMHUB_TEST_DB_USER", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DORMHUB_TEST_DB_PASSWORD", matches = ".+")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BillingPostgresIntegrationTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("DORMHUB_TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> System.getenv("DORMHUB_TEST_DB_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("DORMHUB_TEST_DB_PASSWORD"));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired MockMvc mvc;
    @Autowired RoomRepository rooms;
    @Autowired ContractRepository contracts;
    @Autowired MeterReadingRepository readings;
    @Autowired InvoiceRepository invoices;

    @Test
    void fourRoutesRoundTripAgainstPostgres() throws Exception {
        Room room = rooms.saveAndFlush(new Room());
        Contract contract = contracts.saveAndFlush(new Contract(room,
                LocalDateTime.of(2099, 1, 1, 0, 0), LocalDateTime.of(2099, 12, 31, 23, 59),
                new BigDecimal("1500000"), new BigDecimal("500000"), ContractStatus.ACTIVE));
        readings.saveAndFlush(new MeterReading(room, YearMonth.of(2099, 10),
                new BigDecimal("100"), new BigDecimal("120"),
                new BigDecimal("10"), new BigDecimal("13"),
                new BigDecimal("3500"), new BigDecimal("12000"), ReadingStatus.CONFIRMED,
                LocalDateTime.of(2099, 10, 31, 12, 0)));

        mvc.perform(get("/api/v1/room-monthly-charges")
                .param("roomId", room.getRoomId().toString()).param("month", "2099-10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(106000));
        String utility = "{\"invoiceType\":\"UTILITY\",\"roomId\":" + room.getRoomId()
                + ",\"billingMonth\":\"2099-10\"}";
        mvc.perform(post("/api/v1/invoices").contentType(MediaType.APPLICATION_JSON).content(utility))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.totalAmount").value(106000));
        String roomFee = "{\"invoiceType\":\"ROOM_FEE\",\"contractId\":" + contract.getContractId()
                + ",\"billingMonth\":\"2099-10\"}";
        mvc.perform(post("/api/v1/invoices").contentType(MediaType.APPLICATION_JSON).content(roomFee))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.totalAmount").value(1500000));
        String deposit = "{\"invoiceType\":\"DEPOSIT\",\"contractId\":" + contract.getContractId() + "}";
        mvc.perform(post("/api/v1/invoices").contentType(MediaType.APPLICATION_JSON).content(deposit))
                .andExpect(status().isNotImplemented());
        Invoice existingDeposit = Invoice.deposit(contract, LocalDateTime.of(2099, 10, 31, 12, 0),
                new BigDecimal("500000"));
        existingDeposit.addDetail(new InvoiceDetail(InvoiceItemType.DEPOSIT, "Deposit", BigDecimal.ONE,
                new BigDecimal("500000"), new BigDecimal("500000")));
        Long depositId = invoices.saveAndFlush(existingDeposit).getInvoiceId();
        mvc.perform(get("/api/v1/invoices").param("month", "2099-10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        Long invoiceId = invoices.findAll().stream()
                .filter(invoice -> invoice.getInvoiceType() == InvoiceType.ROOM_FEE
                        && invoice.getContract() != null
                        && invoice.getContract().getContractId().equals(contract.getContractId()))
                .findFirst().orElseThrow().getInvoiceId();
        mvc.perform(get("/api/v1/invoices/{id}", invoiceId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.details[0].itemType").value("ROOM"));
        mvc.perform(get("/api/v1/invoices/{id}", depositId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.details[0].itemType").value("DEPOSIT"));
        mvc.perform(post("/api/v1/invoices").contentType(MediaType.APPLICATION_JSON).content(utility))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/invoices").contentType(MediaType.APPLICATION_JSON).content(deposit))
                .andExpect(status().isNotImplemented());
    }
}
