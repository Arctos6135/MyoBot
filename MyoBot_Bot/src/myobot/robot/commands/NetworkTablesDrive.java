package myobot.robot.commands;

import edu.wpi.first.wpilibj.command.Command;
import myobot.robot.Robot;

/**
 *
 */
public class NetworkTablesDrive extends Command {

    public NetworkTablesDrive() {
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
    	requires(Robot.driveTrain);
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    	int code = Robot.stateEntry.getNumber(Robot.ACT_REST).intValue();
    	switch(code) {
		case Robot.ACT_DRIVEFORWARD:
			Robot.driveTrain.setMotorsVBus(0.5, 0.5);;
			break;
		case Robot.ACT_TURNLEFT:
			Robot.driveTrain.setMotorsVBus(-0.5, 0.5);
			break;
		case Robot.ACT_TURNRIGHT:
			Robot.driveTrain.setMotorsVBus(0.5, -0.5);
			break;
		case Robot.ACT_REST:
		default:
			Robot.driveTrain.setMotorsVBus(0, 0);
			break;
		}
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return false;
    }

    // Called once after isFinished returns true
    protected void end() {
    	Robot.driveTrain.setMotorsVBus(0, 0);
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    	end();
    }
}
