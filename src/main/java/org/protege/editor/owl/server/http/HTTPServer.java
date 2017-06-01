package org.protege.editor.owl.server.http;

import edu.stanford.protege.metaproject.ConfigurationManager;
import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.GlobalPermissions;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.ServerConfiguration;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.exception.ObjectConversionException;
import edu.stanford.protege.metaproject.api.exception.UnknownRoleIdException;
import edu.stanford.protege.metaproject.impl.RoleIdImpl;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.StatusCodes;
import org.protege.editor.owl.server.api.ChangeService;
import org.protege.editor.owl.server.api.ServerLayer;
import org.protege.editor.owl.server.api.LoginService;
import org.protege.editor.owl.server.base.ProtegeServer;
import org.protege.editor.owl.server.change.ChangeDocumentPool;
import org.protege.editor.owl.server.change.ChangeManagementFilter;
import org.protege.editor.owl.server.change.DefaultChangeService;
import org.protege.editor.owl.server.conflict.ConflictDetectionFilter;
import org.protege.editor.owl.server.http.exception.ServerConfigurationInitializationException;
import org.protege.editor.owl.server.http.exception.ServerException;
import org.protege.editor.owl.server.http.handlers.AuthenticationHandler;
import org.protege.editor.owl.server.http.handlers.CodeGenHandler;
import org.protege.editor.owl.server.http.handlers.HTTPChangeService;
import org.protege.editor.owl.server.http.handlers.HTTPLoginService;
import org.protege.editor.owl.server.http.handlers.HTTPServerHandler;
import org.protege.editor.owl.server.http.handlers.MetaprojectHandler;
import org.protege.editor.owl.server.policy.AccessControlFilter;
import org.protege.editor.owl.server.security.DefaultLoginService;
import org.protege.editor.owl.server.security.LoginTimeoutException;
import org.protege.editor.owl.server.security.SSLContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Paths;

import static org.protege.editor.owl.server.http.ServerEndpoints.*;
import static org.protege.editor.owl.server.http.ServerProperties.*;

public final class HTTPServer {

	public static final String SERVER_CONFIGURATION_PROPERTY = "org.protege.owl.server.configuration";

	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_PORT = 8080;

	private static Logger logger = LoggerFactory.getLogger(HTTPServer.class);

	private final String configurationFilePath;

	private final TokenTable loginTokenTable;

	private ServerConfiguration serverConfiguration;

	private Undertow webServer;
	private Undertow adminServer;

	private GracefulShutdownHandler webRouterHandler;
	private GracefulShutdownHandler adminRouterHandler;

	private boolean isRunning = false;
	
	private boolean isPaused = false;
	
	public boolean isPaused() { return isPaused; }
	
	public void pause() {
		isPaused = true;
	}
	
	public void resume() {
		isPaused = false;
	}
	
