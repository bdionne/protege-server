package org.protege.editor.owl.server.http.handlers;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import edu.stanford.protege.metaproject.impl.ServerStatus;
import org.protege.editor.owl.server.api.ServerLayer;
import org.protege.editor.owl.server.api.exception.AuthorizationException;
import org.protege.editor.owl.server.api.exception.ServerServiceException;
import org.protege.editor.owl.server.http.HTTPServer;
import org.protege.editor.owl.server.http.ServerEndpoints;
import org.protege.editor.owl.server.http.ServerProperties;
import org.protege.editor.owl.server.http.exception.ServerException;
import org.protege.editor.owl.server.security.LoginTimeoutException;
import org.protege.editor.owl.server.util.SnapShot;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.protege.metaproject.ConfigurationManager;
import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.Description;
import edu.stanford.protege.metaproject.api.Name;
import edu.stanford.protege.metaproject.api.PolicyFactory;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.ProjectOptions;
import edu.stanford.protege.metaproject.api.Serializer;
import edu.stanford.protege.metaproject.api.ServerConfiguration;
import edu.stanford.protege.metaproject.api.UserId;
import edu.stanford.protege.metaproject.api.exception.ObjectConversionException;
import edu.stanford.protege.metaproject.serialization.DefaultJsonSerializer;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

import javax.swing.text.html.Option;

