package main

import (
	"strings"
	"testing"
)

func TestRun_failsWithoutRequiredConfig__TLY_005_AC2(t *testing.T) {
	t.Setenv("KAFKA_BROKERS", "")

	err := run()
	if err == nil {
		t.Fatal("run() = nil, want error naming missing variables")
	}
	if !strings.Contains(err.Error(), "KAFKA_BROKERS") {
		t.Errorf("run() error %q does not mention KAFKA_BROKERS", err.Error())
	}
}
