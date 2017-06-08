package org.protege.editor.owl.server.base;

import com.google.common.base.Strings;
import edu.stanford.protege.metaproject.ConfigurationManager;
import edu.stanford.protege.metaproject.api.*;
import edu.stanford.protege.metaproject.api.exception.*;
import edu.stanford.protege.metaproject.impl.ConfigurationBuilder;
import org.apache.commons.io.FileUtils;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.api.ServerLayer;
import org.protege.editor.owl.server.api.exception.AuthorizationException;
import org.protege.editor.owl.server.api.exception.ServerServiceException;
import org.protege.editor.owl.server.http.HTTPServer;
import org.protege.editor.owl.server.versioning.ChangeHistoryImpl;
import org.protege.editor.owl.server.versioning.Commit;
import org.protege.editor.owl.server.versioning.InvalidHistoryFileException;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.DocumentRevision;
import org.protege.editor.owl.server.versioning.api.HistoryFile;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The main server that acts as the end-point server where user requests to the
 * server get implemented.
 *
 * @author Josef Hardi <johardi@stanford.edu> <br>
 *         Stanford Center for Biomedical Informatics Research
 */
public class ProtegeServer extends ServerLayer {

    private static final Logger logger = LoggerFactory.getLogger(ProtegeServer.class);

    private static final PolicyFactory factory = ConfigurationManager.getFactory();

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReadLock readLock = readWriteLock.readLock();
    private final WriteLock writeLock = readWriteLock.writeLock();

    private ServerConfiguration configuration;

    public ProtegeServer(ServerConfiguration configuration) {
        this.configuration = checkNotNull(configuration);
    }

