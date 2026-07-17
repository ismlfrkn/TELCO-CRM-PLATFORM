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

/**
 * FR-21: "Aylik bill-run job'u ... fatura keser" - dokumanin kendi kabul kriterinde (bolum 14.2)
 * bu job'un MANUEL tetiklendigi acikca belirtiliyor, bu yuzden zamanlanmis (scheduled) bir gorev
 * kurulmadi; bu endpoint tam olarak o manuel tetiklemeyi temsil eder. Her satir kendi icinde
 * idempotent oldugu (InvoiceService.createInvoice) icin bill-run yanlislikla iki kez calistirilirsa
 * da cift fatura olusmaz.
 */
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

    /**
     * FR-21 otomatik mod: "hangi aboneye fatura kesilecek" ve "aylik ucret ne kadar" artik cagirandan
     * elle alinmiyor - subscription-service'ten TUM ACTIVE abonelikler sayfa sayfa cekilir, her biri
     * icin aylik ucret satiri kurulur. Asim hesaplamasi zaten InvoiceService.processNewInvoice icinde
     * tariffCode doluysa otomatik calisir (FR-22), burada ayrica tetiklemeye gerek yok.
     */
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

    /**
     * FR-08: abonelik bir tarife versiyonuna pinliyse (subscription-service artik PaymentCompleted
     * tuketirken bunu dolduruyor, bkz. subscription-service SagaEventConsumerConfig), aylik ucret
     * satiri o DONMUS versiyondan kurulur - tarife o abonelikten sonra fiyatlandirilmis olsa bile
     * bu abone hala eski fiyatta kalir. tariffVersion null ise (bu duzeltmeden ONCE olusmus eski
     * abonelikler, ya da REST ile elle olusturulmus test verisi) geriye donuk uyumluluk icin guncel
     * tarifeye dusulur - eski davranis aynen sürer.
     */
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
