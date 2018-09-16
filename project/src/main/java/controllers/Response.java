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

}
