package org.protege.editor.owl.server.change;

import edu.stanford.protege.metaproject.api.*;
import org.protege.editor.owl.server.api.CommitBundle;
import org.protege.editor.owl.server.api.ServerFilterAdapter;
import org.protege.editor.owl.server.api.ServerLayer;
import org.protege.editor.owl.server.api.exception.AuthorizationException;
import org.protege.editor.owl.server.api.exception.OutOfSyncException;
import org.protege.editor.owl.server.api.exception.ServerServiceException;
import org.protege.editor.owl.server.versioning.ChangeHistoryImpl;
import org.protege.editor.owl.server.versioning.InvalidHistoryFileException;
import org.protege.editor.owl.server.versioning.api.ChangeHistory;
import org.protege.editor.owl.server.versioning.api.HistoryFile;
import org.protege.editor.owl.server.versioning.api.ServerDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Represents the change history manager that stores new changes from users.
 *
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class ChangeManagementFilter extends ServerFilterAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ChangeManagementFilter.class);

    private final ChangeDocumentPool changePool;

    public ChangeManagementFilter(ServerLayer delegate, ChangeDocumentPool changePool) {
        super(delegate);
        this.changePool = changePool;
    }

    @Override
    public ServerDocument createProject(AuthToken token, ProjectId projectId, Name projectName, Description description,
            UserId owner, Optional<ProjectOptions> options) throws AuthorizationException, ServerServiceException {
        ServerDocument serverDocument = super.createProject(token, projectId, projectName, description, owner, options);
        changePool.appendChanges(serverDocument.getHistoryFile(), ChangeHistoryImpl.createEmptyChangeHistory());
        return serverDocument;
    }

    @Override
    public ChangeHistory commit(AuthToken token, ProjectId projectId, CommitBundle commitBundle)
            throws AuthorizationException, OutOfSyncException, ServerServiceException {
        try {
            ChangeHistory changeHistory = super.commit(token, projectId, commitBundle);
            String projectFilePath = getHistoryFilePath(projectId);
            HistoryFile historyFile = HistoryFile.openExisting(projectFilePath);
            changePool.appendChanges(historyFile, changeHistory);
            return changeHistory;
        }
        catch (InvalidHistoryFileException e) {
            logger.error(printLog(token.getUser(), "Commit changes", e.getMessage()), e);
            throw new ServerServiceException(e.getMessage(), e);
        }
    }

    
}
