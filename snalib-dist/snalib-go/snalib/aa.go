package snalib

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
)

// AAClient handles AA transaction dispatching.
type AAClient struct {
	client *SNAClient
}

// Dispatch sends an AA transaction to the SNA service for async processing.
// A successful return means the service accepted the request for telemetry processing. Please check the SNA logs and verify if the request has been processed successfully.
func (a *AAClient) Dispatch(ctx context.Context, req AARequest) (*DispatchResponse, error) {
	payload := map[string]any{
		"callType":   req.CallType,
		"route":      req.Route,
		"peerId":     req.PeerID,
		"peerType":   req.PeerType,
		"customerId": req.CustomerID,
		"txnCorId":   req.TxnCorID,
		"body":       req.Body,
	}
	if req.HTTPStatus != nil {
		payload["httpStatus"] = *req.HTTPStatus
	}
	if req.AddlAttr != nil {
		payload["addlAttr"] = req.AddlAttr
	}

	body, err := json.Marshal(payload)
	if err != nil {
		return nil, &SNAConnectionError{Message: "failed to marshal dispatch request: " + err.Error()}
	}

	httpReq, err := http.NewRequestWithContext(ctx, http.MethodPost, a.client.baseURL+"/sna/v1/aa", bytes.NewReader(body))
	if err != nil {
		return nil, &SNAConnectionError{Message: err.Error()}
	}
	httpReq.Header.Set("Content-Type", "application/json")

	resp, respBody, err := a.client.do(httpReq)
	if err != nil {
		return nil, err
	}
	if resp.StatusCode != http.StatusOK {
		return nil, a.client.parseError(resp.StatusCode, respBody)
	}

	var result DispatchResponse
	if err := json.Unmarshal(respBody, &result); err != nil {
		return nil, &SNAConnectionError{Message: "failed to decode dispatch response: " + err.Error()}
	}
	return &result, nil
}
