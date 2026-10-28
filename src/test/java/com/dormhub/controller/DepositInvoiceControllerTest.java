package com.dormhub.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dormhub.entity.Contract;
import com.dormhub.entity.ContractStatus;
import com.dormhub.entity.Invoice;
import com.dormhub.entity.InvoiceDetail;
import com.dormhub.entity.InvoiceItemType;
import com.dormhub.entity.Room;
import com.dormhub.dto.request.InvoiceCreateRequest;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.InvoiceRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.service.InvoiceService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DepositInvoiceControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private RoomRepository rooms;
    @Autowired private ContractRepository contracts;
    @Autowired private InvoiceRepository invoices;
    @Autowired private InvoiceService service;

    @Test
    void depositPostIsUnavailableWithOrWithoutContractAndCreatesNothing() throws Exception {
        Contract contract = saveContract();
        for (String body : new String[] {
                "{\"invoiceType\":\"DEPOSIT\",\"contractId\":" + contract.getContractId() + "}",
                "{\"invoiceType\":\"DEPOSIT\"}"
        }) {
            mvc.perform(post("/api/v1/invoices").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isNotImplemented())
                    .andExpect(content().string(containsString("DEPOSIT invoice creation is not supported in this phase")))
                    .andExpect(jsonPath("$.detail").value(
                            "DEPOSIT invoice creation is not supported in this phase"));
        }
        assertEquals(0, invoices.count());
    }

    @Test
    void serviceAlsoBlocksDepositCreation() {
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setInvoiceType("DEPOSIT");
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(request));
        assertEquals(501, error.getStatusCode().value());
        assertEquals(0, invoices.count());
    }

    @Test
    void existingDepositRemainsReadableInListAndDetail() throws Exception {
        Contract contract = saveContract();
        BigDecimal amount = new BigDecimal("1250000");
        Invoice deposit = Invoice.deposit(contract, LocalDateTime.of(2026, 10, 28, 9, 0), amount);
        deposit.addDetail(new InvoiceDetail(InvoiceItemType.DEPOSIT, "Deposit",
                BigDecimal.ONE, amount, amount));
        Long id = invoices.saveAndFlush(deposit).getInvoiceId();

        mvc.perform(get("/api/v1/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].invoiceType").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[0].totalAmount").value(1250000));
        mvc.perform(get("/api/v1/invoices/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractId").value(contract.getContractId()))
                .andExpect(jsonPath("$.billingMonth").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.dueDate").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.overdue").value(false))
                .andExpect(jsonPath("$.details[0].itemType").value("DEPOSIT"))
                .andExpect(jsonPath("$.details[0].amount").value(1250000));
    }

    private Contract saveContract() {
        Room room = rooms.saveAndFlush(new Room());
        return contracts.saveAndFlush(new Contract(room,
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 12, 31, 23, 59),
                new BigDecimal("1000000"), new BigDecimal("1250000"), ContractStatus.ACTIVE));
    }
}
