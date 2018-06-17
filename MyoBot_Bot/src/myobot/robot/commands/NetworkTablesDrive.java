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
    	requires(Robot.elevator);
    	requires(Robot.intake);
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    	Robot.driveTrain.setMotorsVBus(Robot.ntLeftDrive.getDouble(0), Robot.ntRightDrive.getDouble(0));
    	Robot.elevator.set(Robot.ntElevator.getDouble(0));
    	Robot.intake.set(Robot.ntIntake.getDouble(0));
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return false;
    }

    // Called once after isFinished returns true
    protected void end() {
    	Robot.driveTrain.setMotorsVBus(0, 0);
    	Robot.elevator.set(0);
    	Robot.intake.set(0);
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    	end();
    }
}
