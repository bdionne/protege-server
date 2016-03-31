package org.protege.owl.server.api;

import org.protege.owl.server.api.exception.OWLServerException;
import org.protege.owl.server.api.exception.ServerRequestException;
import org.protege.owl.server.api.server.ServerListener;
import org.protege.owl.server.api.server.TransportHandler;

import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ProjectId;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.UserId;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ServerFilterAdapter extends AbstractServerFilter {

    public ServerFilterAdapter(ServerLayer delegate) {
        super(delegate);
    }

    @Override
    public void addUser(AuthToken token, User newUser) throws ServerRequestException {
        getDelegate().addUser(token, newUser);
    }

    @Override
    public void removeUser(AuthToken token, UserId userId) throws ServerRequestException {
        getDelegate().removeUser(token, userId);
    }

    @Override
    public void addProject(AuthToken token, Project newProject) throws ServerRequestException {
        getDelegate().addProject(token, newProject);
    }

    @Override
    public void removeProject(AuthToken token, ProjectId projectId) throws ServerRequestException {
        getDelegate().removeProject(token, projectId);
    }

    @Override
    public void viewProject(AuthToken token, ProjectId projectId) throws ServerRequestException {
        getDelegate().viewProject(token, projectId);
    }

    @Override
    public void commit(AuthToken token, Project project, CommitBundle changes) throws ServerRequestException {
        getDelegate().commit(token, project, changes);
    }

    @Override
    public void setTransport(TransportHandler transport) throws OWLServerException {
        getDelegate().setTransport(transport);
    }

    @Override
    public void addServerListener(ServerListener listener) {
        getDelegate().addServerListener(listener);
    }

    @Override
    public void removeServerListener(ServerListener listener) {
        getDelegate().removeServerListener(listener);
    }
}
