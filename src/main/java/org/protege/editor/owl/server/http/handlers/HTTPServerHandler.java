package org.protege.editor.owl.server.http.handlers;

import org.protege.editor.owl.server.http.HTTPServer;
import org.protege.editor.owl.server.http.ServerEndpoints;
import org.protege.editor.owl.server.http.exception.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

public class HTTPServerHandler extends BaseRoutingHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(HTTPServerHandler.class);
	
	private boolean shutdownServer = false;

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		try {
			handlingRequest(exchange);
		}
		catch (ServerException e) {
			handleServerException(exchange, e);
		}
		finally {
			exchange.endExchange(); // end the request
		}

		
		if (shutdownServer) {
			logger.info("Server shut down gracefully");
			System.exit(0);					
		}
	}

	private void handlingRequest(HttpServerExchange exchange) throws ServerException {
		String requestPath = exchange.getRequestPath();
		HttpString requestMethod = exchange.getRequestMethod();
		if (requestPath.equals(ServerEndpoints.SERVER_RESTART) && requestMethod.equals(Methods.POST)) {
			HTTPServer.server().restart();
		}
		else if (requestPath.equals(ServerEndpoints.SERVER_STOP) && requestMethod.equals(Methods.POST)) {
			HTTPServer.server().stop();
		} else if (requestPath.equals(ServerEndpoints.SERVER_PAUSE) && requestMethod.equals(Methods.GET)) {
			HTTPServer.server().pause();
		} else if (requestPath.equals(ServerEndpoints.SERVER_RESUME) && requestMethod.equals(Methods.GET)) {
			HTTPServer.server().resume();
		} else if (requestPath.equals(ServerEndpoints.SERVER_SHUTDOWN) && requestMethod.equals(Methods.POST)) {
			HTTPServer.server().stop();
			shutdownServer = true;
		}
	}
}
