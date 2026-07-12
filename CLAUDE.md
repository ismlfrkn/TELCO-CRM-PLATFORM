# TelcoX CRM Platform — Proje Bağlam Prompt'u

> Bu doküman, "Telco CRM Platform" adlı microservices eğitim projesinin tam bağlamını içerir. Bir AI asistanına (Claude, ChatGPT, Claude Code vb.) bu projeyle ilgili kod yazdırırken, mimari karar aldırırken, doküman ürettirirken ya da soru sorarken bu dosyayı context/system prompt olarak ver. Böylece projeyi baştan anlatmana gerek kalmaz.

## Nasıl kullanılır
Bu dosyanın tamamını yeni bir konuşmanın başına yapıştır veya dosya olarak yükle, ardından şunun gibi bir talimatla başla: *"Aşağıda TelcoX CRM Platform projesinin tam teknik ve iş bağlamı var. Bundan sonra vereceğim görevleri (kod yazma, servis tasarımı, doküman üretme, hata ayıklama vb.) bu bağlama sadık kalarak yap. Bağlamla çelişen bir öneri yapman gerekirse önce belirt."*

---

## 1. Proje Vizyonu

Telco CRM Platform, bir GSM operatörünün ("TelcoX") abonelerine yönelik tüm yaşam döngüsü süreçlerini — müşteri kaydı, ürün siparişi, faturalandırma, kullanım takibi, müşteri destek — tek bir microservices ekosistemi üzerinden yöneten, ölçeklenebilir, event-driven bir CRM platformudur.

**İş senaryosu:** TelcoX, mevcut monolit CRM sistemini parça parça mikroservislere taşımak istiyor. MVP kapsamında hedef: abone yaşam döngüsünün uçtan uca dijitalleştirilmesi, fatura üretiminin otomatize edilmesi, self-servis kanalların (mobil/web — backend + Swagger UI seviyesinde) açılması.

**Eğitim hedefleri:** DDD ile bounded context çıkarımı; Spring Boot 3 ile production-grade microservices; Spring Cloud (Config, Gateway, Discovery) ile servis topolojisi; Apache Kafka ile event-driven entegrasyon; REST + OpenAPI ile senkron sözleşme yönetimi; database-per-service pattern (PostgreSQL); Redis ile cache-aside ve idempotency; Docker Compose (lokal) + Kubernetes (production); distributed tracing (OpenTelemetry/Zipkin) ve merkezi loglama; JWT + OAuth2 ile gateway seviyesinde güvenlik; Resilience4j ile circuit breaker/retry/bulkhead; CI/CD (GitHub Actions/GitLab CI).

---

## 2. Domain Sözlüğü (Telekom Terimleri)

| Terim | Tanım |
|---|---|
| MSISDN | Abonenin telefon numarası — sistemde unique identifier. |
| IMSI | SIM kart üzerindeki benzersiz kimlik. |
| ICCID | SIM kartın seri numarası. |
| Subscription | Müşterinin bir tarife/pakete olan aktif aboneliği. |
| Tariff/Plan | Dakika/SMS/GB paketi; postpaid ya da prepaid. |
| VAS | Value Added Services — ek servisler (caller tune, cloud vb.). |
| CDR | Call Detail Record — her arama/SMS/data kullanım kaydı; faturalamanın temeli. |
| Top-up | Prepaid hatlara bakiye yükleme. |
| MNP | Mobile Number Portability — numara taşıma. |
| BSCS/OCS | Gerçek operatörlerde kullanılan billing & charging motoru. |
| KYC | Know Your Customer — kimlik doğrulama süreci. |
| KVKK/GDPR | Kişisel veri koruma regülasyonları (Türkiye/AB). |
| Bounded Context | DDD'de bir modelin geçerli olduğu açıkça tanımlı sınır. |
| Saga | Distributed transaction'ları kompansasyon adımlarıyla yöneten pattern. |
| Outbox Pattern | DB transaction + message publish'i atomik yapan tablo bazlı çözüm. |
| Idempotency | Aynı işlemin birden fazla kez yapılmasının sonucu değiştirmemesi. |
| Circuit Breaker | Hata oranı eşiği aşılınca çağrıları otomatik kesen pattern. |
| CQRS | Command (yazma) ve Query (okuma) sorumluluklarının ayrılması. |
| Service Mesh | Servisler arası iletişimi yöneten infra katmanı (Istio vb.). |
| HPA | Horizontal Pod Autoscaler — K8s yatay ölçeklendirme. |

