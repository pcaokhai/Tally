#!/usr/bin/env python3
"""Generates contracts/openapi.yaml (tenant API) and contracts/ops-openapi.yaml (operator API).
The endpoint tables below are the single place to add an endpoint; re-run and commit both outputs."""
import yaml, copy
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def ref(n): return {"$ref": f"#/components/schemas/{n}"}
def page(n): return {"type": "object", "required": ["data", "has_more"], "properties": {
    "data": {"type": "array", "items": ref(n)}, "has_more": {"type": "boolean"},
    "next_cursor": {"type": ["string", "null"]}}}
def obj(req, props): return {"type": "object", "required": req, "properties": props}
def enum(*v): return {"type": "string", "enum": list(v)}
def nts(): return {"type": ["string", "null"], "format": "date-time"}

S = {}
S["Problem"] = obj(["type", "title", "status", "code", "request_id"], {
    "type": {"type": "string", "format": "uri"}, "title": {"type": "string"}, "status": {"type": "integer"},
    "detail": {"type": "string"}, "code": {"type": "string", "examples": ["IDEMPOTENCY_KEY_REUSED"]},
    "param": {"type": ["string", "null"]}, "request_id": {"type": "string"}})
S["Money"] = obj(["amount", "currency"], {
    "amount": {"type": "integer", "format": "int64", "description": "Minor units. Never a float."},
    "currency": {"type": "string", "pattern": "^[a-z]{3}$"}})
S["Timestamp"] = {"type": "string", "format": "date-time"}
TS = ref("Timestamp"); MONEY = ref("Money"); STR = {"type": "string"}; INT = {"type": "integer"}; BOOL = {"type": "boolean"}; NUM = {"type": "number"}
def arr(x): return {"type": "array", "items": x}

S["Me"] = obj(["user", "active_tenant", "memberships"], {"user": obj(["id", "email"], {"id": STR, "email": STR, "name": STR}),
    "active_tenant": obj(["id", "name", "role"], {"id": STR, "name": STR, "role": enum("OWNER", "ADMIN", "DEVELOPER", "FINANCE", "VIEWER")}),
    "memberships": arr(obj(["tenant_id", "tenant_name", "role"], {"tenant_id": STR, "tenant_name": STR, "role": STR})), "livemode": BOOL})
S["ReportSummary"] = obj(["revenue_this_month", "mrr", "outstanding", "failed_payments_7d"], {
    "revenue_this_month": MONEY, "revenue_prev_month": MONEY, "mrr": MONEY, "active_subscriptions": INT,
    "outstanding": MONEY, "outstanding_invoice_count": INT, "overdue": MONEY, "failed_payments_7d": INT})
S["RevenueSeries"] = obj(["range", "points", "total"], {"range": enum("7D", "30D", "12M"), "total": MONEY,
    "points": arr(obj(["date", "amount"], {"date": {"type": "string", "format": "date"}, "amount": INT}))})
S["MrrByPlan"] = obj(["total", "plans"], {"total": MONEY, "plans": arr(obj(["product_id", "name", "mrr"], {"product_id": STR, "name": STR, "mrr": MONEY, "subscription_count": INT}))})
S["AttentionItem"] = obj(["id", "kind", "title", "detail", "action"], {"id": STR, "kind": enum("PAYMENTS_FAILED", "ENDPOINT_DISABLED", "INVOICE_OVERDUE", "TRIAL_ENDING"),
    "title": STR, "detail": STR, "action": obj(["label", "target"], {"label": STR, "target": STR})})
S["Customer"] = obj(["id", "email", "currency", "status", "created_at"], {"id": STR, "email": STR, "name": STR, "currency": STR,
    "status": enum("ACTIVE", "PAST_DUE", "TRIALING", "CHURNED"), "mrr": MONEY, "open_balance": MONEY, "metadata": {"type": "object", "additionalProperties": STR}, "created_at": TS})
S["CustomerCreate"] = obj(["email", "currency"], {"email": {"type": "string", "format": "email"}, "name": STR, "currency": STR, "metadata": {"type": "object", "additionalProperties": STR}})
S["CustomerUpdate"] = obj([], {"name": STR, "email": {"type": "string", "format": "email"}, "metadata": {"type": "object", "additionalProperties": STR}})
S["TimelineEntry"] = obj(["kind", "title", "at"], {"kind": STR, "title": STR, "object_id": STR, "at": TS})
S["PaymentMethod"] = obj(["id", "brand", "last4"], {"id": STR, "type": enum("CARD", "ACH"), "brand": STR, "last4": {"type": "string", "pattern": "^[0-9]{4}$"}, "exp_month": INT, "exp_year": INT})
S["PaymentMethodSetup"] = obj(["setup_token", "processor"], {"setup_token": STR, "processor": enum("STRIPE_TEST", "SIMULATOR"), "expires_at": TS})
S["Product"] = obj(["id", "name", "kind", "active"], {"id": STR, "name": STR, "kind": enum("SEAT", "FLAT", "METERED"), "active": BOOL, "subscription_count": INT})
S["ProductCreate"] = obj(["name", "kind"], {"name": STR, "kind": enum("SEAT", "FLAT", "METERED")})
S["PriceTier"] = obj(["up_to", "unit_amount"], {"up_to": {"type": ["integer", "null"]}, "unit_amount": INT, "flat_amount": INT})
S["Price"] = obj(["id", "product_id", "version", "model", "currency", "status"], {"id": STR, "product_id": STR, "version": INT,
    "model": enum("PER_UNIT", "FLAT", "GRADUATED"), "unit_amount": INT, "currency": STR, "interval": enum("MONTH", "YEAR"),
    "tiers": arr(ref("PriceTier")), "status": enum("CURRENT", "LEGACY", "ARCHIVED"), "subscription_count": INT, "created_at": TS})
S["PriceCreate"] = obj(["model", "currency", "interval"], {"model": enum("PER_UNIT", "FLAT", "GRADUATED"), "unit_amount": INT, "currency": STR, "interval": enum("MONTH", "YEAR"), "tiers": arr(ref("PriceTier"))})
S["SubscriptionItem"] = obj(["price_id", "quantity"], {"id": STR, "price_id": STR, "quantity": {"type": "integer", "minimum": 1}})
S["Subscription"] = obj(["id", "customer_id", "status", "items", "current_period_start", "current_period_end"], {"id": STR, "customer_id": STR,
    "status": enum("TRIALING", "ACTIVE", "PAST_DUE", "CANCELED"), "items": arr(ref("SubscriptionItem")), "mrr": MONEY,
    "current_period_start": TS, "current_period_end": TS, "trial_end": nts(), "cancel_at_period_end": BOOL, "version": INT})
