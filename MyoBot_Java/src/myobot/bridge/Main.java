package myobot.bridge;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

public class Main {
	
	static NetworkTableInstance tableInstance;
	static NetworkTable table;
	
	static NetworkTableEntry state;
	
	static HashMap<Integer, String> actionNames;
	static {
		actionNames = new HashMap<Integer, String>();
		actionNames.put(0x0000, "0x0000 (Rest)");
		actionNames.put(0x0001, "0x0001 (Drive Forward)");
		actionNames.put(0x0002, "0x0002 (Turn Left)");
		actionNames.put(0x0003, "0x0003 (Turn Right)");
		actionNames.put(0x0004, "0x0004 (Drive Backwards)");
	}
	
	public static final int PORT = 6135;
	
	public static int chars2Int(char[] data) {
		if(data.length < 4)
			throw new IllegalArgumentException("Not enough bytes");
		return ((byte) data[0] << 24) | ((byte) data[1] << 16) | ((byte) data[2] << 8) | (byte) data[3];
	}

	public static void main(String[] args) {
		Scanner stdin = new Scanner(System.in);
		System.out.print("Enter FRC Team Number: ");
		int teamNumber = stdin.nextInt();
		stdin.close();
		
		tableInstance = NetworkTableInstance.getDefault();
		table = tableInstance.getTable("control");
		
		state = table.getEntry("state");
		
		tableInstance.setUpdateRate(1.0 / 20);
		tableInstance.startClientTeam(teamNumber);
		tableInstance.startDSClient();
		
		try {
			Socket socket = new Socket("localhost", PORT);
			InputStreamReader in = new InputStreamReader(socket.getInputStream());
			
			char[] buf = new char[4];
			while(in.read(buf) != -1) {
				int msg = chars2Int(buf);
				state.setNumber(msg);
				System.out.println("Action Sent: " + actionNames.get(msg));
			}
			
			in.close();
			socket.close();
		}
		catch(IOException e) {
			if(e instanceof ConnectException) {
				System.err.println("Failed to connect. Please run the Myo reader before running the bridge.");
			}
			else {
				System.err.println("Fatal Error: " + e.toString());
			}
		}
	}

}
