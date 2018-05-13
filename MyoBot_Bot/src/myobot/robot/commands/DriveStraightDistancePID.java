package myobot.robot.commands;

import edu.wpi.first.wpilibj.PIDController;
import edu.wpi.first.wpilibj.command.Command;
import myobot.robot.Robot;
import myobot.robot.RobotMap;

/**
 *	Uses PIDs to drive forward or backwards a straight distance.
 */
public class DriveStraightDistancePID extends Command {
	
	//To be tuned later
	//0.015 0.002 0.4
	public static double kP = 0.02;
	public static double kI = 0.02;
	public static double kD = 0.3;
	
	public static final double TOLERANCE = 2.0;
	
	PIDController leftPID, rightPID;
	double distance;

    public DriveStraightDistancePID(double distance) {
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
    	requires(Robot.driveTrain);
    	this.distance = distance;
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    	Robot.driveTrain.resetEncoders();

    	leftPID = new PIDController(kP, kI, kD, Robot.driveTrain.getLeftEncoder(), RobotMap.leftDrivePIDMotor);
    	rightPID = new PIDController(kP, kI, kD, Robot.driveTrain.getRightEncoder(), RobotMap.rightDrivePIDMotor);
    	
    	leftPID.setOutputRange(-1.0, 1.0);
    	rightPID.setOutputRange(-1.0, 1.0);
    	
    	RobotMap.leftDrivePIDMotor.setPIDController(leftPID);
    	RobotMap.rightDrivePIDMotor.setPIDController(rightPID);
    	
    	leftPID.setContinuous(false);
    	rightPID.setContinuous(false);
    	leftPID.setAbsoluteTolerance(TOLERANCE);
    	rightPID.setAbsoluteTolerance(TOLERANCE);
    	
    	leftPID.setSetpoint(distance);
    	rightPID.setSetpoint(distance);
    	
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    	if(!leftPID.isEnabled())
    		leftPID.enable();
    	if(!rightPID.isEnabled())
    		rightPID.enable();
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
    	//Return true only if left and right PID are on target
        return leftPID.onTarget() && rightPID.onTarget();
    }

    // Called once after isFinished returns true
    protected void end() {
    	leftPID.disable();
    	rightPID.disable();
    	leftPID.free();
    	rightPID.free();
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    	leftPID.disable();
    	rightPID.disable();
    	leftPID.free();
    	rightPID.free();
    }
}
