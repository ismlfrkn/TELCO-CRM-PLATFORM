package com.turkcell.billing.repository;

import com.turkcell.billing.entity.UsageAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UsageAggregateRepository extends JpaRepository<UsageAggregate, UUID> {

    boolean existsBySourceEventId(UUID sourceEventId);

    // invoiceId IS NULL zaten tek basina dogru "henuz faturalanmadi" korumasi (bkz. UsageAggregate
    // javadoc'u) - periodStart/periodEnd'i de bill-run'in cagirandan gelen (ve genellikle bu
    // aboneligin kendi Quota donemiyle hicbir iliskisi olmayan) degerleriyle birebir eslestirmek
    // sadece kirilgan, sessizce basarisiz olan bir ek sart ekliyordu (bkz. InvoiceService).
    List<UsageAggregate> findBySubscriptionIdAndInvoiceIdIsNull(UUID subscriptionId);
}
