package myobot.robot;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

public final class Comms {
	private Comms() {}
	
	public static final int ACTION_SIZE = 2;
	public static final int PARAM_SIZE = 8;
	public static final int MESSAGE_SIZE = ACTION_SIZE + PARAM_SIZE;
	
	public static NetworkTableInstance tableInstance;
	public static NetworkTable table;
	public static NetworkTableEntry actionEntry;
	public static NetworkTableEntry[] paramEntries = new NetworkTableEntry[PARAM_SIZE];
	
	//Messages are all 4 bytes
	public static final short ACT_REST = 0x0000;
	public static final short ACT_DRIVEFORWARD = 0x0001;
	public static final short ACT_TURNLEFT = 0x0002;
	public static final short ACT_TURNRIGHT = 0x0003;
	public static final short ACT_DRIVEBACK = 0x0004;
	public static final short ACT_RAISEELEVATOR = 0x0005;
	public static final short ACT_LOWERELEVATOR = 0x0006;
	public static final short ACT_INTAKE = 0x0007;
	public static final short ACT_OUTTAKE = 0x0008;
	
	public static final byte MASK_TURNDIRECTION = 0b00000001;
	public static final byte MASK_RUNINTAKE = 0b00000010;
	public static final byte MASK_INTAKEDIRECTION = 0b00000100;
	public static final byte MASK_DRIVESPEED = 0b00001000;
	
	public static void init() {
		tableInstance = NetworkTableInstance.getDefault();
		table = tableInstance.getTable("myobot");
		actionEntry = table.getEntry("action");
		for(int i = 0; i < paramEntries.length; i ++) {
			paramEntries[i] = table.getEntry("param" + i);
		}
	}
}
