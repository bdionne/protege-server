package org.protege.editor.owl.server.api;

import edu.stanford.protege.metaproject.api.*;
import org.protege.editor.owl.server.api.exception.AuthorizationException;
import org.protege.editor.owl.server.api.exception.OutOfSyncException;
import org.protege.editor.owl.server.api.exception.ServerServiceException;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.ServerDocument;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 *         Stanford Center for Biomedical Informatics Research
 */
public class ServerFilterAdapter extends ServerLayer {

	final protected ServerLayer delegate;

	public ServerFilterAdapter(ServerLayer delegate) {
		this.delegate = delegate;
	}

    @Override
    public void createUser(AuthToken token, User newUser, Optional<? extends Password> password)
            throws AuthorizationException, ServerServiceException {
			delegate.createUser(token, newUser, password);
    }

    @Override
    public void deleteUser(AuthToken token, UserId userId) throws AuthorizationException, ServerServiceException {
			delegate.deleteUser(token, userId);
    }

    @Override
    public void updateUser(AuthToken token, UserId userId, User user, Optional<? extends Password> password)
            throws AuthorizationException, ServerServiceException {
			delegate.updateUser(token, userId, user, password);
    }

    @Override
    public ServerDocument createProject(AuthToken token, ProjectId projectId, Name projectName, Description description,
            UserId owner, Optional<ProjectOptions> options) throws AuthorizationException, ServerServiceException {
			return delegate.createProject(token, projectId, projectName, description, owner, options);
    }

    @Override
    public void deleteProject(AuthToken token, ProjectId projectId, boolean includeFile)
            throws AuthorizationException, ServerServiceException {
			delegate.deleteProject(token, projectId, includeFile);
    }

    @Override
    public void updateProject(AuthToken token, ProjectId projectId, Project newProject)
            throws AuthorizationException, ServerServiceException {
			delegate.updateProject(token, projectId, newProject);
    }

    @Override
    public ServerDocument openProject(AuthToken token, ProjectId projectId)
            throws AuthorizationException, ServerServiceException {
			return delegate.openProject(token, projectId);
    }

    @Override
    public void createRole(AuthToken token, Role newRole) throws AuthorizationException, ServerServiceException {
			delegate.createRole(token, newRole);
    }

    @Override
    public void deleteRole(AuthToken token, RoleId roleId) throws AuthorizationException, ServerServiceException {
			delegate.deleteRole(token, roleId);
    }

    @Override
    public void updateRole(AuthToken token, RoleId roleId, Role newRole)
            throws AuthorizationException, ServerServiceException {
			delegate.updateRole(token, roleId, newRole);
    }

    @Override
    public void createOperation(AuthToken token, Operation operation)
            throws AuthorizationException, ServerServiceException {
			delegate.createOperation(token, operation);
    }

    @Override
    public void deleteOperation(AuthToken token, OperationId operationId)
            throws AuthorizationException, ServerServiceException {
			delegate.deleteOperation(token, operationId);
    }

    @Override
    public void updateOperation(AuthToken token, OperationId operationId, Operation newOperation)
            throws AuthorizationException, ServerServiceException {
			delegate.updateOperation(token, operationId, newOperation);
    }

    @Override
    public void assignRole(AuthToken token, UserId userId, ProjectId projectId, RoleId roleId)
            throws AuthorizationException, ServerServiceException {
			delegate.assignRole(token, userId, projectId, roleId);
    }

    @Override
    public void retractRole(AuthToken token, UserId userId, ProjectId projectId, RoleId roleId)
            throws AuthorizationException, ServerServiceException {
			delegate.retractRole(token, userId, projectId, roleId);
    }

    @Override
    public Host getHost(AuthToken token) throws AuthorizationException, ServerServiceException {
			return delegate.getHost(token);
    }

    @Override
    public void setHostAddress(AuthToken token, URI hostAddress) throws AuthorizationException, ServerServiceException {
			delegate.setHostAddress(token, hostAddress);
    }

    @Override
    public void setSecondaryPort(AuthToken token, int portNumber)
            throws AuthorizationException, ServerServiceException {
			delegate.setSecondaryPort(token, portNumber);
    }

