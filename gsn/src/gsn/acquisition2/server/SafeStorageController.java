package gsn.acquisition2.server;

import gsn.networking.ActionPort;
import gsn.networking.NetworkAction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import org.apache.log4j.Logger;

public class SafeStorageController {

	public static final String SAFE_STORAGE_SHUTDOWN = "SS SHUTDOWN";

	public static transient Logger logger = Logger.getLogger(SafeStorageController.class);

	public SafeStorageController(final SafeStorageServer safeStorageServer, int safeStorageControllerPort) {
		super();
		logger.info("Started Safe Storage Controller on port " + safeStorageControllerPort);
		ActionPort.listen(safeStorageControllerPort, new NetworkAction(){
			public boolean actionPerformed(Socket socket) {
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					String incomingMsg = reader.readLine();
					
					if (incomingMsg != null && incomingMsg.equalsIgnoreCase(SAFE_STORAGE_SHUTDOWN)) {
						safeStorageServer.shutdown();
						return false;
					}
					else return true;
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					return false;
				}
			}});
	}
}