S["SubscriptionCreate"] = obj(["customer_id", "items"], {"customer_id": STR, "items": {"type": "array", "minItems": 1, "items": ref("SubscriptionItem")}, "trial_days": {"type": "integer", "minimum": 0, "maximum": 90}})
S["SubscriptionUpdate"] = obj([], {"items": arr(ref("SubscriptionItem")), "proration": enum("CREATE_PRORATIONS", "NONE")})
S["SubscriptionPreviewRequest"] = obj(["customer_id", "items"], {"customer_id": STR, "subscription_id": {"type": ["string", "null"]}, "items": arr(ref("SubscriptionItem")), "trial_days": INT})
S["SubscriptionPreview"] = obj(["recurring_total", "due_today", "lines", "explanation"], {"recurring_total": MONEY, "due_today": MONEY, "explanation": STR,
    "lines": arr(obj(["description", "amount"], {"description": STR, "amount": MONEY, "proration": BOOL}))})
S["CancelRequest"] = obj(["at"], {"at": enum("NOW", "PERIOD_END")})
S["InvoiceLine"] = obj(["id", "description", "quantity", "amount"], {"id": STR, "description": STR, "quantity": INT, "unit_amount": INT, "amount": MONEY, "price_id": STR, "usage_rollup_id": {"type": ["string", "null"]}})
S["InvoiceLineCreate"] = obj(["description", "quantity", "unit_amount"], {"description": STR, "quantity": {"type": "integer", "minimum": 1}, "unit_amount": INT})
S["Invoice"] = obj(["id", "customer_id", "status", "total", "amount_due", "lines"], {"id": STR, "number": {"type": ["string", "null"]}, "customer_id": STR, "subscription_id": STR,
    "status": enum("DRAFT", "OPEN", "PAID", "VOID", "UNCOLLECTIBLE"), "overdue_days": INT, "total": MONEY, "amount_due": MONEY, "due_date": {"type": "string", "format": "date"},
    "lines": arr(ref("InvoiceLine")), "finalized_at": nts(), "reminded_at": nts(), "version": INT})
S["InvoiceUpdate"] = obj([], {"due_date": {"type": "string", "format": "date"}, "memo": STR})
S["InvoiceSummary"] = obj(["outstanding", "overdue", "paid_this_month", "counts"], {"outstanding": MONEY, "overdue": MONEY, "paid_this_month": MONEY, "counts": {"type": "object", "additionalProperties": INT}})
S["ReminderRequest"] = obj(["invoice_ids"], {"invoice_ids": {"type": "array", "minItems": 1, "maxItems": 100, "items": STR}})
S["ReminderAccepted"] = obj(["accepted", "skipped"], {"accepted": INT, "skipped": arr(obj(["invoice_id", "reason"], {"invoice_id": STR, "reason": STR}))})
S["PaymentAttempt"] = obj(["attempt_no", "outcome", "attempted_at"], {"attempt_no": INT, "outcome": enum("SUCCEEDED", "DECLINED", "ERROR", "TIMEOUT"), "failure_code": {"type": ["string", "null"]}, "attempted_at": TS})
S["Payment"] = obj(["id", "customer_id", "amount", "status", "created_at"], {"id": STR, "customer_id": STR, "invoice_id": {"type": ["string", "null"]}, "amount": MONEY,
    "status": enum("PROCESSING", "SUCCEEDED", "FAILED", "REFUNDED", "PARTIALLY_REFUNDED"), "payment_method": ref("PaymentMethod"),
    "attempts": arr(ref("PaymentAttempt")), "next_retry_at": nts(), "created_at": TS})
S["PaymentCreate"] = obj(["customer_id", "amount", "payment_method_id"], {"customer_id": STR, "invoice_id": STR, "amount": MONEY, "payment_method_id": STR})
S["PaymentSummary"] = obj(["collected_this_month", "success_rate", "refunded_this_month", "failed_count"], {"collected_this_month": MONEY, "success_rate": NUM, "refunded_this_month": MONEY, "failed_count": INT})
S["Refund"] = obj(["id", "payment_id", "amount", "status"], {"id": STR, "payment_id": STR, "amount": MONEY, "reason": STR, "status": enum("SUCCEEDED", "PENDING", "FAILED"), "created_at": TS})
S["RefundCreate"] = obj(["payment_id", "amount", "reason"], {"payment_id": STR, "amount": MONEY, "reason": STR})
S["UsageEvent"] = obj(["event_id", "meter", "customer_id", "quantity", "occurred_at"], {"event_id": {"type": "string", "maxLength": 128}, "meter": STR, "customer_id": STR, "quantity": {"type": "number", "minimum": 0}, "occurred_at": TS})
S["UsageBatch"] = obj(["events"], {"events": {"type": "array", "minItems": 1, "maxItems": 1000, "items": ref("UsageEvent")}})
S["UsageAccepted"] = obj(["accepted", "duplicates"], {"accepted": INT, "duplicates": INT})
S["Meter"] = obj(["id", "key", "aggregation", "unit"], {"id": STR, "key": {"type": "string", "pattern": "^[a-z][a-z0-9_]{1,63}$"}, "aggregation": enum("SUM", "MAX", "UNIQUE_COUNT"), "unit": STR, "late_event_policy": enum("REJECT", "NEXT_PERIOD")})
S["MeterCreate"] = obj(["key", "aggregation", "unit"], {"key": STR, "aggregation": enum("SUM", "MAX", "UNIQUE_COUNT"), "unit": STR, "late_event_policy": enum("REJECT", "NEXT_PERIOD")})
S["UsageSummary"] = obj(["meter", "period_start", "period_end", "actual", "projected", "included"], {"meter": STR, "period_start": TS, "period_end": TS,
    "actual": NUM, "projected": NUM, "included": NUM, "projected_overage_charge": MONEY,
    "series": arr(obj(["date", "cumulative"], {"date": {"type": "string", "format": "date"}, "cumulative": NUM, "projected": BOOL}))})
S["ApiKey"] = obj(["id", "name", "prefix", "last4", "scopes", "mode", "created_at"], {"id": STR, "name": STR, "prefix": STR, "last4": STR, "mode": enum("LIVE", "TEST"),
    "scopes": arr(STR), "last_used_at": nts(), "revoked_at": nts(), "created_at": TS})
S["ApiKeyCreate"] = obj(["name", "scopes", "mode"], {"name": {"type": "string", "minLength": 1, "maxLength": 80}, "mode": enum("LIVE", "TEST"), "scopes": {"type": "array", "minItems": 1, "items": enum("payments:write", "payments:read", "invoices:write", "invoices:read", "customers:write", "customers:read", "usage:write", "webhooks:read")}})
S["ApiKeyCreated"] = {"allOf": [ref("ApiKey"), obj(["secret"], {"secret": {"type": "string", "description": "Returned exactly once (FR-KEY-01)."}})]}
S["WebhookEndpoint"] = obj(["id", "url", "event_types", "status"], {"id": STR, "url": {"type": "string", "format": "uri"}, "event_types": arr(STR),
    "status": enum("ENABLED", "DEGRADED", "CIRCUIT_OPEN", "DISABLED"), "failure_streak": INT, "disabled_at": nts(), "version": INT})
