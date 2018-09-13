package myobot.robot.commands;

import edu.wpi.first.wpilibj.command.Command;
import myobot.robot.Robot;

/**
 *
 */
public class NTIntake extends Command {

    public NTIntake() {
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
    	requires(Robot.intake);
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    	double output = Robot.ntIntake.getDouble(0);
    	System.out.printf("Intake Output: %f\n", output);
    	Robot.intake.set(output);
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return false;
    }

    // Called once after isFinished returns true
    protected void end() {
    	Robot.intake.set(0);
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    	end();
    }
}
