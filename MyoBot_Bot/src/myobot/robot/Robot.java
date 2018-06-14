/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package myobot.robot;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.command.Scheduler;
import myobot.robot.commands.NetworkTablesDrive;
import myobot.robot.subsystems.DriveTrain;
import myobot.robot.subsystems.Elevator;
import myobot.robot.subsystems.Intake;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the TimedRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the build.properties file in the
 * project.
 */
public class Robot extends TimedRobot {
	public static DriveTrain driveTrain;
	public static Elevator elevator;
	public static Intake intake;
	public static OI oi;
	
	//Shared default command between all subsystems
	public static NetworkTablesDrive defaultCommand;
	
	public static NetworkTableInstance tableInstance;
	public static NetworkTable table;
	public static NetworkTableEntry actionEntry;
	public static NetworkTableEntry[] paramEntries = new NetworkTableEntry[RobotMap.PARAM_SIZE];
	
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

	/**
	 * This function is run when the robot is first started up and should be
	 * used for any initialization code.
	 */
	@Override
	public void robotInit() {
		oi = new OI();
		
		tableInstance = NetworkTableInstance.getDefault();
		table = tableInstance.getTable("myobot");
		actionEntry = table.getEntry("action");
		for(int i = 0; i < paramEntries.length; i ++) {
			paramEntries[i] = table.getEntry("param" + i);
		}
		
		driveTrain = new DriveTrain();
		elevator = new Elevator();
		intake = new Intake();
		defaultCommand = new NetworkTablesDrive();
		driveTrain.setDefaultCommand(defaultCommand);
		elevator.setDefaultCommand(defaultCommand);
		intake.setDefaultCommand(defaultCommand);
	}

	/**
	 * This function is called once each time the robot enters Disabled mode.
	 * You can use it to reset any subsystem information you want to clear when
	 * the robot is disabled.
	 */
	@Override
	public void disabledInit() {

	}

	@Override
	public void disabledPeriodic() {
		Scheduler.getInstance().run();
	}

	/**
	 * This autonomous (along with the chooser code above) shows how to select
	 * between different autonomous modes using the dashboard. The sendable
	 * chooser code works with the Java SmartDashboard. If you prefer the
	 * LabVIEW Dashboard, remove all of the chooser code and uncomment the
	 * getString code to get the auto name from the text box below the Gyro
	 *
	 * <p>You can add additional auto modes by adding additional commands to the
	 * chooser code above (like the commented example) or additional comparisons
	 * to the switch structure below with additional strings & commands.
	 */
	@Override
	public void autonomousInit() {
		
	}

	/**
	 * This function is called periodically during autonomous.
	 */
	@Override
	public void autonomousPeriodic() {
		Scheduler.getInstance().run();
	}

	@Override
	public void teleopInit() {
	}

	/**
	 * This function is called periodically during operator control.
	 */
	@Override
	public void teleopPeriodic() {
		Scheduler.getInstance().run();
	}

	/**
	 * This function is called periodically during test mode.
	 */
	@Override
	public void testPeriodic() {
	}
}
