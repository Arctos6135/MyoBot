/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package myobot.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;

import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.VictorSP;

/**
 * The RobotMap is a mapping from the ports sensors and actuators are wired into
 * to a variable name. This provides flexibility changing wiring, makes checking
 * the wiring easier and significantly reduces the number of magic numbers
 * floating around.
 */
public class RobotMap {
	// For example to map the left and right motors, you could define the
	// following variables to use with your drivetrain subsystem.
	// public static int leftMotor = 1;
	// public static int rightMotor = 2;

	// If you are using multiple modules, make sure to define both the port
	// number and the module. For example you with a rangefinder:
	// public static int rangefinderPort = 1;
	// public static int rangefinderModule = 1;
	
	public static TalonSRX leftDriveTalon1 = new TalonSRX(3);
    public static TalonSRX leftDriveTalon2 = new TalonSRX(2);	
    public static TalonSRX rightDriveTalon1 = new TalonSRX(1);
    public static TalonSRX rightDriveTalon2 = new TalonSRX(4);
    public static VictorSPX leftDriveVictor = new VictorSPX(5);
    public static VictorSPX rightDriveVictor = new VictorSPX(6);
    
    public static VictorSP elevatorVictor = new VictorSP(7);
    public static VictorSP wristVictor = new VictorSP(8);
    public static VictorSP intakeLeft = new VictorSP(5);
    public static VictorSP intakeRight = new VictorSP(6);
    
    public static DigitalInput elevatorTopSwitch = new DigitalInput(9);
    public static DigitalInput elevatorBottomSwitch = new DigitalInput(5);
    
    public static final int ACTION_SIZE = 2;
	public static final int PARAM_SIZE = 4;
	public static final int MESSAGE_SIZE = ACTION_SIZE + PARAM_SIZE;
       
    static {
    	leftDriveTalon2.follow(leftDriveTalon1);
		rightDriveTalon2.follow(rightDriveTalon1);
		leftDriveVictor.follow(leftDriveTalon1);
		rightDriveVictor.follow(rightDriveTalon1);
		leftDriveVictor.setInverted(true);
		rightDriveVictor.setInverted(true);
		
		leftDriveTalon1.set(ControlMode.PercentOutput, 0);
		rightDriveTalon1.set(ControlMode.PercentOutput, 0);
		
		intakeRight.setInverted(false);
		intakeLeft.setInverted(true);
    }
}
