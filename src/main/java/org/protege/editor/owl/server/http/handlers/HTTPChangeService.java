package org.protege.editor.owl.server.http.handlers;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.impl.ProjectIdImpl;
import org.protege.editor.owl.server.api.ChangeService;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.api.ServerLayer;
import org.protege.editor.owl.server.api.exception.AuthorizationException;
import org.protege.editor.owl.server.api.exception.OutOfSyncException;
import org.protege.editor.owl.server.api.exception.ServerServiceException;
import org.protege.editor.owl.server.change.ChangeDocumentPool;
import org.protege.editor.owl.server.change.DefaultChangeService;
import org.protege.editor.owl.server.http.HTTPServer;
import org.protege.editor.owl.server.http.ServerEndpoints;
import org.protege.editor.owl.server.http.ServerProperties;
import org.protege.editor.owl.server.http.exception.ServerException;
import org.protege.editor.owl.server.security.LoginTimeoutException;
import org.protege.editor.owl.server.util.SnapShot;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.HistoryFile;

import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.User;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

public class HTTPChangeService extends BaseRoutingHandler {

	private final ServerLayer serverLayer;
	private ChangeService changeService;

	static enum PauseAllowed {
		OK, NOT_WORKFLOW_MANAGER, NOT_PAUSING_USER, SERVER_PAUSED;

		String message() {
			switch (this) {
				case OK:
					return "";
				case NOT_WORKFLOW_MANAGER:
					return "you aren't a workflow manager";
				case NOT_PAUSING_USER:
					return "Server in maintenance mode. Only pauser can do these actions now";
				case SERVER_PAUSED:
					return "Server in maintenance mode. Please try again later";
			}
			return "impossible"; // impossible to reach
		}

		boolean ok() {
			return this.equals(OK);
		}

		static PauseAllowed create(ProjectId projectId, User user, String path) {
			if (ServerEndpoints.LATEST_CHANGES.equals(path)) {
				return OK;
			} else if (HTTPServer.server().isWorkFlowManager(user, projectId)
			        && HTTPServer.server().isPausingUser(user)) {
				return OK;
			} else {
				return SERVER_PAUSED;
			}
		}
	}

	public HTTPChangeService(ServerLayer serverLayer, ChangeService changeService) {
		this.serverLayer = serverLayer;
		this.changeService = changeService;
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) {
		try {
			handlingRequest(exchange);
		}
		catch (IOException | ClassNotFoundException e) {
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
	}

	private void handlingRequest(HttpServerExchange exchange)
			throws IOException, ClassNotFoundException, LoginTimeoutException, ServerException {
		ObjectInputStream ois = new ObjectInputStream(exchange.getInputStream());
		
		String requestPath = exchange.getRequestPath();
		if (HTTPServer.server().isPaused()) {
			List<String> allowedPausedEndpoints = Lists.newArrayList(
				ServerEndpoints.COMMIT, ServerEndpoints.SQUASH, ServerEndpoints.LATEST_CHANGES);
			if (allowedPausedEndpoints.contains(requestPath)) {
				User user = this.getAuthToken(exchange).getUser();
				PauseAllowed ccs = PauseAllowed.create(projectId(exchange), user, requestPath);
				if (!ccs.ok()) {
					throw new ServerException(StatusCodes.SERVICE_UNAVAILABLE, ccs.message());
				}
			}
			else {
				throw new ServerException(StatusCodes.SERVICE_UNAVAILABLE, PauseAllowed.SERVER_PAUSED.message());
			}
		}

		
		if (	requestPath.equals(ServerEndpoints.COMMIT) ||
				requestPath.equals(ServerEndpoints.HEAD) ||
				requestPath.equals(ServerEndpoints.ALL_CHANGES) ||
				requestPath.equals(ServerEndpoints.LATEST_CHANGES) ||
				requestPath.equals(ServerEndpoints.SQUASH)) {
			ProjectId projectId = projectId(exchange);

			String clientChecksum = exchange.getRequestHeaders()
					.getFirst(ServerProperties.SNAPSHOT_CHECKSUM_HEADER);
			if (clientChecksum == null) {
				throw new ServerException(StatusCodes.BAD_REQUEST, "project " + projectId + " does not have a checksum");
			}
			Optional<String> serverChecksum = serverLayer.getSnapshotChecksum(projectId);
			if(serverChecksum.isPresent() && !clientChecksum.equals(serverChecksum.get())) {
				throw new ServerException(ServerProperties.HISTORY_SNAPSHOT_OUT_OF_DATE,
						"History snapshot out of date for "
								+ projectId + ": " + clientChecksum + " != " + serverChecksum.get());
			}
		}

		

		if (requestPath.equals(ServerEndpoints.COMMIT)) {
			CommitBundle bundle = (CommitBundle) ois.readObject();
			submitCommitBundle(getAuthToken(exchange), projectId(exchange), bundle, exchange.getOutputStream());
		}
		else if (requestPath.equals(ServerEndpoints.ALL_CHANGES)) {
			HistoryFile file = (HistoryFile) ois.readObject();
			retrieveAllChanges(file, exchange.getOutputStream());
		}
		else if (requestPath.equals(ServerEndpoints.LATEST_CHANGES)) {
			HistoryFile file = (HistoryFile) ois.readObject();
			DocumentRevision start = (DocumentRevision) ois.readObject();
			retrieveLatestChanges(file, start, exchange.getOutputStream());
		}
		else if (requestPath.equals(ServerEndpoints.HEAD)) {
			HistoryFile file = (HistoryFile) ois.readObject();
			retrieveHeadRevision(file, exchange.getOutputStream());
		}
		else if (requestPath.equals(ServerEndpoints.SQUASH)) {
			SnapShot snapshot = (SnapShot) ois.readObject();
			squashHistory(snapshot, projectId(exchange), exchange.getOutputStream());
		}
	}

	private ProjectId projectId(HttpServerExchange exchange) throws ServerException {
		String sProjectId = exchange.getRequestHeaders().getFirst(ServerProperties.PROJECTID_HEADER);
		if(sProjectId == null) {
			throw new ServerException(StatusCodes.BAD_REQUEST, "Missing ProjectId");
		}
		return new ProjectIdImpl(sProjectId);
	}

	/*
	 * Private methods that handlers each service provided by the server end-point above.
	 */

	private void submitCommitBundle(AuthToken authToken, ProjectId projectId, CommitBundle bundle,
			OutputStream os) throws ServerException {
		try {
			ChangeHistory hist = serverLayer.commit(authToken, projectId, bundle);
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(hist);
		}
		catch (AuthorizationException e) {
			throw new ServerException(StatusCodes.UNAUTHORIZED, "Access denied", e);
		}
		catch (OutOfSyncException e) {
			throw new ServerException(StatusCodes.CONFLICT, "Commit failed, please update your local copy first", e);
		}
		catch (ServerServiceException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to receive the commit data", e);
		}
		catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to transmit the returned data", e);
		}
	}

