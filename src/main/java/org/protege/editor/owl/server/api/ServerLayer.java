package org.protege.editor.owl.server.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.protege.editor.owl.server.versioning.api.HistoryFile;

import edu.stanford.protege.metaproject.api.Project;
import edu.stanford.protege.metaproject.api.ServerConfiguration;
import edu.stanford.protege.metaproject.api.User;

public abstract class ServerLayer implements Server {

    private List<ServerListener> listeners = new ArrayList<>();

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
    
    public HistoryFile createHistoryFile(String projectId, String projectName) throws IOException {
        String rootDir = getConfiguration().getServerRoot() + File.separator + projectId;
        String filename = projectName.replaceAll("\\s+","_"); // to snake-case
        return HistoryFile.createNew(rootDir, filename);
    }
    
    
    public String  getHistoryFilePath(Project proj) throws IOException {
    	
    	HistoryFile f = createHistoryFile(proj.getId().get(), proj.getName().get());
    	
    	return f.getAbsolutePath();
    }
}
