package com.turkcell.billing.service;

import com.turkcell.billing.client.PageResponse;
import com.turkcell.billing.client.ProductCatalogServiceClient;
import com.turkcell.billing.client.SubscriptionClientDto;
import com.turkcell.billing.client.SubscriptionServiceClient;
import com.turkcell.billing.client.TariffClientDto;
import com.turkcell.billing.client.TariffVersionClientDto;
import com.turkcell.billing.dto.request.BillingRunAutoRequest;
import com.turkcell.billing.dto.request.BillingRunRequest;
import com.turkcell.billing.dto.request.InvoiceCreateRequest;
import com.turkcell.billing.dto.request.InvoiceLineRequest;
import com.turkcell.billing.dto.response.BillingRunResponse;
import com.turkcell.billing.dto.response.InvoiceResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class BillingRunService {

    private static final int PAGE_SIZE = 100;

    private final InvoiceService invoiceService;
    private final SubscriptionServiceClient subscriptionServiceClient;
    private final ProductCatalogServiceClient productCatalogServiceClient;

    public BillingRunService(InvoiceService invoiceService, SubscriptionServiceClient subscriptionServiceClient,
                              ProductCatalogServiceClient productCatalogServiceClient) {
        this.invoiceService = invoiceService;
        this.subscriptionServiceClient = subscriptionServiceClient;
        this.productCatalogServiceClient = productCatalogServiceClient;
    }

    public BillingRunResponse run(BillingRunRequest request) {
        List<InvoiceResponse> invoices = new ArrayList<>();
        for (InvoiceCreateRequest invoiceRequest : request.getInvoices()) {
            invoices.add(invoiceService.createInvoice(invoiceRequest));
        }

        BillingRunResponse response = new BillingRunResponse();
        response.setRequested(request.getInvoices().size());
        response.setProcessed(invoices.size());
        response.setInvoices(invoices);
        return response;
    }

    public BillingRunResponse runAutomatic(BillingRunAutoRequest request) {
        List<InvoiceResponse> invoices = new ArrayList<>();
        List<SubscriptionClientDto> activeSubscriptions = fetchAllActiveSubscriptions();

        for (SubscriptionClientDto subscription : activeSubscriptions) {
            invoices.add(invoiceService.createInvoice(buildInvoiceRequest(subscription, request)));
        }

        BillingRunResponse response = new BillingRunResponse();
        response.setRequested(activeSubscriptions.size());
        response.setProcessed(invoices.size());
        response.setInvoices(invoices);
        return response;
    }

    private List<SubscriptionClientDto> fetchAllActiveSubscriptions() {
        List<SubscriptionClientDto> all = new ArrayList<>();
        int page = 0;
        while (true) {
            PageResponse<SubscriptionClientDto> pageResponse =
                    subscriptionServiceClient.getActiveSubscriptions(PageRequest.of(page, PAGE_SIZE));
            all.addAll(pageResponse.getContent());
            if (pageResponse.isLast() || pageResponse.getContent().isEmpty()) {
                break;
            }
            page++;
        }
        return all;
    }

    private InvoiceCreateRequest buildInvoiceRequest(SubscriptionClientDto subscription, BillingRunAutoRequest request) {
        String name;
        BigDecimal monthlyFee;
        String currency;

        if (subscription.getTariffVersion() != null) {
            TariffVersionClientDto tariffVersion = productCatalogServiceClient.getTariffVersion(
                    subscription.getTariffCode(), subscription.getTariffVersion());
            name = tariffVersion.getName();
            monthlyFee = tariffVersion.getMonthlyFee();
            currency = tariffVersion.getCurrency();
        } else {
            TariffClientDto tariff = productCatalogServiceClient.getTariff(subscription.getTariffCode());
            name = tariff.getName();
            monthlyFee = tariff.getMonthlyFee();
            currency = tariff.getCurrency();
        }

        InvoiceCreateRequest invoiceRequest = new InvoiceCreateRequest();
        invoiceRequest.setCustomerId(subscription.getCustomerId());
        invoiceRequest.setSubscriptionId(subscription.getId());
        invoiceRequest.setTariffCode(subscription.getTariffCode());
        invoiceRequest.setPeriodStart(request.getPeriodStart());
        invoiceRequest.setPeriodEnd(request.getPeriodEnd());
        invoiceRequest.setDueDate(request.getDueDate());
        invoiceRequest.setCurrency(currency);

        InvoiceLineRequest monthlyFeeLine = new InvoiceLineRequest();
        monthlyFeeLine.setDescription(name + " aylik ucret");
        monthlyFeeLine.setQuantity(BigDecimal.ONE);
        monthlyFeeLine.setUnitPrice(monthlyFee);
        invoiceRequest.setLines(List.of(monthlyFeeLine));

        return invoiceRequest;
    }
}
