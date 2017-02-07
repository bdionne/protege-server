package org.protege.editor.owl.server.security;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.net.ssl.SSLContext;

import org.protege.editor.owl.server.api.LoginService;
import org.protege.editor.owl.server.api.exception.ServerServiceException;

import com.unboundid.ldap.sdk.LDAPBindException;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;

import edu.stanford.protege.metaproject.api.AuthToken;
import edu.stanford.protege.metaproject.api.Password;
import edu.stanford.protege.metaproject.api.ServerConfiguration;
import edu.stanford.protege.metaproject.api.UserId;
import edu.stanford.protege.metaproject.api.exception.UnknownUserIdException;
import edu.stanford.protege.metaproject.impl.AuthorizedUserToken;

public class LDAPLoginService implements LoginService {
	
    private ServerConfiguration config;
    
    private static final String LDAPHOST = "ldap_host";
    private static final String LDAPPORT = "ldap_port";
    private static final String LDAPPREFIX = "ldap_dn_prefix";
    private static final String LDAPSUFFIX = "ldap_dn_suffix";
    
    private LoginService backup = null;
    
    public void setBackup(LoginService ls) { this.backup = ls; }
   
    public LDAPLoginService() {}   

    @Override
    public AuthToken login(UserId userid, Password password) throws ServerServiceException {
    	
    	AuthToken result = null;
    	
    	try {
    		String host = config.getProperty(LDAPHOST);
    		int port = Integer.parseInt(config.getProperty(LDAPPORT));
    		
    		String prefix = config.getProperty(LDAPPREFIX);
    		String suffix = config.getProperty(LDAPSUFFIX);
    		
    		SSLContext ctx = new SSLContextFactory().createSslContext();    		
    		
			LDAPConnection ldap = new LDAPConnection(ctx.getSocketFactory(), host, port,
					prefix + userid.get() + suffix, password.getPassword());
			
			//CN=fragosog,OU=Users,OU=NCI,OU=NIH,OU=AD,DC=nih,DC=gov

			
			
			
			if (ldap.isConnected()) {
				try {
					result = new AuthorizedUserToken(config.getUser(userid));
				} catch (UnknownUserIdException e) {
					throw new ServerServiceException("Invalid user id", e);					
				}
				
			} else {
				if (backup != null) {
					result = backup.login(userid, password);
					
				}				
			}
    	} catch (LDAPException | SSLContextInitializationException e1) {
    		if (backup != null) {
    			result = backup.login(userid, password);

    		} else {
    			if (e1 instanceof LDAPBindException) {
    				LDAPBindException ex = (LDAPBindException) e1;
    				ex.getBindResult().getResultCode().intValue();
    				throw new ServerServiceException("Issue with LDAP " + ex.getBindResult().getResultString(), ex);
    			}
    			throw new ServerServiceException("Issue with LDAP ",e1);
    		}
    	} 
    	return result;
    }

	@Override
	public void setConfig(ServerConfiguration config) {
		this.config = checkNotNull(config);
		
	}
}
