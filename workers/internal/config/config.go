// Package config loads typed, fail-fast configuration for the workers binaries from environment variables.
package config

import (
	"fmt"
	"os"
	"strings"
	"time"
)

// Common holds configuration shared by every worker binary.
type Common struct {
	ServiceName     string
	LogLevel        string
	OTLPEndpoint    string
	ShutdownTimeout time.Duration
}

// Dispatcher holds configuration for cmd/dispatcher.
type Dispatcher struct {
	Common
	DatabaseURL  string
	RedisURL     string
	KafkaBrokers []string
}

// Aggregator holds configuration for cmd/aggregator.
type Aggregator struct {
	Common
	KafkaBrokers []string
}

// loader accumulates env var problems so a single error can report all of them.
type loader struct {
	missing []string
	invalid []string
}

func (l *loader) required(name string) string {
	v := os.Getenv(name)
	if v == "" {
		l.missing = append(l.missing, name)
	}
	return v
}

func (l *loader) optional(name, def string) string {
	v := os.Getenv(name)
	if v == "" {
		return def
	}
	return v
}

func (l *loader) duration(name string, def time.Duration) time.Duration {
	v := os.Getenv(name)
	if v == "" {
		return def
	}
	d, err := time.ParseDuration(v)
	if err != nil {
		l.invalid = append(l.invalid, name)
		return def
	}
	return d
}

func (l *loader) list(name string) []string {
	v := l.required(name)
	if v == "" {
		return nil
	}
	parts := strings.Split(v, ",")
	out := make([]string, 0, len(parts))
	for _, p := range parts {
		out = append(out, strings.TrimSpace(p))
	}
	return out
}

func (l *loader) err() error {
	if len(l.missing) == 0 && len(l.invalid) == 0 {
		return nil
	}
	var parts []string
	if len(l.missing) > 0 {
		parts = append(parts, fmt.Sprintf("missing required environment variables: %s", strings.Join(l.missing, ", ")))
	}
	if len(l.invalid) > 0 {
		parts = append(parts, fmt.Sprintf("malformed environment variables: %s", strings.Join(l.invalid, ", ")))
	}
	return fmt.Errorf("config: %s", strings.Join(parts, "; "))
}

func (l *loader) common(serviceName string) Common {
	return Common{
		ServiceName:     l.optional("OTEL_SERVICE_NAME", serviceName),
		LogLevel:        l.optional("LOG_LEVEL", "info"),
		OTLPEndpoint:    l.optional("OTEL_EXPORTER_OTLP_ENDPOINT", ""),
		ShutdownTimeout: l.duration("SHUTDOWN_TIMEOUT", 30*time.Second),
	}
}

// LoadDispatcher loads and validates configuration for cmd/dispatcher.
func LoadDispatcher() (Dispatcher, error) {
	l := &loader{}
	cfg := Dispatcher{
		Common:       l.common("dispatcher"),
		DatabaseURL:  l.required("DATABASE_URL"),
		RedisURL:     l.required("REDIS_URL"),
		KafkaBrokers: l.list("KAFKA_BROKERS"),
	}
	if err := l.err(); err != nil {
		return Dispatcher{}, err
	}
	return cfg, nil
}

// LoadAggregator loads and validates configuration for cmd/aggregator.
func LoadAggregator() (Aggregator, error) {
	l := &loader{}
	cfg := Aggregator{
		Common:       l.common("aggregator"),
		KafkaBrokers: l.list("KAFKA_BROKERS"),
	}
	if err := l.err(); err != nil {
		return Aggregator{}, err
	}
	return cfg, nil
}
