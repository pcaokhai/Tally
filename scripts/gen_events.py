#!/usr/bin/env python3
"""Generates contracts/events/*.schema.json (JSON Schema 2020-12) and validates fixtures against them."""
import json
from pathlib import Path
from jsonschema import Draft202012Validator
ROOT = Path(__file__).resolve().parents[1]
EV = ROOT / "contracts" / "events"
BASE = "https://schemas.tally.example/events/"

money = {"type": "object", "required": ["amount", "currency"], "additionalProperties": False,
         "properties": {"amount": {"type": "integer"}, "currency": {"type": "string", "pattern": "^[a-z]{3}$"}}}
ts = {"type": "string", "format": "date-time"}
def schema(name, title, types, data_props, data_req):
    return {"$schema": "https://json-schema.org/draft/2020-12/schema", "$id": BASE + name, "title": title,
            "description": "Envelope + data. Kafka key = tenant_id. Headers: event-type, tenant-id, traceparent, schema-version (docs/04 §7).",
            "type": "object", "additionalProperties": False,
            "required": ["id", "type", "api_version", "tenant_id", "created_at", "livemode", "data"],
            "properties": {"id": {"type": "string", "pattern": "^evt_[A-Za-z0-9]{8,40}$"}, "type": {"enum": types},
                           "api_version": {"const": "2026-09-01"}, "tenant_id": {"type": "string"}, "created_at": ts,
                           "livemode": {"type": "boolean"},
                           "data": {"type": "object", "required": data_req, "properties": data_props, "additionalProperties": True}},
            "$defs": {"Money": money}}
M = {"$ref": "#/$defs/Money"}
S = {
 "invoice.schema.json": schema("invoice.schema.json", "Invoice events", ["invoice.finalized", "invoice.paid", "invoice.voided", "invoice.overdue"],
    {"invoice_id": {"type": "string"}, "number": {"type": "string"}, "customer_id": {"type": "string"}, "subscription_id": {"type": ["string", "null"]},
     "status": {"enum": ["OPEN", "PAID", "VOID", "UNCOLLECTIBLE"]}, "total": M, "amount_due": M, "due_date": {"type": "string", "format": "date"}},
    ["invoice_id", "customer_id", "status", "total"]),
 "payment.schema.json": schema("payment.schema.json", "Payment events", ["payment.succeeded", "payment.failed", "payment.refunded"],
    {"payment_id": {"type": "string"}, "customer_id": {"type": "string"}, "invoice_id": {"type": ["string", "null"]}, "amount": M,
     "failure_code": {"type": ["string", "null"]}, "attempt_no": {"type": "integer"}, "refund_id": {"type": ["string", "null"]}},
    ["payment_id", "customer_id", "amount"]),
 "subscription.schema.json": schema("subscription.schema.json", "Subscription events", ["subscription.created", "subscription.renewed", "subscription.updated", "subscription.canceled", "subscription.trial_ending"],
    {"subscription_id": {"type": "string"}, "customer_id": {"type": "string"}, "status": {"enum": ["TRIALING", "ACTIVE", "PAST_DUE", "CANCELED"]},
     "current_period_end": ts, "cancel_at_period_end": {"type": "boolean"}, "mrr": M}, ["subscription_id", "customer_id", "status"]),
 "customer.schema.json": schema("customer.schema.json", "Customer events", ["customer.created", "customer.updated"],
    {"customer_id": {"type": "string"}, "email": {"type": "string"}, "currency": {"type": "string"}}, ["customer_id"]),
 "usage-raw.schema.json": schema("usage-raw.schema.json", "Raw usage (core → aggregator, topic tally.usage.raw.v1)", ["usage.recorded"],
    {"event_id": {"type": "string"}, "meter_id": {"type": "string"}, "customer_id": {"type": "string"}, "quantity": {"type": "number", "minimum": 0}, "occurred_at": ts},
    ["event_id", "meter_id", "customer_id", "quantity", "occurred_at"]),
 "usage-rollup.schema.json": schema("usage-rollup.schema.json", "Rollups (aggregator → core, topic tally.usage.rollups.v1)", ["usage.rollup"],
    {"meter_id": {"type": "string"}, "customer_id": {"type": "string"}, "window_start": ts, "window_end": ts, "quantity": {"type": "number"},
     "event_count": {"type": "integer"}, "watermark": ts, "rollup_key": {"type": "string", "description": "tenant:meter:customer:window_start, idempotency key for the core upsert"}},
    ["meter_id", "customer_id", "window_start", "window_end", "quantity", "rollup_key"]),
 "webhook-control.schema.json": schema("webhook-control.schema.json", "Endpoint control (both directions, topic tally.webhooks.control.v1)", ["webhook_endpoint.updated", "webhook_endpoint.deleted", "webhook_endpoint.auto_disabled", "webhook_endpoint.circuit_changed"],
    {"endpoint_id": {"type": "string"}, "url": {"type": "string"}, "event_types": {"type": "array", "items": {"type": "string"}}, "status": {"enum": ["ENABLED", "DEGRADED", "CIRCUIT_OPEN", "DISABLED"]},
     "secret_versions": {"type": "array", "items": {"type": "object", "required": ["id", "expires_at"], "properties": {"id": {"type": "string"}, "expires_at": {"type": ["string", "null"]}}}},
     "failure_streak": {"type": "integer"}}, ["endpoint_id", "status"]),
}
for n, s in S.items():
    Draft202012Validator.check_schema(s)
    (EV / n).write_text(json.dumps(s, indent=2) + "\n")

FX = ROOT / "contracts" / "fixtures"
samples = {
 "invoice.schema.json": {"id": "evt_1Qf7a9Kd2", "type": "invoice.paid", "api_version": "2026-09-01", "tenant_id": "ten_1Aa0", "created_at": "2026-09-22T07:32:07Z", "livemode": False,
   "data": {"invoice_id": "inv_2058", "number": "AR-2058", "customer_id": "cus_Hb7x", "subscription_id": "sub_H7x4", "status": "PAID", "total": {"amount": 190000, "currency": "usd"}, "amount_due": {"amount": 0, "currency": "usd"}, "due_date": "2026-09-27"}},
 "payment.schema.json": {"id": "evt_1Qf6b2Pz1", "type": "payment.failed", "api_version": "2026-09-01", "tenant_id": "ten_1Aa0", "created_at": "2026-09-22T07:02:11Z", "livemode": False,
   "data": {"payment_id": "pay_3Qa91", "customer_id": "cus_Nw3k", "invoice_id": "inv_2061", "amount": {"amount": 125000, "currency": "usd"}, "failure_code": "insufficient_funds", "attempt_no": 1}},
 "usage-rollup.schema.json": {"id": "evt_1Qr0c7Uu4", "type": "usage.rollup", "api_version": "2026-09-01", "tenant_id": "ten_7Pz1", "created_at": "2026-09-22T07:40:00Z", "livemode": True,
   "data": {"meter_id": "mtr_api_calls", "customer_id": "cus_Qn1a", "window_start": "2026-09-22T07:35:00Z", "window_end": "2026-09-22T07:40:00Z", "quantity": 5210, "event_count": 5210, "watermark": "2026-09-22T07:39:30Z", "rollup_key": "ten_7Pz1:mtr_api_calls:cus_Qn1a:2026-09-22T07:35:00Z"}},
}
events_fx = []
for n, sample in samples.items():
    Draft202012Validator(S[n]).validate(sample)
    events_fx.append(sample)
(FX / "events.sample.json").write_text(json.dumps(events_fx, indent=2) + "\n")
print(len(S), "event schemas valid;", len(events_fx), "fixtures validated")