public class MetaprojectHandler extends BaseRoutingHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(MetaprojectHandler.class);
	private static final PolicyFactory f = ConfigurationManager.getFactory();
	private final ServerLayer serverLayer;

	private boolean requiredRestarting = false;

	public MetaprojectHandler(ServerLayer serverLayer) {
		this.serverLayer = serverLayer;
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) {
		try {
			handlingRequest(exchange);
		}
		catch (IOException | ClassNotFoundException | ObjectConversionException e) {
			internalServerErrorStatusCode(exchange, "Server failed to receive the sent data", e);
		}
		catch (LoginTimeoutException e) {
			loginTimeoutErrorStatusCode(exchange, e);
		}
		catch (ServerException e) {
			handleServerException(exchange, e);
		}
		finally {
			exchange.endExchange(); // end the request
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

	private void handlingRequest(HttpServerExchange exchange)
			throws IOException, ClassNotFoundException, ObjectConversionException,
			LoginTimeoutException, ServerException {
		String requestPath = exchange.getRequestPath();
		HttpString requestMethod = exchange.getRequestMethod();
		if (requestPath.equals(ServerEndpoints.PROJECTS)) {
			UserId userId = f.getUserId(getQueryParameter(exchange, "userid"));
			retrieveProjectList(userId, exchange.getOutputStream());
		}
		else if (requestPath.equals(ServerEndpoints.PROJECT) && requestMethod.equals(Methods.POST)) {
			ObjectInputStream ois = new ObjectInputStream(exchange.getInputStream());
			ProjectId pid = (ProjectId) ois.readObject();
			Name pname = (Name) ois.readObject();
			Description desc = (Description) ois.readObject();
			UserId uid = (UserId) ois.readObject();
			Optional<ProjectOptions> oopts = Optional.ofNullable((ProjectOptions) ois.readObject());
			createNewProject(getAuthToken(exchange), pid, pname, desc, uid, oopts, exchange.getOutputStream());
		}
		else if (requestPath.equals(ServerEndpoints.PROJECT) && requestMethod.equals(Methods.GET)) {
			ProjectId projectId = f.getProjectId(getQueryParameter(exchange, "projectid"));
			openExistingProject(getAuthToken(exchange), projectId, exchange.getOutputStream());
			Optional<String> checksum = serverLayer.getSnapshotChecksum(projectId);
			if (checksum.isPresent()) {
				exchange.getResponseHeaders().put(new HttpString(ServerProperties.SNAPSHOT_CHECKSUM_HEADER),
						checksum.get());
			}
		}
		else if (requestPath.equals(ServerEndpoints.PROJECT) && requestMethod.equals(Methods.DELETE)) {
			ProjectId projectId = f.getProjectId(getQueryParameter(exchange, "projectid"));
			deleteExistingProject(getAuthToken(exchange), projectId);
		}
		else if (requestPath.equals(ServerEndpoints.PROJECT_SNAPSHOT) && requestMethod.equals(Methods.POST)) {
			ObjectInputStream ois = new ObjectInputStream(exchange.getInputStream());
			ProjectId pid = (ProjectId) ois.readObject();
			SnapShot snapshot = (SnapShot) ois.readObject();
			createProjectSnapshot(pid, snapshot, exchange.getOutputStream());
		}
		else if (requestPath.equals(ServerEndpoints.PROJECT_SNAPSHOT) && requestMethod.equals(Methods.GET)) {
			ProjectId projectId = f.getProjectId(getQueryParameter(exchange, "projectid"));
			retrieveProjectSnapshot(projectId, exchange.getOutputStream());
		}
		else if (requestPath.equals(ServerEndpoints.METAPROJECT) && requestMethod.equals(Methods.GET)) {
			retrieveMetaproject(exchange);
		}
		else if (requestPath.equals(ServerEndpoints.METAPROJECT) && requestMethod.equals(Methods.POST)) {
			Serializer serl = new DefaultJsonSerializer();
			ServerConfiguration cfg = serl.parse(new InputStreamReader(exchange.getInputStream()), ServerConfiguration.class);
			updateMetaproject(cfg);
			requiredRestarting = true;
		} else if (requestPath.equals(ServerEndpoints.PROJECTS_UNCLASSIFIED) && requestMethod.equals(Methods.GET)) {
        retrieveProjectsUnclassified(exchange);
    } else if (requestPath.equals(ServerEndpoints.SERVER_STATUS) && requestMethod.equals(Methods.GET)) {
			retrieveServerStatus(exchange.getOutputStream());
		}
	}

	public void retrieveProjectsUnclassified(HttpServerExchange exchange) throws ServerException {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(exchange.getOutputStream());
			List<Project> projects = new ArrayList<>();
			try {
				for (Project project : serverLayer.getAllProjects(getAuthToken(exchange))) {
					boolean classifiable = project.getOptions()
						.map(projectOptions -> projectOptions.getValue("classifiable").equals("true")).orElse(false);
					if (classifiable) {
						projects.add(project);
					}
				}
				oos.writeObject(projects);
			} catch (AuthorizationException e) {
				throw new ServerException(StatusCodes.UNAUTHORIZED, e);
			} catch (ServerServiceException e) {
				throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, e);
			} catch (LoginTimeoutException e) {
				throw new RuntimeException(e);
			}
		} catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to transmit returned data", e);
		}
	}

	/*
	 * Private methods that handlers each service provided by the server end-point above.
	 */

	private void retrieveProjectList(UserId userId, OutputStream os) throws ServerException {
		try {
			List<Project> projects = new ArrayList<>(serverLayer.getConfiguration().getProjects(userId));
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(projects);
		}
		catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to transmit the returned data", e);
		}
	}

	private void createNewProject(AuthToken authToken, ProjectId pid, Name pname,
			Description desc, UserId uid, Optional<ProjectOptions> oopts, OutputStream os) throws ServerException {
		try {
			ServerDocument doc = serverLayer.createProject(authToken, pid, pname, desc, uid, oopts);
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(doc);
		}
		catch (AuthorizationException e) {
			throw new ServerException(StatusCodes.UNAUTHORIZED, "Access denied", e);
		}
		catch (ServerServiceException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to create the project", e);
		}
		catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to transmit the returned data", e);
		}
	}

	private void openExistingProject(AuthToken authToken, ProjectId projectId, OutputStream os) throws ServerException {
		try {
			ServerDocument sdoc = serverLayer.openProject(authToken, projectId);
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(sdoc);
		}
		catch (AuthorizationException e) {
			throw new ServerException(StatusCodes.UNAUTHORIZED, "Access denied", e);
		}
		catch (ServerServiceException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to open the project", e);
		}
		catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to transmit the returned data", e);
		}
	}

	private void deleteExistingProject(AuthToken authToken, ProjectId projectId) throws ServerException {
		try {
			serverLayer.deleteProject(authToken, projectId, true);
		}
		catch (AuthorizationException e) {
			throw new ServerException(StatusCodes.UNAUTHORIZED, "Access denied", e);
		}
		catch (ServerServiceException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to delete the project", e);
		}
	}

	private void createProjectSnapshot(ProjectId projectId, SnapShot snapshot, OutputStream os) throws ServerException {
		try {
			serverLayer.saveProjectSnapshot(snapshot, projectId, os);
		}
		catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to create project snapshot", e);
		}
	}

	private void retrieveProjectSnapshot(ProjectId projectId, OutputStream os) throws ServerException {
		try {
			OWLOntology ontIn = serverLayer.loadProjectSnapshot(projectId);
			try {
				ObjectOutputStream oos = new ObjectOutputStream(os);
				oos.writeObject(new SnapShot(ontIn));
				oos.writeObject(serverLayer.getSnapshotChecksum(projectId).get());
			}
			catch (IOException e) {
				throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to transmit the returned data", e);
			}
		}
		catch (OWLOntologyCreationException | IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to fetch project snapshot", e);
		}
	}

	private void retrieveMetaproject(HttpServerExchange exchange) throws ServerException {
		try {
			Serializer serl = new DefaultJsonSerializer();
			exchange.getResponseSender().send(serl.write(serverLayer.getConfiguration(), ServerConfiguration.class));
		}
		catch (Exception e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to get server configuration", e);
		}
	}

	private void updateMetaproject(ServerConfiguration cfg) throws ServerException {
		try {
			String configLocation = System.getProperty(HTTPServer.SERVER_CONFIGURATION_PROPERTY);
			ConfigurationManager.getConfigurationWriter().saveConfiguration(cfg, new File(configLocation));
		}
		catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to save changes of the metaproject", e);
		}
	}

	private void retrieveServerStatus(OutputStream os) throws ServerException {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(os);
			ServerStatus serverStatus = new ServerStatus(HTTPServer.server().pausedUser());
			oos.writeObject(serverStatus);
		}
		catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to construct ServerStatus");
		}
	}
}
