package org.protege.editor.owl.server.http;

/**
 * Represents the constant values for defining server properties.
 *
 * @author Josef Hardi <johardi@stanford.edu> <br>
 * Stanford Center for Biomedical Informatics Research
 */
public final class ServerProperties {

    /*
     * Constants for Code Generation module
     */
    public static final String CODEGEN_PREFIX = "codegen_prefix";
    public static final String CODEGEN_SUFFIX = "codegen_suffix";
    public static final String CODEGEN_DELIMETER = "codegen_delimeter";
    public static final String CODEGEN_FILE = "codegen_file";

    /*
     * Constants for EVS History module
     */
    public static final String CUR_EVS_HISTORY_FILE = "current_evshistory_file";
    public static final String EVS_HISTORY_FILE = "evshistory_file";
    public static final String CON_HISTORY_FILE = "conhistory_file";
    public static final String ARCHIVE_ROOT = "root_archive";

    /*
     * Constants for Authentication module
     */
    public static final String AUTHENTICATION_CLASS = "authenticate";

    /*
     * Constants for timeout period
     */
    public static final String LOGIN_TIMEOUT_PERIOD = "login_timeout_period";

    /*
     * Constants for custom HTTP headers and exceptions
     */
    public static final String PROJECTID_HEADER = "X-ProjectId";
    public static final String SNAPSHOT_CHECKSUM_HEADER = "X-SnapshotId";
    public static final int HISTORY_SNAPSHOT_OUT_OF_DATE = 499;

    // pellette

	public static final String PELLET_ADMIN_PASSWORD = "pellet_admin_password";
}