**Event Storming çıktısı (domain event'leri):** CustomerRegistered, CustomerKYCApproved, CustomerKYCRejected, MSISDNAllocated, MSISDNReleased, OrderCreated, OrderConfirmed, OrderCancelled, SubscriptionActivated, SubscriptionSuspended, SubscriptionTerminated, TariffChanged, AddonPurchased, UsageRecorded, QuotaThresholdReached, QuotaExceeded, InvoiceGenerated, PaymentReceived, PaymentFailed, TicketOpened, TicketAssigned, TicketResolved, NotificationDispatched.

---

## 3. Aktörler

| Aktör | Rolü | Tipik İşlemleri |
|---|---|---|
| Müşteri (Subscriber) | Son kullanıcı | Kayıt, sipariş, fatura görüntüleme, top-up, paket değişikliği, talep açma |
| Çağrı Merkezi Temsilcisi | Support agent | Ticket çözme, abone bilgisi görme, manuel paket değiştirme |
| Saha Bayisi | Dealer | Yeni abone aktivasyonu, SIM satışı, KYC ekran girişi |
| Pazarlama Yöneticisi | Marketing | Kampanya tanımlama, segment çıkarma |
| Sistem Yöneticisi | Admin | Tarife/ürün katalog yönetimi, kullanıcı yetkilendirme |
| Fatura Operatörü | Billing operator | Bill-run job izleme, fatura iptali |
| Sistem (Internal) | Service-to-service | Event publish/consume, scheduled job, CDR mediation |

---

## 4. Fonksiyonel Gereksinimler (FR)

### 4.1 Customer Service
- FR-01: Bireysel/kurumsal müşteri kaydı (TCKN/VKN doğrulamalı).
- FR-02: KYC sonrası müşteri durumu PENDING → ACTIVE/REJECTED.
- FR-03: Adres, iletişim, kimlik belgesi yönetimi.
- FR-04: Silme işlemi soft-delete (KVKK/GDPR).

### 4.2 Product Catalog Service
- FR-05: Tarife/paket/addon/VAS hiyerarşik yönetim.
- FR-06: Ürünlerde effectiveFrom/effectiveTo ve hedef segment.
- FR-07: Postpaid/prepaid/hybrid sınıflandırma.
- FR-08: Tarife değişiklikleri versiyonlanır, eski aboneler eski tarifede kalır.

### 4.3 Order Service
- FR-09: Yeni hat, paket değişikliği, addon siparişi.
- FR-10: Siparişler saga pattern ile çoklu servis koordinasyonu.
- FR-11: Sipariş durumları: DRAFT, PENDING_PAYMENT, PAID, FULFILLED, CANCELLED.
- FR-12: İptalde kompansasyon event'leri tetiklenir.

### 4.4 Subscription Service
- FR-13: Sipariş tamamlanınca abonelik otomatik aktive olur.
- FR-14: Askıya alma, yeniden aktivasyon, sonlandırma desteklenir.
- FR-15: Bir müşterinin birden fazla aboneliği olabilir.
- FR-16: MNP için ayrı state machine.

### 4.5 Usage Service
- FR-17: CDR akışı Kafka'dan tüketilir, kullanım bakiyeleri güncellenir.
- FR-18: Kalan kota (dakika/SMS/MB) anlık görülür.
- FR-19: %80/%100 eşiklerinde notification event'i üretilir.
- FR-20: Aşım kullanımları billing'e agregate edilir.

### 4.6 Billing Service
- FR-21: Aylık bill-run job'u tüm postpaid aboneler için fatura keser.
- FR-22: Fatura kalemleri: aylık ücret, addon, aşım, VAS, vergiler.
- FR-23: Fatura PDF üretilip Notification'a gönderilir.
- FR-24: Ödeme alınınca InvoicePaid event'i üretilir.

### 4.7 Payment Service
- FR-25: Kredi kartı, banka transferi, cüzdan desteği.
- FR-26: İdempotent çalışır (paymentRequestId ile).
- FR-27: Başarısız ödemelerde 24/72/168 saat retry.

### 4.8 Notification Service
- FR-28: SMS, e-posta, push kanalları.
- FR-29: Şablonlu (template) bildirim yönetimi.
- FR-30: Opt-in/opt-out tercihlerine saygı.

### 4.9 Ticket Service
- FR-31: Şikayet/talep/arıza kaydı.
- FR-32: SLA bazlı otomatik ekip ataması.
- FR-33: Ticket açıldığında bildirim gider.

---

## 5. Fonksiyonel Olmayan Gereksinimler (NFR)

| Kategori | Gereksinim | Hedef |
|---|---|---|
| Performans | API yanıt süresi (p95) | < 300 ms |
| Performans | Bill-run job süresi | 100K abone < 30 dk |
| Ölçeklenebilirlik | Yatay ölçeklenebilirlik | Stateless servisler, K8s HPA |
| Erişilebilirlik | Servis uptime | %99.5 (MVP) |
| Güvenlik | Auth | OAuth2/JWT, gateway seviyesinde |
| Güvenlik | Veri | PII şifreli (TCKN, kart no) |
| Gözlemlenebilirlik | Tracing | OpenTelemetry + Zipkin/Jaeger |
| Gözlemlenebilirlik | Loglama | JSON yapılandırılmış, merkezi (ELK/Loki) |
| Gözlemlenebilirlik | Metrikler | Prometheus + Grafana |
| Dayanıklılık | Circuit breaker | Resilience4j, tüm dış çağrılarda |
| Veri Tutarlılığı | Model | Eventual consistency (Outbox pattern) |
| Uyumluluk | Regülasyon | KVKK/GDPR, audit log zorunlu |

---

## 6. MVP Kapsamı

**Scope IN:** Bireysel müşteri kaydı ve KYC; postpaid tarife siparişi ve aktivasyon; aylık faturalama (sabit ücret + aşım); kredi kartı ödeme (mock PSP); SMS/e-posta bildirim (mock kanal); kota görüntüleme ve eşik bildirimleri; temel ticketing; admin panel ürün katalog CRUD.

**Scope OUT (MVP sonrası):** Prepaid top-up ve gerçek zamanlı charging; MNP süreci; kurumsal müşteri/filo yönetimi; kampanya/promosyon motoru; BTK regülasyon raporları; roaming takibi; mobil uygulama (sadece backend + Swagger UI var).

---

## 7. Microservices Mimarisi

| Servis | Port | Bounded Context | Temel Agregatlar |
|---|---|---|---|
| api-gateway | 8080 | Edge routing | — |
| discovery-server | 8761 | Service registry | — |
| config-server | 8888 | Centralized config | — |
| identity-service | 9001 | Kimlik & yetki | User, Role, Permission |
| customer-service | 9002 | Müşteri yönetimi | Customer, Address, Document |
| product-catalog-service | 9003 | Ürün kataloğu | Tariff, Addon, ProductOffering |
| order-service | 9004 | Sipariş orkestrasyonu | Order, OrderItem, SagaState |
| subscription-service | 9005 | Abonelik yaşam döngüsü | Subscription, MSISDN, SimCard |
| usage-service | 9006 | Kullanım & kota | UsageRecord, Quota, CdrEvent |
| billing-service | 9007 | Fatura üretimi | Invoice, InvoiceLine, BillCycle |
| payment-service | 9008 | Ödeme | Payment, PaymentAttempt, Wallet |
| notification-service | 9009 | Bildirim | Notification, Template, Channel |
| ticket-service | 9010 | Müşteri talepleri | Ticket, Comment, SLA |

Her servis kendi PostgreSQL şemasına sahiptir (database-per-service).

**Altyapı bileşenleri:** PostgreSQL (servis başına şema/DB), Apache Kafka (event broker), Redis (cache + rate limiting + idempotency key), Keycloak (opsiyonel, OAuth2/OIDC), MinIO/local FS (PDF/belge depolama), Zipkin + ELK + Prometheus + Grafana (observability).

**Mantıksal akış:** Client → API Gateway (JWT validation, rate limit, routing) → Discovery/Config Server aracılığıyla domain servislerine (identity, customer, catalog, order, subscription, usage) REST yönlendirme → bu servisler Kafka Bus üzerinden billing/payment/notification/ticket (ve gelecekte analytics) servisleriyle asenkron haberleşir.

### Servis Detayları (Sorumluluk / API / Event)

- **customer-service**: Müşteri kimlik & iletişim master kaydı. API: POST/GET/PUT `/api/v1/customers`, POST `/customers/{id}/documents`, POST `/customers/{id}/kyc/approve`. Publish: CustomerRegistered, CustomerKYCApproved, CustomerUpdated.
- **product-catalog-service**: Tarife/addon/VAS master katalog, read-heavy + Redis cache. API: GET `/tariffs`, GET `/tariffs/{code}`, POST `/tariffs` (admin), GET `/addons?tariffCode=`. Publish: TariffCreated, TariffPriceChanged.
- **order-service**: Sipariş alma + Saga orkestrasyonu (Customer→Catalog→Subscription→Payment). API: POST `/orders`, GET `/orders/{id}`, POST `/orders/{id}/cancel`. Publish: OrderCreated, OrderConfirmed, OrderCancelled. Consume: PaymentCompleted, PaymentFailed, SubscriptionActivated.
- **subscription-service**: Abonelik state machine, MSISDN allocation/release. API: POST `/subscriptions` (internal), GET `/subscriptions/{id}`, POST `/subscriptions/{id}/suspend|reactivate|terminate`. Publish: SubscriptionActivated, SubscriptionSuspended, SubscriptionTerminated. Consume: OrderConfirmed, PaymentFailed (grace period sonrası).
- **usage-service**: CDR event tüketimi, kullanım sayaçları, write-heavy. API: GET `/usage/subscriptions/{id}/quota`, GET `/usage/subscriptions/{id}/history`. Consume: CdrRecorded. Publish: QuotaThresholdReached, QuotaExceeded.
- **billing-service**: Aylık bill-run scheduler, fatura üretimi. API: GET `/invoices`, GET `/invoices/{id}`, GET `/invoices/{id}/pdf`, POST `/billing/runs` (admin). Publish: InvoiceGenerated, InvoicePaid, InvoiceOverdue. Consume: UsageAggregated, SubscriptionActivated, PaymentCompleted.
- **payment-service**: Ödeme alma, mock PSP entegrasyonu. API: POST `/payments`, GET `/payments/{id}`, POST `/payments/{id}/refund`. Publish: PaymentCompleted, PaymentFailed, PaymentRefunded. Consume: InvoiceGenerated (auto-pay).
- **notification-service**: Çok kanallı bildirim. API: POST `/notifications` (internal), GET `/notifications/users/{id}/history`. Consume: neredeyse tüm domain event'leri (template eşleştirmeli).
- **ticket-service**: Destek talebi/SLA. API: POST `/tickets`, GET `/tickets/{id}`, POST `/tickets/{id}/comments|assign|resolve`. Publish: TicketOpened, TicketResolved, SlaBreached.

---

## 8. Servisler Arası İletişim

### 8.1 Senkron vs Asenkron Kararları

| Senaryo | Tip | Gerekçe |
|---|---|---|
| Order → Customer kontrolü | Senkron (REST) | Anlık doğrulama gerekli |
| Order → Catalog fiyat | Senkron (REST+cache) | Fiyat snapshot alınmalı |
| Order → Subscription aktivasyonu | Asenkron (Kafka) | Geri alınabilir, eventual consistency |
| Subscription → Billing | Asenkron (Kafka) | Loose coupling |
| CDR → Usage | Asenkron (Kafka) | Yüksek hacim, geriye dönük işlenebilir |
| Invoice → Notification | Asenkron (Kafka) | Notification fail etse de fatura geçerli kalır |
| Payment doğrulama | Senkron (PSP REST) | Müşteriye anlık geri dönüş gerekli |

**Genel kural:** Sonraki adımı doğrudan belirleyen/anlık cevap gereken işlemler → senkron. Geri planda tamamlanabilen, yüksek hacimli veya diğer servisleri kilitlememesi gereken işlemler → asenkron.

### 8.2 Saga Örneği: Yeni Hat Siparişi (choreography-based)

1. Müşteri → `POST /orders` → Order Service
2. Order Service: kaydeder → publish `OrderCreated`
3. Payment Service: consume `OrderCreated` → charge attempt → publish `PaymentCompleted`
4. Subscription Service: consume `PaymentCompleted` → MSISDN allocate → Subscription oluştur → publish `SubscriptionActivated`
5. Order Service: consume `SubscriptionActivated` → order FULFILLED
6. Notification Service: consume `SubscriptionActivated` → welcome SMS

**Kompansasyon (başarısızlık):** SubscriptionActivation fail → Subscription Service: `SubscriptionActivationFailed` → Payment Service: refund tetiklenir → Order Service: order CANCELLED.

### 8.3 Outbox Pattern (Zorunlu)

DB yazma + Kafka publish atomik olmalı. Her serviste outbox tablosu tutulur; ayrı bir publisher worker bu tabloyu tarar ve Kafka'ya gönderir. **Transactional outbox + idempotent consumer** kombinasyonu MVP'de zorunludur (en-az-bir-kez teslim garantisi + tüketici tarafında tekrar işlememe garantisi).

---

## 9. Veri Modeli (Yüksek Seviye)

- **Customer**: `Customer(id, type[INDIVIDUAL|CORPORATE], firstName, lastName, identityNumber, dateOfBirth, status, createdAt)`, `Address(id, customerId, line1, city, district, postalCode, isDefault)`, `Document(id, customerId, type[ID_CARD|PASSPORT], fileRef, verifiedAt)`
- **Product Catalog**: `Tariff(id, code, name, type[POSTPAID|PREPAID], monthlyFee, minutesIncluded, smsIncluded, dataMbIncluded, status, effectiveFrom, effectiveTo)`, `Addon(id, code, name, price, type[DATA|SMS|MINUTES|VAS], validityDays)`, `TariffAddon(tariffId, addonId)` (M:N)
- **Order**: `Order(id, customerId, status, totalAmount, currency, createdAt)`, `OrderItem(id, orderId, productCode, productType, quantity, unitPrice)`, `SagaState(id, orderId, currentStep, payload, lastUpdated)`
- **Subscription**: `Subscription(id, customerId, msisdn, tariffCode, status[ACTIVE|SUSPENDED|TERMINATED], activatedAt, terminatedAt)`, `MsisdnPool(msisdn, status[FREE|RESERVED|ALLOCATED], reservedUntil)`, `SimCard(iccid, imsi, msisdn, status)`
- **Usage**: `Quota(id, subscriptionId, periodStart, periodEnd, minutesRemaining, smsRemaining, mbRemaining)`, `UsageRecord(id, subscriptionId, type[VOICE|SMS|DATA], quantity, recordedAt, cdrRef)`
- **Billing**: `Invoice(id, customerId, subscriptionId, periodStart, periodEnd, subTotal, tax, grandTotal, status, dueDate, issuedAt)`, `InvoiceLine(id, invoiceId, description, quantity, unitPrice, lineTotal)`, `BillCycle(id, customerId, dayOfMonth, nextRunDate)`
- **Payment**: `Payment(id, invoiceId, amount, method, status, externalRef, paidAt)`, `PaymentAttempt(id, paymentId, attemptNo, response, attemptedAt)`
- **Notification**: `NotificationTemplate(id, code, channel, locale, subject, bodyTemplate)`, `Notification(id, userId, templateCode, channel, payloadJson, status, sentAt)`
- **Ticket**: `Ticket(id, customerId, category, priority, status, slaDueAt, createdAt)`, `TicketComment(id, ticketId, authorId, body, createdAt)`

---

## 10. Teknoloji Yığını

| Katman | Teknoloji | Sürüm/Not |
|---|---|---|
| Dil | Java | 21 (LTS) |
| Framework | Spring Boot | 3.3.x |
| Spring Cloud | Gateway, Config, Eureka/Consul, OpenFeign | 2023.0.x |
| Build | Maven | Multi-module önerilir |
| DB | PostgreSQL | 16, servis başına ayrı schema |
| Cache | Redis | 7 |
| Broker | Apache Kafka | 3.7+ (KRaft mode) |
| Messaging Abstraction | Spring Cloud Stream (Kafka binder) | `spring-cloud-starter-stream-kafka` — outbox publisher'lar `StreamBridge` ile, consumer'lar fonksiyonel `Consumer<T>` bean'leriyle yazılır; `KafkaTemplate`/`@KafkaListener` kullanılmaz. Binding ayarları `application.yml`'de `spring.cloud.stream.bindings.*` altında tanımlanır. |
| Migration | Flyway | Her serviste |
| ORM | Spring Data JPA + Hibernate | — |
| Mapping | MapStruct | — |
| Validation | Jakarta Bean Validation | — |
| Auth | Spring Security + JWT (jjwt 0.12.x) | Gateway'de relay |
| Doc | Springdoc OpenAPI | Her servis ayrı Swagger UI |
| Resilience | Resilience4j | Circuit breaker, retry, bulkhead |
| Observability | Micrometer + OpenTelemetry + Zipkin | — |
| Test | JUnit 5, Mockito, Testcontainers, RestAssured | — |
| Container | Docker, Docker Compose | Lokal geliştirme |
| Orchestration | Kubernetes | Minikube/Kind ile lokal |
| CI/CD | GitHub Actions | Build → test → docker push → kubectl apply |

---

## 11. API Tasarım Standartları

- Tüm REST API'ler `/api/v1` prefix kullanır (URI bazlı versiyonlama).
- Resource isimleri çoğul (`customers`, `orders`, `subscriptions`).
- HTTP method semantiği: GET (read), POST (create/komut), PUT (full update), PATCH (partial), DELETE (soft delete).
- Hata formatı RFC 7807 Problem Details standardına uyar.
- Pagination: `?page=0&size=20&sort=createdAt,desc` (Spring Data Pageable).
- `Idempotency-Key` header POST işlemlerinde desteklenir (özellikle Payment, Order).
- `Correlation-Id` header gateway tarafından enjekte edilir, tüm servis loglarına yazılır.
- Tüm tarihler ISO-8601 UTC.
- Para alanları `BigDecimal` + ayrı currency code (TRY).

**Örnek hata cevabı (RFC 7807):**
```json
{
  "type": "https://telco.example/errors/customer-not-found",
  "title": "Customer not found",
  "status": 404,
  "detail": "Customer with id 1234 does not exist",
  "instance": "/api/v1/customers/1234",
  "correlationId": "9f3c1b..."
}
```

---

## 12. Güvenlik Mimarisi

- Auth: identity-service login üzerinden JWT (access + refresh) üretir.
- API Gateway her isteği JWT ile doğrular, `X-User-Id`/`X-User-Roles` header'ları olarak downstream'e iletir.
- Servisler JWT'yi tekrar doğrulamaz; gateway'e güvenilir (production'da mTLS önerilir, MVP'de scope dışı).
- Refresh token rotation: her refresh sonrası eski token Redis blacklist'e eklenir; reuse tespit edilirse tüm tokenlar iptal edilir.
- Yetkilendirme `@PreAuthorize` ile role/permission bazlı, özellikle admin endpoint'lerinde.
- PII şifreleme: TCKN ve kart no AES-GCM ile şifrelenir, key Vault/K8s Secret'tan okunur.
- Audit log: identity, customer, payment, subscription servislerinde her değişiklik `audit_log` tablosuna yazılır.
- Rate limit: Gateway'de Redis tabanlı, kullanıcı başına varsayılan 100 req/min.

---

## 13. Kabul Kriterleri (MVP)

**Senaryo 1 — Yeni Abone Onboarding:** Müşteri başvurusu (`POST /customers`) → KYC belgesi yüklenir ve onaylanır → postpaid tarife siparişi → mock PSP ile ödeme başarılı → subscription otomatik aktive olur, MSISDN atanır → welcome SMS (mock log) gider.

**Senaryo 2 — Aylık Fatura:** Bill-run job manuel tetiklenir → aktif abonelerin son 1 aylık usage'i agregate edilir → her abone için invoice + PDF üretilir → InvoiceGenerated ile notification e-posta atar → müşteri öderse InvoicePaid tetiklenir.

**Senaryo 3 — Kota Aşımı:** CDR simulator usage event üretir → usage service kotaları azaltır → %80'de uyarı SMS'i → %100'de ek paket önerili SMS → aşım sonrası kullanım billing'e overage olarak gider.

---

## 14. AI Asistanı İçin Çalışma Kuralları

Bu bağlamı kullanan bir AI, aşağıdaki prensiplere bağlı kalmalı:

1. **Scope'a sadık kal.** Bölüm 6'daki Scope OUT listesindeki özellikleri (prepaid top-up, MNP, kurumsal müşteri, kampanya motoru, BTK raporları, roaming, mobil uygulama) MVP'de önerme/implemente etme; sorulmadıkça bunlara girme.
2. **Database-per-service kuralına uy.** Bir servis başka bir servisin veritabanına doğrudan erişmemeli; sadece REST veya event üzerinden konuşmalı.
3. **Sync/async ayrımını Bölüm 8.1'deki tabloya göre yap.** Yeni bir entegrasyon önerirken hangi tipin uygun olduğunu bu tablodaki mantıkla gerekçelendir.
4. **Kritik akışlarda (Payment, Order) idempotency ve outbox pattern'i atlama.** Bu iki desen MVP'de zorunlu.
5. **API tasarımında Bölüm 11'deki standartlara (URI versiyonlama, RFC 7807 hata formatı, pagination, header'lar) uy.**
6. **Güvenlik kararlarında Bölüm 12'yi referans al** (gateway'de JWT doğrulama, servislerde tekrar doğrulama yapılmaz, PII şifreleme zorunlu).
7. **Yeni kod/servis üretirken Bölüm 10'daki teknoloji yığınının dışına çıkma** (ör. farklı bir ORM, farklı bir mesaj broker önerme).
8. **Belirsizlik durumunda varsayım yapmadan önce hangi bölümle çeliştiğini belirt** ve kullanıcıya sor.


