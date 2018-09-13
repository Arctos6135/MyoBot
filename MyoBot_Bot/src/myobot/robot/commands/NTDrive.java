package myobot.robot.commands;

import edu.wpi.first.wpilibj.command.Command;
import myobot.robot.Robot;

/**
 *
 */
public class NTDrive extends Command {

    public NTDrive() {
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
    	requires(Robot.driveTrain);
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    	double left = Robot.ntLeftDrive.getDouble(0);
    	double right = Robot.ntRightDrive.getDouble(0);
    	System.out.printf("Left Motor Output: %f, Right Motor Output: %f\n", left, right);
    	Robot.driveTrain.setMotorsVBus(left, right);
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return false;
    }

    // Called once after isFinished returns true
    protected void end() {
    	//Should never happen but just in case
    	Robot.driveTrain.setMotorsVBus(0, 0);
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    	end();
    }
}