S["WebhookEndpointCreate"] = obj(["url", "event_types"], {"url": {"type": "string", "format": "uri"}, "event_types": {"type": "array", "minItems": 1, "items": STR}})
S["WebhookEndpointUpdate"] = obj([], {"url": {"type": "string", "format": "uri"}, "event_types": arr(STR)})
S["WebhookSecret"] = obj(["secret", "overlap_until"], {"secret": STR, "overlap_until": nts()})
S["WebhookEndpointCreated"] = {"allOf": [ref("WebhookEndpoint"), obj(["secret"], {"secret": STR})]}
S["EndpointHealth"] = obj(["endpoint_id", "status", "success_rate_24h", "hours"], {"endpoint_id": STR, "status": STR, "success_rate_24h": NUM, "p95_ms": INT, "next_probe_at": nts(),
    "hours": {"type": "array", "maxItems": 24, "items": obj(["hour", "delivered", "failed"], {"hour": TS, "delivered": INT, "failed": INT})}})
S["DeliveryAttempt"] = obj(["attempt_no", "attempted_at"], {"attempt_no": INT, "status_code": {"type": ["integer", "null"]}, "latency_ms": INT, "error": {"type": ["string", "null"]}, "attempted_at": TS,
    "timing": obj([], {"dns_ms": INT, "connect_ms": INT, "tls_ms": INT, "wait_ms": INT, "download_ms": INT})})
S["WebhookDelivery"] = obj(["id", "event_id", "event_type", "endpoint_id", "status", "attempts"], {"id": STR, "event_id": STR, "event_type": STR, "endpoint_id": STR,
    "status": enum("DELIVERED", "RETRYING", "FAILED", "HELD", "DEAD_LETTERED"), "attempt_count": INT, "next_attempt_at": nts(),
    "request_headers": {"type": "object", "additionalProperties": STR}, "response_status": {"type": ["integer", "null"]}, "attempts": arr(ref("DeliveryAttempt")), "replay_of": {"type": ["string", "null"]}})
S["Event"] = obj(["id", "type", "api_version", "created_at", "data"], {"id": STR, "type": STR, "api_version": STR, "created_at": TS, "data": {"type": "object"},
    "deliveries": arr(obj(["endpoint_id", "status"], {"endpoint_id": STR, "status": STR}))})
S["RequestLog"] = obj(["ts", "method", "path", "status", "latency_ms"], {"ts": TS, "method": STR, "path": STR, "status": INT, "latency_ms": INT, "api_key_name": {"type": ["string", "null"]}, "request_id": STR})
S["TeamMember"] = obj(["user_id", "email", "role", "status"], {"user_id": STR, "email": STR, "name": STR, "role": enum("OWNER", "ADMIN", "DEVELOPER", "FINANCE", "VIEWER"), "status": enum("ACTIVE", "INVITED"), "mfa": STR})
S["TeamMemberUpdate"] = obj(["role"], {"role": enum("ADMIN", "DEVELOPER", "FINANCE", "VIEWER")})
S["InvitationCreate"] = obj(["email", "role"], {"email": {"type": "string", "format": "email"}, "role": enum("ADMIN", "DEVELOPER", "FINANCE", "VIEWER")})
S["Invitation"] = obj(["id", "email", "role", "expires_at"], {"id": STR, "email": STR, "role": STR, "expires_at": TS})
S["Branding"] = obj(["accent_hex"], {"accent_hex": {"type": "string", "pattern": "^#[0-9A-Fa-f]{6}$"}, "logo_url": {"type": ["string", "null"]}})
S["AccountPlan"] = obj(["plan_code", "version", "limits"], {"plan_code": STR, "version": INT, "renews_at": TS,
    "limits": arr(obj(["key", "limit", "used"], {"key": STR, "limit": INT, "used": INT}))})
S["ImpersonationSession"] = obj(["id", "operator_name", "mode", "reason", "started_at"], {"id": STR, "operator_name": STR, "mode": enum("READ_ONLY", "WRITE"), "reason": STR, "started_at": TS, "ended_at": nts(), "write_count": INT})
S["DataExport"] = obj(["id", "status"], {"id": STR, "status": enum("QUEUED", "RUNNING", "READY", "EXPIRED"), "download_url": {"type": ["string", "null"]}, "expires_at": TS})
S["TestClock"] = obj(["id", "frozen_time"], {"id": STR, "frozen_time": TS, "subscription_ids": arr(STR)})
S["TestClockAdvance"] = obj(["to"], {"to": TS})

