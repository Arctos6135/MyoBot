package myobot.bridge.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class Speedometer extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7000034129617442900L;
	
	public static final Color COLOR_FORWARD = new Color(0x00, 0xE0, 0x00);
	public static final Color COLOR_REVERSE = new Color(0xE0, 0x00, 0x00);
	public static final Color COLOR_OFF = new Color(0xE7, 0xE7, 0xE7);

	int width;
	Image baseImage;
	Image overlayImage;
	Ellipse2D light;
	Stroke lightStroke;
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
		
		light = new Ellipse2D.Double(width - width / 10.7, width / 32.0, width / 12.0, width / 12.0);
		lightStroke = new BasicStroke((float) (width / 128.0), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		
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
		graphics.setStroke(lightStroke);
		if(speed > 0) {
			graphics.setPaint(COLOR_FORWARD);
		}
		else if(speed < 0) {
			graphics.setPaint(COLOR_REVERSE);
		}
		else {
			graphics.setPaint(COLOR_OFF);
		}
		
		AffineTransform old = graphics.getTransform();
		graphics.translate(width / 2, width / 2);
		graphics.rotate(Math.PI * speed - (Math.PI / 2));
		graphics.drawImage(overlayImage, -width / 2, -width / 2, null);
		graphics.setTransform(old);
		graphics.fill(light);
		graphics.setPaint(Color.BLACK);
		graphics.draw(light);
	}
}
