// Package lint proves the depguard domain rule of TLY-005 AC4 fires.
package lint

import (
	"os/exec"
	"path/filepath"
	"strings"
	"testing"
)

func TestDepguard_rejectsInfraImportsInDomain__TLY_005_AC4(t *testing.T) {
	if _, err := exec.LookPath("golangci-lint"); err != nil {
		t.Skip("golangci-lint not installed")
	}

	moduleRoot, err := filepath.Abs("../..")
	if err != nil {
		t.Fatalf("resolve module root: %v", err)
	}

	cmd := exec.Command("golangci-lint", "run", "--build-tags=depguardfixture", "./internal/example/domain/...")
	cmd.Dir = moduleRoot
	out, err := cmd.CombinedOutput()

	if err == nil {
		t.Fatalf("expected golangci-lint to fail on the fixture, but it succeeded:\n%s", out)
	}
	if !strings.Contains(string(out), "net/http") {
		t.Fatalf("expected output to mention net/http, got:\n%s", out)
	}
	if !strings.Contains(string(out), "depguard") {
		t.Fatalf("expected the depguard linter to have fired, got:\n%s", out)
	}
}
