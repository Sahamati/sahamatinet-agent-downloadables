package snalib

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
)

// EntityClient handles entity registration and token retrieval.
type EntityClient struct {
	client *SNAClient
}

// Register registers an entity with the SNA service.
// Provide Secret, Token, or both — if both are present, the service uses Token (service-side rule).
// Never pass Secret or Token values to logs or error output.
func (e *EntityClient) Register(ctx context.Context, req RegisterRequest) (*RegisterResponse, error) {
	payload := map[string]any{"entity_id": req.EntityID}
	if req.Secret != "" {
		payload["secret"] = req.Secret
	}
	if req.Token != "" {
		payload["token"] = req.Token
	}

	body, err := json.Marshal(payload)
	if err != nil {
		return nil, &SNAConnectionError{Message: "failed to marshal register request: " + err.Error()}
	}

	httpReq, err := http.NewRequestWithContext(ctx, http.MethodPost, e.client.baseURL+"/sna/v1/entity/register", bytes.NewReader(body))
	if err != nil {
		return nil, &SNAConnectionError{Message: err.Error()}
	}
	httpReq.Header.Set("Content-Type", "application/json")

	resp, respBody, err := e.client.do(httpReq)
	if err != nil {
		return nil, err
	}
	if resp.StatusCode != http.StatusOK {
		return nil, e.client.parseError(resp.StatusCode, respBody)
	}

	var result RegisterResponse
	if err := json.Unmarshal(respBody, &result); err != nil {
		return nil, &SNAUnexpectedResponseError{HTTPStatus: resp.StatusCode, Message: "failed to decode register response: " + err.Error(), RawBody: string(respBody)}
	}
	return &result, nil
}

// GetToken retrieves the stored IAM token for the registered entity.
func (e *EntityClient) GetToken(ctx context.Context) (*TokenResponse, error) {
	httpReq, err := http.NewRequestWithContext(ctx, http.MethodGet, e.client.baseURL+"/sna/v1/entity/token/generate", nil)
	if err != nil {
		return nil, &SNAConnectionError{Message: err.Error()}
	}

	resp, respBody, err := e.client.do(httpReq)
	if err != nil {
		return nil, err
	}
	if resp.StatusCode != http.StatusOK {
		return nil, e.client.parseError(resp.StatusCode, respBody)
	}

	var result TokenResponse
	if err := json.Unmarshal(respBody, &result); err != nil {
		return nil, &SNAUnexpectedResponseError{HTTPStatus: resp.StatusCode, Message: "failed to decode token response: " + err.Error(), RawBody: string(respBody)}
	}
	return &result, nil
}
