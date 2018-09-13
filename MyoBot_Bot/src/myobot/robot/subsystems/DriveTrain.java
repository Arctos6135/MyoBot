package myobot.robot.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;

import edu.wpi.first.wpilibj.command.Subsystem;
import myobot.robot.RobotMap;
import myobot.robot.commands.NTDrive;

/**
 *	Subsystem for the driving of the robot
 */
public class DriveTrain extends Subsystem {

    // Put methods for controlling this subsystem
    // here. Call these from Commands.

	/**
	 * Sets the speed of the drive motors for both sides of the robot.
	 * @param leftMotorVBus - The left output percentage
	 * @param rightMotorVBus - The right ouput percentage
	 */
	public void setMotorsVBus(double leftMotorVBus, double rightMotorVBus) {
		
		RobotMap.leftDriveTalon1.set(ControlMode.PercentOutput, leftMotorVBus);
		RobotMap.rightDriveTalon1.set(ControlMode.PercentOutput, -rightMotorVBus);
	}
	
    public void initDefaultCommand() {
        setDefaultCommand(new NTDrive());
    }
}

