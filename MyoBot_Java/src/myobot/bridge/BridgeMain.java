package myobot.bridge;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import com.sun.glass.events.KeyEvent;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import myobot.bridge.myo.EulerOrientation;
import myobot.bridge.myo.Myo;
import myobot.bridge.myo.MyoException;
import myobot.bridge.ui.AngleVisualizer;
import myobot.bridge.ui.Speedometer;

public class BridgeMain {
	
	public static final Color LF_BACKGROUND = new Color(230, 230, 230);
	public static final Color LF_COLOR = new Color(34, 167, 240);
	public static final Color DISABLED_COLOR = new Color(230, 230, 230);
	public static final int DELAY_TOOLTIP = 500;
	public static final String TOOLTIP_UNLOCKED = "<html>Myo is unlocked.<br>Click to lock.</html>";
	public static final String TOOLTIP_LOCKED = "<html>Myo is locked.<br>Click to unlock.</html>";
	public static final String TOOLTIP_ONARM = "<html>Myo is on arm.</html>";
	public static final String TOOLTIP_OFFARM = "<html>Myo is not on arm.</html>";
	public static final String TOOLTIP_NORMAL = "<html>Orientation is not inverted.<br>Click to invert.</html>";
	public static final String TOOLTIP_INVERTED = "<html>Orientation is inverted.<br>Click to return to normal.</html>";
	public static final int GYRO_SIZE = 80;
	public static final int SPEEDOMETER_SIZE = 80;
	public static final Dimension BUTTON_SIZE = new Dimension(80, 30);
	public static final int VERTICAL_SPACING_SMALL = 5;
	public static final int HORIZONTAL_SPACING_SMALL = 20;
	public static final Dimension TEXT_FIELD_MAX_SIZE = new Dimension(80, Integer.MAX_VALUE);
	public static final Dimension SMALL_ICON_SIZE = new Dimension(24, 24);
	public static final Dimension POSE_ICON_SIZE = new Dimension(100, 100);
	public static final Dimension DIRECTION_ICON_SIZE = new Dimension(50, 50);
	
	static double driveMaxSpeed = 0.5, elevatorMaxSpeed = 0.6, intakeMaxSpeed = 0.7;
	static double leftMotorSpeed, rightMotorSpeed, elevatorSpeed, intakeSpeed;
	
	static boolean myoIsUnlocked = false;
	
	//If true then Euler angles will be inverted
	//This is for when the Myo is worn upside down
	static boolean invertAngles = false;
	
	static final Myo myo = new Myo();

	//Main JFrame
	static JFrame mainFrame;
	//Panel that stores the top status icons and buttons
	static JPanel topBarPanel;
	//Buttons in the top bar
	static JButton lockUnlockButton, invertButton, updateRefButton;
	//Different icons
	static ImageIcon unlockStatusIcon, onArmStatusIcon, invertStatusIcon;
	static ImageIcon poseIcon;
	//Labels for the icons
	static JLabel unlockStatusLabel, onArmStatusLabel, invertStatusLabel;
	//Orientation components
	static AngleVisualizer yawVisualizer, pitchVisualizer, rollVisualizer;
	static JPanel yawPanel, pitchPanel, rollPanel;
	static JTextField yawField, pitchField, rollField;
	static JPanel angleVisualizerPanel;
	//Pose components
	static JPanel posePanel;
	static JLabel poseLabel;
	static JLabel poseNameLabel;
	//Speedometer components
	static JPanel speedometerPanel;
	static JPanel driveSpeedPanel;
	static JPanel leftMotorPanel;
	static JPanel rightMotorPanel;
	static JPanel attachmentsPanel;
	static JPanel elevatorSpeedPanel;
	static JPanel intakeSpeedPanel;
	static JTextField leftMotorField, rightMotorField, elevatorSpeedField, intakeSpeedField;
	static JLabel driveDirectionLabel;
	static ImageIcon driveDirectionIcon;
	static Speedometer leftMotorSpeedometer, rightMotorSpeedometer, elevatorSpeedometer, intakeSpeedometer;
	//Last time's status
	//Used to determine whether or not to update the icons
	static boolean lastOnArm = false;
	static int lastPose = Myo.POSE_REST;
	//The "connecting to myo" dialog
	static JDialog connectingDialog;
	//Different icon images
	static Image imgLocked, imgUnlocked, imgOnArm, imgOffArm, imgNonInverted, imgInverted;
	static Image imgFist, imgSpreadFingers, imgWaveIn, imgWaveOut, imgDoubleTap, imgNoPose;
	static Image imgForward, imgBackward, imgFLeft, imgBLeft, imgFRight, imgBRight, imgNoMovement;
	
