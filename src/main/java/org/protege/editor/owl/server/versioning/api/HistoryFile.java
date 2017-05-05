package org.protege.editor.owl.server.versioning.api;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.protege.editor.owl.server.versioning.InvalidHistoryFileException;

/**
 * Represents the binary history file used by the Protege server to track changes.
 *
 * @author Josef Hardi <johardi@stanford.edu> <br>
 *         Stanford Center for Biomedical Informatics Research
 */
public class HistoryFile extends File {

    private static final long serialVersionUID = 5138690689632511640L;

    public static final String FILENAME = "history";

    /*
     * Avoid external initialization
     */
    private HistoryFile(@Nonnull String filePath) {
        super(checkNotNull(filePath));
    }

    /**
     * Returns a history file object and creates a new file located at the given file path.
     *
     * @param filepath
     *          A valid file location
     * @return A <code>HistoryFile</code> object.
     * @throws IOException If an I/O problem occurs
     */
    public static HistoryFile createNew(@Nonnull String filepath) throws IOException {
        return createNew(filepath, true);
    }

    /**
     * Returns a history file object with an option to create the file at the given file path.
     *
     * @param filepath
     *          A valid file location
     * @param doCreate
     *          Creates a new file in the file system if set <code>true</code>.
     * @return A <code>HistoryFile</code> object.
     * @throws IOException If an I/O problem occurs
     */
    public static HistoryFile createNew(@Nonnull String filepath, boolean doCreate) throws IOException {
        checkNotNull(filepath);
        if(!filepath.endsWith(FILENAME)) {
            if(!filepath.endsWith(File.separator)) {
                filepath = filepath + File.separator;
            }
            filepath = filepath + FILENAME;
        }
        HistoryFile f = new HistoryFile(filepath);
        if (doCreate) {
            FileUtils.touch(f); // Create an empty file in the file system
        }
        return f;
    }

    /**
     * Returns a history file object by opening an existing history file at the given parent
     * directory and input file name.
     *
     * @param filepath
     *          A valid file location
     * @return A <code>HistoryFile</code> object.
     * @throws InvalidHistoryFileException If the input file name is not followed by .history extension
     */
    public static HistoryFile openExisting(String filepath) throws InvalidHistoryFileException {
        return checkAndReturnHistoryFileWhenValid(filepath);
    }

    /*
     * Private helper methods
     */

    private static HistoryFile checkAndReturnHistoryFileWhenValid(String filepath) throws InvalidHistoryFileException {
        if (!filepath.endsWith(FILENAME)) {
            String message = String.format("File name must be %s", FILENAME);
            throw new InvalidHistoryFileException(message);
        }
        HistoryFile f = new HistoryFile(filepath);
        if (!f.exists()) {
            String message = String.format("Cannot found history file at path %s", filepath);
            throw new InvalidHistoryFileException(message);
        }
        return f;
    }
}
