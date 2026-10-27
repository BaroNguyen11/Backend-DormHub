package com.dormhub.service;

import com.dormhub.dto.request.InvoiceCreateRequest;
import com.dormhub.dto.response.InvoicePageResponse;
import com.dormhub.dto.response.InvoiceResponse;

public interface InvoiceService {

    InvoiceResponse create(InvoiceCreateRequest request);

    InvoicePageResponse list(String month, int page, int size);

    InvoiceResponse get(Long invoiceId);
}
