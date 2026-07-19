package com.turkcell.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockNotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationDispatcher.class);

    public void dispatch(String sourceTopic, String eventType, String aggregateId) {
        log.info("[MOCK SMS] source={} eventType={} aggregateId={} -> \"Merhaba, {} bildirimi alindi.\"",
                sourceTopic, eventType, aggregateId, eventType);
        log.info("[MOCK EMAIL] source={} eventType={} aggregateId={} -> gonderim simule edildi.",
                sourceTopic, eventType, aggregateId);
    }

    public void dispatchQuotaThresholdWarning(String sourceTopic, String subscriptionId) {
        log.info("[MOCK SMS] source={} subscriptionId={} -> \"Kullaniminizin %80'ine ulastiniz, kalan kotanizi kontrol edin.\"",
                sourceTopic, subscriptionId);
    }

    public void dispatchQuotaExceededWithAddonSuggestion(String sourceTopic, String subscriptionId) {
        log.info("[MOCK SMS] source={} subscriptionId={} -> \"Kotaniz doldu, kesintisiz kullanim icin ek paket satin alabilirsiniz.\"",
                sourceTopic, subscriptionId);
    }
}
