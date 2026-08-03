using System;

namespace Sahamati.SnaLib
{
    public class SNAConnectionError : Exception
    {
        public SNAConnectionError(string message) : base($"sna connection error: {message}") { }
    }

    public class SNAValidationError : Exception
    {
        public int HttpStatus { get; }
        public string ErrorCode { get; }

        public SNAValidationError(int httpStatus, string message, string errorCode)
            : base($"sna validation error (HTTP {httpStatus}, code {errorCode}): {message}")
        {
            HttpStatus = httpStatus;
            ErrorCode = errorCode;
        }
    }

    public class SNAServerError : Exception
    {
        public int HttpStatus { get; }
        public string ErrorCode { get; }

        public SNAServerError(int httpStatus, string message, string errorCode)
            : base($"sna server error (HTTP {httpStatus}, code {errorCode}): {message}")
        {
            HttpStatus = httpStatus;
            ErrorCode = errorCode;
        }
    }

    public class SNANotImplementedError : Exception
    {
        public int HttpStatus { get; }
        public string ErrorCode { get; }

        public SNANotImplementedError(int httpStatus, string message, string errorCode)
            : base($"sna not implemented (HTTP {httpStatus}, code {errorCode}): {message}")
        {
            HttpStatus = httpStatus;
            ErrorCode = errorCode;
        }
    }

    public class SNAUnexpectedResponseError : Exception
    {
        public int HttpStatus { get; }
        public string RawBody { get; }

        public SNAUnexpectedResponseError(int httpStatus, string message, string rawBody)
            : base($"sna unexpected response (HTTP {httpStatus}): {message}")
        {
            HttpStatus = httpStatus;
            RawBody = rawBody;
        }
    }
}
