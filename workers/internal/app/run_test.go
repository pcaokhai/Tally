package app

import (
	"context"
	"errors"
	"os/signal"
	"strings"
	"syscall"
	"testing"
	"time"

	"go.uber.org/goleak"
)

func TestMain(m *testing.M) {
	goleak.VerifyTestMain(m)
}

func TestRun_stopsIntakeOnSignalAndExits__TLY_005_AC3(t *testing.T) {
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGTERM)
	t.Cleanup(stop)

	observed := make(chan struct{}, 1)
	intake := func(ctx context.Context) error {
		<-ctx.Done()
		observed <- struct{}{}
		return nil
	}

	done := make(chan error, 1)
	go func() {
		done <- Run(ctx, 5*time.Second, intake)
	}()

	if err := syscall.Kill(syscall.Getpid(), syscall.SIGTERM); err != nil {
		t.Fatalf("send SIGTERM: %v", err)
	}

	select {
	case err := <-done:
		if err != nil {
			t.Fatalf("Run() = %v, want nil", err)
		}
	case <-time.After(5 * time.Second):
		t.Fatal("Run did not return within deadline")
	}

	select {
	case <-observed:
	case <-time.After(time.Second):
		t.Fatal("intake did not observe cancellation")
	}
}

func TestRun_returnsErrorWhenIntakeOutlivesTimeout__TLY_005_AC3(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	release := make(chan struct{})
	intake := func(context.Context) error {
		<-release
		return nil
	}

	cancel()
	err := Run(ctx, 50*time.Millisecond, intake)
	close(release)

	if err == nil {
		t.Fatal("Run() = nil, want timeout error")
	}
	if !strings.Contains(err.Error(), "50ms") {
		t.Fatalf("Run() error = %q, want it to mention the timeout", err.Error())
	}
}

var errIntakeSentinel = errors.New("intake boom")

func TestRun_propagatesIntakeError__TLY_005_AC3(t *testing.T) {
	ctx := context.Background()
	intake := func(context.Context) error {
		return errIntakeSentinel
	}

	err := Run(ctx, time.Second, intake)
	if !errors.Is(err, errIntakeSentinel) {
		t.Fatalf("Run() = %v, want wrapped %v", err, errIntakeSentinel)
	}
}