	public boolean isWorkFlowManager(User user, ProjectId pid) {
		try {
			return serverConfiguration.getRoles(user.getId(), pid, 
					GlobalPermissions.EXCLUDED).contains(serverConfiguration.getRole(new RoleIdImpl("mp-project-manager")));
		} catch (UnknownRoleIdException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	private static HTTPServer server;

	/**
	 * Default constructor
	 */
	public HTTPServer() throws Exception {
		this(System.getProperty(SERVER_CONFIGURATION_PROPERTY));
	}

	/**
	 * HTTP server constructor.
	 *
	 * @param configurationFilePath
	 *			The location of the server configuration file.
	 */
	public HTTPServer(@Nonnull String configurationFilePath) throws Exception {
		this.configurationFilePath = configurationFilePath;
		loadConfig(configurationFilePath);
		loginTokenTable = createLoginTokenTable();
		server = this;
	}

	public static HTTPServer server() {
		return server;
	}

	public void addSession(String key, AuthToken tok) {
		loginTokenTable.put(key, tok);
	}

	public AuthToken getAuthToken(String tok) throws LoginTimeoutException {
		return loginTokenTable.get(tok);
	}

	private void loadConfig(String filePath) throws ServerConfigurationInitializationException {
		try {
			serverConfiguration = ConfigurationManager.getConfigurationLoader()
					.loadConfiguration(new File(filePath));
		}
		catch (FileNotFoundException | ObjectConversionException e) {
			logger.error("Unable to load server configuration at location: " + filePath, e);
			throw new ServerConfigurationInitializationException("Unable to load server configuration", e);
		}
	}

	private void reloadConfig() throws ServerConfigurationInitializationException {
		loadConfig(configurationFilePath);
	}

	public void start() throws Exception {
		/*
		 * Instantiate Protege server modules
		 */
		ChangeDocumentPool changePool = new ChangeDocumentPool();
		ChangeService changeService = new DefaultChangeService(changePool);
		ProtegeServer pserver = new ProtegeServer(serverConfiguration);
		ServerLayer cmf = new ChangeManagementFilter(pserver, changePool);
		ServerLayer acf = new AccessControlFilter(new ConflictDetectionFilter(cmf, changeService));
		
		/*
		 * Instantiate and setup HTTP routing handlers
		 */
		RoutingHandler webRouter = Handlers.routing();
		RoutingHandler adminRouter = Handlers.routing();
		
		// use default login service for admin web server
	    LoginService adminLoginService = new DefaultLoginService();
		
		// create login handler for web server
		LoginService loginService = instantiateLoginService();
		
		loginService.setBackup(adminLoginService);
		
		HttpHandler login_handler = new BlockingHandler(new HTTPLoginService(loginService));
		
		webRouter.add("POST", LOGIN, login_handler);
		
		
		adminLoginService.setConfig(serverConfiguration);
		HttpHandler admin_login_handler = new BlockingHandler(new HTTPLoginService(adminLoginService));
		
		adminRouter.add("POST", LOGIN, admin_login_handler);
		
		// create change service handler
		HttpHandler changeServiceHandler = new AuthenticationHandler(new BlockingHandler(new HTTPChangeService(acf, changeService)));
		webRouter.add("POST", COMMIT,  changeServiceHandler);
		webRouter.add("POST", HEAD,  changeServiceHandler);
		webRouter.add("POST", LATEST_CHANGES,  changeServiceHandler);
		webRouter.add("POST", ALL_CHANGES,  changeServiceHandler);
		
		// create code generator handler
		HttpHandler codeGenHandler = new AuthenticationHandler(new BlockingHandler(new CodeGenHandler(serverConfiguration)));
		webRouter.add("GET", GEN_CODE, codeGenHandler);
		webRouter.add("POST", SET_CODEGEN_SEQ, codeGenHandler);
		webRouter.add("POST", EVS_REC, codeGenHandler);
		webRouter.add("GET", GEN_CON_HIST, codeGenHandler);
		
		
		// create mataproject handler
		HttpHandler metaprojectHandler = new AuthenticationHandler(new BlockingHandler(new MetaprojectHandler(pserver)));
		webRouter.add("GET", METAPROJECT, metaprojectHandler);
		webRouter.add("GET", PROJECT,  metaprojectHandler);
		webRouter.add("GET", PROJECT_SNAPSHOT,  metaprojectHandler);
		webRouter.add("GET", PROJECTS, metaprojectHandler);
		webRouter.add("GET", PROJECTS_UNCLASSIFIED, metaprojectHandler);
		
		adminRouter.add("GET", METAPROJECT, metaprojectHandler);
		adminRouter.add("POST", METAPROJECT, metaprojectHandler);
		adminRouter.add("POST", PROJECT,  metaprojectHandler);
		adminRouter.add("POST", PROJECT_SNAPSHOT,  metaprojectHandler);
		adminRouter.add("DELETE", PROJECT,  metaprojectHandler);
		
		ResourceHandler rh = Handlers.resource(
				new PathResourceManager(Paths.get(serverConfiguration.getServerRoot()), 100, false, null))
				.setDirectoryListingEnabled(false);
		
		HttpHandler arh = new AuthenticationHandler(rh);
		
		adminRouter.add("GET", serverConfiguration.getProperty(CON_HISTORY_FILE), arh);
   
		
		// create server handler
		AuthenticationHandler serverHandler = new AuthenticationHandler(new BlockingHandler(new HTTPServerHandler()));
		adminRouter.add("POST", SERVER_RESTART, serverHandler);
		adminRouter.add("POST", SERVER_STOP, serverHandler);
		adminRouter.add("POST", SERVER_SHUTDOWN, serverHandler);
		
		webRouter.add("GET", SERVER_PAUSE, serverHandler);
		webRouter.add("GET", SERVER_RESUME, serverHandler);

		
		// Build the servers
		webRouterHandler = Handlers.gracefulShutdown(Handlers.exceptionHandler(webRouter));
		adminRouterHandler = Handlers.gracefulShutdown(Handlers.exceptionHandler(adminRouter));
		
		logger.info("Starting server instances");
		final URI serverHostUri = serverConfiguration.getHost().getUri();
		final int serverAdminPort = serverConfiguration.getHost().getSecondaryPort().get().get();
		if (serverHostUri.getScheme().equalsIgnoreCase("https")) {
			SSLContext ctx = new SSLContextFactory().createSslContext();
			webServer = Undertow.builder()
					.addHttpsListener(serverHostUri.getPort(), serverHostUri.getHost(), ctx)
					.setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
					.setHandler(webRouterHandler)
					.build();
			webServer.start();
			logger.info("... Web server has started at port " + serverHostUri.getPort());
			
			adminServer = Undertow.builder()
					.addHttpsListener(serverAdminPort, serverHostUri.getHost(), ctx)
					.setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
					.setHandler(adminRouterHandler)
					.build();
			adminServer.start();
			logger.info("... Admin server has started at port " + serverAdminPort);
		}
		else {
			webServer = Undertow.builder()
					.addHttpListener(serverHostUri.getPort(), serverHostUri.getHost())
					.setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
					.setHandler(webRouterHandler)
					.build();
			webServer.start();
			logger.info("... Web server has started at port " + serverHostUri.getPort());
			
			adminServer = Undertow.builder()
					.addHttpListener(serverAdminPort, serverHostUri.getHost())
					.setServerOption(UndertowOptions.ALWAYS_SET_DATE, true)
					.setHandler(adminRouterHandler)
					.build();
			adminServer.start();
			logger.info("... Admin server has started at port " + serverAdminPort);
		}
		isRunning = true;
	}

	@Nonnull
	private LoginService instantiateLoginService() throws ServerException {
		String authClassName = serverConfiguration.getProperty(AUTHENTICATION_CLASS);
		if (authClassName == null) {
			throw new RuntimeException("Failed to initialize LoginService. " + AUTHENTICATION_CLASS + " property not set");
		}
		try {
			LoginService service = (LoginService) Class.forName(authClassName).newInstance();
			service.setConfig(serverConfiguration);
			return service;

		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	private TokenTable createLoginTokenTable() {
		long loginTimeout = TokenTable.DEFAULT_TIMEOUT_PERIOD;
		String loginTimeoutValue = serverConfiguration.getProperty(LOGIN_TIMEOUT_PERIOD);
		if (loginTimeoutValue != null && !loginTimeoutValue.isEmpty()) {
			loginTimeout = Long.parseLong(loginTimeoutValue);
		}
		return new TokenTable(loginTimeout);
	}

	public void stop() throws ServerException {
		if (isRunning) {
			logger.info("Stopping server instances");
			try {
				if (webServer != null) {
					if (webRouterHandler != null) {
						webRouterHandler.shutdown();
					}
					webServer.stop();
					webServer = null;
					logger.info("... Web server has stopped");
				}
				if (adminServer != null) {
					if (adminRouterHandler != null) {
						adminRouterHandler.shutdown();
					}
					adminServer.stop();
					adminServer = null;
					logger.info("... Admin server has stopped");
				}
				isRunning = false;
			}
			catch (Exception e) {
				throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, e.getMessage());
			}
		}
	}

	public void restart() throws ServerException {
		try {
			logger.info("Received request to restart");
			stop();
			reloadConfig();
			start();
		}
		catch (Exception e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	public static void main(final String[] args) throws ServerException {
		try {
			HTTPServer s = new HTTPServer();
			s.start();
		}
		catch (Exception e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
}
