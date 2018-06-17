package myobot.bridge.myo;

public class EulerOrientation {
	
	double yaw, pitch, roll;
	
	public EulerOrientation(double yaw, double pitch, double roll) {
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
	}
	
	public double getYaw() {
		return yaw;
	}
	public double getPitch() {
		return pitch;
	}
	public double getRoll() {
		return roll;
	}
	
	public double getYawDegrees() {
		return Math.toDegrees(getYaw());
	}
	public double getPitchDegrees() {
		return Math.toDegrees(getPitch());
	}
	public double getRollDegrees() {
		return Math.toDegrees(getRoll());
	}
}
