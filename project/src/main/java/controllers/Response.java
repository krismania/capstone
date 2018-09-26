package controllers;

class Response {

    private Response() {
    }

    static class ErrorResponse {
	String message;

	ErrorResponse(String message) {
	    this.message = message;
	}
    }

    static class ClientIdResponse {
	String clientId;

	public ClientIdResponse(String clientId) {
	    this.clientId = clientId;
	}
    }

}