	//Flag that will be set to true once the UI is up
	//Used to make sure the main thread does not run ahead of the EDT
	static boolean uiIsSetUp = false;
	//Used to determine whether the main window has been closed, since we can't use EXIT_ON_CLOSE
	static boolean exit = false;
	
	//NT stuff
	static NetworkTableInstance ntInstance;
	static NetworkTable ntTable;
	static NetworkTableEntry ntDriveLeft, ntDriveRight, ntElevator, ntIntake;
	
	/**
	 * Loads an image from the {@code /resources/ui/icons/} directory and rescales it to 24 by 24.
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public static Image loadUIImage(String name, Dimension size) throws IOException {
		InputStream stream = BridgeMain.class.getClass().getResourceAsStream("/resources/ui/icons/" + name);
		Image img = ImageIO.read(stream).getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);
		stream.close();
		return img;
	}
	/**
	 * Rounds a number to a certain number of decimal places.
	 * @param num
	 * @param places
	 * @return
	 */
	public static double roundToPlaces(double num, int places) {
		double factor = Math.pow(10, places);
		return Math.round(num * factor) / factor;
	}
	/**
	 * Sets a component's minimum, maximum and preferred sizes to its current size.
	 * @param component
	 */
	public static void fixSize(JComponent component) {
		Dimension size = component.getSize();
		component.setPreferredSize(size);
		component.setMaximumSize(size);
		component.setMinimumSize(size);
	}
	
	/**
	 * Loads the images.
	 * @throws IOException 
	 */
	public static void loadImages() throws IOException {
		imgLocked = loadUIImage("locked.png", SMALL_ICON_SIZE);
		imgUnlocked = loadUIImage("unlocked.png", SMALL_ICON_SIZE);
		imgOnArm = loadUIImage("on_arm.png", SMALL_ICON_SIZE);
		imgOffArm = loadUIImage("not_on_arm.png", SMALL_ICON_SIZE);
		imgInverted = loadUIImage("inverted.png", SMALL_ICON_SIZE);
		imgNonInverted = loadUIImage("not_inverted.png", SMALL_ICON_SIZE);
		
		imgFist = loadUIImage("fist.png", POSE_ICON_SIZE);
		imgSpreadFingers = loadUIImage("spread_fingers.png", POSE_ICON_SIZE);
		imgWaveIn = loadUIImage("wave_in.png", POSE_ICON_SIZE);
		imgWaveOut = loadUIImage("wave_out.png", POSE_ICON_SIZE);
		imgDoubleTap = loadUIImage("double_tap.png", POSE_ICON_SIZE);
		imgNoPose = loadUIImage("no_pose.png", POSE_ICON_SIZE);

		imgForward = loadUIImage("arrow_front.png", DIRECTION_ICON_SIZE);
		imgBackward = loadUIImage("arrow_back.png", DIRECTION_ICON_SIZE);
		imgFLeft = loadUIImage("arrow_front_left.png", DIRECTION_ICON_SIZE);
		imgBLeft = loadUIImage("arrow_back_left.png", DIRECTION_ICON_SIZE);
		imgFRight = loadUIImage("arrow_front_right.png", DIRECTION_ICON_SIZE);
		imgBRight = loadUIImage("arrow_back_right.png", DIRECTION_ICON_SIZE);
		imgNoMovement = loadUIImage("no_movement.png", DIRECTION_ICON_SIZE);
	}
	/**
	 * Sets up the Look And Feel.
	 */
	public static void setupLookAndFeel() {
		try {
			boolean found = false;
			for(LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if(info.getName().equalsIgnoreCase("Nimbus")) {
					UIManager.setLookAndFeel(info.getClassName());
					found = true;
					break;
				}
			}
			if(!found) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		} 
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		UIManager.getLookAndFeelDefaults().put("background", LF_BACKGROUND);
		UIManager.put("nimbusOrange", LF_COLOR);
		ToolTipManager.sharedInstance().setInitialDelay(DELAY_TOOLTIP);
	}
	