---

## 15. Geliştirme Komutları

Yukarıdaki bölümler (1-14) **hedef mimariyi** tanımlar. Bu bölüm ve bir sonraki bölüm, reponun **şu an gerçekte nasıl çalıştığını** anlatır — ikisi her zaman birebir örtüşmez, aşağıdaki Bölüm 16'daki sapmalara dikkat et.

**Ortam değişkenleri (.env):**
`configs/` altındaki tüm sırlar (DB kullanıcı/parola, `gateway.internal-secret`, `jwt.secret`, `pii.encryption-key`) artık düz metin değil, `${VAR}` placeholder'ı olarak tutuluyor — gerçek değerler repo kökündeki `.env` dosyasından (gitignore'da, commit edilmez) okunur. İlk kurulumda:
```bash
cp .env.example .env   # PowerShell: Copy-Item .env.example .env
```
`docker compose` bu dosyayı otomatik okur, ekstra adım gerekmez. Ama bir servisi `mvn spring-boot:run` ile **doğrudan host üzerinde** çalıştırmadan önce `.env`'i o terminal oturumuna yüklemen gerekir — yoksa config-server'ın ilettiği placeholder çözülemez ve servis "Could not resolve placeholder" hatasıyla başlamaz:
```powershell
. .\scripts\load-env.ps1
```
```bash
source scripts/load-env.sh
```
Bu yükleme her yeni terminal/servis için ayrı ayrı gerekir (env değişkenleri sadece o oturuma özeldir).

