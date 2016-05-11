package org.protege.editor.owl.server.versioning;

import java.io.Serializable;

/**
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class DocumentRevision implements Comparable<DocumentRevision>, Serializable {

    private static final long serialVersionUID = 7037205560605439026L;

    public static final DocumentRevision START_REVISION = DocumentRevision.create(0);

    private int revision;

    private DocumentRevision(int revision) {
        this.revision = revision;
    }

    /**
     * Creates a document revision given its revision number.
     *
     * @param revision
     *          The revision number
     * @return an instance of {@code DocumentRevision}.
     */
    public static DocumentRevision create(int revision) {
        return new DocumentRevision(revision);
    }

    /**
     * Measures the length difference between the start revision and the end
     * revision. A positive result means the end revision is greater than the
     * start revision (forward-step). A negative result means the end revision
     * is lesser than the start revision (backward-step). A zero result means
     * both revisions are the same (no-step).
     * 
     * @param start
     *            The start revision.
     * @param end
     *            The end revision.
     * @return An integer number that measures the distance from the start to
     *         the end revision.
     */
    public static int delta(DocumentRevision start, DocumentRevision end) {
        return -1 * (start.revision - end.revision);
    }

    /**
     * Returns <code>true</code> if this revision is greater than the given
     * origin revision.
     */
    public boolean aheadOf(DocumentRevision origin) {
        return delta(this, origin) < 0;
    }

    /**
     * Returns <code>true</code> if this revision is lesser than the given
     * origin revision.
     */
    public boolean behind(DocumentRevision origin) {
        return delta(this, origin) > 0;
    }

    public DocumentRevision next() {
        return new DocumentRevision(revision + 1);
    }

    public DocumentRevision add(int delta) {
        return new DocumentRevision(revision + delta);
    }

    @Override
    public int compareTo(DocumentRevision o) {
        if (revision > o.revision) {
            return 1;
        }
        else if (revision < o.revision) {
            return -1;
        }
        else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DocumentRevision)) {
            return false;
        }
        return revision == ((DocumentRevision) other).revision;
    }

    @Override
    public int hashCode() {
        return revision + 42;
    }

    @Override
    public String toString() {
        return "" + revision;
    }
}
