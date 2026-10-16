// Verifies Tally-Signature per docs/03 §4 against every golden vector.
// Usage: go run verify.go [path/to/signature-vectors.json]
package main

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strconv"
	"strings"
)

type vector struct {
	Name        string   `json:"name"`
	Secrets     []string `json:"secrets_valid_for_receiver"`
	Now         int64    `json:"now"`
	RawBody     string   `json:"raw_body"`
	Header      string   `json:"header"`
	Expected    string   `json:"expected"`
}

type vectorFile struct {
	ToleranceSeconds int64    `json:"tolerance_seconds"`
	Vectors          []vector `json:"vectors"`
}

func sign(secret string, t int64, body string) string {
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(fmt.Sprintf("%d.%s", t, body)))
	return hex.EncodeToString(mac.Sum(nil))
}

func classify(header, rawBody string, secrets []string, now, tolerance int64) (string, error) {
	parts := strings.Split(header, ",")
	t, err := strconv.ParseInt(strings.TrimPrefix(parts[0], "t="), 10, 64)
	if err != nil {
		return "", err
	}
	if now-t > tolerance {
		return "REJECT_TIMESTAMP", nil
	}
	for _, part := range parts[1:] {
		sig := strings.TrimPrefix(part, "v1=")
		for _, secret := range secrets {
			expected := sign(secret, t, rawBody)
			if hmac.Equal([]byte(expected), []byte(sig)) {
				return "VALID", nil
			}
		}
	}
	return "REJECT_SIGNATURE", nil
}

func main() {
	path := "../../contracts/webhooks/signature-vectors.json"
	if len(os.Args) > 1 {
		path = os.Args[1]
	}
	raw, err := os.ReadFile(filepath.Clean(path))
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
	var doc vectorFile
	if err := json.Unmarshal(raw, &doc); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
	for _, v := range doc.Vectors {
		got, err := classify(v.Header, v.RawBody, v.Secrets, v.Now, doc.ToleranceSeconds)
		if err != nil {
			fmt.Fprintln(os.Stderr, err)
			os.Exit(1)
		}
		if got != v.Expected {
			fmt.Fprintf(os.Stderr, "FAIL %s: got %s, expected %s\n", v.Name, got, v.Expected)
			os.Exit(1)
		}
		fmt.Printf("ok %s -> %s\n", v.Name, got)
	}
	fmt.Printf("%d vectors verified (go)\n", len(doc.Vectors))
}