**Build:**
```bash
mvn clean install          # tüm modülleri derle + test et (root'tan)
mvn -pl <modul> -am compile   # tek modülü (ve bağımlılıklarını) derle, örn: mvn -pl customer-service -am compile
```
Root'ta `mvnw` yok — sadece `discovery-server`, `config-server`, `api-gateway` kendi wrapper'larına sahip (Spring Initializr artığı). Diğer 10 modülde sistemde kurulu global `mvn` kullanılır.

**Test:**
```bash
mvn test                                   # tüm modüllerin testleri
mvn -pl <modul> test                       # tek modülün testleri
mvn -pl <modul> test -Dtest=ClassName      # tek bir test sınıfı
mvn -pl <modul> test -Dtest=ClassName#methodName   # tek bir test metodu
```
Concurrency/idempotency testleri (`*ConcurrencyTest.java` — payment, order, subscription, usage, billing modüllerinde) Testcontainers ile gerçek bir Postgres container'ı ayağa kaldırır; bunlar için Docker'ın çalışıyor olması gerekir.

**Altyapı (Docker):**
```bash
docker compose up -d          # postgres, pgadmin, kafka, redis
docker exec -it telco-postgres psql -U telco_admin -l   # servis başına DB'lerin oluştuğunu doğrula
```
pgAdmin (`localhost:5050`) container'dan bağlanırken host olarak `postgres` (container adı) + port `5432` kullanır — `localhost:5433` sadece host makineden (senin bilgisayarından) bağlanırken geçerlidir.

