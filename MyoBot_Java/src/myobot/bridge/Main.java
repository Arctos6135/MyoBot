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
	
	static NetworkTableEntry state;
	
	public static final int PORT = 6135;
	
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
		
		state = table.getEntry("state");
		
		state.setNumber(0x0001);
		try {
			Thread.sleep(3000);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		state.setNumber(0x0000);
		
		/*try {
			Socket socket = new Socket("localhost", PORT);
			InputStreamReader in = new InputStreamReader(socket.getInputStream());
			
			char[] buf = new char[4];
			while(in.read(buf) != -1) {
				int msg = chars2Int(buf);
				state.setNumber(msg);
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
		}*/
	}

}
