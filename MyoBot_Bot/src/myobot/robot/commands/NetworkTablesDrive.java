package myobot.robot.commands;

import edu.wpi.first.wpilibj.command.Command;
import myobot.robot.Robot;
import myobot.robot.RobotMap;
import myobot.robot.subsystems.Elevator;
import myobot.robot.subsystems.Intake;

/**
 *
 */
public class NetworkTablesDrive extends Command {
	
	public static final double DRIVE_SPEED = 0.45;

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
    
    public static double constructSpeed(char hi, char lo) {
    	int s = (hi & 0xFF) * 0x100 + (lo & 0xFF);
    	return ((double) s) / 0xFFFF;
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    	short code = Robot.actionEntry.getNumber(Robot.ACT_REST).shortValue();
    	char[] param = new char[RobotMap.PARAM_SIZE];
    	
    	for(int i = 0; i < param.length; i ++) {
    		//Ugly conversion because of lack of unsigned data types
    		//To avoid problems with sign, use a 16 bit char instead of a 8 bit byte to store the data
    		//First retrieve the object from NT and cast to a Byte
    		//Then cast to an unsigned integer because directly casting to char will keep the sign
    		//Then take only the last byte of the integer and cast to char
    		param[i] = (char) (Byte.toUnsignedInt((Byte) Robot.paramEntries[i].getValue().getValue()) & 0xFF);
    	}
    	
    	switch(code) {
    	case Robot.ACT_REST:
    		Robot.driveTrain.setMotorsVBus(0, 0);
    		Robot.elevator.set(0);
    		Robot.intake.set(0);
    		break;
    	case Robot.ACT_INTAKE:
    		Robot.intake.set(0.7, Intake.IN);
    		Robot.elevator.set(0);
    		Robot.driveTrain.setMotorsVBus(0, 0);
    		break;
    	case Robot.ACT_OUTTAKE:
    		Robot.intake.set(0.7, Intake.OUT);
    		Robot.elevator.set(0);
    		Robot.driveTrain.setMotorsVBus(0, 0);
    		break;
    	case Robot.ACT_RAISEELEVATOR:
    		Robot.elevator.set(constructSpeed(param[0], param[1]), Elevator.UP);
    		Robot.intake.set(0);
    		Robot.driveTrain.setMotorsVBus(0, 0);
    		break;
    	case Robot.ACT_LOWERELEVATOR:
    		Robot.elevator.set(constructSpeed(param[0], param[1]), Elevator.DOWN);
    		Robot.intake.set(0);
    		Robot.driveTrain.setMotorsVBus(0, 0);
    		break;
    	case Robot.ACT_DRIVEFORWARD:
    	{
    		double turnSpeed = constructSpeed(param[0], param[1]) * DRIVE_SPEED;
    		int direction = param[2] == 1 ? -1 : 1;
    		
    		Robot.driveTrain.setMotorsVBus(DRIVE_SPEED + turnSpeed * direction, DRIVE_SPEED - turnSpeed * direction);
    		Robot.elevator.set(0);
    		Robot.intake.set(0);
    		break;
    	}
    	case Robot.ACT_DRIVEBACK:
    	{
    		double turnSpeed = constructSpeed(param[0], param[1]) * DRIVE_SPEED;
    		int direction = param[2] == 1 ? -1 : 1;
    		
    		Robot.driveTrain.setMotorsVBus(-DRIVE_SPEED + turnSpeed * direction, -DRIVE_SPEED - turnSpeed * direction);
    		Robot.elevator.set(0);
    		Robot.intake.set(0);
    		break;
    	}
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
