package main

import (
	"strings"
	"testing"
)

func TestRun_failsWithoutRequiredConfig__TLY_005_AC2(t *testing.T) {
	t.Setenv("DATABASE_URL", "")
	t.Setenv("REDIS_URL", "")
	t.Setenv("KAFKA_BROKERS", "")

	err := run()
	if err == nil {
		t.Fatal("run() = nil, want error naming missing variables")
	}
	for _, name := range []string{"DATABASE_URL", "REDIS_URL", "KAFKA_BROKERS"} {
		if !strings.Contains(err.Error(), name) {
			t.Errorf("run() error %q does not mention %s", err.Error(), name)
		}
	}
}
