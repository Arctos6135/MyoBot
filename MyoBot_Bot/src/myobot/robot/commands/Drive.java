package myobot.robot.commands;

import edu.wpi.first.wpilibj.command.InstantCommand;
import myobot.robot.Robot;

/**
 *
 */
public class Drive extends InstantCommand {
	
	final double speed;

    public Drive(double speed) {
        super();
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
        requires(Robot.driveTrain);
        this.speed = speed;
    }

    // Called once when the command executes
    protected void initialize() {
    	Robot.driveTrain.setMotorsVBus(speed, speed);
    }

}
