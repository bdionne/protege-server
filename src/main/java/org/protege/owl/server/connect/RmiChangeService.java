package org.protege.owl.server.connect;

import org.protege.owl.server.api.ChangeService;
import org.protege.owl.server.changes.OntologyDocumentRevision;
import org.protege.owl.server.changes.api.ChangeHistory;

import java.rmi.RemoteException;

import edu.stanford.protege.metaproject.api.Address;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class RmiChangeService implements RemoteChangeService {

    public static String CHANGE_SERVICE = "RmiChangeService";

    private ChangeService changeService;

    public RmiChangeService(ChangeService changeService) {
        this.changeService = changeService;
    }

    @Override
    public ChangeHistory getChanges(Address resourceLocation, OntologyDocumentRevision startRevision,
            OntologyDocumentRevision endRevision) throws Exception {
        try {
            return changeService.getChanges(resourceLocation, startRevision, endRevision);
        }
        catch (Exception e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public ChangeHistory getAllChanges(Address resourceLocation) throws Exception {
        try {
            return changeService.getAllChanges(resourceLocation);
        }
        catch (Exception e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }

    @Override
    public ChangeHistory getLatestChanges(Address resourceLocation, OntologyDocumentRevision startRevision)
            throws Exception {
        try {
            return changeService.getLatestChanges(resourceLocation, startRevision);
        }
        catch (Exception e) {
            throw new RemoteException(e.getMessage(), e);
        }
    }
}
