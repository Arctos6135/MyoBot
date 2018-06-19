package myobot.bridge.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
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
	Image lightForward, lightReverse, lightOff;
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
		imageStream = getClass().getResourceAsStream("/resources/ui/gyro/speedometer_light_green.png");
		lightForward = ImageIO.read(imageStream).getScaledInstance(width / 12, width / 12, Image.SCALE_SMOOTH);
		imageStream.close();imageStream = getClass().getResourceAsStream("/resources/ui/gyro/speedometer_light_red.png");
		lightReverse = ImageIO.read(imageStream).getScaledInstance(width / 12, width / 12, Image.SCALE_SMOOTH);
		imageStream.close();imageStream = getClass().getResourceAsStream("/resources/ui/gyro/speedometer_light_off.png");
		lightOff = ImageIO.read(imageStream).getScaledInstance(width / 12, width / 12, Image.SCALE_SMOOTH);
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
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D graphics = (Graphics2D) g;
		
		graphics.drawImage(baseImage, 0, 0, null);
		Image img;
		if(speed > 0) {
			img = lightForward;
		}
		else if(speed < 0) {
			img = lightReverse;
		}
		else {
			img = lightOff;
		}
		graphics.drawImage(img, width - width / 12, 0, null);
		
		AffineTransform old = graphics.getTransform();
		graphics.translate(width / 2, width / 2);
		graphics.rotate(Math.PI * speed - (Math.PI / 2));
		graphics.drawImage(overlayImage, -width / 2, -width / 2, null);
		graphics.setTransform(old);
	}
}
