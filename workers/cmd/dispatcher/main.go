// Command dispatcher delivers webhooks with per-tenant fairness.
package main

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"os/signal"
	"syscall"

	"github.com/pcaokhai/tally/workers/internal/app"
	"github.com/pcaokhai/tally/workers/internal/config"
	"github.com/pcaokhai/tally/workers/internal/telemetry"
)

func main() {
	if err := run(); err != nil {
		fmt.Fprintf(os.Stderr, "dispatcher: %v\n", err)
		os.Exit(1)
	}
}

func run() error {
	cfg, err := config.LoadDispatcher()
	if err != nil {
		return err
	}

	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	shutdownTelemetry, err := telemetry.Setup(ctx, telemetry.Options{
		ServiceName:  cfg.ServiceName,
		LogLevel:     cfg.LogLevel,
		OTLPEndpoint: cfg.OTLPEndpoint,
	})
	if err != nil {
		return err
	}
	defer func() {
		shutdownCtx, cancel := context.WithTimeout(context.WithoutCancel(ctx), cfg.ShutdownTimeout)
		defer cancel()
		if err := shutdownTelemetry(shutdownCtx); err != nil {
			fmt.Fprintf(os.Stderr, "dispatcher: telemetry shutdown: %v\n", err)
		}
	}()

	slog.InfoContext(ctx, "dispatcher starting", "service", cfg.ServiceName)
	return app.Run(ctx, cfg.ShutdownTimeout, intake)
}

// intake blocks until ctx is cancelled.
// ponytail: the Kafka consumer lands in TLY-304; this is the seam it plugs into.
func intake(ctx context.Context) error {
	<-ctx.Done()
	return nil
}
