package myobot.bridge;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import myobot.bridge.myo.Myo;
import myobot.bridge.myo.MyoException;

public class BridgeMain {
	//If true then Euler angles will be inverted
	//This is for when the Myo is worn upside down
	static boolean invertAngles = false;
	
	static final Myo myo = new Myo();

	//Main JFrame
	static JFrame mainFrame;
	//Panel that stores the top status icons and buttons
	static JPanel iconsAndButtonsPanel;
	//Buttons in the top bar
	static JButton lockUnlockButton;
	static JButton invertButton;
	//Different icons
	static ImageIcon unlockStatusIcon, onArmStatusIcon, invertedStatusIcon;
	//Last time's status
	//Used to determine whether or not to update the icons
	static boolean lastUnlocked = false, lastOnArm = false, lastInverted = invertAngles;
	//The "connecting to myo" dialog
	static JDialog connectingToMyoDialog;
	//Different icon images
	static Image iconLocked, iconUnlocked, iconOnArm, iconOffArm, iconNormal, iconInverted;
	
	//Flag that will be set to true once the UI is up
	//Used to make sure the main thread does not run ahead of the EDT
	static boolean uiIsSetUp = false;
	
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
		UIManager.getLookAndFeelDefaults().put("background", new Color(230, 230, 230));
	}
	
	public static void constructAndShowUI() throws IOException {
		setupLookAndFeel();
		
		mainFrame = new JFrame("MyoBot Control Center");
		mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		//"Connecting to Myo" Dialog
		connectingToMyoDialog = new JDialog(mainFrame, "Please Wait...");
		connectingToMyoDialog.setModal(true);
		connectingToMyoDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		connectingToMyoDialog.setLayout(new BorderLayout());
		JLabel l = new JLabel("Connecting to Myo...");
		l.setFont(l.getFont().deriveFont(18.0f));
		l.setHorizontalAlignment(JLabel.CENTER);
		l.setVerticalAlignment(JLabel.CENTER);
		l.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
		connectingToMyoDialog.add(l, BorderLayout.CENTER);
		JProgressBar connectionProgress = new JProgressBar();
		connectionProgress.setPreferredSize(new Dimension(350, 30));
		connectionProgress.setIndeterminate(true);
		JPanel progressBarPanel = new JPanel();
		progressBarPanel.add(connectionProgress);
		progressBarPanel.setBorder(BorderFactory.createEmptyBorder(0, 25, 20, 25));
		connectingToMyoDialog.add(progressBarPanel, BorderLayout.PAGE_END);
		connectingToMyoDialog.setUndecorated(true);
		connectingToMyoDialog.pack();
		connectingToMyoDialog.setLocationRelativeTo(null);
		
		//Load images
		mainFrame.setSize(new Dimension(500, 500));
		iconLocked = loadUIImage("locked.png");
		iconUnlocked = loadUIImage("unlocked.png");
		iconOnArm = loadUIImage("on_arm.png");
		iconOffArm = loadUIImage("not_on_arm.png");
		iconInverted = loadUIImage("inverted.png");
		iconNormal = loadUIImage("not_inverted.png");
		
		//Top bar with the icons and buttons
		mainFrame.setLayout(new BoxLayout(mainFrame.getContentPane(), BoxLayout.Y_AXIS));
		iconsAndButtonsPanel = new JPanel();
		unlockStatusIcon = new ImageIcon(iconLocked);
		onArmStatusIcon = new ImageIcon(iconOffArm);
		invertedStatusIcon = new ImageIcon(invertAngles ? iconInverted : iconNormal);
		iconsAndButtonsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 5));
		iconsAndButtonsPanel.add(new JLabel(unlockStatusIcon));
		iconsAndButtonsPanel.add(new JLabel(onArmStatusIcon));
		iconsAndButtonsPanel.add(new JLabel(invertedStatusIcon));
		JPanel topButtonsPanel = new JPanel();
		lockUnlockButton = new JButton("Unlock");
		lockUnlockButton.setPreferredSize(new Dimension(80, 30));
		lockUnlockButton.addActionListener(e -> {
			if(myo.isLocked()) {
				myo.unlock();
				lockUnlockButton.setText("Lock");
				unlockStatusIcon.setImage(iconUnlocked);
			}
			else {
				myo.lock();
				lockUnlockButton.setText("Unlock");
				unlockStatusIcon.setImage(iconLocked);
			}
			iconsAndButtonsPanel.revalidate();
			iconsAndButtonsPanel.repaint();
		});
		invertButton = new JButton("Invert");
		invertButton.setPreferredSize(new Dimension(80, 30));
		topButtonsPanel.add(lockUnlockButton);
		topButtonsPanel.add(invertButton);
		iconsAndButtonsPanel.add(topButtonsPanel);
		
		mainFrame.add(iconsAndButtonsPanel);
		
		mainFrame.setVisible(true);
		
		JPanel promptPanel = new JPanel();
		promptPanel.setLayout(new BorderLayout());
		promptPanel.add(new JLabel("Enter FRC Team Number, or 0 for Dry-Run:"), BorderLayout.CENTER);
		JTextArea teamNumber = new JTextArea();
		promptPanel.add(teamNumber, BorderLayout.PAGE_END);
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
		connectingToMyoDialog.setVisible(true);
	}
	
	public static void updateUI() {
		
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
		connectingToMyoDialog.setVisible(false);
		connectingToMyoDialog.dispose();
		
		myo.startHubThread(100);
	}

}
