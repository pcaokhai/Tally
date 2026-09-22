// Command aggregator turns raw usage events into windowed rollups.
package main

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/pcaokhai/tally/workers/internal/app"
	"github.com/pcaokhai/tally/workers/internal/config"
	"github.com/pcaokhai/tally/workers/internal/telemetry"
)

// telemetryFlushTimeout bounds the deferred telemetry shutdown so a hung
// intake plus an unreachable collector cannot together exceed AC3's 30 s.
const telemetryFlushTimeout = 5 * time.Second

func main() {
	if err := run(); err != nil {
		fmt.Fprintf(os.Stderr, "aggregator: %v\n", err)
		os.Exit(1)
	}
}

func run() error {
	cfg, err := config.LoadAggregator()
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
		shutdownCtx, cancel := context.WithTimeout(context.WithoutCancel(ctx), telemetryFlushTimeout)
		defer cancel()
		if err := shutdownTelemetry(shutdownCtx); err != nil {
			fmt.Fprintf(os.Stderr, "aggregator: telemetry shutdown: %v\n", err)
		}
	}()

	slog.InfoContext(ctx, "aggregator starting")
	return app.Run(ctx, cfg.ShutdownTimeout, intake)
}

// intake blocks until ctx is cancelled.
// ponytail: the Kafka consumer lands in TLY-304; this is the seam it plugs into.
func intake(ctx context.Context) error {
	<-ctx.Done()
	return nil
}
