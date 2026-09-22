package config

import (
	"strings"
	"testing"
	"time"
)

func clearDispatcherEnv(t *testing.T) {
	t.Helper()
	for _, name := range []string{"DATABASE_URL", "REDIS_URL", "KAFKA_BROKERS", "LOG_LEVEL", "SHUTDOWN_TIMEOUT", "OTEL_SERVICE_NAME", "OTEL_EXPORTER_OTLP_ENDPOINT"} {
		t.Setenv(name, "")
	}
}

func clearAggregatorEnv(t *testing.T) {
	t.Helper()
	for _, name := range []string{"KAFKA_BROKERS", "LOG_LEVEL", "SHUTDOWN_TIMEOUT", "OTEL_SERVICE_NAME", "OTEL_EXPORTER_OTLP_ENDPOINT"} {
		t.Setenv(name, "")
	}
}

func TestLoadDispatcher_reportsEveryMissingRequiredVar__TLY_005_AC2(t *testing.T) {
	clearDispatcherEnv(t)
	t.Setenv("KAFKA_BROKERS", "broker1:9092")

	_, err := LoadDispatcher()
	if err == nil {
		t.Fatal("expected error for missing DATABASE_URL and REDIS_URL")
	}
	if !strings.Contains(err.Error(), "DATABASE_URL") {
		t.Errorf("error %q does not mention DATABASE_URL", err.Error())
	}
	if !strings.Contains(err.Error(), "REDIS_URL") {
		t.Errorf("error %q does not mention REDIS_URL", err.Error())
	}
}

func TestLoadAggregator_reportsMissingRequiredVar__TLY_005_AC2(t *testing.T) {
	clearAggregatorEnv(t)

	_, err := LoadAggregator()
	if err == nil {
		t.Fatal("expected error for missing KAFKA_BROKERS")
	}
	if !strings.Contains(err.Error(), "KAFKA_BROKERS") {
		t.Errorf("error %q does not mention KAFKA_BROKERS", err.Error())
	}
}

func TestLoad_appliesDefaults__TLY_005_AC2(t *testing.T) {
	t.Run("dispatcher", func(t *testing.T) {
		clearDispatcherEnv(t)
		t.Setenv("DATABASE_URL", "postgres://localhost/tally")
		t.Setenv("REDIS_URL", "redis://localhost:6379")
		t.Setenv("KAFKA_BROKERS", "broker1:9092")

		cfg, err := LoadDispatcher()
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if cfg.LogLevel != "info" {
			t.Errorf("LogLevel = %q, want info", cfg.LogLevel)
		}
		if cfg.ShutdownTimeout != 30*time.Second {
			t.Errorf("ShutdownTimeout = %v, want 30s", cfg.ShutdownTimeout)
		}
		if cfg.ServiceName != "dispatcher" {
			t.Errorf("ServiceName = %q, want dispatcher", cfg.ServiceName)
		}
	})

	t.Run("aggregator", func(t *testing.T) {
		clearAggregatorEnv(t)
		t.Setenv("KAFKA_BROKERS", "broker1:9092")

		cfg, err := LoadAggregator()
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
		if cfg.LogLevel != "info" {
			t.Errorf("LogLevel = %q, want info", cfg.LogLevel)
		}
		if cfg.ShutdownTimeout != 30*time.Second {
			t.Errorf("ShutdownTimeout = %v, want 30s", cfg.ShutdownTimeout)
		}
		if cfg.ServiceName != "aggregator" {
			t.Errorf("ServiceName = %q, want aggregator", cfg.ServiceName)
		}
	})
}

func TestLoad_rejectsMalformedDuration__TLY_005_AC2(t *testing.T) {
	clearDispatcherEnv(t)
	t.Setenv("DATABASE_URL", "postgres://localhost/tally")
	t.Setenv("REDIS_URL", "redis://localhost:6379")
	t.Setenv("KAFKA_BROKERS", "broker1:9092")
	t.Setenv("SHUTDOWN_TIMEOUT", "banana")

	_, err := LoadDispatcher()
	if err == nil {
		t.Fatal("expected error for malformed SHUTDOWN_TIMEOUT")
	}
	if !strings.Contains(err.Error(), "SHUTDOWN_TIMEOUT") {
		t.Errorf("error %q does not mention SHUTDOWN_TIMEOUT", err.Error())
	}
}
