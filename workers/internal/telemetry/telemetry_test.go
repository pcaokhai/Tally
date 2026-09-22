package telemetry

import (
	"context"
	"testing"
)

func TestSetup_withoutEndpointStillReturnsShutdown__TLY_005_AC1(t *testing.T) {
	shutdown, err := Setup(context.Background(), Options{ServiceName: "x", LogLevel: "info"})
	if err != nil {
		t.Fatalf("Setup returned error: %v", err)
	}
	if shutdown == nil {
		t.Fatal("Setup returned nil shutdown")
	}
	if err := shutdown(context.Background()); err != nil {
		t.Fatalf("first shutdown call returned error: %v", err)
	}
	if err := shutdown(context.Background()); err != nil {
		t.Fatalf("second shutdown call returned error: %v", err)
	}
}

func TestSetup_rejectsUnknownLogLevel__TLY_005_AC1(t *testing.T) {
	_, err := Setup(context.Background(), Options{ServiceName: "x", LogLevel: "shouty"})
	if err == nil {
		t.Fatal("expected error for unknown log level, got nil")
	}
}
