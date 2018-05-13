package myobot.bridge;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

public class Main {
	
	static NetworkTableInstance tableInstance;
	static NetworkTable table;
	static NetworkTableEntry driveForwardEntry;
	
	public static final int PORT = 6135;
	
	//Messages are all 4 bytes
	public static final int ACT_REST = 0x0000;
	public static final int ACT_DRIVEFORWARD = 0x0001;
	
	public static int chars2Int(char[] data) {
		if(data.length < 4)
			throw new IllegalArgumentException("Not enough bytes");
		return ((byte) data[0] << 24) | ((byte) data[1] << 16) | ((byte) data[2] << 8) | (byte) data[3];
	}

	public static void main(String[] args) {
		tableInstance = NetworkTableInstance.getDefault();
		table = tableInstance.getTable("control");
		
		tableInstance.startClientTeam(6135);
		tableInstance.startDSClient();
		
		driveForwardEntry = table.getEntry("driveForward");
		
		try {
			Socket socket = new Socket("localhost", PORT);
			InputStreamReader in = new InputStreamReader(socket.getInputStream());
			
			char[] buf = new char[4];
			while(in.read(buf) != -1) {
				int msg = chars2Int(buf);
				switch(msg) {
				//Set all actions to false
				case ACT_REST:
					driveForwardEntry.setBoolean(false);
					break;
				case ACT_DRIVEFORWARD:
					driveForwardEntry.setBoolean(true);
					break;
				//Set all actions to false and report error
				default:
					driveForwardEntry.setBoolean(false);
					System.err.println("Error: Unrecognized action code received");
					break;
				}
				System.out.println("Action sent: " + msg);
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
