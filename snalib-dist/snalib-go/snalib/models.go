package snalib

// VersionResponse is returned by Version().
type VersionResponse struct {
	AgentVersion string `json:"agentVersion"`
	Name         string `json:"name"`
}

// TokenResponse maps both the full and minimal shapes from GET /sna/v1/entity/token/generate.
// Fields absent in the minimal shape are zero-valued.
type TokenResponse struct {
	Ver              string `json:"ver"`
	Timestamp        string `json:"timestamp"`
	TxnID            string `json:"txnId"`
	AccessToken      string `json:"accessToken"`
	ExpiresIn        int    `json:"expiresIn"`
	RefreshExpiresIn int    `json:"refreshExpiresIn"`
	TokenType        string `json:"tokenType"`
	NotBeforePolicy  int    `json:"notBeforePolicy"`
	Scope            string `json:"scope"`
}

// RegisterRequest is the input for Entity.Register.
type RegisterRequest struct {
	EntityID string
	Secret   string // optional; pass empty string to omit
	Token    string // optional; takes precedence over Secret if both provided (service-side rule)
}

// RegisterResponse is returned by Entity.Register.
type RegisterResponse struct {
	Message  string `json:"message"`
	EntityID string `json:"entity_id"`
	Status   string `json:"status"`
}

// AARequest is the input for AA.Dispatch.
type AARequest struct {
	CallType   string
	Route      string
	PeerID     string
	PeerType   string
	CustomerID string
	HTTPStatus *int           // required for responseIn/responseOut; nil otherwise
	TxnCorID   string
	Body       any            // required — pass a struct, map[string]any, or json.RawMessage for a raw JSON string
	AddlAttr   map[string]any // optional
}

// DispatchResponse is returned by AA.Dispatch.
// Message "accepted" means the service received the request, not that it succeeded — processing is async.
type DispatchResponse struct {
	Message string `json:"message"`
}

// errorResponse is used internally to parse error payloads from the service.
type errorResponse struct {
	Message   string `json:"message"`
	ErrorCode string `json:"errorCode"`
}
