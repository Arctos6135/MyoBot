package myobot.robot.commands;

import edu.wpi.first.wpilibj.command.InstantCommand;
import myobot.robot.Robot;

/**
 *
 */
public class Drive extends InstantCommand {
	
	final double lspeed, rspeed;

    public Drive(double lspeed, double rspeed) {
        super();
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
        requires(Robot.driveTrain);
        this.lspeed = lspeed;
        this.rspeed = rspeed;
    }

    // Called once when the command executes
    protected void initialize() {
    	Robot.driveTrain.setMotorsVBus(lspeed, rspeed);
    }

}