	public static JPanel makeNumberDisplayModule(String name, JPanel display, JTextField numDisplay) {
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		JLabel l = new JLabel(name);
		l.setAlignmentX(JLabel.CENTER_ALIGNMENT);;
		mainPanel.add(l);
		mainPanel.add(Box.createVerticalStrut(VERTICAL_SPACING_SMALL));
		mainPanel.add(display);
		mainPanel.add(Box.createVerticalStrut(VERTICAL_SPACING_SMALL));
		numDisplay.setText("0.0");
		numDisplay.setHorizontalAlignment(JTextField.CENTER);
		numDisplay.setEditable(false);
		numDisplay.setBackground(DISABLED_COLOR);
		numDisplay.setMaximumSize(TEXT_FIELD_MAX_SIZE);
		mainPanel.add(numDisplay);
		return mainPanel;
	}
	
	public static void constructAndShowUI() throws IOException {
		setupLookAndFeel();
		loadImages();
		
		mainFrame = new JFrame("MyoBot Control Center");
		mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		mainFrame.setLayout(new BoxLayout(mainFrame.getContentPane(), BoxLayout.Y_AXIS));
		
		//"Connecting to Myo" Dialog
		connectingDialog = new JDialog(mainFrame, "Please Wait...");
		connectingDialog.setModal(true);
		connectingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		JPanel connectingDialogPanel = new JPanel();
		connectingDialogPanel.setBorder(BorderFactory.createLineBorder(new Color(10, 10, 10), 1, true));
		connectingDialogPanel.setLayout(new BorderLayout());
		JLabel l = new JLabel("Connecting to Myo...");
		l.setFont(l.getFont().deriveFont(18.0f));
		l.setHorizontalAlignment(JLabel.CENTER);
		l.setVerticalAlignment(JLabel.CENTER);
		//Create an empty border around the label
		l.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
		connectingDialogPanel.add(l, BorderLayout.CENTER);

		JProgressBar connectionProgress = new JProgressBar();
		connectionProgress.setPreferredSize(new Dimension(350, 30));
		connectionProgress.setIndeterminate(true);
		//The progress bar has its own panel, because directly adding a border to it doesn't work
		JPanel progressBarPanel = new JPanel();
		progressBarPanel.add(connectionProgress);
		progressBarPanel.setBorder(BorderFactory.createEmptyBorder(0, 25, 20, 25));
		connectingDialogPanel.add(progressBarPanel, BorderLayout.PAGE_END);
		connectingDialog.setContentPane(connectingDialogPanel);
		connectingDialog.setUndecorated(true);
		connectingDialog.pack();
		//Center the dialog
		connectingDialog.setLocationRelativeTo(null);
		
		//Top bar with the icons and buttons
		mainFrame.setLayout(new BoxLayout(mainFrame.getContentPane(), BoxLayout.Y_AXIS));
		topBarPanel = new JPanel();
		//Construct ImageIcons
		unlockStatusIcon = new ImageIcon(imgLocked);
		onArmStatusIcon = new ImageIcon(imgOffArm);
		invertStatusIcon = new ImageIcon(invertAngles ? imgInverted : imgNonInverted);
		topBarPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 5));
		//Store these lambdas as runnables
		//Reused later
		Runnable lockUnlockMyo = () -> {
			if(myo.isLocked()) {
				myo.unlock();
				myoIsUnlocked = true;
				lockUnlockButton.setText("Lock");
				unlockStatusIcon.setImage(imgUnlocked);
				unlockStatusLabel.setToolTipText(TOOLTIP_UNLOCKED);
			}
			else {
				myo.lock();
				myoIsUnlocked = false;
				lockUnlockButton.setText("Unlock");
				unlockStatusIcon.setImage(imgLocked);
				unlockStatusLabel.setToolTipText(TOOLTIP_LOCKED);
			}
			topBarPanel.revalidate();
			topBarPanel.repaint();
		};
		Runnable invertUninvertAngles = () -> {
			invertAngles = !invertAngles;
			invertStatusIcon.setImage(invertAngles ? imgInverted : imgNonInverted);
			invertStatusLabel.setToolTipText(invertAngles ? TOOLTIP_INVERTED : TOOLTIP_NORMAL);
			
			topBarPanel.revalidate();
			topBarPanel.repaint();
		};
		
		//Add our ImageIcons to the top bar
		unlockStatusLabel = new JLabel(unlockStatusIcon);
		unlockStatusLabel.setToolTipText(TOOLTIP_LOCKED);
		//Make them clickable
		unlockStatusLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				lockUnlockMyo.run();
			}
		});
		topBarPanel.add(unlockStatusLabel);
		onArmStatusLabel = new JLabel(onArmStatusIcon);
		onArmStatusLabel.setToolTipText(TOOLTIP_OFFARM);
		topBarPanel.add(onArmStatusLabel);
		invertStatusLabel = new JLabel(invertStatusIcon);
		invertStatusLabel.setToolTipText(TOOLTIP_NORMAL);
		invertStatusLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				invertUninvertAngles.run();
			}
		});
		topBarPanel.add(invertStatusLabel);
		
		//Buttons have their own sub-panel so they can have different gaps
		JPanel topButtonsPanel = new JPanel();
		lockUnlockButton = new JButton();
		lockUnlockButton.setPreferredSize(BUTTON_SIZE);
		@SuppressWarnings("serial")
		Action lockUnlockMyoAction = new AbstractAction("Unlock") {
			@Override
			public void actionPerformed(ActionEvent e) {
				lockUnlockMyo.run();
			}
		};
		lockUnlockButton.setAction(lockUnlockMyoAction);
		lockUnlockMyoAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_L);
		
		invertButton = new JButton();
		invertButton.setPreferredSize(BUTTON_SIZE);
		@SuppressWarnings("serial")
		Action invertAction = new AbstractAction("Invert") {
			@Override
			public void actionPerformed(ActionEvent e) {
				invertUninvertAngles.run();
			}
		};
		invertButton.setAction(invertAction);
		invertAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);
		
		updateRefButton = new JButton();
		@SuppressWarnings("serial")
		Action updateRefAction = new AbstractAction("Reset Orientation") {
			@Override
			public void actionPerformed(ActionEvent e) {
				myo.updateReferenceOrientation();
			}
		};
		updateRefButton.setAction(updateRefAction);
		updateRefAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O);
		
		topButtonsPanel.add(lockUnlockButton);
		topButtonsPanel.add(invertButton);
		topButtonsPanel.add(updateRefButton);
		topBarPanel.add(topButtonsPanel);
		mainFrame.add(topBarPanel);
		
		JPanel middleRow = new JPanel();
		middleRow.setLayout(new GridBagLayout());
		//The panel with the orientation
		angleVisualizerPanel = new JPanel();
		angleVisualizerPanel.setLayout(new BoxLayout(angleVisualizerPanel, BoxLayout.X_AXIS));
		angleVisualizerPanel.setBorder(BorderFactory.createTitledBorder("Orientation"));
		
		angleVisualizerPanel.add(Box.createHorizontalGlue());
		yawPanel = makeNumberDisplayModule("Yaw", yawVisualizer = new AngleVisualizer(GYRO_SIZE), yawField = new JTextField());
		angleVisualizerPanel.add(yawPanel);
		angleVisualizerPanel.add(Box.createHorizontalGlue());
		pitchPanel = makeNumberDisplayModule("Pitch", pitchVisualizer = new AngleVisualizer(GYRO_SIZE), pitchField = new JTextField());
		angleVisualizerPanel.add(pitchPanel);
		angleVisualizerPanel.add(Box.createHorizontalGlue());
		rollPanel = makeNumberDisplayModule("Roll", rollVisualizer = new AngleVisualizer(GYRO_SIZE), rollField = new JTextField());
		angleVisualizerPanel.add(rollPanel);
		angleVisualizerPanel.add(Box.createHorizontalGlue());
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.BOTH;
		middleRow.add(angleVisualizerPanel, constraints);
		
		//Pose section
		posePanel = new JPanel();
		posePanel.setLayout(new BoxLayout(posePanel, BoxLayout.Y_AXIS));
		posePanel.setBorder(BorderFactory.createTitledBorder("Current Pose"));
		poseIcon = new ImageIcon(imgNoPose);
		poseLabel = new JLabel(poseIcon);
		poseLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		posePanel.add(poseLabel);
		poseNameLabel = new JLabel("Rest/Unknown");
		posePanel.add(Box.createVerticalStrut(VERTICAL_SPACING_SMALL));
		poseNameLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		poseNameLabel.setHorizontalAlignment(JLabel.CENTER);
		posePanel.add(poseNameLabel);
		
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.weightx = 0.5;
		constraints.fill = GridBagConstraints.BOTH;
		middleRow.add(posePanel, constraints);
		mainFrame.add(middleRow);
		
		//Speedometers
		speedometerPanel = new JPanel();
		speedometerPanel.setBorder(BorderFactory.createTitledBorder("Robot Status"));
		speedometerPanel.setLayout(new GridBagLayout());
		
		driveSpeedPanel = new JPanel();
		driveSpeedPanel.setBorder(BorderFactory.createTitledBorder("Drive Status"));
		driveSpeedPanel.setLayout(new BoxLayout(driveSpeedPanel, BoxLayout.X_AXIS));
		driveSpeedPanel.add(Box.createHorizontalGlue());
		
		leftMotorPanel = makeNumberDisplayModule("Left Motor", leftMotorSpeedometer = new Speedometer(SPEEDOMETER_SIZE), leftMotorField = new JTextField());
		driveSpeedPanel.add(leftMotorPanel);
		driveSpeedPanel.add(Box.createHorizontalGlue());
		rightMotorPanel = makeNumberDisplayModule("Right Motor", rightMotorSpeedometer = new Speedometer(SPEEDOMETER_SIZE), leftMotorField = new JTextField());
		driveSpeedPanel.add(rightMotorPanel);
		driveSpeedPanel.add(Box.createHorizontalGlue());
		
		driveDirectionIcon = new ImageIcon(imgNoMovement);
		driveDirectionLabel = new JLabel(driveDirectionIcon);
		driveDirectionLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		driveDirectionLabel.setHorizontalAlignment(JLabel.CENTER);
		driveDirectionLabel.setToolTipText("Drive Direction");
		driveSpeedPanel.add(driveDirectionLabel);
		driveSpeedPanel.add(Box.createHorizontalGlue());
		
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		speedometerPanel.add(driveSpeedPanel, constraints);
		
		attachmentsPanel = new JPanel();
		attachmentsPanel.setBorder(BorderFactory.createTitledBorder("Attachments Status"));
		attachmentsPanel.setLayout(new BoxLayout(attachmentsPanel, BoxLayout.X_AXIS));
		attachmentsPanel.add(Box.createHorizontalGlue());
		
		elevatorSpeedPanel = makeNumberDisplayModule("Elevator", elevatorSpeedometer = new Speedometer(SPEEDOMETER_SIZE), elevatorSpeedField = new JTextField());
		attachmentsPanel.add(elevatorSpeedPanel);
		attachmentsPanel.add(Box.createHorizontalGlue());
		intakeSpeedPanel = makeNumberDisplayModule("Intake", intakeSpeedometer = new Speedometer(SPEEDOMETER_SIZE), intakeSpeedField = new JTextField());
		attachmentsPanel.add(intakeSpeedPanel);
		attachmentsPanel.add(Box.createHorizontalGlue());
		
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 0.67;
		speedometerPanel.add(attachmentsPanel, constraints);
		
		mainFrame.add(speedometerPanel);
		
		mainFrame.pack();
		mainFrame.setSize(600, mainFrame.getSize().height);
		fixSize(yawField);
		fixSize(pitchField);
		fixSize(rollField);
		mainFrame.setVisible(true);
		
		//Prompt for team number
		JPanel promptPanel = new JPanel();
		promptPanel.setLayout(new BorderLayout());
		promptPanel.add(new JLabel("Enter FRC Team Number, or 0 for Dry-Run:"), BorderLayout.CENTER);
		JTextField teamNumber = new JTextField();
		promptPanel.add(teamNumber, BorderLayout.PAGE_END);
		//Initialize NT
		ntInstance = NetworkTableInstance.getDefault();
		ntTable = ntInstance.getTable("myobot");
		ntDriveLeft = ntTable.getEntry("leftDrive");
		ntDriveRight = ntTable.getEntry("rightDrive");
		ntElevator = ntTable.getEntry("elevator");
		ntIntake = ntTable.getEntry("intake");
		boolean done = false;
		while(!done) {
			JOptionPane.showMessageDialog(mainFrame, promptPanel, "Enter Team Number", JOptionPane.PLAIN_MESSAGE);
			try {
				int team = Integer.parseInt(teamNumber.getText());
				if(team != 0) {
					ntInstance.setUpdateRate(1.0 / 15);
					ntInstance.startClientTeam(team);
					ntInstance.startDSClient();
				}
				done = true;
			}
			catch(NumberFormatException nfe) {
				JOptionPane.showMessageDialog(mainFrame, "Please enter a valid team number.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		uiIsSetUp = true;
		connectingDialog.setVisible(true);
	}
	
	public static void updateUI(boolean onArm, EulerOrientation orientation, int pose) {
		//No need to worry about locked/unlocked or inverted/normal
		if(onArm != lastOnArm) {
			if(onArm) {
				onArmStatusIcon.setImage(imgOnArm);
				onArmStatusLabel.setToolTipText(TOOLTIP_ONARM);
			}
			else {
				onArmStatusIcon.setImage(imgOffArm);
				onArmStatusLabel.setToolTipText(TOOLTIP_OFFARM);
				
				yawField.setText("0.0");
				pitchField.setText("0.0");
				rollField.setText("0.0");
				yawVisualizer.updateAngleNoRepaint(0);
				pitchVisualizer.updateAngleNoRepaint(0);
				rollVisualizer.updateAngleNoRepaint(0);
				poseIcon.setImage(imgNoPose);
				poseNameLabel.setText("Rest/Unknown");
				lockUnlockButton.setText("Unlock");
				unlockStatusIcon.setImage(imgLocked);
				unlockStatusLabel.setToolTipText(TOOLTIP_LOCKED);
				mainFrame.repaint();
			}
			
			lastOnArm = onArm;
			topBarPanel.repaint();
		}
		
		if(!onArm) {
			return;
		}
		
		if(orientation.getYawDegrees() != yawVisualizer.getAngle()) {
			yawField.setText(String.valueOf(roundToPlaces(orientation.getYawDegrees(), 2)));
			//Values are negated because moving left causes a negative yaw
			//This makes the angle visualizers more intuitive
			yawVisualizer.updateAngleNoRepaint(-orientation.getYawDegrees());
			yawPanel.repaint();
		}
		if(orientation.getPitchDegrees() != pitchVisualizer.getAngle()) {
			pitchField.setText(String.valueOf(roundToPlaces(orientation.getPitchDegrees(), 2)));
			pitchVisualizer.updateAngleNoRepaint(-orientation.getPitchDegrees());
			pitchPanel.repaint();
		}
		if(orientation.getRollDegrees() != rollVisualizer.getAngle()) {
			rollField.setText(String.valueOf(roundToPlaces(orientation.getRollDegrees(), 2)));
			rollVisualizer.updateAngleNoRepaint(-orientation.getRollDegrees());
			rollPanel.repaint();
		}
		
		if(pose != lastPose) {
			Image imgPose;
			String poseName;
			switch(pose) {
			case Myo.POSE_FIST:
				imgPose = imgFist;
				poseName = "Fist";
				break;
			case Myo.POSE_SPREADFINGERS:
				imgPose = imgSpreadFingers;
				poseName = "Spread Fingers";
				break;
			case Myo.POSE_WAVEIN:
				imgPose = imgWaveIn;
				poseName = "Wave In";
				break;
			case Myo.POSE_WAVEOUT:
				imgPose = imgWaveOut;
				poseName = "Wave Out";
				break;
			case Myo.POSE_DOUBLETAP:
				imgPose = imgDoubleTap;
				poseName = "Double Tap";
				break;
			case Myo.POSE_REST:
			case Myo.POSE_UNKNOWN:
			default:
				imgPose = imgNoPose;
				poseName = "Rest/Unknown";
				break;
			}
			
			poseIcon.setImage(imgPose);
			poseNameLabel.setText(poseName);
			posePanel.repaint();
			
			lastPose = pose;
		}
	}
	
	static boolean drivingForwards = true;
	static boolean doubleTapping = false;
	/**
	 * Computes the speeds that are to be sent to the robot based on the Myo's orientation and pose.
	 */
	public static void computeSpeeds(EulerOrientation orientation, int pose) {
		if(pose == Myo.POSE_DOUBLETAP && !doubleTapping) {
			drivingForwards = !drivingForwards;
			doubleTapping = true;
			myo.notifyUserAction();
		}
		else if(pose != Myo.POSE_DOUBLETAP) {
			doubleTapping = false;
			
			if(orientation.getPitchDegrees() > -45) {
				elevatorSpeed = 0;
				double drivingSpeed = Math.min(90, orientation.getPitchDegrees() + 45) / 90.0;
				
				if(!drivingForwards) {
					drivingSpeed = -drivingSpeed;
				}
				
				double turningSpeed = 0;
				if(Math.abs(orientation.getYawDegrees()) >= 10) {
					turningSpeed = Math.min(35, Math.abs(turningSpeed) - 10) / (35);
					//Turning left
					if(orientation.getYawDegrees() > 0) {
						turningSpeed = -turningSpeed;
					}
				}
				
				leftMotorSpeed = drivingSpeed * driveMaxSpeed + turningSpeed * driveMaxSpeed;
				rightMotorSpeed = drivingSpeed * driveMaxSpeed - turningSpeed * driveMaxSpeed;
				
				intakeSpeed = 0;
				if(pose == Myo.POSE_FIST) {
					intakeSpeed = intakeMaxSpeed;
				}
				else if(pose == Myo.POSE_SPREADFINGERS) {
					intakeSpeed = -intakeMaxSpeed;
				}
			}
			else if(pose == Myo.POSE_FIST) {
				intakeSpeed = intakeMaxSpeed;
				elevatorSpeed = 0;
				leftMotorSpeed = rightMotorSpeed = 0;
			}
			else if(pose == Myo.POSE_SPREADFINGERS) {
				intakeSpeed = -intakeMaxSpeed;
				elevatorSpeed = 0;
				leftMotorSpeed = rightMotorSpeed = 0;
			}
			else if(pose == Myo.POSE_WAVEOUT) {
				elevatorSpeed = -elevatorMaxSpeed;
				intakeSpeed = 0;
				leftMotorSpeed = rightMotorSpeed = 0;
			}
			else if(pose == Myo.POSE_WAVEIN) {
				elevatorSpeed = elevatorMaxSpeed;
				intakeSpeed = 0;
				leftMotorSpeed = rightMotorSpeed = 0;
			}
			else {
				elevatorSpeed = intakeSpeed = leftMotorSpeed = rightMotorSpeed = 0;
			}
		}
	}
	/**
	 * Sends the speed values over NetworkTables to the robot.
	 */
	public static void updateNT() {
		ntDriveLeft.forceSetDouble(leftMotorSpeed);
		ntDriveRight.forceSetDouble(rightMotorSpeed);
		ntElevator.forceSetDouble(elevatorSpeed);
		ntIntake.forceSetDouble(intakeSpeed);
	}
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		
		//Add shutdown hook to make sure Myo is cleaned up
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if(myo.isInitialized()) {
					myo.cleanup();
				}
			}
		});
		
		SwingUtilities.invokeLater(() -> {
			try {
				constructAndShowUI();
			}
			catch(IOException e) {
				e.printStackTrace();
				System.exit(0);
			}
		});
		
		//Wait until UI is set up
		while(!uiIsSetUp) {
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		//Initialize Myo
		try {
			myo.init("org.usfirst.frc.team6135.MyoBot");
		}
		catch(MyoException e) {
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(mainFrame, "Cannot connect to Myo!\nThe program will now exit.", "Error", JOptionPane.ERROR_MESSAGE);
				System.exit(0);
			});
		}
		myo.setLockingPolicy(Myo.LOCKING_POLICY_NONE);
		//Dispose of the dialog
		connectingDialog.setVisible(false);
		connectingDialog.dispose();
		
		//Add close listener to know when to exit
		mainFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				exit = true;
			}
		});
		
		while(!exit) {
			myo.runHub(100);
			
			boolean onArm = myo.isOnArm();
			EulerOrientation orientation = invertAngles ? myo.getOrientation().negate() : myo.getOrientation();
			int pose = myo.getPose();
			
			if(onArm && myoIsUnlocked) {
				computeSpeeds(orientation, pose);
			}
			else {
				leftMotorSpeed = rightMotorSpeed = elevatorSpeed = intakeSpeed = 0;
			}
			updateNT();
			
			SwingUtilities.invokeLater(() -> {				
				updateUI(onArm, orientation, pose);
			});
		}
	}

}