T = [
 ("get", "/v1/me", "getMe", "Identity", "TLY-103", "Current user, active tenant and memberships", None, "Me", 200, ""),
 ("get", "/v1/reports/summary", "getReportSummary", "Reports", "TLY-706", "Home KPIs from read models", None, "ReportSummary", 200, ""),
 ("get", "/v1/reports/revenue", "getRevenueSeries", "Reports", "TLY-706", "Revenue series for 7D/30D/12M", None, "RevenueSeries", 200, "range"),
 ("get", "/v1/reports/mrr-by-plan", "getMrrByPlan", "Reports", "TLY-706", "MRR split by product", None, "MrrByPlan", 200, ""),
 ("get", "/v1/attention-items", "listAttentionItems", "Reports", "TLY-706", "Needs-attention items for the current user", None, "page:AttentionItem", 200, "list"),
 ("post", "/v1/attention-items/{id}/dismiss", "dismissAttentionItem", "Reports", "TLY-706", "Dismiss an item for the current user", None, None, 204, ""),
 ("get", "/v1/customers", "listCustomers", "Customers", "TLY-403", "Search and filter customers", None, "page:Customer", 200, "list,q,status,sort"),
 ("post", "/v1/customers", "createCustomer", "Customers", "TLY-403", "Create a customer", "CustomerCreate", "Customer", 201, "idem"),
 ("get", "/v1/customers/{id}", "getCustomer", "Customers", "TLY-403", "Get a customer", None, "Customer", 200, ""),
 ("patch", "/v1/customers/{id}", "updateCustomer", "Customers", "TLY-403", "Update a customer", "CustomerUpdate", "Customer", 200, "etag"),
 ("get", "/v1/customers/{id}/timeline", "getCustomerTimeline", "Customers", "TLY-403", "Activity timeline", None, "page:TimelineEntry", 200, "list"),
 ("post", "/v1/customers/{id}/payment_method_setups", "createPaymentMethodSetup", "Customers", "TLY-404", "Start processor-side card/ACH setup", None, "PaymentMethodSetup", 201, "idem"),
 ("get", "/v1/customers/{id}/payment_methods", "listPaymentMethods", "Customers", "TLY-404", "List tokenized payment methods", None, "page:PaymentMethod", 200, "list"),
 ("delete", "/v1/customers/{id}/payment_methods/{pmId}", "deletePaymentMethod", "Customers", "TLY-404", "Detach a payment method", None, None, 204, ""),
 ("get", "/v1/products", "listProducts", "Catalog", "TLY-401", "List products", None, "page:Product", 200, "list"),
 ("post", "/v1/products", "createProduct", "Catalog", "TLY-401", "Create a product", "ProductCreate", "Product", 201, "idem"),
 ("get", "/v1/products/{id}", "getProduct", "Catalog", "TLY-401", "Get a product", None, "Product", 200, ""),
 ("get", "/v1/products/{id}/prices", "listPrices", "Catalog", "TLY-401", "Price versions with subscription_count", None, "page:Price", 200, "list"),
 ("post", "/v1/products/{id}/prices", "createPriceVersion", "Catalog", "TLY-401", "Create a new immutable price version", "PriceCreate", "Price", 201, "idem"),
 ("post", "/v1/prices/{id}/archive", "archivePrice", "Catalog", "TLY-401", "Archive a price (409 if subscriptions remain)", None, "Price", 200, ""),
 ("get", "/v1/subscriptions", "listSubscriptions", "Subscriptions", "TLY-501", "Filter by plan, status, renews_within", None, "page:Subscription", 200, "list,status,plan,renews"),
 ("post", "/v1/subscriptions/preview", "previewSubscription", "Subscriptions", "TLY-502", "Side-effect-free price and proration preview", "SubscriptionPreviewRequest", "SubscriptionPreview", 200, ""),
 ("post", "/v1/subscriptions", "createSubscription", "Subscriptions", "TLY-501", "Create a subscription", "SubscriptionCreate", "Subscription", 201, "idem"),
 ("get", "/v1/subscriptions/{id}", "getSubscription", "Subscriptions", "TLY-501", "Get a subscription", None, "Subscription", 200, ""),
 ("patch", "/v1/subscriptions/{id}", "updateSubscription", "Subscriptions", "TLY-502", "Change items with proration", "SubscriptionUpdate", "Subscription", 200, "etag"),
 ("post", "/v1/subscriptions/{id}/cancel", "cancelSubscription", "Subscriptions", "TLY-501", "Cancel now or at period end", "CancelRequest", "Subscription", 200, "idem"),
 ("get", "/v1/invoices", "listInvoices", "Invoices", "TLY-504", "List invoices by status", None, "page:Invoice", 200, "list,status"),
 ("get", "/v1/invoices/summary", "getInvoiceSummary", "Invoices", "TLY-504", "Outstanding, overdue, paid totals", None, "InvoiceSummary", 200, ""),
 ("get", "/v1/invoices/{id}", "getInvoice", "Invoices", "TLY-504", "Get an invoice", None, "Invoice", 200, ""),
 ("patch", "/v1/invoices/{id}", "updateDraftInvoice", "Invoices", "TLY-504", "Edit a draft (409 once finalized)", "InvoiceUpdate", "Invoice", 200, "etag"),
 ("post", "/v1/invoices/{id}/lines", "addInvoiceLine", "Invoices", "TLY-504", "Add a line to a draft", "InvoiceLineCreate", "Invoice", 201, "idem"),
 ("delete", "/v1/invoices/{id}/lines/{lineId}", "deleteInvoiceLine", "Invoices", "TLY-504", "Remove a line from a draft", None, "Invoice", 200, ""),
 ("post", "/v1/invoices/{id}/finalize", "finalizeInvoice", "Invoices", "TLY-505", "Finalize: assign number, post ledger entry", None, "Invoice", 200, "idem"),
 ("post", "/v1/invoices/{id}/void", "voidInvoice", "Invoices", "TLY-505", "Void via reversing entries", None, "Invoice", 200, "idem"),
 ("post", "/v1/invoices/reminders", "sendInvoiceReminders", "Invoices", "TLY-507", "Bulk reminders for open/overdue invoices", "ReminderRequest", "ReminderAccepted", 202, "idem"),
 ("get", "/v1/invoices/{id}/pdf", "getInvoicePdf", "Invoices", "TLY-505", "Rendered PDF", None, "pdf", 200, ""),
 ("get", "/v1/payments", "listPayments", "Payments", "TLY-702", "List payments by status", None, "page:Payment", 200, "list,status"),
 ("get", "/v1/payments/summary", "getPaymentSummary", "Payments", "TLY-702", "Collected, success rate, refunded, failed", None, "PaymentSummary", 200, ""),
 ("post", "/v1/payments", "createPayment", "Payments", "TLY-702", "Charge a payment method", "PaymentCreate", "Payment", 201, "idem"),
 ("get", "/v1/payments/{id}", "getPayment", "Payments", "TLY-702", "Get a payment (expand=attempts)", None, "Payment", 200, "expand"),
 ("post", "/v1/refunds", "createRefund", "Payments", "TLY-704", "Full or partial refund", "RefundCreate", "Refund", 201, "idem"),
 ("get", "/v1/refunds", "listRefunds", "Payments", "TLY-704", "List refunds", None, "page:Refund", 200, "list"),
 ("post", "/v1/usage_events", "ingestUsageEvents", "Usage", "TLY-601", "Batch ingest (≤1000, dedup by event_id)", "UsageBatch", "UsageAccepted", 202, "idem"),
 ("get", "/v1/usage_events", "listUsageEvents", "Usage", "TLY-603", "Recent usage events", None, "page:UsageEvent", 200, "list,meter"),
 ("get", "/v1/meters", "listMeters", "Usage", "TLY-603", "List meters", None, "page:Meter", 200, "list"),
 ("post", "/v1/meters", "createMeter", "Usage", "TLY-603", "Create a meter", "MeterCreate", "Meter", 201, "idem"),
 ("get", "/v1/usage/summary", "getUsageSummary", "Usage", "TLY-603", "Actual, projection and overage for a period", None, "UsageSummary", 200, "meter,customer"),
 ("get", "/v1/api_keys", "listApiKeys", "Developers", "TLY-105", "List API keys (never secrets)", None, "page:ApiKey", 200, "list"),
 ("post", "/v1/api_keys", "createApiKey", "Developers", "TLY-105", "Create a key; secret returned once", "ApiKeyCreate", "ApiKeyCreated", 201, "idem"),
 ("post", "/v1/api_keys/{id}/revoke", "revokeApiKey", "Developers", "TLY-105", "Revoke a key", None, "ApiKey", 200, ""),
 ("get", "/v1/webhook_endpoints", "listWebhookEndpoints", "Webhooks", "TLY-303", "List endpoints", None, "page:WebhookEndpoint", 200, "list"),
 ("post", "/v1/webhook_endpoints", "createWebhookEndpoint", "Webhooks", "TLY-303", "Register an endpoint (SSRF-checked)", "WebhookEndpointCreate", "WebhookEndpointCreated", 201, "idem"),
 ("patch", "/v1/webhook_endpoints/{id}", "updateWebhookEndpoint", "Webhooks", "TLY-303", "Update URL or event types", "WebhookEndpointUpdate", "WebhookEndpoint", 200, "etag"),
 ("delete", "/v1/webhook_endpoints/{id}", "deleteWebhookEndpoint", "Webhooks", "TLY-303", "Delete an endpoint", None, None, 204, ""),
 ("post", "/v1/webhook_endpoints/{id}/enable", "enableWebhookEndpoint", "Webhooks", "TLY-303", "Re-enable after auto-disable (sends a test event)", None, "WebhookEndpoint", 200, ""),
 ("post", "/v1/webhook_endpoints/{id}/rotate_secret", "rotateWebhookSecret", "Webhooks", "TLY-303", "Rotate with overlap window", None, "WebhookSecret", 200, "idem"),
 ("get", "/v1/webhook_endpoints/health", "getWebhookEndpointHealth", "Webhooks", "TLY-307", "24 hourly buckets per endpoint", None, "array:EndpointHealth", 200, ""),
 ("get", "/v1/webhook_deliveries", "listWebhookDeliveries", "Webhooks", "TLY-307", "Deliveries by endpoint and status", None, "page:WebhookDelivery", 200, "list,status,endpoint"),
 ("get", "/v1/webhook_deliveries/{id}", "getWebhookDelivery", "Webhooks", "TLY-307", "Headers, response, timing, attempts", None, "WebhookDelivery", 200, ""),
 ("post", "/v1/webhook_deliveries/{id}/replay", "replayWebhookDelivery", "Webhooks", "TLY-307", "Replay a delivery", None, "WebhookDelivery", 202, "idem"),
 ("get", "/v1/streams/webhook_deliveries", "streamWebhookDeliveries", "Webhooks", "TLY-307", "SSE stream of delivery updates", None, "sse", 200, ""),
 ("get", "/v1/events", "listEvents", "Events", "TLY-302", "Events of the last 30 days", None, "page:Event", 200, "list,type"),
 ("get", "/v1/events/{id}", "getEvent", "Events", "TLY-302", "Event payload and per-endpoint status", None, "Event", 200, ""),
 ("get", "/v1/request_logs", "listRequestLogs", "Events", "TLY-311", "API request log (14 days)", None, "page:RequestLog", 200, "list,status"),
 ("get", "/v1/team/members", "listTeamMembers", "Settings", "TLY-107", "Members and pending invitations", None, "page:TeamMember", 200, "list"),
 ("patch", "/v1/team/members/{userId}", "updateTeamMember", "Settings", "TLY-107", "Change a member's role", "TeamMemberUpdate", "TeamMember", 200, ""),
 ("delete", "/v1/team/members/{userId}", "removeTeamMember", "Settings", "TLY-107", "Remove a member", None, None, 204, ""),
 ("post", "/v1/team/invitations", "createInvitation", "Settings", "TLY-107", "Invite (expires in 7 days)", "InvitationCreate", "Invitation", 201, "idem"),
 ("delete", "/v1/team/invitations/{id}", "revokeInvitation", "Settings", "TLY-107", "Revoke an invitation", None, None, 204, ""),
 ("get", "/v1/settings/branding", "getBranding", "Settings", "TLY-109", "Branding", None, "Branding", 200, ""),
 ("patch", "/v1/settings/branding", "updateBranding", "Settings", "TLY-109", "Update accent color", "Branding", "Branding", 200, "etag"),
 ("post", "/v1/settings/branding/logo", "uploadBrandingLogo", "Settings", "TLY-109", "Upload logo (SVG/PNG ≤ 512 KB)", "multipart", "Branding", 200, ""),
 ("get", "/v1/account/plan", "getAccountPlan", "Settings", "TLY-109", "Plan, limits and usage against limits", None, "AccountPlan", 200, ""),
 ("get", "/v1/account/impersonations", "listImpersonationSessions", "Settings", "TLY-806", "Operator sessions on this account", None, "page:ImpersonationSession", 200, "list"),
 ("post", "/v1/account/exports", "requestDataExport", "Settings", "TLY-905", "Request a full data export", None, "DataExport", 202, "idem"),
 ("get", "/v1/account/exports/{id}", "getDataExport", "Settings", "TLY-905", "Export status and download link", None, "DataExport", 200, ""),
 ("post", "/v1/test_clocks", "createTestClock", "Testing", "TLY-508", "Create a test clock (test mode only)", "TestClock", "TestClock", 201, "idem"),
 ("post", "/v1/test_clocks/{id}/advance", "advanceTestClock", "Testing", "TLY-508", "Advance time and run due billing", "TestClockAdvance", "TestClock", 200, "idem"),
]

