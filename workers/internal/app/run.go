// Package app provides the shared graceful-shutdown runner used by the
// dispatcher and aggregator binaries.
package app

import (
	"context"
	"fmt"
	"time"
)

// Run executes intake until ctx is cancelled, then gives it shutdownTimeout
// to return. ponytail: no supervisor tree — one intake func per binary is all
// TLY-304/TLY-4xx need; add one when a binary genuinely runs two intakes.
func Run(ctx context.Context, shutdownTimeout time.Duration, intake func(context.Context) error) error {
	result := make(chan error, 1)
	go func() {
		result <- intake(ctx)
	}()

	select {
	case err := <-result:
		if err != nil {
			return fmt.Errorf("intake: %w", err)
		}
		return nil
	case <-ctx.Done():
	}

	select {
	case err := <-result:
		if err != nil {
			return fmt.Errorf("intake: %w", err)
		}
		return nil
	case <-time.After(shutdownTimeout):
		return fmt.Errorf("shutdown timed out after %s", shutdownTimeout)
	}
}
