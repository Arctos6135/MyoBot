package myobot.bridge;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

public class Main {
	
	static NetworkTableInstance tableInstance;
	static NetworkTable table;
	static NetworkTableEntry driveForwardEntry;
	
	public static final int PORT = 6135;

	public static void main(String[] args) {
		tableInstance = NetworkTableInstance.getDefault();
		table = tableInstance.getTable("control");
		
		tableInstance.startClientTeam(6135);
		tableInstance.startDSClient();
		
		driveForwardEntry = table.getEntry("driveForward");
		
		try {
			Socket socket = new Socket("localhost", PORT);
			InputStreamReader in = new InputStreamReader(socket.getInputStream());
			
			//TODO: Put code here to handle messages
			
			in.close();
			socket.close();
		}
		catch(IOException e) {
			System.err.println("Fatal Error: " + e.toString());
		}
	}

}
