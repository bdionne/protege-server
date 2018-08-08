package org.protege.editor.owl.server.api;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import edu.stanford.protege.metaproject.api.ProjectId;
import org.protege.editor.owl.server.http.HTTPServer;
import org.protege.editor.owl.server.http.exception.ServerException;
import org.protege.editor.owl.server.util.SnapShot;
import org.protege.editor.owl.server.versioning.api.HistoryFile;

import edu.stanford.protege.metaproject.api.ServerConfiguration;
import edu.stanford.protege.metaproject.api.User;
import org.semanticweb.binaryowl.BinaryOWLOntologyDocumentSerializer;
import org.semanticweb.binaryowl.owlapi.BinaryOWLOntologyBuildingHandler;
import org.semanticweb.binaryowl.owlapi.OWLOntologyWrapper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

import static org.protege.editor.owl.server.http.ServerProperties.CODEGEN_FILE;

public abstract class ServerLayer implements ServerServices {

    private static final String SNAPSHOT_CHECKSUM = "-checksum";

    private List<ServerListener> listeners = new ArrayList<>();

    private static Logger logger = LoggerFactory.getLogger(HTTPServer.class);

    /**
     * Get the server configuration
     *
     * @return Server configuration
     */
    public abstract ServerConfiguration getConfiguration();

    public void addServerListener(ServerListener listener) {
        listeners.add(listener);
    }

    public void removeServerListener(ServerListener listener) {
        int index = listeners.indexOf(listener);
        if (index != -1) {
            listeners.remove(index);
        }
    }

    protected static String printLog(User requester, String operation, String message) {
        if (requester != null) {
            String template = "[Request from %s (%s) - %s] %s";
            return String.format(template, requester.getId().get(), requester.getName().get(), operation, message);
        }
        else {
            String template = "[%s] %s";
            return String.format(template, operation, message);
        }
    }
    
    public HistoryFile createHistoryFile(ProjectId projectId) throws IOException {
        return HistoryFile.createNew(getHistoryFilePath(projectId));
    }

    public void createCodegenFile(String projectId) throws IOException {
        String rootDir = getConfiguration().getServerRoot() + File.separator + projectId;
        String filename = rootDir + File.separator + getConfiguration().getProperty(CODEGEN_FILE);
        OutputStream os = new FileOutputStream(filename);
        os.write("999999".getBytes());
    }
    
    public String getHistoryFilePath(@Nonnull ProjectId projectId) {
        return getConfiguration().getServerRoot()
                + File.separator
                + projectId.get()
                + File.separator
                + "history";
    }

    public void saveProjectSnapshot(SnapShot snapshot, @Nonnull ProjectId projectId, OutputStream responseStream)
            throws IOException {
        File snapshotFile = getSnapShotFile(projectId);
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(snapshotFile));
        try {
            BinaryOWLOntologyDocumentSerializer serializer = new BinaryOWLOntologyDocumentSerializer();
            long start = System.currentTimeMillis();
            serializer.write(new OWLOntologyWrapper(snapshot.getOntology()), new DataOutputStream(outputStream));
            logger.info("Saving snapshot in " + (System.currentTimeMillis() - start) + " ms");

            String snapshotChecksum = String.valueOf(snapshot.toString().hashCode());

            ObjectOutputStream oos = new ObjectOutputStream(responseStream);
            oos.writeObject(snapshotChecksum);

            OutputStream os = new FileOutputStream(snapshotFile.getAbsolutePath() + SNAPSHOT_CHECKSUM);
            os.write(snapshotChecksum.getBytes());
        }
        finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                }
                catch (IOException e) {
                    // Ignore the exception but report it into the log
                    logger.warn("Unable to close the file output stream used to save the snapshot", e);
                }
            }
        }
    }

    private File getSnapShotFile(@Nonnull ProjectId projectId) {
        return new File(getHistoryFilePath(projectId) + "-snapshot");
    }

    public OWLOntology loadProjectSnapshot(ProjectId projectId) throws OWLOntologyCreationException, IOException, ServerException {
        File snapshotFile = getSnapShotFile(projectId);
        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(snapshotFile));
        try {
            OWLOntologyManager ontoManager = OWLManager.createOWLOntologyManager();
            OWLOntology ontology = ontoManager.createOntology(); // use as a placeholder
            BinaryOWLOntologyDocumentSerializer serializer = new BinaryOWLOntologyDocumentSerializer();
            long start = System.currentTimeMillis();
            serializer.read(inputStream,
                    new BinaryOWLOntologyBuildingHandler(ontology),
                    ontoManager.getOWLDataFactory());
            System.out.println("Reading snapshot in " + (System.currentTimeMillis() - start) + " ms");
            return ontology;
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    // Ignore the exception but report it into the log
                    logger.warn("Unable to close the file input stream used to load the snapshot", e);
                }
            }
        }
    }

    public Optional<String> getSnapshotChecksum(ProjectId projectId) {
        Path path = Paths.get(getSnapShotFile(projectId).getAbsolutePath() + SNAPSHOT_CHECKSUM);
        try {
            return Optional.ofNullable(new String(Files.readAllBytes(path), Charset.defaultCharset()));
        }
        catch (IOException e) {
            // snapshot is not present
            return Optional.empty();
        }
    }
}
