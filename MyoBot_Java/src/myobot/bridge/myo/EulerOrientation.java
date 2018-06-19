package myobot.bridge.myo;

public class EulerOrientation {
	
	double yaw, pitch, roll;
	double degYaw = Double.NaN, degPitch = Double.NaN, degRoll = Double.NaN;
	
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
		return Double.isNaN(degYaw) ? degYaw = Math.toDegrees(yaw) : degYaw;
	}
	public double getPitchDegrees() {
		return Double.isNaN(degPitch) ? degPitch = Math.toDegrees(pitch) : degPitch;
	}
	public double getRollDegrees() {
		return Double.isNaN(degRoll) ? degRoll = Math.toDegrees(roll) : degRoll;
	}
	
	public EulerOrientation negate() {
		return new EulerOrientation(-yaw, -pitch, -roll);
	}
}
