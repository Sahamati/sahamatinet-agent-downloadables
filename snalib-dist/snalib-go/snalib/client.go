package snalib

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"os"
	"strings"
	"time"
)

// Config holds all SNAClient configuration. Zero values use built-in defaults.
type Config struct {
	BaseURL       string        // required: e.g. "http://localhost:4044"
	MaxRetries    int           // default: 3
	RetryInterval time.Duration // default: 1s, doubles each attempt
	MaxWait       time.Duration // default: 15s total across all retry attempts
	Timeout       time.Duration // default: 30s per request
	LogEnabled    bool          // default: false; when false only warnings are emitted; when true all logs are emitted
	OnWarning     func(msg string) // optional: route SDK warnings to your own logger; if nil, warnings go to stderr via slog
}

// SNAClient is the main entry point. Instantiate once and reuse across your application lifetime.
type SNAClient struct {
	baseURL    string
	httpClient *http.Client
	logger     *slog.Logger
	onWarning  func(msg string)
	logEnabled bool
	Entity     *EntityClient
	AA         *AAClient
}

// NewClient creates an SNAClient, verifies connectivity via /ping with exponential backoff,
// and returns SNAConnectionError if the service is unreachable after all retries.
func NewClient(cfg Config) (*SNAClient, error) {
	if cfg.MaxRetries == 0 {
		cfg.MaxRetries = 3
	}
	if cfg.RetryInterval == 0 {
		cfg.RetryInterval = time.Second
	}
	if cfg.MaxWait == 0 {
		cfg.MaxWait = 15 * time.Second
	}
	if cfg.Timeout == 0 {
		cfg.Timeout = 30 * time.Second
	}

	// Always create a slog logger unless a custom OnWarning handler covers warnings
	// and verbose logging is disabled — in that case no slog output is needed at all.
	var logger *slog.Logger
	if cfg.LogEnabled || cfg.OnWarning == nil {
		level := slog.LevelWarn // warnings only by default
		if cfg.LogEnabled {
			level = slog.LevelInfo // all logs when verbose
		}
		logger = slog.New(slog.NewJSONHandler(os.Stderr, &slog.HandlerOptions{Level: level}))
	}

	c := &SNAClient{
		baseURL: strings.TrimRight(cfg.BaseURL, "/"),
		httpClient: &http.Client{
			Timeout: cfg.Timeout,
			Transport: &http.Transport{
				MaxIdleConns:    10,
				IdleConnTimeout: 90 * time.Second,
			},
		},
		logger:     logger,
		onWarning:  cfg.OnWarning,
		logEnabled: cfg.LogEnabled,
	}
	c.Entity = &EntityClient{client: c}
	c.AA = &AAClient{client: c}

	var connectErr error
	if err := c.connectWithRetry(cfg); err != nil {
		c.log("warn", err.Error())
		connectErr = err
	}
	return c, connectErr
}

// Ping checks liveness of the SNA service.
func (c *SNAClient) Ping(ctx context.Context) error {
	return c.ping(ctx)
}

// Version returns the SNA service version info.
func (c *SNAClient) Version(ctx context.Context) (*VersionResponse, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, c.baseURL+"/sna/v1/version", nil)
	if err != nil {
		return nil, &SNAConnectionError{Message: err.Error()}
	}
	resp, body, err := c.do(req)
	if err != nil {
		return nil, err
	}
	if resp.StatusCode != http.StatusOK {
		return nil, c.parseError(resp.StatusCode, body)
	}
	var result VersionResponse
	if err := json.Unmarshal(body, &result); err != nil {
		return nil, &SNAUnexpectedResponseError{HTTPStatus: resp.StatusCode, Message: "failed to decode version response: " + err.Error(), RawBody: string(body)}
	}
	return &result, nil
}

func (c *SNAClient) connectWithRetry(cfg Config) error {
	deadline := time.Now().Add(cfg.MaxWait)
	interval := cfg.RetryInterval

	var lastErr error
	for attempt := 0; attempt <= cfg.MaxRetries; attempt++ {
		ctx, cancel := context.WithTimeout(context.Background(), cfg.Timeout)
		lastErr = c.ping(ctx)
		cancel()

		if lastErr == nil {
			c.log("info", "connected to SNA service")
			return nil
		}

		if attempt == cfg.MaxRetries || time.Now().Add(interval).After(deadline) {
			break
		}
		c.log("warn", "SNA unreachable, retrying", "attempt", attempt+1, "backoff_ms", interval.Milliseconds())
		time.Sleep(interval)
		interval *= 2
	}

	return &SNAConnectionError{
		Message: fmt.Sprintf("service at %s unreachable after %d attempts", cfg.BaseURL, cfg.MaxRetries+1),
	}
}

func (c *SNAClient) ping(ctx context.Context) error {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, c.baseURL+"/sna/v1/ping", nil)
	if err != nil {
		return &SNAConnectionError{Message: err.Error()}
	}
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return &SNAConnectionError{Message: err.Error()}
	}
	defer resp.Body.Close()
	io.Copy(io.Discard, resp.Body) //nolint:errcheck — draining body to allow connection reuse
	if resp.StatusCode != http.StatusOK {
		return &SNAConnectionError{Message: fmt.Sprintf("ping returned HTTP %d", resp.StatusCode)}
	}
	return nil
}

// do executes the request, reads and closes the body, and returns the raw bytes.
// Callers may only use resp.StatusCode after this call — resp.Body is already closed.
func (c *SNAClient) do(req *http.Request) (*http.Response, []byte, error) {
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, nil, &SNAConnectionError{Message: err.Error()}
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return resp, nil, &SNAConnectionError{Message: "failed to read response body: " + err.Error()}
	}
	return resp, body, nil
}

func (c *SNAClient) parseError(statusCode int, body []byte) error {
	var e errorResponse
	json.Unmarshal(body, &e) //nolint:errcheck — best-effort parse
	switch statusCode {
	case http.StatusBadRequest:
		return &SNAValidationError{HTTPStatus: statusCode, Message: e.Message, ErrorCode: e.ErrorCode}
	case http.StatusNotImplemented:
		return &SNANotImplementedError{HTTPStatus: statusCode, Message: e.Message, ErrorCode: e.ErrorCode}
	default:
		return &SNAServerError{HTTPStatus: statusCode, Message: e.Message, ErrorCode: e.ErrorCode}
	}
}

func (c *SNAClient) log(level, msg string, args ...any) {
	switch level {
	case "warn", "error":
		if c.onWarning != nil {
			c.onWarning(formatLogMsg(msg, args...))
			return
		}
	case "info":
		if !c.logEnabled {
			return
		}
	}
	if c.logger == nil {
		return
	}
	switch level {
	case "info":
		c.logger.Info(msg, args...)
	case "warn":
		c.logger.Warn(msg, args...)
	case "error":
		c.logger.Error(msg, args...)
	}
}

func formatLogMsg(msg string, args ...any) string {
	if len(args) == 0 {
		return msg
	}
	var b strings.Builder
	b.WriteString(msg)
	for i := 0; i+1 < len(args); i += 2 {
		fmt.Fprintf(&b, " %v=%v", args[i], args[i+1])
	}
	return b.String()
}
