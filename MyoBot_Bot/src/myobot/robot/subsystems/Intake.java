package myobot.robot.subsystems;

import edu.wpi.first.wpilibj.command.Subsystem;
import myobot.robot.RobotMap;
import myobot.robot.commands.NTIntake;

/**
 *
 */
public class Intake extends Subsystem {

    // Put methods for controlling this subsystem
    // here. Call these from Commands.
	public static final int IN = 1;
	public static final int OUT = -1;
	
	static double constrain(double val, double min, double max) {
		return Math.max(min, Math.min(max, val));
	}
	/**
	 * Sets the speed of the intake.
	 * @param speed The speed of the intake
	 */
	public void set(double speed) {
		RobotMap.intakeLeft.set(constrain(speed, -1, 1));
		RobotMap.intakeRight.set(constrain(speed, -1, 1));
	}
	/**
	 * Sets the absolute speed and direction of the intake.
	 * @param speed The speed of the intake
	 * @param direction The direction, {@link #IN} or {@link #OUT}
	 */
	public void set(double speed, int direction) {
		RobotMap.intakeLeft.set(constrain(speed * direction, -1, 1));
		RobotMap.intakeRight.set(constrain(speed * direction, -1, 1));
	}

    public void initDefaultCommand() {
        // Set the default command for a subsystem here.
        //setDefaultCommand(new MySpecialCommand());
    	setDefaultCommand(new NTIntake());
    }
}

