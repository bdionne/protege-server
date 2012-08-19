package org.protege.owl.server.policy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.protege.owl.server.api.AuthToken;
import org.protege.owl.server.api.ChangeHistory;
import org.protege.owl.server.api.ChangeMetaData;
import org.protege.owl.server.api.DocumentFactory;
import org.protege.owl.server.api.OntologyDocumentRevision;
import org.protege.owl.server.api.Server;
import org.protege.owl.server.api.ServerDirectory;
import org.protege.owl.server.api.ServerDocument;
import org.protege.owl.server.api.ServerFilter;
import org.protege.owl.server.api.ServerOntologyDocument;
import org.protege.owl.server.api.ServerPath;
import org.protege.owl.server.api.ServerTransport;
import org.protege.owl.server.api.exception.AuthenticationFailedException;
import org.protege.owl.server.api.exception.OWLServerException;
import org.protege.owl.server.connect.rmi.RMITransport;
import org.protege.owl.server.policy.generated.UsersAndGroupsLexer;
import org.protege.owl.server.policy.generated.UsersAndGroupsParser;

public class Authenticator extends ServerFilter {
    private Logger logger = Logger.getLogger(Authenticator.class.getCanonicalName());
    private UserDatabase userDb;
    private BasicLoginService loginService;

    public Authenticator(Server delegate) throws IOException, RecognitionException, OWLServerException {
        super(delegate);
        parse();
        loginService = new BasicLoginService(userDb);
    }
    
    private void parse() throws IOException, RecognitionException, OWLServerException {
        InputStream fis = getConfigurationInputStream("UsersAndGroups");
        try {
            ANTLRInputStream input = new ANTLRInputStream(fis);
            UsersAndGroupsLexer lexer = new UsersAndGroupsLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            UsersAndGroupsParser parser = new UsersAndGroupsParser(tokens);
            parser.top();
            userDb = parser.getUserDatabase();
        }
        finally {
            fis.close();
        }
    }

    @Override
    public void setTransports(Collection<ServerTransport> transports) {
        try {
            loadRMITransports(transports);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Authentication Service failed to start up.  Users may not be able to authenticate.", e);
        }
        getDelegate().setTransports(transports);
    }
    
    private void loadRMITransports(Collection<ServerTransport> transports) throws RemoteException, AlreadyBoundException {
        int rmitransports = 0;
        int othertransports = 0;
        for (ServerTransport transport : transports) {
            if (transport instanceof RMITransport) {
                rmitransports++;
                loadRMITransport((RMITransport) transport);
            }
            else {
                othertransports++;
            }
        }
        if (rmitransports == 0) {
            logger.warning("Did not find communications suitable for login.  Clients will be locked out.");
        }
        if (othertransports > 0) {
            logger.warning("Clients must use RMI to login.  Thereafter they can communicate with the server in other ways.");
        }      
    }
    
    private void loadRMITransport(RMITransport transport) throws RemoteException, AlreadyBoundException {
        int serverPort = transport.getServerPort();
        Remote remote = UnicastRemoteObject.exportObject(loginService, serverPort);
        transport.getRegistry().bind(LoginService.SERVICE, remote);
        logger.info("Authentication service started");
    }
    
    private void ensureUserIdCorrect(AuthToken u) throws AuthenticationFailedException {
        if (!loginService.checkAuthentication(u)) {
            throw new AuthenticationFailedException();
        }
    }

    @Override
    public Collection<ServerTransport> getTransports() {
        return getDelegate().getTransports();
    }

    @Override
    public ServerDocument getServerDocument(AuthToken u, ServerPath serverPath) throws OWLServerException  {
        ensureUserIdCorrect(u);
        return getDelegate().getServerDocument(u, serverPath);
    }

    @Override
    public Collection<ServerDocument> list(AuthToken u, ServerDirectory dir) throws OWLServerException {
        ensureUserIdCorrect(u);
        return getDelegate().list(u, dir);
    }

    @Override
    public ServerDirectory createDirectory(AuthToken u, ServerPath serverPath) throws OWLServerException {
        ensureUserIdCorrect(u);
        return getDelegate().createDirectory(u, serverPath);
    }

    @Override
    public ServerOntologyDocument createOntologyDocument(AuthToken u, ServerPath serverPath, Map<String, Object> settings) throws OWLServerException {
        ensureUserIdCorrect(u);
        return getDelegate().createOntologyDocument(u, serverPath, settings);
    }

    @Override
    public ChangeHistory getChanges(AuthToken u, ServerOntologyDocument doc, OntologyDocumentRevision start, OntologyDocumentRevision end) throws OWLServerException {
        ensureUserIdCorrect(u);
        return getDelegate().getChanges(u, doc, start, end);
    }

    @Override
    public void commit(AuthToken u, 
                                  ServerOntologyDocument doc, 
                                  ChangeHistory clientChanges) throws OWLServerException {
        ensureUserIdCorrect(u);
        ChangeHistory serverSideChanges = getChanges(u, doc, OntologyDocumentRevision.START_REVISION, null);
        ChangeMetaData commitComment = clientChanges.getMetaData(clientChanges.getStartRevision());
        if (commitComment == null) {
            throw new IllegalStateException("Changes to be committed must have metadata");
        }
        commitComment.setUser(u);
        getDelegate().commit(u, doc, clientChanges);
    }

    @Override
    public void shutdown() {
        getDelegate().shutdown();
    }

    @Override
    public InputStream getConfigurationInputStream(String fileName) throws OWLServerException {
        return getDelegate().getConfigurationInputStream(fileName);
    }
    
    @Override
    public OutputStream getConfigurationOutputStream(String fileName) throws OWLServerException {
        return getDelegate().getConfigurationOutputStream(fileName);
    }

    @Override
    public InputStream getConfigurationInputStream(ServerDocument doc, String extension) throws OWLServerException {
        return getDelegate().getConfigurationInputStream(doc, extension);
    }
    
    @Override
    public OutputStream getConfigurationOutputStream(ServerDocument doc, String extension) throws OWLServerException {
        return getDelegate().getConfigurationOutputStream(doc, extension);
    }
    
    @Override
    public DocumentFactory getDocumentFactory() {
        return getDelegate().getDocumentFactory();
    }

}