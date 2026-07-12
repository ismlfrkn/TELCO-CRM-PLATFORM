package com.turkcell.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * FR-28/FR-29 ile ayni kapsam disi birakma (bkz. NotificationService dogum yorumu): gercek bir
 * SMS/e-posta saglayicisi yok, gonderim sadece loglanir. Diger domain servislerinden gelen Kafka
 * event'leri icin, gercek bir Notification/Template/Channel eslesmesi kurmak yerine (henuz her event
 * turu icin sablon seed'i yok) dogrudan mock bir SMS/e-posta log'u uretilir.
 */
@Component
public class MockNotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationDispatcher.class);

    public void dispatch(String sourceTopic, String eventType, String aggregateId) {
        log.info("[MOCK SMS] source={} eventType={} aggregateId={} -> \"Merhaba, {} bildirimi alindi.\"",
                sourceTopic, eventType, aggregateId, eventType);
        log.info("[MOCK EMAIL] source={} eventType={} aggregateId={} -> gonderim simule edildi.",
                sourceTopic, eventType, aggregateId);
    }

    /**
     * FR-19: kota %80 esigine ulasildiginda uyari SMS'i (CLAUDE.md 14.1 Senaryo 3, ilk adim).
     */
    public void dispatchQuotaThresholdWarning(String sourceTopic, String subscriptionId) {
        log.info("[MOCK SMS] source={} subscriptionId={} -> \"Kullaniminizin %80'ine ulastiniz, kalan kotanizi kontrol edin.\"",
                sourceTopic, subscriptionId);
    }

    /**
     * FR-19: kota %100 asildiginda ek paket onerili SMS'i (CLAUDE.md 14.1 Senaryo 3, ikinci adim).
     */
    public void dispatchQuotaExceededWithAddonSuggestion(String sourceTopic, String subscriptionId) {
        log.info("[MOCK SMS] source={} subscriptionId={} -> \"Kotaniz doldu, kesintisiz kullanim icin ek paket satin alabilirsiniz.\"",
                sourceTopic, subscriptionId);
    }
}