**Servisleri ayağa kaldırma sırası** (ABOUT.md'de detaylı anlatılıyor):
1. `discovery-server` (`:8761`) — Eureka, diğer her şey buna register olur.
2. `config-server` (`:8888`) — `configs/` klasörünü GitHub'dan okur; diğer servisler config'lerini buradan çeker.
3. `api-gateway` (`:8080`) — JWT doğrulama + routing.
4. Herhangi bir domain servisi, örn: `mvn -pl customer-service -am spring-boot:run` (önce o terminalde `.env` yüklenmiş olmalı, yukarıya bak).

Her `spring-boot:run` komutu terminali bloke eder — her servis ayrı bir terminalde çalıştırılmalı.

**Servis portları:**

| Servis | Port | | Servis | Port |
|---|---|---|---|---|
| discovery-server | 8761 | | subscription-service | 9005 |
| config-server | 8888 | | usage-service | 9006 |
| api-gateway | 8080 | | billing-service | 9007 |
| identity-service | 9001 | | payment-service | 9008 |
| customer-service | 9002 | | notification-service | 9009 |
| product-catalog-service | 9003 | | ticket-service | 9010 |
| order-service | 9011 | | | |

---

## 16. Mevcut Kod Mimarisi ve Spesifikasyondan Bilinçli Sapmalar

**Ortak katman deseni (her serviste tekrarlanır, paylaşılan modül yok):** Root `pom.xml`'in `<modules>` listesinde 13 servis var, ama bir `common`/`shared` modülü **yok**. `OutboxEventService`, `AuditLogService`, `CorrelationIdFilter`, `GlobalExceptionHandler` gibi cross-cutting sınıflar her serviste kendi paketi altında (`com.turkcell.<servis>.service`/`.config`/`.exception`) **kopyala-yapıştır** olarak duruyor. Bu sınıflardan birinde bug bulursan, aynı bug'ın kardeş servislerde de tekrarlanmış olup olmadığını kontrol et.

**Standart paket yapısı** (yeni servis/özellik eklerken bunu takip et — `customer-service` ve `product-catalog-service` referans alınabilir):
```
entity/ · repository/ · service/ · controller/ · mapper/ (MapStruct)
dto/request/ · dto/response/ · exception/ · validation/ · config/
src/main/resources/db/migration/  (Flyway, V1__..., V2__...)
```
Hata yönetimi her serviste aynı: `GlobalExceptionHandler` + RFC 7807 `ProblemDetail` + `CorrelationIdFilter` (MDC `correlationId`). `ddl-auto: validate` — şemayı Hibernate değil Flyway yönetir.

**Auth gerçekten çalışıyor (Bölüm 12 ile uyumlu):** `identity-service` JWT üretiyor (`JwtTokenProvider`, jjwt/HMAC, access 15dk + refresh 1 gün). `api-gateway`'deki `JwtAuthenticationGlobalFilter` her isteği doğrulayıp `X-User-Id`/`X-User-Roles` + paylaşılan bir internal secret header'ı (`GatewayTrustProperties`) enjekte ediyor; downstream servisler JWT'yi tekrar doğrulamıyor, sadece bu gateway header'larına ve secret'a güveniyor (spec'teki "gateway'e güven" modeliyle birebir).

