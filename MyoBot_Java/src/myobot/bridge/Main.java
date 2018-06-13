package myobot.bridge;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

/**
 * This class is the bridge between the native Myo reader program and the robot.
 * The Myo reader sends data to this program over a socket, and the program re-sends that data over
 * to the robot using NetworkTables.
 * @author Tyler Tian
 *
 */
public class Main {
	//The names of the tables and table entries
	public static final String TABLE_NAME = "myobot";
	public static final String ACTION_ENTRY_NAME = "action";
	public static final String PARAM_ENTRY_NAME = "param";
	
	public static final int ACTION_SIZE = 2;
	public static final int PARAM_SIZE = 4;
	public static final int MESSAGE_SIZE = ACTION_SIZE + PARAM_SIZE;
	
	//NetworkTables instance and table
	static NetworkTableInstance tableInstance;
	static NetworkTable table;
	
	//The entry that stores the action is a single value, while the parameter entries are an array of 4
	static NetworkTableEntry actionEntry;
	static NetworkTableEntry[] paramEntries = new NetworkTableEntry[4];
	
	//This map matches action codes to their corresponding names so we can output the name of the action
	static HashMap<Short, String> actionNames;
	static {
		actionNames = new HashMap<Short, String>();
		actionNames.put((short) 0x0000, "Rest");
		actionNames.put((short) 0x0001, "Drive Forward");
		actionNames.put((short) 0x0002, "Turn Left");
		actionNames.put((short) 0x0003, "Turn Right");
		actionNames.put((short) 0x0004, "Drive Backwards");
		actionNames.put((short) 0x0005, "Raise Elevator");
		actionNames.put((short) 0x0006, "Lower Elevator");
		actionNames.put((short) 0x0007, "Intake");
		actionNames.put((short) 0x0008, "Outtake");
	}
	
	public static final int PORT = 6135;
	
	//Used later to convert the array of bytes from the socket to a hexadecimal representation
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String charsToHex(char[] chars) {
	    char[] hexChars = new char[chars.length * 2];
	    for ( int j = 0; j < chars.length; j++ ) {
	        int v = chars[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	//Length of last message
	//Used to overwrite last message
	static int lastMessageLen = 0;

	public static void main(String[] args) throws FileNotFoundException {
		//Prompt for team number, which is used later for the NetworkTables connection.
		Scanner stdin = new Scanner(System.in);
		System.out.print("Enter FRC Team Number (Enter 0 For Dry Run): ");
		int teamNumber = stdin.nextInt();
		stdin.close();
		
		//Redirect error log to file
		PrintStream pw = new PrintStream(new FileOutputStream("bridge.log"));
		System.setErr(pw);
		
		//Create tables and entries
		tableInstance = NetworkTableInstance.getDefault();
		table = tableInstance.getTable("myobot");
		
		actionEntry = table.getEntry("action");
		for(int i = 0; i < 4; i ++) {
			paramEntries[i] = table.getEntry("param" + i);
		}
		
		//Default update rate is too slow
		//Change to 20 times a second instead
		if(teamNumber != 0) {
			tableInstance.setUpdateRate(1.0 / 20);
			tableInstance.startClientTeam(teamNumber);
			tableInstance.startDSClient();
		}
		
		try {
			Socket socket = new Socket("localhost", PORT);
			InputStreamReader in = new InputStreamReader(socket.getInputStream());
			
			char[] buf = new char[MESSAGE_SIZE];
			char[] actionBytes = new char[ACTION_SIZE];
			char[] param = new char[PARAM_SIZE];
			while(in.read(buf) != -1) {
				for(int i = 0; i < ACTION_SIZE; i ++) {
					actionBytes[i] = buf[i];
				}
				for(int i = 0; i < PARAM_SIZE; i ++) {
					param[i] = buf[i + ACTION_SIZE];
				}
				short action = (short) ((actionBytes[0] & 0xFF << 8) | (actionBytes[1] & 0xFF));
				
				//Set the values to send them over NetworkTables
				actionEntry.forceSetNumber(action);
				for(int i = 0; i < 4; i ++) {
					paramEntries[i].forceSetValue((byte) param[i]);
				}
				
				//Output information
				String message = "Action Sent: " + Integer.toHexString(action) + " (" +
						(actionNames.containsKey(action) ? actionNames.get(action) : "Unknown") + ")    " +
						"Parameter Data: 0x" + charsToHex(param);
				//Output backspace characters to erase the last line before outputting our new message.
				//This makes sure the line is cleared
				//First, output backspaces to go to the beginning of the line
				for(int i = 0; i < lastMessageLen; i ++) {
					System.out.print("\b");
				}
				//Then output spaces to overwrite the data from last time
				for(int i = 0; i < lastMessageLen; i ++) {
					System.out.print(" ");
				}
				//Finally, output backspaces again to return to the beginning of the line again
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
		pw.flush();
	}

}