O = {}
O["OpsTenant"] = obj(["id", "name", "plan", "tier", "status", "health"], {"id": STR, "name": STR, "plan": STR, "tier": enum("POOL", "SILO"), "status": enum("ACTIVE", "PAST_DUE", "CANCELING", "SUSPENDED", "SUSPEND_PENDING"), "mrr": MONEY, "health": {"type": "integer", "minimum": 0, "maximum": 100}, "api_rpm": INT, "last_activity_at": TS, "signals": arr(STR)})
O["OpsTenantCreate"] = obj(["name", "plan_code", "owner_email"], {"name": STR, "plan_code": STR, "owner_email": {"type": "string", "format": "email"}, "region": STR})
O["TenantContact"] = obj(["email", "masked"], {"email": STR, "masked": BOOL})
O["ApprovalRequest"] = obj(["id", "action", "status", "payload", "payload_sha256", "requested_by"], {"id": STR, "action": enum("TENANT_SUSPEND", "REFUND_CREATE", "OPERATOR_GRANT_ROLE", "IMPERSONATE_WRITE", "TENANT_OFFBOARD", "TENANT_MIGRATE_SILO"), "status": enum("PENDING", "APPROVED", "REJECTED", "EXPIRED"),
    "target_tenant_id": {"type": ["string", "null"]}, "reason": STR, "payload": {"type": "object"}, "payload_sha256": STR, "impact": STR, "requested_by": STR, "decided_by": {"type": ["string", "null"]}, "expires_at": TS})
O["Accepted"] = obj(["approval_id"], {"approval_id": STR, "status": STR})
O["Decision"] = obj([], {"note": STR})
O["OverviewKpis"] = obj(["active_tenants", "platform_mrr", "payment_success_rate", "webhook_success_rate", "attention"], {"active_tenants": INT, "platform_mrr": MONEY, "payment_success_rate": NUM, "webhook_success_rate": NUM,
    "attention": arr(obj(["id", "tag", "title", "target"], {"id": STR, "tag": STR, "title": STR, "detail": STR, "target": STR}))})
