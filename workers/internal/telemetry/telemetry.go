// Package telemetry configures the shared slog JSON logger and OpenTelemetry
// tracer/meter that both worker binaries set up at startup.
package telemetry

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"os"
	"sync"

	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/otlp/otlpmetric/otlpmetrichttp"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracehttp"
	"go.opentelemetry.io/otel/propagation"
	"go.opentelemetry.io/otel/sdk/metric"
	"go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	semconv "go.opentelemetry.io/otel/semconv/v1.26.0"
)

// Options configures Setup.
type Options struct {
	ServiceName  string
	LogLevel     string
	OTLPEndpoint string
}

// Setup configures the default slog JSON logger and, when OTLPEndpoint is
// set, registers OTLP-over-HTTP trace and metric providers with a W3C
// TraceContext+Baggage propagator. When OTLPEndpoint is empty, no exporters
// or providers are registered and the returned shutdown is a no-op — this is
// the supported local-dev mode with no collector running (R2).
//
// The returned shutdown is idempotent and safe to call more than once.
func Setup(ctx context.Context, o Options) (shutdown func(context.Context) error, err error) {
	var lvl slog.Level
	if err := lvl.UnmarshalText([]byte(o.LogLevel)); err != nil {
		return nil, fmt.Errorf("telemetry: invalid log level %q: %w", o.LogLevel, err)
	}
	handler := slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: lvl})
	slog.SetDefault(slog.New(handler).With("service", o.ServiceName))

	otel.SetTextMapPropagator(propagation.NewCompositeTextMapPropagator(
		propagation.TraceContext{}, propagation.Baggage{},
	))

	if o.OTLPEndpoint == "" {
		return func(context.Context) error { return nil }, nil
	}

	res, err := resource.Merge(resource.Default(), resource.NewWithAttributes(
		semconv.SchemaURL, semconv.ServiceName(o.ServiceName),
	))
	if err != nil {
		return nil, fmt.Errorf("telemetry: build resource: %w", err)
	}

	traceExp, err := otlptracehttp.New(ctx, otlptracehttp.WithEndpointURL(o.OTLPEndpoint))
	if err != nil {
		return nil, fmt.Errorf("telemetry: build trace exporter: %w", err)
	}
	metricExp, err := otlpmetrichttp.New(ctx, otlpmetrichttp.WithEndpointURL(o.OTLPEndpoint))
	if err != nil {
		return nil, fmt.Errorf("telemetry: build metric exporter: %w", err)
	}

	tp := sdktrace.NewTracerProvider(
		sdktrace.WithBatcher(traceExp),
		sdktrace.WithResource(res),
	)
	mp := metric.NewMeterProvider(
		metric.WithReader(metric.NewPeriodicReader(metricExp)),
		metric.WithResource(res),
	)
	otel.SetTracerProvider(tp)
	otel.SetMeterProvider(mp)

	var once sync.Once
	var shutdownErr error
	return func(ctx context.Context) error {
		once.Do(func() {
			shutdownErr = errors.Join(tp.Shutdown(ctx), mp.Shutdown(ctx))
		})
		return shutdownErr
	}, nil
}
