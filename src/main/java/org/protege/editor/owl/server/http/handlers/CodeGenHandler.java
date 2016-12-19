package org.protege.editor.owl.server.http.handlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.protege.editor.owl.server.http.ServerEndpoints;
import static org.protege.editor.owl.server.http.ServerProperties.*;
import org.protege.editor.owl.server.http.exception.ServerException;
import org.protege.editor.owl.server.http.messages.History;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.protege.metaproject.api.ServerConfiguration;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

public class CodeGenHandler extends BaseRoutingHandler {

	private static Logger logger = LoggerFactory.getLogger(CodeGenHandler.class);

	private final ServerConfiguration serverConfiguration;
	
	public CodeGenHandler(@Nonnull ServerConfiguration serverConfiguration) {
		this.serverConfiguration = serverConfiguration;
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) {
		try {
			handlingRequest(exchange);
		}
		catch (IOException | ClassNotFoundException e) {
			internalServerErrorStatusCode(exchange, "Server failed to receive the sent data", e);
		}
		catch (ServerException e) {
			handleServerException(exchange, e);
		}
		finally {
			exchange.endExchange(); // end request
		}
	}

	private void handlingRequest(HttpServerExchange exchange)
			throws IOException, ClassNotFoundException, ServerException {
		String requestPath = exchange.getRequestPath();
		HttpString requestMethod = exchange.getRequestMethod();
		if (requestPath.equals(ServerEndpoints.GEN_CODE)) {
			int cnt = readIntParameter("count", exchange);
			String p = serverConfiguration.getProperty(CODEGEN_PREFIX);
			String s = serverConfiguration.getProperty(CODEGEN_SUFFIX);
			String d = serverConfiguration.getProperty(CODEGEN_DELIMETER);
			String cfn = serverConfiguration.getProperty(CODEGEN_FILE);
			int seq = 0;
			try {
				File codeGenFile = new File(cfn);
				FileReader fileReader = new FileReader(codeGenFile);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				seq = Integer.parseInt(bufferedReader.readLine().trim());
				
				List<String> codes = new ArrayList<String>();
				String sseq = "0";
				for (int j = 0; j < cnt; j++) {
					sseq = (new Integer(seq++)).toString();
					String code = "";
					if (p != null) code += p;
					if (d != null) code += d;
					code += sseq;					
					if (s != null) {
						if (d != null) {
							code += d + s;
						} else {
							code += s;
						}					
					}
					codes.add(code);
				}
				ObjectOutputStream os = new ObjectOutputStream(exchange.getOutputStream());
				os.writeObject(codes);
				
				try {
					fileReader.close();
				}
				catch (IOException e) {
					// Ignore the exception but report it into the log
					logger.warn("Unable to close the file reader stream used to read the code generator configuration", e);
				}
				flushCode(codeGenFile, seq);
			}
			catch (IOException e) {
				internalServerErrorStatusCode(exchange, "Server failed to read code generator configuration", e);
			}
		} 
		else if (requestPath.equals(ServerEndpoints.SET_CODEGEN_SEQ) && requestMethod.equals(Methods.POST)) {
			int seq = readIntParameter("seq", exchange);			
			String cfn = serverConfiguration.getProperty(CODEGEN_FILE);
			File codeGenFile = new File(cfn);
			flushCode(codeGenFile, seq);			
		}
		else if (requestPath.equals(ServerEndpoints.EVS_REC)) {
			ObjectInputStream ois = new ObjectInputStream(exchange.getInputStream());
			History hist = (History) ois.readObject();
			recordEvsHistory(hist);
		} else if (requestPath.equals(ServerEndpoints.CON_HISTORY_REC) 
				&& requestMethod.equals(Methods.POST)) {
			ObjectInputStream ois = new ObjectInputStream(exchange.getInputStream());
			History hist = (History) ois.readObject();
			recordConceptHistory(hist);
		}
	}

	private int readIntParameter(String name, HttpServerExchange exchange) {
		int res = 1;
		String sres = "";
		try {
			sres = getQueryParameter(exchange, name);
			res = Integer.parseInt(sres);
		}
		catch (ServerException e) {
			// Ignore the exception but report it into the log
			logger.warn(e.getLocalizedMessage());
			logger.warn("... Using default value (" + name + " = " + res + ")");
		}
		catch (NumberFormatException e) {
			// Ignore the exception but report it into the log
			logger.warn("Unable to convert to number (" + name + " = " + sres + ")");
			logger.warn("... Using default value (" + name + " = " + res + ")");
		}
		return res;
	}

	private void flushCode(File codeGenFile, int seq) throws ServerException {
		try {
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(codeGenFile)));
			pw.println(seq);
			pw.close();
		}
		catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to generate code", e);
		}
	}

	private void recordEvsHistory(History hist) throws ServerException {
		try {
			String hisfile = serverConfiguration.getProperty(EVS_HISTORY_FILE);
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(hisfile, true)));
			pw.println(hist.toRecord());
			pw.close();
		}
		catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to record EVS history", e);
		}
	}
	
	private void recordConceptHistory(History hist) throws ServerException {
		try {
			String hisfile = serverConfiguration.getProperty(CON_HISTORY_FILE);
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(hisfile, true)));
			pw.println(hist.toRecord());
			pw.close();
		}
		catch (IOException e) {
			throw new ServerException(StatusCodes.INTERNAL_SERVER_ERROR, "Server failed to record EVS history", e);
		}
	}
	
}