O["ServiceHealth"] = obj(["name", "signal", "value", "threshold", "level", "series"], {"name": STR, "signal": STR, "value": NUM, "unit": STR, "threshold": NUM, "level": enum("OK", "WARN", "CRITICAL"), "series": arr(NUM)})
O["TenantLoad"] = obj(["tenant_id", "api_share"], {"tenant_id": STR, "name": STR, "api_share": NUM, "rpm": INT, "usage_eps": INT, "webhook_backlog": INT, "rate_limited": BOOL})
O["Alert"] = obj(["severity", "title", "since"], {"severity": enum("SEV1", "SEV2", "SEV3", "INFO"), "title": STR, "detail": STR, "since": TS})
O["RateLimitOverride"] = obj(["rpm", "reason"], {"rpm": {"type": "integer", "minimum": 1}, "reason": STR, "expires_at": TS})
O["FleetTenant"] = obj(["tenant_id", "backlog", "dlq"], {"tenant_id": STR, "name": STR, "backlog": INT, "dlq": INT, "endpoints": INT})
O["ReplayPlan"] = obj(["events", "endpoints", "estimated_seconds"], {"replay_id": {"type": ["string", "null"]}, "events": INT, "endpoints": INT, "estimated_seconds": INT, "done": INT, "status": enum("DRY_RUN", "RUNNING", "DONE")})
O["Job"] = obj(["name", "schedule", "last_status"], {"name": STR, "description": STR, "schedule": STR, "last_status": enum("SUCCEEDED", "FAILED", "RUNNING"), "last_duration_ms": INT, "next_run_at": TS, "history": arr(enum("SUCCEEDED", "FAILED"))})
O["JobRunRequest"] = obj(["date"], {"tenant_id": {"type": ["string", "null"]}, "date": {"type": "string", "format": "date"}})
O["JobRun"] = obj(["id", "job", "status"], {"id": STR, "job": STR, "status": STR, "error": {"type": ["string", "null"]}})
O["ReconRun"] = obj(["date", "matched", "mismatched"], {"date": {"type": "string", "format": "date"}, "matched": INT, "mismatched": INT, "difference": MONEY})
O["ReconMismatch"] = obj(["id", "kind", "tenant_id"], {"id": STR, "payment_id": STR, "tenant_id": STR, "kind": enum("TIMING", "DUPLICATE", "MISSING_CAPTURE", "AMOUNT"), "ledger_amount": MONEY, "processor_amount": MONEY, "resolved_note": {"type": ["string", "null"]}})
O["ResolveRequest"] = obj(["classification", "note"], {"classification": enum("TIMING", "DUPLICATE", "MISSING_CAPTURE", "AMOUNT"), "note": {"type": "string", "minLength": 5}})
O["OpsRefundCreate"] = obj(["target", "amount", "reason", "kind"], {"target": STR, "amount": MONEY, "reason": STR, "kind": enum("REFUND", "CREDIT")})
O["OpsRefund"] = obj(["id", "status"], {"id": STR, "target": STR, "kind": STR, "amount": MONEY, "status": enum("EXECUTED", "PENDING_APPROVAL", "REJECTED"), "approval_id": {"type": ["string", "null"]}})
O["LedgerAccount"] = obj(["code", "type", "balance"], {"code": STR, "type": STR, "balance": MONEY})
O["JournalEntry"] = obj(["id", "source_type", "postings"], {"id": STR, "source_type": STR, "source_id": STR, "description": STR, "posted_at": TS,
    "postings": arr(obj(["account", "direction", "amount"], {"account": STR, "direction": enum("DEBIT", "CREDIT"), "amount": MONEY}))})
O["PlanVersion"] = obj(["plan_code", "version", "limits"], {"plan_code": STR, "version": INT, "limits": {"type": "object", "additionalProperties": INT}, "tenant_count": INT})
O["PlanVersionCreate"] = obj(["limits"], {"limits": {"type": "object", "additionalProperties": INT}})
O["PlanDiff"] = obj(["next_version", "changes", "affected_tenants"], {"next_version": INT, "affected_tenants": INT, "changes": arr(obj(["key", "from", "to"], {"key": STR, "from": INT, "to": INT}))})
O["PlanMigration"] = obj(["from_version", "to_version"], {"from_version": INT, "to_version": INT, "tenant_ids": arr(STR)})
O["Flag"] = obj(["key", "enabled", "rollout_pct", "targets"], {"key": STR, "description": STR, "enabled": BOOL, "rollout_pct": {"type": "integer", "enum": [0, 10, 25, 50, 100]}, "targets": arr(STR), "affected_tenants": INT})
O["FlagUpdate"] = obj([], {"enabled": BOOL, "rollout_pct": {"type": "integer", "enum": [0, 10, 25, 50, 100]}, "targets": arr(STR)})
O["ImpersonationCreate"] = obj(["tenant_id", "reason", "duration_minutes"], {"tenant_id": STR, "reason": {"type": "string", "minLength": 4}, "duration_minutes": {"type": "integer", "enum": [15, 30, 60]}, "mode": enum("READ_ONLY", "WRITE"), "approval_id": {"type": ["string", "null"]}})
O["Impersonation"] = obj(["id", "tenant_id", "mode", "expires_at"], {"id": STR, "tenant_id": STR, "operator_id": STR, "mode": STR, "reason": STR, "expires_at": TS, "access_token": {"type": ["string", "null"], "description": "Token-exchange result with act claim; only on create"}})
O["AuditEntry"] = obj(["seq", "operator_id", "action", "at", "hash"], {"seq": INT, "operator_id": STR, "action": STR, "kind": enum("READ", "WRITE", "APPROVAL", "IMPERSONATION"), "tenant_id": {"type": ["string", "null"]}, "reason": STR, "at": TS, "prev_hash": STR, "hash": STR})
O["AuditVerification"] = obj(["verified_through_seq", "verified_at", "ok"], {"verified_through_seq": INT, "verified_at": TS, "ok": BOOL, "first_broken_seq": {"type": ["integer", "null"]}})
O["Operator"] = obj(["id", "name", "roles"], {"id": STR, "name": STR, "email": STR, "roles": arr(STR), "pending_roles": arr(STR), "mfa": STR, "last_login_at": TS})
O["RoleRequest"] = obj(["role", "reason"], {"role": enum("SUPPORT", "FINANCE", "SRE", "PLATFORM_ADMIN", "VIEWER"), "reason": STR, "expires_at": TS})

