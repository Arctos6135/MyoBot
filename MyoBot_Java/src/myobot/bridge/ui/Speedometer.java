package myobot.bridge.ui;

import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class Speedometer extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7000034129617442900L;

	int width;
	Image baseImage;
	Image overlayImage;
	double speed = +0.0;
	
	public Speedometer(int width) throws IOException {
		super();
		this.width = width;
		
		InputStream imageStream = getClass().getResourceAsStream("/resources/ui/gyro/speedometer_base.png");
		baseImage = ImageIO.read(imageStream).getScaledInstance(width, width / 2, Image.SCALE_SMOOTH);
		imageStream.close();
		imageStream = getClass().getResourceAsStream("/resources/ui/gyro/gyro_pointer.png");
		overlayImage = ImageIO.read(imageStream).getScaledInstance(width, width, Image.SCALE_SMOOTH);
		imageStream.close();
		
		setPreferredSize(new Dimension(width, width / 2));
		setMinimumSize(new Dimension(width, width / 2));
		setMaximumSize(new Dimension(width, width / 2));
	}
	
	public double getSpeed() {
		return speed;
	}
	public void updateSpeed(double speed) {
		this.speed = speed;
		repaint();
	}
	public void updateSpeedNoRepaint(double speed) {
		this.speed = speed;
	}
}
