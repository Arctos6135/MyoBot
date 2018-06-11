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
	
	static NetworkTableEntry actionEntry;
	static NetworkTableEntry[] paramEntries;
	
	static HashMap<Integer, String> actionNames;
	static {
		actionNames = new HashMap<Integer, String>();
		actionNames.put(0x0000, "Rest");
		actionNames.put(0x0001, "Drive Forward");
		actionNames.put(0x0002, "Turn Left");
		actionNames.put(0x0003, "Turn Right");
		actionNames.put(0x0004, "Drive Backwards");
		actionNames.put(0x0005, "Raise Elevator");
		actionNames.put(0x0006, "Lower Elevator");
		actionNames.put(0x0007, "Intake");
		actionNames.put(0x0008, "Outtake");
	}
	
	public static final int PORT = 6135;
	
	public static int chars2Int(char[] data) {
		if(data.length < 4)
			throw new IllegalArgumentException("Not enough bytes");
		return ((byte) data[0] << 24) | ((byte) data[1] << 16) | ((byte) data[2] << 8) | (byte) data[3];
	}
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	static int lastMessageLen = 0;

	public static void main(String[] args) {
		Scanner stdin = new Scanner(System.in);
		System.out.print("Enter FRC Team Number: ");
		int teamNumber = stdin.nextInt();
		stdin.close();
		
		tableInstance = NetworkTableInstance.getDefault();
		table = tableInstance.getTable("myobot");
		
		actionEntry = table.getEntry("action");
		for(int i = 0; i < 4; i ++) {
			paramEntries[i] = table.getEntry("param" + i);
		}
		
		tableInstance.setUpdateRate(1.0 / 20);
		tableInstance.startClientTeam(teamNumber);
		tableInstance.startDSClient();
		
		try {
			Socket socket = new Socket("localhost", PORT);
			InputStreamReader in = new InputStreamReader(socket.getInputStream());
			
			char[] buf = new char[8];
			char[] actionBytes = new char[4];
			byte[] param = new byte[4];
			while(in.read(buf) != -1) {
				for(int i = 0; i < 4; i ++) {
					actionBytes[i] = buf[i];
					param[i] = (byte) buf[i + 4];
				}
				int action = chars2Int(actionBytes);
				
				actionEntry.forceSetNumber(action);
				for(int i = 0; i < 4; i ++) {
					paramEntries[i].forceSetValue(param[i]);
				}
				
				String message = "Action Sent: " + Integer.toHexString(action) + "(" +
						(actionNames.containsKey(action) ? actionNames.get(action) : "Unknown") + ")\n" +
						"Parameter Data: 0x" + bytesToHex(param);
				for(int i = 0; i < lastMessageLen; i ++) {
					System.out.print("\b");
				}
				lastMessageLen = message.length();
				
				System.out.print(message);
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