OPS = [
 ("get", "/ops/v1/overview", "opsGetOverview", "Overview", "TLY-804", "Platform KPIs and attention queue", None, "OverviewKpis", 200, ""),
 ("post", "/ops/v1/attention/{id}/ack", "opsAckAttention", "Overview", "TLY-804", "Acknowledge an attention item (audited)", None, None, 204, ""),
 ("get", "/ops/v1/tenants", "opsListTenants", "Tenants", "TLY-804", "Tenants by view, sorted by health", None, "page:OpsTenant", 200, "list,view,q"),
 ("post", "/ops/v1/tenants", "opsCreateTenant", "Tenants", "TLY-101", "Provision a tenant (idempotent)", "OpsTenantCreate", "OpsTenant", 201, "idem"),
 ("get", "/ops/v1/tenants/{id}", "opsGetTenant", "Tenants", "TLY-804", "Tenant quick view (reason required)", None, "OpsTenant", 200, "reason"),
 ("get", "/ops/v1/tenants/{id}/contact", "opsGetTenantContact", "Tenants", "TLY-804", "Masked contact; reveal=true is logged", None, "TenantContact", 200, "reason,reveal"),
 ("post", "/ops/v1/tenants/{id}/suspend", "opsRequestSuspend", "Tenants", "TLY-804", "Creates an approval request", None, "Accepted", 202, "reason,idem"),
 ("post", "/ops/v1/tenants/{id}/rate-limit", "opsSetRateLimit", "Health", "TLY-808", "Temporary per-tenant rate limit", "RateLimitOverride", None, 204, "reason"),
 ("post", "/ops/v1/tenants/{id}/offboarding", "opsRequestOffboarding", "Tenants", "TLY-905", "Export then crypto-shred (approval)", None, "Accepted", 202, "reason,idem"),
 ("post", "/ops/v1/tenants/{id}/silo-migration", "opsRequestSiloMigration", "Tenants", "TLY-906", "Pool to silo migration (approval)", None, "Accepted", 202, "reason,idem"),
 ("get", "/ops/v1/health/services", "opsListServiceHealth", "Health", "TLY-808", "Service tiles from Prometheus", None, "array:ServiceHealth", 200, ""),
 ("get", "/ops/v1/health/top-tenants", "opsTopTenantsByLoad", "Health", "TLY-808", "Top tenants by load (5 min window)", None, "array:TenantLoad", 200, ""),
 ("get", "/ops/v1/alerts", "opsListAlerts", "Health", "TLY-808", "Active alerts mirrored from Alertmanager", None, "array:Alert", 200, ""),
 ("get", "/ops/v1/streams/health", "opsStreamHealth", "Health", "TLY-808", "SSE health updates", None, "sse", 200, ""),
 ("get", "/ops/v1/webhooks/fleet", "opsGetWebhookFleet", "Fleet", "TLY-810", "Backlog and DLQ by tenant", None, "array:FleetTenant", 200, ""),
 ("post", "/ops/v1/webhooks/tenants/{id}/dlq/replay", "opsReplayTenantDlq", "Fleet", "TLY-810", "Dry run or start a throttled replay", None, "ReplayPlan", 202, "reason,dry,idem"),
 ("get", "/ops/v1/replays/{id}", "opsGetReplay", "Fleet", "TLY-810", "Replay progress", None, "ReplayPlan", 200, ""),
 ("get", "/ops/v1/jobs", "opsListJobs", "Jobs", "TLY-810", "Scheduled jobs with run history", None, "array:Job", 200, ""),
 ("post", "/ops/v1/jobs/{name}/runs", "opsRunJob", "Jobs", "TLY-810", "Idempotent re-run for a tenant/date", "JobRunRequest", "JobRun", 202, "reason,idem"),
 ("get", "/ops/v1/reconciliation/runs", "opsListReconRuns", "Money", "TLY-811", "Daily reconciliation runs", None, "page:ReconRun", 200, "list"),
 ("get", "/ops/v1/reconciliation/runs/{date}/mismatches", "opsListMismatches", "Money", "TLY-811", "Mismatches for a day", None, "page:ReconMismatch", 200, "list"),
 ("post", "/ops/v1/reconciliation/mismatches/{id}/resolve", "opsResolveMismatch", "Money", "TLY-811", "Resolve with classification and note", "ResolveRequest", "ReconMismatch", 200, "reason"),
 ("get", "/ops/v1/refunds", "opsListRefunds", "Money", "TLY-811", "Operator refunds and credits", None, "page:OpsRefund", 200, "list"),
 ("post", "/ops/v1/refunds", "opsCreateRefund", "Money", "TLY-811", "Executes, or 202 + approval above threshold", "OpsRefundCreate", "OpsRefund", 201, "reason,idem"),
 ("get", "/ops/v1/tenants/{id}/ledger/accounts", "opsListLedgerAccounts", "Money", "TLY-811", "Read-only balances", None, "array:LedgerAccount", 200, "reason"),
 ("get", "/ops/v1/tenants/{id}/ledger/journal_entries", "opsListJournalEntries", "Money", "TLY-811", "Read-only journal", None, "page:JournalEntry", 200, "reason,list"),
 ("get", "/ops/v1/plans", "opsListPlans", "Config", "TLY-813", "Plans with current versions", None, "array:PlanVersion", 200, ""),
 ("post", "/ops/v1/plans/{code}/versions", "opsCreatePlanVersion", "Config", "TLY-813", "dry_run=true returns the diff", "PlanVersionCreate", "PlanDiff", 201, "dry,reason,idem"),
 ("post", "/ops/v1/plans/{code}/migrations", "opsMigratePlanTenants", "Config", "TLY-813", "Migrate tenants between versions", "PlanMigration", None, 202, "reason,idem"),
 ("get", "/ops/v1/flags", "opsListFlags", "Config", "TLY-813", "Feature flags", None, "array:Flag", 200, ""),
 ("patch", "/ops/v1/flags/{key}", "opsUpdateFlag", "Config", "TLY-813", "dry_run=true returns affected count", "FlagUpdate", "Flag", 200, "dry,reason"),
 ("get", "/ops/v1/approvals", "opsListApprovals", "Governance", "TLY-802", "Pending or decided approvals", None, "page:ApprovalRequest", 200, "list,status"),
 ("get", "/ops/v1/approvals/{id}", "opsGetApproval", "Governance", "TLY-802", "Approval with stored payload", None, "ApprovalRequest", 200, ""),
 ("post", "/ops/v1/approvals/{id}/approve", "opsApprove", "Governance", "TLY-802", "Step-up required; executes stored payload", "Decision", "ApprovalRequest", 200, ""),
 ("post", "/ops/v1/approvals/{id}/reject", "opsReject", "Governance", "TLY-802", "Reject a request", "Decision", "ApprovalRequest", 200, ""),
 ("post", "/ops/v1/impersonations", "opsStartImpersonation", "Governance", "TLY-806", "Token exchange with act claim", "ImpersonationCreate", "Impersonation", 201, "reason,idem"),
 ("get", "/ops/v1/impersonations", "opsListImpersonations", "Governance", "TLY-806", "Active and past sessions", None, "page:Impersonation", 200, "list,status"),
 ("post", "/ops/v1/impersonations/{id}/end", "opsEndImpersonation", "Governance", "TLY-806", "End a session", None, "Impersonation", 200, ""),
 ("get", "/ops/v1/audit", "opsListAudit", "Governance", "TLY-801", "Operator audit log", None, "page:AuditEntry", 200, "list,q,kind"),
 ("get", "/ops/v1/audit/verification", "opsGetAuditVerification", "Governance", "TLY-801", "Last hash-chain verification", None, "AuditVerification", 200, ""),
 ("get", "/ops/v1/operators", "opsListOperators", "Governance", "TLY-803", "Operators and roles", None, "array:Operator", 200, ""),
 ("post", "/ops/v1/operators/{id}/role-requests", "opsRequestRole", "Governance", "TLY-803", "Creates an approval request", "RoleRequest", "Accepted", 202, "idem"),
]

