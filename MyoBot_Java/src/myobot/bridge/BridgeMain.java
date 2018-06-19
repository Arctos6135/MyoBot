package myobot.bridge;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

import com.sun.glass.events.KeyEvent;

import javax.swing.UnsupportedLookAndFeelException;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import myobot.bridge.myo.EulerOrientation;
import myobot.bridge.myo.Myo;
import myobot.bridge.myo.MyoException;
import myobot.bridge.ui.AngleVisualizer;

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
	public static final Dimension BUTTON_SIZE = new Dimension(80, 30);
	public static final Dimension VERTICAL_SPACING_SMALL = new Dimension(1, 5);
	public static final Dimension TEXT_FIELD_MAX_SIZE = new Dimension(80, Integer.MAX_VALUE);
	
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
	//Labels for the icons
	static JLabel unlockStatusLabel, onArmStatusLabel, invertStatusLabel;
	static AngleVisualizer yawVisualizer, pitchVisualizer, rollVisualizer;
	static JPanel yawPanel, pitchPanel, rollPanel;
	static JTextField yawField, pitchField, rollField;
	static JPanel angleVisualizerPanel;
	//Last time's status
	//Used to determine whether or not to update the icons
	static boolean lastOnArm = false;
	//The "connecting to myo" dialog
	static JDialog connectingDialog;
	//Different icon images
	static Image iconLocked, iconUnlocked, iconOnArm, iconOffArm, iconNormal, iconInverted;
	
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
	public static Image loadUIImage(String name) throws IOException {
		InputStream stream = BridgeMain.class.getClass().getResourceAsStream("/resources/ui/icons/" + name);
		Image img = ImageIO.read(stream).getScaledInstance(24, 24, Image.SCALE_SMOOTH);
		stream.close();
		return img;
	}
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
	
	public static void constructAndShowUI() throws IOException {
		setupLookAndFeel();
		
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
		
		//Load images
		iconLocked = loadUIImage("locked.png");
		iconUnlocked = loadUIImage("unlocked.png");
		iconOnArm = loadUIImage("on_arm.png");
		iconOffArm = loadUIImage("not_on_arm.png");
		iconInverted = loadUIImage("inverted.png");
		iconNormal = loadUIImage("not_inverted.png");
		
		//Top bar with the icons and buttons
		mainFrame.setLayout(new BoxLayout(mainFrame.getContentPane(), BoxLayout.Y_AXIS));
		topBarPanel = new JPanel();
		//Construct ImageIcons
		unlockStatusIcon = new ImageIcon(iconLocked);
		onArmStatusIcon = new ImageIcon(iconOffArm);
		invertStatusIcon = new ImageIcon(invertAngles ? iconInverted : iconNormal);
		topBarPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 5));
		//Store these lambdas as runnables
		//Reused later
		Runnable lockUnlockMyo = () -> {
			if(myo.isLocked()) {
				myo.unlock();
				lockUnlockButton.setText("Lock");
				unlockStatusIcon.setImage(iconUnlocked);
				unlockStatusLabel.setToolTipText(TOOLTIP_UNLOCKED);
			}
			else {
				myo.lock();
				lockUnlockButton.setText("Unlock");
				unlockStatusIcon.setImage(iconLocked);
				unlockStatusLabel.setToolTipText(TOOLTIP_LOCKED);
			}
			topBarPanel.revalidate();
			topBarPanel.repaint();
		};
		Runnable invertUninvertAngles = () -> {
			invertAngles = !invertAngles;
			invertStatusIcon.setImage(invertAngles ? iconInverted : iconNormal);
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
		
		//The panel with the orientation
		angleVisualizerPanel = new JPanel();
		angleVisualizerPanel.setLayout(new BoxLayout(angleVisualizerPanel, BoxLayout.X_AXIS));
		angleVisualizerPanel.setBorder(BorderFactory.createTitledBorder("Orientation"));
		
		angleVisualizerPanel.add(Box.createHorizontalGlue());
		yawPanel = new JPanel();
		yawPanel.setLayout(new BoxLayout(yawPanel, BoxLayout.Y_AXIS));
		JLabel yawLabel = new JLabel("Yaw");
		yawLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		yawPanel.add(yawLabel);
		yawPanel.add(Box.createRigidArea(VERTICAL_SPACING_SMALL));
		yawVisualizer = new AngleVisualizer(80);
		yawPanel.add(yawVisualizer);
		yawPanel.add(Box.createRigidArea(VERTICAL_SPACING_SMALL));
		yawField = new JTextField();
		yawField.setText("0.0");
		yawField.setHorizontalAlignment(JTextField.CENTER);
		yawField.setEditable(false);
		yawField.setBackground(DISABLED_COLOR);
		yawField.setMaximumSize(TEXT_FIELD_MAX_SIZE);
		yawPanel.add(yawField);
		angleVisualizerPanel.add(yawPanel);
		angleVisualizerPanel.add(Box.createHorizontalGlue());
		
		pitchPanel = new JPanel();
		pitchPanel.setLayout(new BoxLayout(pitchPanel, BoxLayout.Y_AXIS));
		JLabel pitchLabel = new JLabel("Pitch");
		pitchLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		pitchPanel.add(pitchLabel);
		pitchPanel.add(Box.createRigidArea(VERTICAL_SPACING_SMALL));
		pitchVisualizer = new AngleVisualizer(80);
		pitchPanel.add(pitchVisualizer);
		pitchPanel.add(Box.createRigidArea(VERTICAL_SPACING_SMALL));
		pitchField = new JTextField();
		pitchField.setText("0.0");
		pitchField.setHorizontalAlignment(JTextField.CENTER);
		pitchField.setEditable(false);
		pitchField.setBackground(DISABLED_COLOR);
		pitchField.setMaximumSize(TEXT_FIELD_MAX_SIZE);
		pitchPanel.add(pitchField);
		angleVisualizerPanel.add(pitchPanel);
		angleVisualizerPanel.add(Box.createHorizontalGlue());
		
		rollPanel = new JPanel();
		rollPanel.setLayout(new BoxLayout(rollPanel, BoxLayout.Y_AXIS));
		JLabel rollLabel = new JLabel("Roll");
		rollLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		rollPanel.add(rollLabel);
		rollPanel.add(Box.createRigidArea(VERTICAL_SPACING_SMALL));
		rollVisualizer = new AngleVisualizer(80);
		rollPanel.add(rollVisualizer);
		rollPanel.add(Box.createRigidArea(VERTICAL_SPACING_SMALL));
		rollField = new JTextField();
		rollField.setText("0.0");
		rollField.setHorizontalAlignment(JTextField.CENTER);
		rollField.setEditable(false);
		rollField.setBackground(DISABLED_COLOR);
		rollField.setMaximumSize(TEXT_FIELD_MAX_SIZE);
		rollPanel.add(rollField);
		angleVisualizerPanel.add(rollPanel);
		angleVisualizerPanel.add(Box.createHorizontalGlue());
		
		mainFrame.add(angleVisualizerPanel);
		mainFrame.pack();
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
	
	public static void updateUI(boolean onArm, EulerOrientation orientation) {
		//No need to worry about locked/unlocked or inverted/normal
		if(onArm != lastOnArm) {
			if(onArm) {
				onArmStatusIcon.setImage(iconOnArm);
				onArmStatusLabel.setToolTipText(TOOLTIP_ONARM);
			}
			else {
				onArmStatusIcon.setImage(iconOffArm);
				onArmStatusLabel.setToolTipText(TOOLTIP_OFFARM);
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
			SwingUtilities.invokeLater(() -> {				
				updateUI(onArm, invertAngles ? myo.getOrientation().negate() : myo.getOrientation());
			});
		}
	}

}
