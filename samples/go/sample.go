package main

import (
  ...
)

/* ========================================================================= 

func: sna_SendTransactionInfo

This is sample code snippet to send request to SahamatiNet Agent(SNA)
This has to be called for any API Request/Response, Notification/Response
Please refer to the usage of this function call, in sample call flows shown
below the definition here.
========================================================================= */

func sna_SendTransactionInfo(body map[string]interface, callType, peerId, peerType, customerId, route string) error {

	//sna_pod_fqdn could be internal_sna_ip or internal_sna_fqdn
	//port, 4044, or as configured by RE

	url := "https://" + "< sna_pod_fqdn:port >" + "/sna/v1/aa"

	payload := map[string]interface{}{
		{
			"callType": callType,
			"route": route,
			"peerId": peerId,
			"peerType": peerType,
			"customerId": customerId,
			"httpStatus": 0,	//0, as it is not applicable to this call type
			"addlAttr": {},
			"body": body
		  }
	}

	// ... Build HTTP request
	req := buildHttpRequest(url, payload)

	// ... Send HTTP request to SNA	
	err := sendHttp(req)
	if err != nil {
		return fmt.Errorf("send to SNA: %s", err.Error())
	}

	return nil
}


/* ========================================================================= 
 This is a sample flow to make the SNA API call by AA before sending 
 an API request to FIP, and after receiving the response from FIP.
 ex: sending Account Discovery Request
========================================================================= */

func executeDiscoveryRequest() (string, error) {
	route := "/Accounts/discover"
	fipId := "fip-Bank1-001"

	rebitRequestBody := marshalRequestBody(...)
	/*
	// ReBIT defined request body
	rebitRequestBody := map[string]interface{}{
		"ver":      "2.0.0",
		"timestamp": time.Now().UTC().Format(time.RFC3339),
		"txnid":    "f35761ac-4a18-11e8-96ff-0277a9fbfedc2",
		"Customer": map[string]interface{}{
			"id": "9876543210@myid-aa-001",
			"Identifiers": []map[string]interface{}{
				{
					"category": "STRONG",
					"type":     "AADHAAR",
					"value":    "XXXXXXXXXXXXXXXX",
				},
			},
		},
		"FITypes": []string{"DEPOSIT"},
	}
	*/

	...

	//inform SahamatiNetAgent, about the request being sent
	sna_SendTransactionInfo(rebitRequestBody, "requestOut", fipId, "FIP", "9876543210@myid-aa-001", route, 0)

	//continue to send the request to FIP
	resp, err := httpClient.Do(req)
	if err != nil {
		return "", fmt.Errorf("request execution failed: %w", err)
	}
	defer resp.Body.Close()

	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("failed to read response body: %w", err)
	}

	rebitResponseBody := unMarshalResponseBody(respBody)
	/*
	// ReBIT defined request body
	rebitResponseBody := map[string]interface{}{
		"ver": "2.0.0",
		"timestamp": "2023-06-26T11:39:57.153Z",
		"txnid": "f35761ac-4a18-11e8-96ff-0277a9fbfedc",
		"DiscoveredAccounts": [
		  {
			"FIType": "DEPOSIT",
			"accType": "SAVINGS",
			"accRefNumber": "BANK11111111",
			"maskedAccNumber": "XXXXXXX3468"
		  }
		]
	  }
	*/

	//inform SahamatiNetAgent, about the response received
	sna_SendTransactionInfo(rebitResponseBody, "responseIn", fipId, "FIP", "9876543210@myid-aa-001", route, resp.StatusCode)

	return string(respBody), nil
}

/* ========================================================================= 
 This is a sample flow to make the SNA API call by AA, when it receives a 
 notification from FIP and after sending the response to FIP.
 ex: receiving FI Notification Request
========================================================================= */

func handleFINotificationfromFIP() (string, error) {
	route := "/FI/Notification"
	fipId := "fip-Bank1-001"

	rebitRequestBody := unMarshalRequestBody(...)
	/*
	// ReBIT defined request body
	rebitRequestBody := map[string]interface{}{
		"ver": "2.0.0",
		"timestamp": "2023-06-26T11:39:57.153Z",
		"txnid": "0b811819-9044-4856-b0ee-8c88035f8858",
		"Notifier": {
		  "type": "FIP",
		  "id": "FIP-1"
		},
		"FIStatusNotification": {
		  "sessionId": "XXXX0-XXXX-XXXX",
		  "sessionStatus": "ACTIVE",
		  "FIStatusResponse": [
			{
			  "fipID": "FIP-1",
			  "Accounts": [
				{
				  "linkRefNumber": "XXXX-XXXX-XXXX",
				  "FIStatus": "READY",
				  "description": ""
				}
			  ]
			}
		  ]
		}
	  }
	*/

	...

	//inform SahamatiNetAgent, about the notification received
	sna_SendTransactionInfo(rebitRequestBody, "requestIn", fipId, "FIP", "9876543210@myid-aa-001", route, 0)

	//continue to process response from FIP
	rebitResponseBody, statusCode := processFIPNotification(fipId, rebitRequestBody)
	/*
	rebitResponseBody:= map[string]interface{} {
		"ver": "2.0.0",
		"timestamp": "2023-06-26T06:13:30.967+0000",
		"txnid": "f35761ac-4a18-11e8-96ff-0277a9fbfedc",
		"response": "OK"
	  }
	*/

	//inform SahamatiNetAgent, about the response being sent
	sna_SendTransactionInfo(rebitResponseBody, "responseOut", fipId, "FIP", "9876543210@myid-aa-001", route, statusCode)

	//continue to process response to FIP
	sendFIPNotificationResponse(fipId, rebitResponseBody)

}
