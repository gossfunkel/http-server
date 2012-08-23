import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.*;

public class server {
	
	// printing log:

	public void printLog(String data) {
		data = data + "\n";
		FileOutputStream log;
		PrintStream p;
		try {
			log = new FileOutputStream("log.sfra");
			p = new PrintStream(log);
			p.println(data);
			p.close();
		}
		catch (Exception e) {
			System.err.println ("WriteError: log: " + e);
		}
	}
	
	public void run() {
		final double gameHertz = 100.0;
		final double timeBetweenUpdates = 1000000 / gameHertz;
		final int maxUpdatesBeforeRuntime = 20;
		double lastUpdateTime = System.nanoTime();
		double lastRuntime = System.nanoTime();
		final double targetFPS = 40;
		final double targetTimeBetweenRuntimes = 1000000000 / targetFPS;
		int lastSecondTime = (int) (lastUpdateTime / 1000000000);
		boolean looping = true;
		try{
			server = new ServerSocket(1042); 
		}
		catch (IOException e) {
			printLog("LISTEN FAIL on 1042: " + e);	
			System.err.println("Could not listen on port 1042.");
			System.exit(-1);
		}
		while(looping) {
			double now = System.nanoTime();
			int updateCount = 0;
			while( now - lastUpdateTime > timeBetweenUpdates && updateCount < maxUpdatesBeforeRuntime ) 
			{
				runServer();
				lastUpdateTime += timeBetweenUpdates;
				updateCount++;
			}
			if ( now - lastUpdateTime > timeBetweenUpdates)
			{
				lastUpdateTime = now - timeBetweenUpdates;
			}
			while ( now - lastRuntime < targetTimeBetweenRuntimes && now - lastUpdateTime < timeBetweenUpdates)
			{
				Thread.yield();
				try {
					Thread.sleep(1);
				} 
				catch(Exception e) {
				}
				now = System.nanoTime();
			}
		}
	}
		
	private void runServer() {
		try{
			client = server.accept();
			printLog("ACCEPT on 1042: " + client);
			System.out.println(client);
		}
		catch (IOException e) {
			printLog("ACCEPT FAIL on 1042: " + e);	
			System.err.println("Accept failed: 1042");
		}
		try{
			inComing = new BufferedReader(new InputStreamReader(client.getInputStream()));
			outGoing = new PrintWriter(client.getOutputStream(), true);
		}
		catch (IOException e) {
			printLog("READ FAIL on 1042: " + e);
			System.err.println("Read failed");
			System.exit(-1);
		}
		
		try{
			clientData = inComing.readLine();	
			if (clientData.equals(null)) {
				System.err.println("NULL INPUT");
				outGoing.println("ILLEGAL INPUT: NULL");
			}
			else {
				System.out.println(clientData);
				serve(clientData, client);
			}
		}
		catch (IOException e) {
			printLog("READ FAIL on 1042: " + e);
			System.out.println("Read failed");
			System.exit(-1);
		}
	}
	
	server() {
		System.out.println("Parent constructed");
		runServer();
	}
	
	public static void main(String args[]) throws FileNotFoundException, IOException 	{
		System.out.println("Startup initiated");
		File log = new File("log.dat");
		if (log.exists()) {
    	   	System.out.println("Logfile exists.");
    	}
    	else {
    	   	try {
	        	log.createNewFile();
	        } catch (IOException e) {
	        	System.err.println("Error in log file creation:" + e);
	        }
    	}
		server s = new server();
	}

	private class serve {
	
		private Thread t;
		
		// main thread entry point:
		
		public void serve(String clientData, Socket client) {
			System.out.println("Serving...");
			
			t = new Thread(clientData, client) {
			
				public void run() {
			
					String commandType = clientData.substring(0,2);
					if (commandType == "GET") {
						int posDot = clientData.indexOf(".");
						int posSpa = clientData.indexOf(" ",posDot);
						String filetype = clientData.substring(posDot,posSpa);
						String location = clientData.substring(3,posSpa);
						if (filetype == ".jpg") {
							try {
								outGoing.println ("JPG IMG");
								outGoing.println ("");
								FileInputStream requestedFile = new FileInputStream(location);
								DataInputStream in = new DataInputStream(requestedFile);
		  						BufferedReader br = new BufferedReader(new InputStreamReader(in));
		  						String currentLine;
								try {
									while ((currentLine = br.readLine()) != null) {
										outGoing.println (currentLine);
									}
								}
								catch (Exception e) {
									System.err.println ("WriteError: " + e);
									server.printLog("WriteError: " + e);
								}
							}
							catch (FileNotFoundException e) {
								outGoing.println ("ERROR 404: NOT FOUND");
								System.err.println("404");
							}
						}
						if (filetype == ".txt") {
							try {
								outGoing.println ("TEXT");
								outGoing.println ("");
								FileInputStream requestedFile = new FileInputStream(location);
								DataInputStream in = new DataInputStream(requestedFile);
		  						BufferedReader br = new BufferedReader(new InputStreamReader(in));
		  						String currentLine;
								try {
									while ((currentLine = br.readLine()) != null) {
										outGoing.println (currentLine);
									}
								}
								catch (Exception e) {
									System.err.println ("WriteError: " + e);
									printLog("WriteError: " + e);
								}
							}
							catch (FileNotFoundException e) {
								outGoing.println ("ERROR 404: NOT FOUND");
								System.err.println("404");
							}
						}
						else {
							outGoing.println("ILLEGAL FILETYPE");
							printLog("Illegal filetype requested");
							System.err.println("Illegal filetype requested");
						}
					}
				}
			};
			t.start();
		}
	}
	
	// RESOURCES ----------------------------------------------------------
		
	public static int portAssigner() {
		for (usedPort = 1024; usedport++; usedPort = 49150) {
			if (isAvailable(usedPort)) {
				break;
			}
		}
		if (usedPort == 49150) {
			System.err.println("no remaining ports");
			System.exit(-1);
		}
		return usedPort;
	}
	
	// from Apache Mina:
	public static boolean isAvailable(int port) {
		if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
			throw new IllegalArgumentException("Invalid start port: " + port);
		}
		try {
			ss = new ServerSocket(port);
			ss.setReuseAddress(true);
			ds = new DatagramSocket(port);
			ds.setReuseAddress(true);
			return true;
		} 
		catch (IOException e) {
		} 
		finally {
			if (ds != null) {
           		ds.close();
   			}
			if (ss != null) {
				try {
					ss.close();
				} 
				catch (IOException e) {
					/* should not be thrown */
				}
			}
		}
		ServerSocket ss = null;
		DatagramSocket ds = null;
		
		return false;
	}
	
	private BufferedReader inComing;
	private PrintWriter outGoing;

	private ServerSocket server = null;
	private Socket client;

	private String clientData;
}