	private void retrieveAllChanges(HistoryFile file, OutputStream os) throws ServerException {
		try {
			DocumentRevision headRevision = changeService.getHeadRevision(file);
			ChangeHistory history = changeService.getChanges(file, DocumentRevision.START_REVISION, headRevision);
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(history);
		}
		catch (ServerServiceException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to get all changes", e);
		}
		catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to transmit the returned data", e);
		}
	}

	private void retrieveLatestChanges(HistoryFile file, DocumentRevision start, OutputStream os)
			throws ServerException {
		try {
			DocumentRevision headRevision = changeService.getHeadRevision(file);
			ChangeHistory history = changeService.getChanges(file, start, headRevision);
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(history);
		}
		catch (ServerServiceException | IllegalArgumentException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to get the latest changes", e);
		}
		catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to transmit the returned data", e);
		}
	}

	private void retrieveHeadRevision(HistoryFile file, OutputStream os) throws ServerException {
		try {
			DocumentRevision headRevision = changeService.getHeadRevision(file);
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(headRevision);
		}
		catch (ServerServiceException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to get the head revision", e);
		}
		catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to transmit the returned data", e);
		}
	}

	private void squashHistory(SnapShot snapShot, ProjectId projectId, OutputStream os) throws IOException {
		HistoryFile historyFile = serverLayer.createHistoryFile(projectId);
		String historyName = historyFile.getName();

		String archiveDir = serverLayer.getConfiguration().getProperty(ServerProperties.ARCHIVE_ROOT)
				+ File.separator
				+ projectId.get()
				+ File.separator
				+ "squash-"
				+ LocalDateTime.now()
				+ File.separator;

		String dataDir = serverLayer.getConfiguration().getServerRoot()
				+ File.separator
				+ projectId.get()
				+ File.separator;

		String snapshotName = historyName + "-snapshot";

		String fullHistoryPath = dataDir + historyName;
		String backupName = new StringBuilder(fullHistoryPath).insert(fullHistoryPath.lastIndexOf(File.separator) + 1, "~").toString();

		Files.createDirectories(Paths.get(archiveDir));
		Files.move(Paths.get(dataDir + historyName), Paths.get(archiveDir + historyName));
		Files.move(Paths.get(dataDir + snapshotName), Paths.get(archiveDir + snapshotName));
		try {
			Files.delete(Paths.get(backupName));
		} catch (NoSuchFileException e) {
			// no back up means no history, ok to proceed, just ignore this
		}
		Files.createFile(Paths.get(dataDir + historyName));

		serverLayer.saveProjectSnapshot(snapShot, projectId, os);
		
		changeService.clearHistoryCacheEntry(historyFile);
	}
}