    @Override
    public ServerConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void createUser(AuthToken token, User newUser, Optional<? extends Password> newPassword)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            logger.info(printLog(token.getUser(), "Add user", newUser.toString()));
            configuration = new ConfigurationBuilder(configuration)
                    .addUser(newUser)
                    .createServerConfiguration();
            if (newPassword.isPresent()) {
                Password password = newPassword.get();
                if (password instanceof SaltedPasswordDigest) {
                    configuration = new ConfigurationBuilder(configuration)
                            .registerUser(newUser.getId(), (SaltedPasswordDigest) password)
                            .createServerConfiguration();
                }
            }
            saveChanges();
        }
        catch (IdAlreadyInUseException e) {
            logger.error(printLog(token.getUser(), "Add user", e.getMessage()));
            throw new ServerServiceException(e.getMessage(), e);
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void deleteUser(AuthToken token, UserId userId)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            User user = configuration.getUser(userId);
            logger.info(printLog(token.getUser(), "Remove user", user.toString()));
            configuration = new ConfigurationBuilder(configuration)
                    .removeUser(user)
                    .removePolicy(userId)
                    .unregisterUser(userId)
                    .createServerConfiguration();
            saveChanges();
        }
        catch (UnknownUserIdException e) {
            logger.error(printLog(token.getUser(), "Remove user", e.getMessage()));
            throw new ServerServiceException(e.getMessage(), e);
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void updateUser(AuthToken token, UserId userId, User updatedUser,
            Optional<? extends Password> updatedPassword)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            logger.info(printLog(token.getUser(), "Modify user", updatedUser.toString()));
            configuration = new ConfigurationBuilder(configuration)
                    .setUser(userId, updatedUser)
                    .createServerConfiguration();
            if (updatedPassword.isPresent()) {
                Password password = updatedPassword.get();
                if (password instanceof SaltedPasswordDigest) {
                    configuration = new ConfigurationBuilder(configuration)
                            .changePassword(userId, (SaltedPasswordDigest) password)
                            .createServerConfiguration();
                }
            }
            saveChanges();
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public ServerDocument createProject(AuthToken token, ProjectId projectId, Name projectName,
            Description description, UserId owner, Optional<ProjectOptions> options)
            throws AuthorizationException, ServerServiceException {
        try {
            HistoryFile historyFile = createHistoryFile(projectId);
            createCodegenFile(projectId.get());
            Project newProject = factory.getProject(
                    projectId, projectName, description, owner, options);
            try {
                writeLock.lock();
                logger.info(printLog(token.getUser(), "Add project", newProject.toString()));
                configuration = new ConfigurationBuilder(configuration)
                        .addProject(newProject)
                        .createServerConfiguration();
                saveChanges();
                return createServerDocument(historyFile);
            }
            catch (IdAlreadyInUseException e) {
                logger.error(printLog(token.getUser(), "Add project", e.getMessage()));
                throw new ServerServiceException(e.getMessage(), e);
            }
            finally {
                writeLock.unlock();
            }
        }
        catch (IOException e) {
            String message = "Failed to create history file in remote server";
            logger.error(printLog(token.getUser(), "Add project", message));
            throw new ServerServiceException(message, e);
        }
    }

    
   
    private ServerDocument createServerDocument(HistoryFile historyFile) {
        final URI serverAddress = configuration.getHost().getUri();
        final Optional<Port> registryPort = configuration.getHost().getSecondaryPort();
        if (registryPort.isPresent()) {
            Port port = registryPort.get();
            return new ServerDocument(serverAddress, port.get(), historyFile);
        }
        else {
            return new ServerDocument(serverAddress, historyFile);
        }
    }

    @Override
    public void deleteProject(AuthToken token, ProjectId projectId, boolean includeFile)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            Project project = configuration.getProject(projectId);
            logger.info(printLog(token.getUser(), "Remove project", project.toString()));
            configuration = new ConfigurationBuilder(configuration)
                    .removeProject(project)
                    .removePolicy(projectId)
                    .createServerConfiguration();
            if (includeFile) {
                String projectFilePath = getHistoryFilePath(projectId);
                HistoryFile historyFile = HistoryFile.openExisting(projectFilePath);
                File projectDir = historyFile.getParentFile();
                FileUtils.deleteDirectory(projectDir);
            }
            saveChanges();
        }
        catch (UnknownProjectIdException e) {
            logger.error(printLog(token.getUser(), "Remove project", e.getMessage()));
            throw new ServerServiceException(e.getMessage(), e);
        }
        catch (InvalidHistoryFileException e) {
            logger.error(printLog(token.getUser(), "Remove project", e.getMessage()));
            throw new ServerServiceException(e.getMessage(), e);
        }
        catch (IOException e) {
            logger.error(printLog(token.getUser(), "Remove project", e.getMessage()));
            throw new ServerServiceException(e.getMessage(), e);
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void updateProject(AuthToken token, ProjectId projectId, Project updatedProject)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            logger.info(printLog(token.getUser(), "Modify project", updatedProject.toString()));
            configuration = new ConfigurationBuilder(configuration)
                    .setProject(projectId, updatedProject)
                    .createServerConfiguration();
            saveChanges();
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public ServerDocument openProject(AuthToken token, ProjectId projectId)
            throws AuthorizationException, ServerServiceException {
        try {
            readLock.lock();
            Project project = configuration.getProject(projectId);
            logger.info(printLog(token.getUser(), "Open project", project.toString()));
            final URI serverAddress = configuration.getHost().getUri();
            final Optional<Port> registryPort = configuration.getHost().getSecondaryPort();
            final String path = getHistoryFilePath(projectId);
            if (registryPort.isPresent()) {
                Port port = registryPort.get();
                return new ServerDocument(serverAddress, port.get(), HistoryFile.openExisting(path));
            }
            else {
                return new ServerDocument(serverAddress, HistoryFile.openExisting(path));
            }
        }
        catch (UnknownProjectIdException e) {
            logger.error(printLog(token.getUser(), "Open project", e.getMessage()));
            throw new ServerServiceException(e);
        }
        catch (InvalidHistoryFileException e) {
            String message = "Unable to access history file in remote server";
            logger.error(printLog(token.getUser(), "Open project", message), e);
            throw new ServerServiceException(message, e);
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public void createRole(AuthToken token, Role newRole)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            logger.info(printLog(token.getUser(), "Add role", newRole.toString()));
            configuration = new ConfigurationBuilder(configuration)
                    .addRole(newRole)
                    .createServerConfiguration();
            saveChanges();
        }
        catch (IdAlreadyInUseException e) {
            logger.error(printLog(token.getUser(), "Add role", e.getMessage()));
            throw new ServerServiceException(e.getMessage(), e);
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void deleteRole(AuthToken token, RoleId roleId)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            Role role = configuration.getRole(roleId);
            logger.info(printLog(token.getUser(), "Remove role", role.toString()));
            configuration = new ConfigurationBuilder(configuration)
                    .removeRole(role)
                    .removePolicy(roleId)
                    .createServerConfiguration();
            saveChanges();
        }
        catch (UnknownRoleIdException e) {
            logger.error(printLog(token.getUser(), "Remove role", e.getMessage()));
            throw new ServerServiceException(e.getMessage(), e);
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void updateRole(AuthToken token, RoleId roleId, Role updatedRole) throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            logger.info(printLog(token.getUser(), "Modify role", updatedRole.toString()));
            configuration = new ConfigurationBuilder(configuration)
                    .setRole(roleId, updatedRole)
                    .createServerConfiguration();
            saveChanges();
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void createOperation(AuthToken token, Operation newOperation)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            logger.info(printLog(token.getUser(), "Add operation", newOperation.toString()));
            configuration = new ConfigurationBuilder(configuration)
                    .addOperation(newOperation)
                    .createServerConfiguration();
            saveChanges();
        }
        catch (IdAlreadyInUseException e) {
            logger.error(printLog(token.getUser(), "Add operation", e.getMessage()));
            throw new ServerServiceException(e.getMessage(), e);
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void deleteOperation(AuthToken token, OperationId operationId)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            Operation operation = configuration.getOperation(operationId);
            logger.info(printLog(token.getUser(), "Remove operation", operation.toString()));
            configuration = new ConfigurationBuilder(configuration)
                    .removeOperation(operation)
                    .removePolicy(operationId)
                    .createServerConfiguration();
            saveChanges();
        }
        catch (UnknownOperationIdException e) {
            logger.error(printLog(token.getUser(), "Remove operation", e.getMessage()));
            throw new ServerServiceException(e.getMessage(), e);
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void updateOperation(AuthToken token, OperationId operationId, Operation updatedOperation)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            logger.info(printLog(token.getUser(), "Modify operation", updatedOperation.toString()));
            configuration = new ConfigurationBuilder(configuration)
                    .setOperation(operationId, updatedOperation)
                    .createServerConfiguration();
            saveChanges();
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void assignRole(AuthToken token, UserId userId, ProjectId projectId, RoleId roleId)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            configuration = new ConfigurationBuilder(configuration)
                    .addPolicy(userId, projectId, roleId)
                    .createServerConfiguration();
            saveChanges();
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void retractRole(AuthToken token, UserId userId, ProjectId projectId, RoleId roleId)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            configuration = new ConfigurationBuilder(configuration)
                    .addPolicy(userId, projectId, roleId)
                    .createServerConfiguration();
            saveChanges();
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public Host getHost(AuthToken token) throws AuthorizationException, ServerServiceException {
        try {
            readLock.lock();
            return configuration.getHost();
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public void setHostAddress(AuthToken token, URI hostAddress)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            Optional<Port> secondaryPort = getHost(token).getSecondaryPort();
            Host updatedHost = factory.getHost(hostAddress, secondaryPort);
            configuration = new ConfigurationBuilder(configuration)
                    .setHost(updatedHost)
                    .createServerConfiguration();
            saveChanges();
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public void setSecondaryPort(AuthToken token, int portNumber)
            throws AuthorizationException, ServerServiceException {
        URI hostAddress = getHost(token).getUri();
        try {
            writeLock.lock();
            Optional<Port> secondaryPort = Optional.empty();
            if (portNumber > 0) {
                secondaryPort = Optional.of(factory.getPort(portNumber));
            }
            Host updatedHost = factory.getHost(hostAddress, secondaryPort);
            configuration = new ConfigurationBuilder(configuration)
                    .setHost(updatedHost)
                    .createServerConfiguration();
            saveChanges();
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public String getRootDirectory(AuthToken token)
            throws AuthorizationException, ServerServiceException {
        try {
            readLock.lock();
            return configuration.getServerRoot().toString();
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public void setRootDirectory(AuthToken token, String rootDirectory)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            configuration = new ConfigurationBuilder(configuration)
                    .setServerRoot(rootDirectory)
                    .createServerConfiguration();
            saveChanges();
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public Map<String, String> getServerProperties(AuthToken token)
            throws AuthorizationException, ServerServiceException {
        try {
            readLock.lock();
            return configuration.getProperties();
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public void setServerProperty(AuthToken token, String property, String value)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            configuration = new ConfigurationBuilder(configuration)
                    .addProperty(property, value)
                    .createServerConfiguration();
            saveChanges();
        }
        finally {
            writeLock.lock();
        }
    }

    @Override
    public void unsetServerProperty(AuthToken token, String property)
            throws AuthorizationException, ServerServiceException {
        try {
            writeLock.lock();
            configuration = new ConfigurationBuilder(configuration)
                    .removeProperty(property)
                    .createServerConfiguration();
            saveChanges();
        }
        finally {
            writeLock.unlock();
        }
    }

    @Override
    public ChangeHistory commit(AuthToken token, ProjectId projectId, CommitBundle commitBundle)
            throws AuthorizationException, ServerServiceException {
        DocumentRevision baseRevision = commitBundle.getBaseRevision();
        ChangeHistory changeHistory = ChangeHistoryImpl.createEmptyChangeHistory(baseRevision);
        for (Commit commit : commitBundle.getCommits()) {
            changeHistory.addRevision(commit.getMetadata(), commit.getChanges());
            String message = String.format("Receive revision %s: %s",
                    changeHistory.getHeadRevision(), commit.getMetadata().getComment());
            logger.info(printLog(token.getUser(), "Commit changes", message));
        }
        return changeHistory;
    }


    @Override
    public List<User> getAllUsers(AuthToken token)
            throws AuthorizationException, ServerServiceException {
        try {
            readLock.lock();
            return new ArrayList<>(configuration.getUsers());
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Project> getProjects(AuthToken token, UserId userId)
            throws AuthorizationException, ServerServiceException {
        try {
            readLock.lock();
            return new ArrayList<>(configuration.getProjects(userId));
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Project> getAllProjects(AuthToken token)
            throws AuthorizationException, ServerServiceException {
        try {
            readLock.lock();
            return new ArrayList<>(configuration.getProjects());
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public Map<ProjectId, List<Role>> getRoles(AuthToken token, UserId userId,
            GlobalPermissions globalPermissions)
            throws AuthorizationException, ServerServiceException {
        Map<ProjectId, List<Role>> roleMap = new HashMap<>();
        for (Project project : getAllProjects(token)) {
            roleMap.put(project.getId(), getRoles(token, userId, project.getId(), globalPermissions));
        }
        return roleMap;
    }

    @Override
    public List<Role> getRoles(AuthToken token, UserId userId, ProjectId projectId,
            GlobalPermissions globalPermissions)
            throws AuthorizationException, ServerServiceException {
        try {
            readLock.lock();
            return new ArrayList<>(configuration.getRoles(userId, projectId, globalPermissions));
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Role> getAllRoles(AuthToken token)
            throws AuthorizationException, ServerServiceException {
        try {
            readLock.lock();
            return new ArrayList<>(configuration.getRoles());
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public Map<ProjectId, List<Operation>> getOperations(AuthToken token, UserId userId,
            GlobalPermissions globalPermissions)
            throws AuthorizationException, ServerServiceException {
        Map<ProjectId, List<Operation>> operationMap = new HashMap<>();
        for (Project project : getAllProjects(token)) {
            operationMap.put(project.getId(), getOperations(token, userId, project.getId(), globalPermissions));
        }
        return operationMap;
    }

    @Override
    public List<Operation> getOperations(AuthToken token, UserId userId, ProjectId projectId,
            GlobalPermissions globalPermissions)
            throws AuthorizationException, ServerServiceException {
        try {
            readLock.lock();
            return new ArrayList<>(configuration.getOperations(userId, projectId, globalPermissions));
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Operation> getOperations(AuthToken token, RoleId roleId)
            throws AuthorizationException, ServerServiceException {
        try {
            readLock.lock();
            return new ArrayList<>(configuration.getOperations(configuration.getRole(roleId)));
        }
        catch (UnknownRoleIdException e) {
            logger.error(printLog(token.getUser(), "List operations", e.getMessage()));
            throw new ServerServiceException(e.getMessage(), e);
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Operation> getAllOperations(AuthToken token)
            throws AuthorizationException, ServerServiceException {
        try {
            readLock.lock();
            return new ArrayList<>(configuration.getOperations());
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isOperationAllowed(AuthToken token, OperationId operationId, ProjectId projectId, UserId userId)
            throws AuthorizationException, ServerServiceException {
        try {
            readLock.lock();
            return configuration.isOperationAllowed(operationId, projectId, userId);
        }
        finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isOperationAllowed(AuthToken token, OperationId operationId, UserId userId)
            throws AuthorizationException, ServerServiceException {
        try {
            readLock.lock();
            return configuration.isOperationAllowed(operationId, userId);
        }
        finally {
            readLock.unlock();
        }
    }

    private void saveChanges() throws ServerServiceException {
        try {
            String configLocation = System.getProperty(HTTPServer.SERVER_CONFIGURATION_PROPERTY);
            if (Strings.isNullOrEmpty(configLocation)) {
            	throw new RuntimeException("Config property " + HTTPServer.SERVER_CONFIGURATION_PROPERTY + " isn't set");
						}
            File configurationFile = new File(configLocation);
            ConfigurationManager.getConfigurationWriter().saveConfiguration(configuration, configurationFile);
        }
        catch (IOException e) {
            String message = "Unable to save server configuration";
            logger.error(printLog(null, "Save configuration", message), e);
            throw new ServerServiceException(message, e);
        }
    }
    
    
}
