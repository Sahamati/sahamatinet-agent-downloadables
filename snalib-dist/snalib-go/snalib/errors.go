package snalib

import "fmt"



// SNAConnectionError is returned when the SNA service is unreachable or a network failure occurs.
type SNAConnectionError struct {
	Message string
}

func (e *SNAConnectionError) Error() string {
	return fmt.Sprintf("sna connection error: %s", e.Message)
}

// SNAValidationError is returned when the service responds with HTTP 400 (bad input).
type SNAValidationError struct {
	HTTPStatus int
	Message    string
	ErrorCode  string
}

func (e *SNAValidationError) Error() string {
	return fmt.Sprintf("sna validation error (HTTP %d, code %s): %s", e.HTTPStatus, e.ErrorCode, e.Message)
}

// SNAServerError is returned when the service responds with HTTP 500.
type SNAServerError struct {
	HTTPStatus int
	Message    string
	ErrorCode  string
}

func (e *SNAServerError) Error() string {
	return fmt.Sprintf("sna server error (HTTP %d, code %s): %s", e.HTTPStatus, e.ErrorCode, e.Message)
}

// SNANotImplementedError is returned when the service responds with HTTP 501 (e.g. peerType FIU).
type SNANotImplementedError struct {
	HTTPStatus int
	Message    string
	ErrorCode  string
}

func (e *SNANotImplementedError) Error() string {
	return fmt.Sprintf("sna not implemented (HTTP %d, code %s): %s", e.HTTPStatus, e.ErrorCode, e.Message)
}

// SNAUnexpectedResponseError is returned when the service responds with a success status
// but the response body cannot be parsed as the expected type.
type SNAUnexpectedResponseError struct {
	HTTPStatus int
	Message    string
	RawBody    string
}

func (e *SNAUnexpectedResponseError) Error() string {
	return fmt.Sprintf("sna unexpected response (HTTP %d): %s", e.HTTPStatus, e.Message)
}
