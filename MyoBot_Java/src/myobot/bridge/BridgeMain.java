package myobot.bridge;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.List;
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
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import com.thalmic.myo.Arm;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.MyoException;
import com.thalmic.myo.Pose;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import myobot.bridge.myo.DataCollector;
import myobot.bridge.myo.EulerOrientation;
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
	public static final String TOOLTIP_1_0 = "<html>Controls: Version 1.0<br>Click to cycle through all supported controls versions.</html>";
	public static final String TOOLTIP_2_0 = "<html>Controls: Version 2.0<br>Click to cycle through all supported controls versions.</html>";
	public static final String TOOLTIP_2_1 = "<html>Controls: Version 2.1<br>Click to cycle through all supported controls versions.</html>";
	public static final String TOOLTIP_FORWARDS = "<html>Drive Direction: Forward<br>Double-Tap or click to change.</html>";
	public static final String TOOLTIP_BACKWARDS = "<html>Drive Direction: Backward<br>Double-Tap or click to change.</html>";
	public static final int GYRO_SIZE = 80;
	public static final int SPEEDOMETER_SIZE = 80;
	public static final Dimension BUTTON_SIZE = new Dimension(80, 30);
	public static final int VERTICAL_SPACING_SMALL = 5;
	public static final int HORIZONTAL_SPACING_SMALL = 20;
	public static final Dimension TEXT_FIELD_MAX_SIZE = new Dimension(80, Integer.MAX_VALUE);
	public static final Dimension SLIDER_MAX_SIZE = new Dimension(200, 39);
	public static final Dimension SMALL_ICON_SIZE = new Dimension(24, 24);
	public static final Dimension POSE_ICON_SIZE = new Dimension(100, 100);
	public static final Dimension DIRECTION_ICON_SIZE = new Dimension(50, 50);
	
	static double driveMaxSpeed = 0.5, elevatorMaxSpeed = 0.6, intakeMaxSpeed = 0.7;
	static double leftMotorSpeed, rightMotorSpeed, elevatorSpeed, intakeSpeed;
	
	static boolean drivingForwards = true;
	
	//If true then Euler angles will be inverted
	//This is for when the Myo is worn upside down
	static boolean invertAngles = false;
	
	static Hub myoHub = null;
	static Myo myo;
	static DataCollector collector = new DataCollector();
	static boolean paused = false;

	//Main JFrame
	static JFrame mainFrame;
	//Panel that stores the top status icons and buttons
	static JPanel topBarPanel;
	//Buttons in the top bar
	static JButton lockUnlockButton, invertButton, updateRefButton, changeControlsModeButton;
	//Different icons
	static ImageIcon unlockStatusIcon, onArmStatusIcon, invertStatusIcon, controlsModeIcon, directionIcon;
	static ImageIcon poseIcon;
	//Labels for the icons
	static JLabel unlockStatusLabel, onArmStatusLabel, invertStatusLabel, controlsModeLabel, directionLabel;
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
	//Slider components
	static JPanel maxSpeedPanel;
	static JPanel maxDriveSpeedPanel;
	static JPanel maxElevatorSpeedPanel;
	static JPanel maxIntakeSpeedPanel;
	static JSlider maxDriveSpeedSlider;
	static JSlider maxElevatorSpeedSlider;
	static JSlider maxIntakeSpeedSlider;
	static JTextField maxDriveSpeedField;
	static JTextField maxElevatorSpeedField;
	static JTextField maxIntakeSpeedField;
	//Prompt stuff
	static JPanel promptPanel;
	static JTextField teamNumber;
	//Last time's status
	//Used to determine whether or not to update the icons
	static boolean lastOnArm = false;
	static boolean lastUnlocked = false;
	static boolean lastDrivingForwards = drivingForwards;
	static Pose lastPose = Pose.rest;
	//The "connecting to myo" dialog
	static JDialog connectingDialog;
	//The loading dialog
	static JDialog loadingDialog;
	static JLabel loadingStatusLabel;
	//Different icon images
	static Image imgMyoBotIcon;
	static Image imgLocked, imgUnlocked, imgOnArm, imgOffArm, imgNonInverted, imgInverted, imgForwardSmall, imgBackwardSmall,
			imgControls1_0, imgControls2_0, imgControls2_1;
	static Image imgFist, imgSpreadFingers, imgWaveIn, imgWaveOut, imgDoubleTap, imgNoPose;
	static Image imgForward, imgBackward, imgFLeft, imgBLeft, imgFRight, imgBRight, imgTurnCW, imgTurnCCW, imgNoMovement;
	
	enum ControlsMode {
		V1_0, V2_0, V2_1;
		
		public Image getIcon() {
			switch(this) {
			case V1_0:
				return imgControls1_0;
			case V2_0:
				return imgControls2_0;
			case V2_1:
				return imgControls2_1;
			default:
				return null;
			}
		}
		public String getTooltip() {
			switch(this) {
			case V1_0:
				return TOOLTIP_1_0;
			case V2_0:
				return TOOLTIP_2_0;
			case V2_1:
				return TOOLTIP_2_1;
			default:
				return null;
			}
		}
		public ControlsMode getNext() {
			switch(this) {
			case V1_0:
				return V2_0;
			case V2_0:
				return V2_1;
			case V2_1:
				return V1_0;
			default:
				return null;
			}
		}
	}
	static ControlsMode controlsMode = ControlsMode.V2_1;
	
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
	public static Image loadControlsVersionImage(String version) throws IOException {
		InputStream stream = BridgeMain.class.getClass().getResourceAsStream("/resources/ui/icons/control_mode_base.png");
		BufferedImage img = ImageIO.read(stream);
		stream.close();
		Font versionFont = new Font("Arial", Font.PLAIN, 90);
		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setPaint(Color.BLACK);
		g.setFont(versionFont);
		g.drawString(version, 110, 80);
		return img.getScaledInstance(SMALL_ICON_SIZE.width, SMALL_ICON_SIZE.height, Image.SCALE_SMOOTH);
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
	 * Returns the complete stack trace of a {@code Throwable} as a {@code String}.
	 * @param t The {@code Throwable} to take the stack trace from
	 * @return The complete stack trace of {@code t}, as a {@code String}
	 */
	public static String getStackTraceAsString(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.close();
		return sw.toString();
	}
	public static void reportFatalError(Throwable e) {
		e.printStackTrace();
		//Close ALL windows
		//If we don't do this, the windows and dialogs can cover our message!
		Window[] windows = Window.getWindows();
		for(Window w : windows) {
			w.dispose();
		}
		JOptionPane.showMessageDialog(null, "A fatal error has occurred.\nStack Trace:\n" + getStackTraceAsString(e) + "\nThe application will now exit.", "Fatal Error", JOptionPane.ERROR_MESSAGE);
		System.exit(1);
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
		
		imgControls1_0 = loadControlsVersionImage("1.0");
		imgControls2_0 = loadControlsVersionImage("2.0");
		imgControls2_1 = loadControlsVersionImage("2.1");
		
		imgFist = loadUIImage("fist.png", POSE_ICON_SIZE);
		imgSpreadFingers = loadUIImage("spread_fingers.png", POSE_ICON_SIZE);
		imgWaveIn = loadUIImage("wave_in.png", POSE_ICON_SIZE);
		imgWaveOut = loadUIImage("wave_out.png", POSE_ICON_SIZE);
		imgDoubleTap = loadUIImage("double_tap.png", POSE_ICON_SIZE);
		imgNoPose = loadUIImage("no_pose.png", POSE_ICON_SIZE);

		imgForward = loadUIImage("arrow_front.png", DIRECTION_ICON_SIZE);
		imgBackward = loadUIImage("arrow_back.png", DIRECTION_ICON_SIZE);
		imgForwardSmall = imgForward.getScaledInstance(SMALL_ICON_SIZE.width, SMALL_ICON_SIZE.height, Image.SCALE_SMOOTH);
		imgBackwardSmall = imgBackward.getScaledInstance(SMALL_ICON_SIZE.width, SMALL_ICON_SIZE.height, Image.SCALE_SMOOTH);
		imgFLeft = loadUIImage("arrow_front_left.png", DIRECTION_ICON_SIZE);
		imgBLeft = loadUIImage("arrow_back_left.png", DIRECTION_ICON_SIZE);
		imgFRight = loadUIImage("arrow_front_right.png", DIRECTION_ICON_SIZE);
		imgBRight = loadUIImage("arrow_back_right.png", DIRECTION_ICON_SIZE);
		imgNoMovement = loadUIImage("no_movement.png", DIRECTION_ICON_SIZE);
		imgTurnCW = loadUIImage("arrow_rotate_right.png", DIRECTION_ICON_SIZE);
		imgTurnCCW = loadUIImage("arrow_rotate_left.png", DIRECTION_ICON_SIZE);
		
		imgMyoBotIcon = loadUIImage("myobot_icon.png", new Dimension(512, 512));
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
	
	public static JPanel makeDisplayModule1(String name, JComponent display, JTextField numDisplay) {
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

		loadingDialog = new JDialog();
		loadingDialog.setAlwaysOnTop(true);
		loadingDialog.setResizable(false);
		loadingDialog.setUndecorated(true);
		loadingDialog.setTitle("Loading...");
		loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		JPanel loadingPanel = new JPanel();
		loadingPanel.setBorder(BorderFactory.createLineBorder(new Color(10, 10, 10), 1, true));
		loadingPanel.setLayout(new GridBagLayout());
		JLabel loadingMainLabel = new JLabel("Loading User Interface...");
		loadingMainLabel.setFont(loadingMainLabel.getFont().deriveFont(18.0f));
		loadingMainLabel.setHorizontalAlignment(JLabel.CENTER);
		loadingMainLabel.setVerticalAlignment(JLabel.CENTER);
		loadingMainLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty =	1.0;
		loadingPanel.add(loadingMainLabel, c);
		loadingStatusLabel = new JLabel("Loading Images...");
		loadingStatusLabel.setFont(loadingStatusLabel.getFont().deriveFont(10.0f));
		loadingStatusLabel.setHorizontalAlignment(JLabel.CENTER);
		loadingStatusLabel.setVerticalAlignment(JLabel.BOTTOM);
		c.gridy = 1;
		c.weighty = 0.2;
		loadingPanel.add(loadingStatusLabel, c);
		JProgressBar loadingProgressBar = new JProgressBar();
		loadingProgressBar.setIndeterminate(true);
		loadingProgressBar.setPreferredSize(new Dimension(350, 30));
		JPanel loadingProgressBarPanel = new JPanel();
		loadingProgressBarPanel.add(loadingProgressBar);
		loadingProgressBarPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
		c.gridy = 2;
		c.weighty = 0.5;
		loadingPanel.add(loadingProgressBarPanel, c);
		
		loadingDialog.setContentPane(loadingPanel);
		loadingDialog.pack();
		loadingDialog.setLocationRelativeTo(null);
		loadingDialog.setVisible(true);
		
		SwingWorker<Void, String> uiWorker = new SwingWorker<Void, String>() {
			
			@Override
			protected Void doInBackground() throws Exception {
				loadImages();
				publish("Constructing Dialogs...");
				return null;
			}
			
			@Override
			protected void process(List<String> chunks) {
				loadingStatusLabel.setText(chunks.get(chunks.size() - 1));
				loadingStatusLabel.paintImmediately(loadingStatusLabel.getVisibleRect());
			}
			
			protected void done() {
				try {
					//Initialize prompt for team number
					promptPanel = new JPanel();
					promptPanel.setLayout(new BorderLayout());
					promptPanel.add(new JLabel("Enter FRC Team Number, or 0 for Dry-Run:"), BorderLayout.CENTER);
					teamNumber = new JTextField();
					promptPanel.add(teamNumber, BorderLayout.PAGE_END);
					
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
					
					loadingStatusLabel.setText("Constructing Main Window...");
					loadingStatusLabel.paintImmediately(loadingStatusLabel.getVisibleRect());
					
					mainFrame = new JFrame("MyoBot Control Center");
					mainFrame.setIconImage(imgMyoBotIcon);
					//Handled later
					mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
					mainFrame.setLayout(new GridBagLayout());
					mainFrame.setLayout(new BoxLayout(mainFrame.getContentPane(), BoxLayout.Y_AXIS));
					
					//Top bar with the icons and buttons
					topBarPanel = new JPanel();
					//Construct ImageIcons
					unlockStatusIcon = new ImageIcon(imgLocked);
					onArmStatusIcon = new ImageIcon(imgOffArm);
					invertStatusIcon = new ImageIcon(invertAngles ? imgInverted : imgNonInverted);
					controlsModeIcon = new ImageIcon(controlsMode.getIcon());
					directionIcon = new ImageIcon(drivingForwards ? imgForwardSmall : imgBackwardSmall);
					topBarPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 5));
					//Store these lambdas as runnables
					//Reused later
					Runnable lockUnlockMyo = () -> {
						//If the Myo is not on an arm, locking and unlocking should do nothing
						if(!collector.onArm()) {
							Toolkit.getDefaultToolkit().beep();
							return;
						}
						if(!collector.isUnlocked()) {
							myo.unlock(Myo.UnlockType.unlockHold);
							lockUnlockButton.setText("Lock");
							unlockStatusIcon.setImage(imgUnlocked);
							unlockStatusLabel.setToolTipText(TOOLTIP_UNLOCKED);
						}
						else {
							myo.lock();
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
					Runnable changeControlsMode = () -> {
						controlsMode = controlsMode.getNext();
						
						controlsModeIcon.setImage(controlsMode.getIcon());
						controlsModeLabel.setToolTipText(controlsMode.getTooltip());
						if(controlsMode == ControlsMode.V2_1) {
							directionIcon.setImage(drivingForwards ? imgForwardSmall : imgBackwardSmall);
							directionLabel.setToolTipText(drivingForwards ? TOOLTIP_FORWARDS : TOOLTIP_BACKWARDS);
							directionLabel.setVisible(true);
						}
						else {
							directionLabel.setVisible(false);
						}
						
						if(collector.isUnlocked() && collector.onArm()) {
							myo.lock();
							lockUnlockButton.setText("Unlock");
							unlockStatusIcon.setImage(imgLocked);
							unlockStatusLabel.setToolTipText(TOOLTIP_LOCKED);
						}
						drivingForwards = true;
						doubleTapping = false;
						
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
					invertStatusLabel.setToolTipText(invertAngles ? TOOLTIP_INVERTED : TOOLTIP_NORMAL);
					invertStatusLabel.addMouseListener(new MouseAdapter() {
						@Override
						public void mouseClicked(MouseEvent e) {
							invertUninvertAngles.run();
						}
					});
					topBarPanel.add(invertStatusLabel);
					controlsModeLabel = new JLabel(controlsModeIcon);
					controlsModeLabel.setToolTipText(controlsMode.getTooltip());
					controlsModeLabel.addMouseListener(new MouseAdapter() {
						@Override
						public void mouseClicked(MouseEvent e) {
							changeControlsMode.run();
						}
					});
					topBarPanel.add(controlsModeLabel);
					directionLabel = new JLabel(directionIcon);
					if(controlsMode != ControlsMode.V2_1) {
						directionLabel.setVisible(false);
					}
					directionLabel.addMouseListener(new MouseAdapter() {
						@Override
						public void mouseClicked(MouseEvent e) {
							drivingForwards = !drivingForwards;
							lastDrivingForwards = drivingForwards;
							directionIcon.setImage(drivingForwards ? imgForwardSmall : imgBackwardSmall);
							directionLabel.setToolTipText(drivingForwards ? TOOLTIP_FORWARDS : TOOLTIP_BACKWARDS);
							directionLabel.repaint();
						}
					});
					directionLabel.setToolTipText(drivingForwards ? TOOLTIP_FORWARDS : TOOLTIP_BACKWARDS);
					topBarPanel.add(directionLabel);
					
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
							if(myo != null)
								collector.setRefOrientation(collector.getRawOrientation());
						}
					};
					updateRefButton.setAction(updateRefAction);
					updateRefAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O);
					
					changeControlsModeButton = new JButton();
					@SuppressWarnings("serial")
					Action changeControlsAction = new AbstractAction("Change Controls Version") {
						@Override
						public void actionPerformed(ActionEvent e) {
							changeControlsMode.run();
						}
					};
					changeControlsModeButton.setAction(changeControlsAction);
					changeControlsAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
					
					topButtonsPanel.add(lockUnlockButton);
					topButtonsPanel.add(invertButton);
					topButtonsPanel.add(updateRefButton);
					topButtonsPanel.add(changeControlsModeButton);
					topBarPanel.add(topButtonsPanel);
					GridBagConstraints c = new GridBagConstraints();
					c.gridx = 0;
					c.gridy = 0;
					c.fill = GridBagConstraints.BOTH;
					c.weightx = 1.0;
					c.weighty = 0.5;
					mainFrame.add(topBarPanel, c);
					
					JPanel middleRow = new JPanel();
					middleRow.setLayout(new GridBagLayout());
					//The panel with the orientation
					angleVisualizerPanel = new JPanel();
					angleVisualizerPanel.setLayout(new BoxLayout(angleVisualizerPanel, BoxLayout.X_AXIS));
					angleVisualizerPanel.setBorder(BorderFactory.createTitledBorder("Orientation"));
					
					angleVisualizerPanel.add(Box.createHorizontalGlue());
					yawPanel = makeDisplayModule1("Yaw", yawVisualizer = new AngleVisualizer(GYRO_SIZE), yawField = new JTextField());
					angleVisualizerPanel.add(yawPanel);
					angleVisualizerPanel.add(Box.createHorizontalGlue());
					pitchPanel = makeDisplayModule1("Pitch", pitchVisualizer = new AngleVisualizer(GYRO_SIZE), pitchField = new JTextField());
					angleVisualizerPanel.add(pitchPanel);
					angleVisualizerPanel.add(Box.createHorizontalGlue());
					rollPanel = makeDisplayModule1("Roll", rollVisualizer = new AngleVisualizer(GYRO_SIZE), rollField = new JTextField());
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
					c = new GridBagConstraints();
					c.gridx = 0;
					c.gridy = 1;
					c.fill = GridBagConstraints.BOTH;
					c.weightx = 1.0;
					c.weighty = 1.0;
					mainFrame.add(middleRow, c);
					
					//Speedometers
					speedometerPanel = new JPanel();
					//speedometerPanel.setBorder(BorderFactory.createTitledBorder("Robot Status"));
					speedometerPanel.setLayout(new GridBagLayout());
					
					driveSpeedPanel = new JPanel();
					driveSpeedPanel.setBorder(BorderFactory.createTitledBorder("Drive Status"));
					driveSpeedPanel.setLayout(new BoxLayout(driveSpeedPanel, BoxLayout.X_AXIS));
					driveSpeedPanel.add(Box.createHorizontalGlue());
					
					leftMotorPanel = makeDisplayModule1("Left Motor", leftMotorSpeedometer = new Speedometer(SPEEDOMETER_SIZE), leftMotorField = new JTextField());
					driveSpeedPanel.add(leftMotorPanel);
					driveSpeedPanel.add(Box.createHorizontalGlue());
					rightMotorPanel = makeDisplayModule1("Right Motor", rightMotorSpeedometer = new Speedometer(SPEEDOMETER_SIZE), rightMotorField = new JTextField());
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
					
					elevatorSpeedPanel = makeDisplayModule1("Elevator", elevatorSpeedometer = new Speedometer(SPEEDOMETER_SIZE), elevatorSpeedField = new JTextField());
					attachmentsPanel.add(elevatorSpeedPanel);
					attachmentsPanel.add(Box.createHorizontalGlue());
					intakeSpeedPanel = makeDisplayModule1("Intake", intakeSpeedometer = new Speedometer(SPEEDOMETER_SIZE), intakeSpeedField = new JTextField());
					attachmentsPanel.add(intakeSpeedPanel);
					attachmentsPanel.add(Box.createHorizontalGlue());
					
					constraints.gridx = 1;
					constraints.gridy = 0;
					constraints.fill = GridBagConstraints.BOTH;
					constraints.weightx = 0.67;
					speedometerPanel.add(attachmentsPanel, constraints);
					
					c = new GridBagConstraints();
					c.gridx = 0;
					c.gridy = 2;
					c.fill = GridBagConstraints.BOTH;
					c.weightx = 1.0;
					c.weighty = 0.8;
					mainFrame.add(speedometerPanel, c);
					
					maxSpeedPanel = new JPanel();
					maxSpeedPanel.setLayout(new BoxLayout(maxSpeedPanel, BoxLayout.LINE_AXIS));
					maxSpeedPanel.setBorder(BorderFactory.createTitledBorder("Maximum Speeds"));
					maxSpeedPanel.add(Box.createHorizontalGlue());
					
					maxDriveSpeedSlider = new JSlider(0, 100, (int) (driveMaxSpeed * 100));
					maxDriveSpeedSlider.setMajorTickSpacing(25);
					maxDriveSpeedSlider.setMinorTickSpacing(5);
					maxDriveSpeedSlider.setPaintTicks(true);
					Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
					labelTable.put(0, new JLabel("0"));
					labelTable.put(25, new JLabel("0.25"));
					labelTable.put(50, new JLabel("0.5"));
					labelTable.put(75, new JLabel("0.75"));
					labelTable.put(100, new JLabel("1"));
					maxDriveSpeedSlider.setLabelTable(labelTable);
					maxDriveSpeedSlider.setPaintLabels(true);
					maxDriveSpeedSlider.setMaximumSize(SLIDER_MAX_SIZE);
					maxDriveSpeedSlider.addChangeListener(e -> {
						JSlider source = (JSlider) e.getSource();
						driveMaxSpeed = source.getValue() / 100.0;
						String speed = String.valueOf(driveMaxSpeed);
						if(!(speed.equals("1") || speed.equals("0")) &&
								speed.substring(2).length() < 2) {
							speed += "0";
						}
						maxDriveSpeedField.setText(speed);
					});
					maxDriveSpeedPanel = makeDisplayModule1("Drive", maxDriveSpeedSlider, maxDriveSpeedField = new JTextField());
					maxDriveSpeedField.setText(String.valueOf(roundToPlaces(driveMaxSpeed, 2)));
					maxSpeedPanel.add(maxDriveSpeedPanel);
					maxSpeedPanel.add(Box.createHorizontalGlue());
					
					maxElevatorSpeedSlider = new JSlider(0, 100, (int) (elevatorMaxSpeed * 100));
					maxElevatorSpeedSlider.setMajorTickSpacing(25);
					maxElevatorSpeedSlider.setMinorTickSpacing(5);
					maxElevatorSpeedSlider.setPaintTicks(true);
					maxElevatorSpeedSlider.setLabelTable(labelTable);
					maxElevatorSpeedSlider.setPaintLabels(true);
					maxElevatorSpeedSlider.setMaximumSize(SLIDER_MAX_SIZE);
					maxElevatorSpeedSlider.addChangeListener(e -> {
						JSlider source = (JSlider) e.getSource();
						elevatorMaxSpeed = source.getValue() / 100.0;
						String speed = String.valueOf(elevatorMaxSpeed);
						if(!(speed.equals("1") || speed.equals("0")) &&
								speed.substring(2).length() < 2) {
							speed += "0";
						}
						maxElevatorSpeedField.setText(speed);
					});
					maxElevatorSpeedPanel = makeDisplayModule1("Elevator", maxElevatorSpeedSlider, maxElevatorSpeedField = new JTextField());
					maxElevatorSpeedField.setText(String.valueOf(roundToPlaces(elevatorMaxSpeed, 2)));
					maxSpeedPanel.add(maxElevatorSpeedPanel);
					maxSpeedPanel.add(Box.createHorizontalGlue());
					
					maxIntakeSpeedSlider = new JSlider(0, 100, (int) (intakeMaxSpeed * 100));
					maxIntakeSpeedSlider.setMajorTickSpacing(25);
					maxIntakeSpeedSlider.setMinorTickSpacing(5);
					maxIntakeSpeedSlider.setPaintTicks(true);
					maxIntakeSpeedSlider.setLabelTable(labelTable);
					maxIntakeSpeedSlider.setPaintLabels(true);
					maxIntakeSpeedSlider.setMaximumSize(SLIDER_MAX_SIZE);
					maxIntakeSpeedSlider.addChangeListener(e -> {
						JSlider source = (JSlider) e.getSource();
						intakeMaxSpeed = source.getValue() / 100.0;
						String speed = String.valueOf(intakeMaxSpeed);
						if(!(speed.equals("1") || speed.equals("0")) &&
								speed.substring(2).length() < 2) {
							speed += "0";
						}
						maxIntakeSpeedField.setText(speed);
					});
					maxIntakeSpeedPanel = makeDisplayModule1("Intake", maxIntakeSpeedSlider, maxIntakeSpeedField = new JTextField());
					maxIntakeSpeedField.setText(String.valueOf(roundToPlaces(intakeMaxSpeed, 2)));
					maxSpeedPanel.add(maxIntakeSpeedPanel);
					maxSpeedPanel.add(Box.createHorizontalGlue());
					
					c = new GridBagConstraints();
					c.gridx = 0;
					c.gridy = 3;
					c.fill = GridBagConstraints.BOTH;
					c.weightx = 1.0;
					c.weighty = 0.8;
					mainFrame.add(maxSpeedPanel, c);
					
					mainFrame.pack();
					mainFrame.setSize(800, mainFrame.getSize().height);
					fixSize(yawField);
					fixSize(pitchField);
					fixSize(rollField);
					fixSize(leftMotorField);
					fixSize(rightMotorField);
					fixSize(elevatorSpeedField);
					fixSize(intakeSpeedField);
					fixSize(maxDriveSpeedField);
					fixSize(maxElevatorSpeedField);
					fixSize(maxIntakeSpeedField);
					
					loadingStatusLabel.setText("Initializing NetworkTables...");
					loadingStatusLabel.paintImmediately(loadingStatusLabel.getVisibleRect());
					
					//Initialize NT
					ntInstance = NetworkTableInstance.getDefault();
					ntTable = ntInstance.getTable("myobot");
					ntDriveLeft = ntTable.getEntry("leftDrive");
					ntDriveRight = ntTable.getEntry("rightDrive");
					ntElevator = ntTable.getEntry("elevator");
					ntIntake = ntTable.getEntry("intake");
					
					loadingDialog.setVisible(false);
					loadingDialog.dispose();
					mainFrame.setVisible(true);
					
					if(connectToNT) {
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
					}
					
					uiIsSetUp = true;
					connectingDialog.setVisible(true);
				}
				catch(Throwable e) {
					reportFatalError(e);
				}
			}
		};
		uiWorker.execute();
	}
	
	public static void updateUI(boolean onArm, boolean unlocked, EulerOrientation orientation, Pose pose) {
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
		
		if(unlocked != lastUnlocked) {
			if(unlocked) {
				unlockStatusIcon.setImage(imgUnlocked);
				unlockStatusLabel.setToolTipText(TOOLTIP_UNLOCKED);
				lockUnlockButton.setText("Lock");
			}
			else {
				unlockStatusIcon.setImage(imgLocked);
				unlockStatusLabel.setToolTipText(TOOLTIP_LOCKED);
				lockUnlockButton.setText("Unlock");
			}
			
			lastUnlocked = unlocked;
			topBarPanel.repaint();
		}
		
		if(!onArm) {
			return;
		}
		
		if(lastDrivingForwards != drivingForwards) {
			lastDrivingForwards = drivingForwards;
			directionIcon.setImage(drivingForwards ? imgForwardSmall : imgBackwardSmall);
			directionLabel.setToolTipText(drivingForwards ? TOOLTIP_FORWARDS : TOOLTIP_BACKWARDS);
			directionLabel.repaint();
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
			case fist:
				imgPose = imgFist;
				poseName = "Fist";
				break;
			case fingersSpread:
				imgPose = imgSpreadFingers;
				poseName = "Spread Fingers";
				break;
			case waveIn:
				imgPose = imgWaveIn;
				poseName = "Wave In";
				break;
			case waveOut:
				imgPose = imgWaveOut;
				poseName = "Wave Out";
				break;
			case doubleTap:
				imgPose = imgDoubleTap;
				poseName = "Double Tap";
				break;
			case rest:
			case unknown:
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
		
		leftMotorField.setText(String.valueOf(roundToPlaces(leftMotorSpeed, 2)));
		rightMotorField.setText(String.valueOf(roundToPlaces(rightMotorSpeed, 2)));
		leftMotorSpeedometer.updateSpeedNoRepaint(leftMotorSpeed);
		rightMotorSpeedometer.updateSpeedNoRepaint(rightMotorSpeed);
		if(leftMotorSpeed == 0 && rightMotorSpeed == 0) {
			if(driveDirectionIcon.getImage() != imgNoMovement) {
				driveDirectionIcon.setImage(imgNoMovement);
			}
		}
		else if(leftMotorSpeed > 0 && rightMotorSpeed > 0) {
			if(leftMotorSpeed > rightMotorSpeed) {
				if(driveDirectionIcon.getImage() != imgFRight) {
					driveDirectionIcon.setImage(imgFRight);
				}
			}
			else if(leftMotorSpeed < rightMotorSpeed) {
				if(driveDirectionIcon.getImage() != imgFLeft) {
					driveDirectionIcon.setImage(imgFLeft);
				}
			}
			else if(driveDirectionIcon.getImage() != imgForward) {
				driveDirectionIcon.setImage(imgForward);
			}
		}
		else if(leftMotorSpeed < 0 && rightMotorSpeed < 0) {
			if(leftMotorSpeed > rightMotorSpeed) {
				if(driveDirectionIcon.getImage() != imgBLeft) {
					driveDirectionIcon.setImage(imgBLeft);
				}
			}
			else if(leftMotorSpeed < rightMotorSpeed) {
				if(driveDirectionIcon.getImage() != imgBRight) {
					driveDirectionIcon.setImage(imgBRight);
				}
			}
			else if(driveDirectionIcon.getImage() != imgBackward) {
				driveDirectionIcon.setImage(imgBackward);
			}
		}
		else {
			if(leftMotorSpeed > rightMotorSpeed) {
				if(driveDirectionIcon.getImage() != imgTurnCW) {
					driveDirectionIcon.setImage(imgTurnCW);
				}
			}
			else if(driveDirectionIcon.getImage() != imgTurnCCW) {
				driveDirectionIcon.setImage(imgTurnCCW);
			}
		}
		elevatorSpeedField.setText(String.valueOf(roundToPlaces(elevatorSpeed, 2)));
		intakeSpeedField.setText(String.valueOf(roundToPlaces(intakeSpeed, 2)));
		elevatorSpeedometer.updateSpeedNoRepaint(elevatorSpeed);
		intakeSpeedometer.updateSpeedNoRepaint(intakeSpeed);
		speedometerPanel.repaint();
	}
	
	static boolean doubleTapping = false;
	/**
	 * Computes the speeds that are to be sent to the robot based on the Myo's orientation and pose.
	 */
	public static void computeSpeeds(EulerOrientation orientation, Pose pose) {
		if(controlsMode == ControlsMode.V2_1) {
			if(pose != Pose.doubleTap && doubleTapping) {
				doubleTapping = false;
			}
			if(orientation.getPitchDegrees() > -20) {
				elevatorSpeed = 0;
				double drivingSpeed = Math.min(45, orientation.getPitchDegrees() + 20) / 45.0;
				//Square to make small movements easier to control
				drivingSpeed *= drivingSpeed;
				
				if(!drivingForwards) {
					drivingSpeed = -drivingSpeed;
				}
				
				double turningSpeed = 0;
				if(Math.abs(orientation.getYawDegrees()) >= 15) {
					turningSpeed = Math.min(70, Math.abs(orientation.getYawDegrees()) - 15) / 70;
					//Square the turning speed to make small turns easier to control
					turningSpeed *= turningSpeed;
					//Turning left
					if(orientation.getYawDegrees() > 0) {
						turningSpeed = -turningSpeed;
					}
				}
				
				drivingSpeed -= Math.copySign(turningSpeed, drivingSpeed);
				leftMotorSpeed = drivingSpeed * driveMaxSpeed + turningSpeed * driveMaxSpeed;
				rightMotorSpeed = drivingSpeed * driveMaxSpeed - turningSpeed * driveMaxSpeed;
				
				intakeSpeed = 0;
				if(pose == Pose.fist) {
					intakeSpeed = intakeMaxSpeed;
				}
				else if(pose == Pose.fingersSpread) {
					intakeSpeed = -intakeMaxSpeed;
				}
			}
			else if(pose == Pose.fist) {
				intakeSpeed = intakeMaxSpeed;
				elevatorSpeed = 0;
				leftMotorSpeed = rightMotorSpeed = 0;
			}
			else if(pose == Pose.fingersSpread) {
				intakeSpeed = -intakeMaxSpeed;
				elevatorSpeed = 0;
				leftMotorSpeed = rightMotorSpeed = 0;
			}
			else if(pose == Pose.waveOut) {
				elevatorSpeed = -elevatorMaxSpeed;
				intakeSpeed = 0;
				leftMotorSpeed = rightMotorSpeed = 0;
			}
			else if(pose == Pose.waveIn) {
				elevatorSpeed = elevatorMaxSpeed;
				intakeSpeed = 0;
				leftMotorSpeed = rightMotorSpeed = 0;
			}
			else if(pose == Pose.doubleTap && !doubleTapping) {
				drivingForwards = !drivingForwards;
				doubleTapping = true;
				myo.notifyUserAction();
			}
			else {
				elevatorSpeed = intakeSpeed = leftMotorSpeed = rightMotorSpeed = 0;
			}
		}
		else if(controlsMode == ControlsMode.V2_0) {
			if(Math.abs(orientation.getPitchDegrees()) >= 15) {
				double pitch = Math.max(-60, Math.min(60, orientation.getPitchDegrees()));
				pitch -= Math.copySign(15, pitch);
				
				double frac = Math.abs(pitch) / 45;
				
				intakeSpeed = leftMotorSpeed = rightMotorSpeed = 0;
				if(pitch >= 0) {
					elevatorSpeed = frac * -elevatorMaxSpeed;
				}
				else {
					elevatorSpeed = frac * elevatorMaxSpeed;
				}
			}
			else if(pose == Pose.fist || pose == Pose.fingersSpread) {
				
				intakeSpeed = elevatorSpeed = 0;
				if(Math.abs(orientation.getRollDegrees()) >= 10) {
					double turningSpeed = (Math.min(45, Math.abs(orientation.getRollDegrees())) - 10) / 35;
					//Square the turning speed to make small turns easier to control
					turningSpeed *= turningSpeed;
					//Turning left
					if(orientation.getRollDegrees() > 0) {
						turningSpeed = -turningSpeed;
					}
					
					leftMotorSpeed = Math.max(-driveMaxSpeed, Math.min(driveMaxSpeed, (pose == Pose.fist ? driveMaxSpeed : -driveMaxSpeed) + turningSpeed * (pose == Pose.fist ? driveMaxSpeed : -driveMaxSpeed)));
					rightMotorSpeed = Math.max(-driveMaxSpeed, Math.min(driveMaxSpeed, (pose == Pose.fist ? driveMaxSpeed : -driveMaxSpeed) - turningSpeed * (pose == Pose.fist ? driveMaxSpeed : -driveMaxSpeed)));
				}
				else {
					leftMotorSpeed = rightMotorSpeed = pose == Pose.fist ? driveMaxSpeed : -driveMaxSpeed;
				}
			}
			else if(pose == Pose.waveIn) {
				leftMotorSpeed = rightMotorSpeed = elevatorSpeed = 0;
				intakeSpeed = intakeMaxSpeed;
			}
			else if(pose == Pose.waveOut) {
				leftMotorSpeed = rightMotorSpeed = elevatorSpeed = 0;
				intakeSpeed = -intakeMaxSpeed;
			}
			else {
				elevatorSpeed = intakeSpeed = leftMotorSpeed = rightMotorSpeed = 0;
			}
		}
		else if(controlsMode == ControlsMode.V1_0) {
			switch(pose) {
			case fist:
				leftMotorSpeed = rightMotorSpeed = driveMaxSpeed;
				intakeSpeed = elevatorSpeed = 0;
				break;
			case fingersSpread:
				leftMotorSpeed = rightMotorSpeed = -driveMaxSpeed;
				intakeSpeed = elevatorSpeed = 0;
				break;
			case waveIn:
				intakeSpeed = elevatorSpeed = 0;
				if(collector.getArm() == Arm.armRight || collector.getArm() == Arm.armUnknown) {
					leftMotorSpeed = -driveMaxSpeed;
					rightMotorSpeed = driveMaxSpeed;
				}
				else {
					leftMotorSpeed = driveMaxSpeed;
					rightMotorSpeed = -driveMaxSpeed;
				}
				break;
			case waveOut:
				intakeSpeed = elevatorSpeed = 0;
				if(collector.getArm() == Arm.armRight || collector.getArm() == Arm.armUnknown) {
					leftMotorSpeed = driveMaxSpeed;
					rightMotorSpeed = -driveMaxSpeed;
				}
				else {
					leftMotorSpeed = -driveMaxSpeed;
					rightMotorSpeed = driveMaxSpeed;
				}
				break;
			default:
				leftMotorSpeed = rightMotorSpeed = intakeSpeed = elevatorSpeed = 0;
				break;
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
	
	static boolean connectToMyo = true, connectToNT = true;
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		
		//Debug flags
		for(String s : args) {
			switch (s) {
			case "--no-myo":
				connectToMyo = false;
				connectToNT = false;
				break;
			case "--no-nt":
				connectToNT = false;
				break;
			default:
				break;
			}
		}
		
		//Add shutdown hook to make sure Myo is cleaned up
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if(connectToNT) {
					ntDriveLeft.forceSetDouble(0);
					ntDriveRight.forceSetDouble(0);
					ntElevator.forceSetDouble(0);
					ntIntake.forceSetDouble(0);
				}
				if(connectToMyo) {
					if(myoHub != null) {
						if(myo != null)
							myo.lock();
						myoHub.release();
					}
				}
			}
		});
		
		SwingUtilities.invokeLater(() -> {
			try {
				constructAndShowUI();
			}
			catch(Throwable e) {
				reportFatalError(e);
			}
		});
		
		//Wait until UI is set up
		while(!uiIsSetUp) {
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
				reportFatalError(e);
			}
		}
		
		//Initialize Myo
		if(connectToMyo) {
			try {
				myoHub = new Hub("org.usfirst.frc.team6135.MyoBot");
				myo = myoHub.waitForMyo(10000);
				myoHub.addListener(collector);
				myoHub.setLockingPolicy(Hub.LockingPolicy.lockingPolicyNone);
			}
			catch(MyoException e) {
				SwingUtilities.invokeLater(() -> {
					JOptionPane.showMessageDialog(mainFrame, "Cannot connect to Myo!\nThe program will now exit.", "Error", JOptionPane.ERROR_MESSAGE);
					System.exit(0);
				});
			}
		}
		
		//Dispose of the dialog
		connectingDialog.setVisible(false);
		connectingDialog.dispose();
		
		//Add close listener to know when to exit
		mainFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				paused = true;
				
				int ret = JOptionPane.showConfirmDialog(mainFrame, "Are you sure you want to exit the MyoBot Control Center?", "Confirm Exit", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
				if(ret == JOptionPane.YES_OPTION) {
					mainFrame.dispose();
				}
				else {
					paused = false;
				}
			}
			@Override
			public void windowClosed(WindowEvent e) {
				exit = true;
			}
		});
		
		while(!exit) {
			if(myoHub != null)
				myoHub.run(100);
			
			boolean onArm = collector.onArm();
			boolean myoUnlocked = collector.isUnlocked();
			EulerOrientation orientation = invertAngles ? collector.getOrienationEuler().negate() : collector.getOrienationEuler();
			Pose pose = collector.getPose();
			
			if(onArm && myoUnlocked && !paused) {
				computeSpeeds(orientation, pose);
			}
			else {
				leftMotorSpeed = rightMotorSpeed = elevatorSpeed = intakeSpeed = 0;
			}
			updateNT();
			
			if(!paused) {
				try {
					SwingUtilities.invokeAndWait(() -> {				
						updateUI(onArm, myoUnlocked, orientation, pose);
					});
				} 
				catch (InvocationTargetException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

}
