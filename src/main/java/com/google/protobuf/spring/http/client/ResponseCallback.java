package com.google.protobuf.spring.http.client;

import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;


/**
 * Callback interface for code that operates on a {@link ClientHttpResponse}. Allows to manipulate the request
 * headers, and write to the request body.
 *
 * <p>Used internally by the {@link ClientHttpInvocationHandler}, but also useful for application code.
 *
 * @author Alex Antonov (based on org.springframework.web.client.RequestCallback by Arjen Poutsma)
 */
public interface ResponseCallback {

	/**
	 * Gets called by {@link ClientHttpInvocationHandler#invoke} with an opened {@code ClientHttpResponse}.
	 * Does not need to care about closing the request or about handling errors:
	 * this will all be handled by the {@code ClientHttpInvocationHandler}.
	 * @param response from the HTTP request
	 * @throws IOException in case of I/O errors
	 */
	void doWithResponse(ClientHttpResponse response) throws IOException;

}