    @Override
    public String getRootDirectory(AuthToken token) throws AuthorizationException, ServerServiceException {
			return delegate.getRootDirectory(token);
    }

    @Override
    public void setRootDirectory(AuthToken token, String rootDirectory)
            throws AuthorizationException, ServerServiceException {
			delegate.setRootDirectory(token, rootDirectory);
    }

    @Override
    public Map<String, String> getServerProperties(AuthToken token)
            throws AuthorizationException, ServerServiceException {
			return delegate.getServerProperties(token);
    }

    @Override
    public void setServerProperty(AuthToken token, String property, String value)
            throws AuthorizationException, ServerServiceException {
			delegate.setServerProperty(token, property, value);
    }

    @Override
    public void unsetServerProperty(AuthToken token, String property)
            throws AuthorizationException, ServerServiceException {
			delegate.unsetServerProperty(token, property);
    }

    @Override
    public ChangeHistory commit(AuthToken token, ProjectId projectId, CommitBundle commitBundle)
            throws AuthorizationException, OutOfSyncException, ServerServiceException {
			return delegate.commit(token, projectId, commitBundle);
    }

    

    @Override
    public void addServerListener(ServerListener listener) {
			delegate.addServerListener(listener);
    }

    @Override
    public void removeServerListener(ServerListener listener) {
			delegate.removeServerListener(listener);
    }

    @Override
    public List<User> getAllUsers(AuthToken token) throws AuthorizationException, ServerServiceException {
			return delegate.getAllUsers(token);
    }

    @Override
    public List<Project> getProjects(AuthToken token, UserId userId)
            throws AuthorizationException, ServerServiceException {
			return delegate.getProjects(token, userId);
    }

    @Override
    public List<Project> getAllProjects(AuthToken token) throws AuthorizationException, ServerServiceException {
			return delegate.getAllProjects(token);
    }

    @Override
    public Map<ProjectId, List<Role>> getRoles(AuthToken token, UserId userId, GlobalPermissions globalPermissions)
            throws AuthorizationException, ServerServiceException {
			return delegate.getRoles(token, userId, globalPermissions);
    }

    @Override
    public List<Role> getRoles(AuthToken token, UserId userId, ProjectId projectId, GlobalPermissions globalPermissions)
            throws AuthorizationException, ServerServiceException {
			return delegate.getRoles(token, userId, projectId, globalPermissions);
    }

    @Override
    public List<Role> getAllRoles(AuthToken token) throws AuthorizationException, ServerServiceException {
			return delegate.getAllRoles(token);
    }

    @Override
    public Map<ProjectId, List<Operation>> getOperations(AuthToken token, UserId userId, GlobalPermissions globalPermissions)
            throws AuthorizationException, ServerServiceException {
			return delegate.getOperations(token, userId, globalPermissions);
    }

    @Override
    public List<Operation> getOperations(AuthToken token, UserId userId, ProjectId projectId, GlobalPermissions globalPermissions)
            throws AuthorizationException, ServerServiceException {
			return delegate.getOperations(token, userId, projectId, globalPermissions);
    }

    @Override
    public List<Operation> getOperations(AuthToken token, RoleId roleId)
            throws AuthorizationException, ServerServiceException {
			return delegate.getOperations(token, roleId);
    }

    @Override
    public List<Operation> getAllOperations(AuthToken token) throws AuthorizationException, ServerServiceException {
			return delegate.getAllOperations(token);
    }

    @Override
    public boolean isOperationAllowed(AuthToken token, OperationId operationId, ProjectId projectId, UserId userId)
            throws AuthorizationException, ServerServiceException {
			return delegate.isOperationAllowed(token, operationId, projectId, userId);
    }

    @Override
    public boolean isOperationAllowed(AuthToken token, OperationId operationId, UserId userId)
            throws AuthorizationException, ServerServiceException {
			return delegate.isOperationAllowed(token, operationId, userId);
    }

	@Override
    public ServerConfiguration getConfiguration() {
		return delegate.getConfiguration();
    }
}
