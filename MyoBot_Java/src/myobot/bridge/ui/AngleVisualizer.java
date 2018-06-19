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

public class AngleVisualizer extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6154496883322201997L;
	
	int size;
	Image baseImage;
	Image overlayImage;
	double angle = 0;
	double offset = 0;
	public AngleVisualizer(int size, double offset) throws IOException {
		super();
		this.size = size;
		this.offset = offset;
		
		InputStream imageStream = getClass().getResourceAsStream("/resources/ui/gyro/gyro_base.png");
		baseImage = ImageIO.read(imageStream).getScaledInstance(size, size, Image.SCALE_SMOOTH);
		imageStream.close();
		imageStream = getClass().getResourceAsStream("/resources/ui/gyro/gyro_pointer.png");
		overlayImage = ImageIO.read(imageStream).getScaledInstance(size, size, Image.SCALE_SMOOTH);
		imageStream.close();
		
		setPreferredSize(new Dimension(size, size));
		setMinimumSize(new Dimension(size, size));
		setMaximumSize(new Dimension(size, size));
	}
	public AngleVisualizer(int size) throws IOException {
		this(size, 0);
	}
	
	public double getAngle() {
		return angle;
	}
	public void updateAngle(double newAngle) {
		angle = newAngle;
		repaint();
	}
	public void updateAngleNoRepaint(double newAngle) {
		angle = newAngle;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D graphics = (Graphics2D) g;
		
		graphics.drawImage(baseImage, 0, 0, null);
		
		AffineTransform old = graphics.getTransform();
		
		graphics.translate(size / 2, size / 2);
		graphics.rotate(Math.toRadians(angle + offset));
		graphics.drawImage(overlayImage, -size / 2, -size / 2, null);
		
		graphics.setTransform(old);
	}
}