**Outbox pattern kısmen implemente (Bölüm 8.3'ten sapma):** Her serviste `OutboxEventService.publish()` var ama sadece `outbox_events` tablosuna satır yazıyor — **Kafka'ya gönderen bir publisher worker yok**, hiçbir modülün `pom.xml`'inde `spring-kafka` bağımlılığı yok. `docker-compose.yml`'deki Kafka container'ı şu an uygulama kodu tarafından hiç kullanılmıyor. Servisler arası event akışının gerçekten çalıştığını **varsayma** — outbox tablosuna yazılıyor, o kadar.

**Saga orkestrasyonu senkron (Bölüm 8.2'den sapma):** Spec, Order→Payment→Subscription akışını Kafka choreography ile tanımlıyor. Gerçek kod (`order-service`'teki `OrderService.executeSaga`) bunu tek bir istek/cevap döngüsünde **senkron REST/OpenFeign zinciri** olarak yapıyor (Payment çağır → başarısızsa iptal + outbox event; Subscription çağır → başarısızsa Payment'e refund çağrısı + iptal). `SagaState` entity'si var ama kullanılmıyor (kullanılmayan iskelet).

**Test yaklaşımı:** Servis mantığı için düz JUnit5 + Mockito + AssertJ (`*ServiceTest.java`). Concurrency/idempotency'nin gerçekten kritik olduğu yollar (payment/order idempotency key'leri, MSISDN havuzu tahsisi, CDR işleme, kota güncelleme, fatura üretimi) için `@SpringBootTest(webEnvironment = NONE)` + Testcontainers `PostgreSQLContainer` + gerçek eşzamanlı thread'lerle (`ExecutorService`/`CountDownLatch`) yarış durumu testi yapılıyor — mock'la simüle edilemeyecek DB-seviyeli garantiler (unique constraint vb.) böyle doğrulanıyor. Yeni idempotent/concurrent mantık eklerken aynı ayrımı kullan.

**Henüz implemente edilmemiş (spec'te var, kodda yok):** Redis cache-aside, OpenAPI/Swagger (springdoc hiçbir serviste yok), Resilience4j circuit breaker, OpenTelemetry/Zipkin tracing, tarife versiyonlama (FR-08), PII alan şifreleme (AES-GCM).
