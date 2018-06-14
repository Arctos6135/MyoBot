package myobot.robot.subsystems;

import edu.wpi.first.wpilibj.command.Subsystem;
import myobot.robot.RobotMap;

/**
 *
 */
public class Elevator extends Subsystem {

    // Put methods for controlling this subsystem
    // here. Call these from Commands.
	
	//Directional constants
	public static final int UP = -1;
	public static final int DOWN = 1;
	
	public boolean atTop() {
		//The switches are wired so that they're grounded when activated
		//So when they're pressed get() returns true
		return !RobotMap.elevatorTopSwitch.get();
	}
	public boolean atBottom() {
		return !RobotMap.elevatorBottomSwitch.get();
	}
	
	/**
	 * Sets the speed of the elevator. This method does not account for whether the elevator is at the top
	 * or bottom. Use the {@link #set(double)} method instead.
	 * @param speed The speed of the elevator
	 */
	public void setRaw(double speed) {
		RobotMap.elevatorVictor.set(Math.max(-1.0, Math.min(1.0, speed)));
	}
	/**
	 * Sets the speed of the elevator. If the elevator cannot move anymore in the specified direction,
	 * the speed will not be set and this method will return false. Otherwise the speed is set by calling
	 * {@link #setRaw(double)} and this method returns true.
	 * @param speed The speed of the elevator
	 * @return Whether the speed has been set successfully
	 */
	public boolean set(double speed) {
		int sign = (int) Math.signum(speed);
		
		if((sign == UP && atTop()) ||
				(sign == DOWN && atBottom())) {
			return false;
		}
		setRaw(speed);
		return true;
	}
	/**
	 * Sets the absolute speed and direction of the elevator.
	 * Like {@link #set(double)}, this method also takes into account the position of the elevator.
	 * @param speed The absolute speed of the elevator
	 * @param direction The direction, {@link #UP} or {@link #DOWN}
	 * @return Whether the speed has been set successfully
	 */
	public boolean set(double speed, int direction) {
		return set(speed * direction);
	}

    public void initDefaultCommand() {
        // Set the default command for a subsystem here.
        //setDefaultCommand(new MySpecialCommand());
    	//Will be set later by Robot.java
    }
}

