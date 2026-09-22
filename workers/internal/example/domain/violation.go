//go:build depguardfixture

// Package domain is a lint fixture: it exists only to prove the depguard rule
// of TLY-005 AC4 fires on a domain package importing infrastructure.
// The build tag keeps it out of every normal build, test and lint run.
package domain

import "net/http"

var _ = http.DefaultClient
