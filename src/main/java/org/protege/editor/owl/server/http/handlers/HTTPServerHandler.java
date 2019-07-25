package org.protege.editor.owl.server.http.handlers;

import org.protege.editor.owl.server.http.HTTPServer;
import org.protege.editor.owl.server.http.ServerEndpoints;
import org.protege.editor.owl.server.http.exception.ServerException;
import org.protege.editor.owl.server.security.LoginTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

public class HTTPServerHandler extends BaseRoutingHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(HTTPServerHandler.class);
	
	private boolean shutdownServer = false;
	private boolean requiredRestarting = false;

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		try {
			handlingRequest(exchange);
		}
		catch (ServerException e) {
			handleServerException(exchange, e);
		}
		catch (LoginTimeoutException e) {
			loginTimeoutErrorStatusCode(exchange, e);
		}
		finally {
			exchange.endExchange(); // end the request
		}

		if (shutdownServer) {
			HTTPServer.server().stop();
			logger.info("Server shut down gracefully");
			System.exit(0);					
		}
		
		// A special directive when the server needs to be restarted after completing the request
		if (requiredRestarting) {
			try {
				HTTPServer.server().restart();
			}
			catch (ServerException e) {
				handleServerException(exchange, e);
			}
			finally {
				requiredRestarting = false;
			}
		}
	}

	private void handlingRequest(HttpServerExchange exchange) throws ServerException, LoginTimeoutException {
		String requestPath = exchange.getRequestPath();
		HttpString requestMethod = exchange.getRequestMethod();
		if (requestPath.equals(ServerEndpoints.SERVER_RESTART) && requestMethod.equals(Methods.POST)) {
			HTTPServer.server().restart();
		}
		else if (requestPath.equals(ServerEndpoints.SERVER_STOP) && requestMethod.equals(Methods.POST)) {
			HTTPServer.server().stop();
		} else if (requestPath.equals(ServerEndpoints.SERVER_PAUSE) && requestMethod.equals(Methods.GET)) {
			HTTPServer.server().pause(getAuthToken(exchange).getUser());
		} else if (requestPath.equals(ServerEndpoints.SERVER_RESUME) && requestMethod.equals(Methods.GET)) {
			HTTPServer.server().resume(getAuthToken(exchange).getUser());
			requiredRestarting = true;
		} else if (requestPath.equals(ServerEndpoints.SERVER_SHUTDOWN) && requestMethod.equals(Methods.POST)) {
			shutdownServer = true;
		}
	}
}