PARAMS = {
 "IdempotencyKey": {"name": "Idempotency-Key", "in": "header", "required": True, "schema": {"type": "string", "minLength": 8, "maxLength": 128}, "description": "Required on writes; replayed for 24 h (ADR-006)."},
 "IfMatch": {"name": "If-Match", "in": "header", "required": True, "schema": STR, "description": "ETag from the last read; 412 on mismatch."},
 "Limit": {"name": "limit", "in": "query", "schema": {"type": "integer", "minimum": 1, "maximum": 100, "default": 50}},
 "StartingAfter": {"name": "starting_after", "in": "query", "schema": STR},
 "AccessReason": {"name": "X-Access-Reason", "in": "header", "required": True, "schema": {"type": "string", "minLength": 4}, "description": "Recorded in the operator audit log (NFR-ADM-AUD-01)."},
 "DryRun": {"name": "dry_run", "in": "query", "schema": {"type": "boolean", "default": False}},
}
QUERY = {"q": ("q", STR), "status": ("status", STR), "sort": ("sort", enum("-mrr", "mrr", "-created_at")), "plan": ("plan", STR), "renews": ("renews_within", {"type": "string", "examples": ["7d"]}),
         "range": ("range", enum("7D", "30D", "12M")), "meter": ("meter", STR), "customer": ("customer", STR), "type": ("type", STR), "endpoint": ("endpoint", STR),
         "expand": ("expand", STR), "view": ("view", enum("NEEDS_ATTENTION", "ALL", "SCALE", "SILO", "SUSPENDED")), "kind": ("kind", STR), "reveal": ("reveal", BOOL)}

def build(title, desc, server, rows, schemas, sec):
    comps = {"schemas": copy.deepcopy({**S, **schemas}), "parameters": copy.deepcopy(PARAMS),
             "responses": {"Problem": {"description": "Error (RFC 9457)", "content": {"application/problem+json": {"schema": ref("Problem")}}}},
             "securitySchemes": sec}
    paths = {}
    for m, p, op, tag, story, summ, req, resp, code, flags in rows:
        f = [x for x in flags.split(",") if x]
        params = []
        for seg in [s[1:-1] for s in p.split("/") if s.startswith("{")]:
            params.append({"name": seg, "in": "path", "required": True, "schema": {"type": "string", "format": "date"} if seg == "date" else STR})
        if "idem" in f: params.append({"$ref": "#/components/parameters/IdempotencyKey"})
        if "etag" in f: params.append({"$ref": "#/components/parameters/IfMatch"})
        if "reason" in f: params.append({"$ref": "#/components/parameters/AccessReason"})
        if "dry" in f: params.append({"$ref": "#/components/parameters/DryRun"})
        if "list" in f: params += [{"$ref": "#/components/parameters/Limit"}, {"$ref": "#/components/parameters/StartingAfter"}]
        for k, (name, sch) in QUERY.items():
            if k in f: params.append({"name": name, "in": "query", "schema": sch})
        o = {"operationId": op, "tags": [tag], "summary": summ, "description": f"Story {story}.", "x-story": story}
        if params: o["parameters"] = params
        if req == "multipart":
            o["requestBody"] = {"required": True, "content": {"multipart/form-data": {"schema": obj(["file"], {"file": {"type": "string", "format": "binary"}})}}}
        elif req:
            o["requestBody"] = {"required": True, "content": {"application/json": {"schema": ref(req)}}}
        r = {}
        c = str(code)
        if resp is None: r[c] = {"description": "Done"}
        elif resp == "pdf": r[c] = {"description": "PDF", "content": {"application/pdf": {"schema": {"type": "string", "format": "binary"}}}}
        elif resp == "sse": r[c] = {"description": "text/event-stream; heartbeat every 15 s (NFR-RT-01)", "content": {"text/event-stream": {"schema": STR}}}
        elif resp.startswith("page:"): r[c] = {"description": "Page", "content": {"application/json": {"schema": page(resp[5:])}}}
        elif resp.startswith("array:"): r[c] = {"description": "List", "content": {"application/json": {"schema": arr(ref(resp[6:]))}}}
        else:
            r[c] = {"description": "OK", "content": {"application/json": {"schema": ref(resp)}}}
            if "etag" in f or m == "get": r[c]["headers"] = {"ETag": {"schema": STR}}
        r["default"] = {"$ref": "#/components/responses/Problem"}
        o["responses"] = r
        paths.setdefault(p, {})[m] = o
    return {"openapi": "3.1.0", "info": {"title": title, "version": "1.0.0", "description": desc},
            "servers": [{"url": server}], "security": [{k: []} for k in sec], "paths": paths, "components": comps}

tenant = build("Tally tenant API", "Normative contract for the tenant API (docs/04). Money is always integer minor units. Tenant comes from the credential only.",
               "https://api.tally.example", T, {}, {"bearerApiKey": {"type": "http", "scheme": "bearer", "description": "sk_live_/sk_test_ key or BFF-minted JWT"}})
ops = build("Tally operator API", "Operator console API (docs/04 §6). VPN-only origin, operator realm tokens, reason header on tenant data.",
            "https://ops.internal", OPS, O, {"operatorJwt": {"type": "http", "scheme": "bearer", "bearerFormat": "JWT"}})

class D(yaml.SafeDumper): pass
D.ignore_aliases = lambda *a: True
for name, spec in [("openapi.yaml", tenant), ("ops-openapi.yaml", ops)]:
    (ROOT / "contracts" / name).write_text("# GENERATED by scripts/gen_openapi.py; edit the tables there, then run `make contracts`.\n" + yaml.dump(spec, Dumper=D, sort_keys=False, allow_unicode=True, width=140))

def table(rows):
    out = ["| Method | Path | Story | Purpose |", "| --- | --- | --- | --- |"]
    for m, p, op, tag, story, summ, *_ in rows:
        out.append(f"| {m.upper()} | `{p}` | {story} | {summ} |")
    return "\n".join(out)
(ROOT / "scripts" / ".catalogue-tenant.md").write_text(table(T))
(ROOT / "scripts" / ".catalogue-ops.md").write_text(table(OPS))
print(len(T), "tenant operations,", len(OPS), "operator operations")
