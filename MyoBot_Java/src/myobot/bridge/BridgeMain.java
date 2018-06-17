package myobot.bridge;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import myobot.bridge.myo.Myo;

public class BridgeMain {

	static JFrame mainFrame;
	static JDialog connectingToMyoDialog;
	
	public static void constructAndShowUI() {
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
		
		mainFrame = new JFrame("MyoBot Control Center");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		connectingToMyoDialog = new JDialog(mainFrame, "Please Wait...");
		connectingToMyoDialog.setModal(true);
		connectingToMyoDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		connectingToMyoDialog.setLayout(new BorderLayout());
		JLabel l = new JLabel("Connecting to Myo...");
		l.setFont(l.getFont().deriveFont(15.0f));
		l.setHorizontalAlignment(JLabel.CENTER);
		l.setVerticalAlignment(JLabel.CENTER);
		l.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
		connectingToMyoDialog.add(l, BorderLayout.CENTER);
		JProgressBar connectionProgress = new JProgressBar();
		connectionProgress.setPreferredSize(new Dimension(350, 30));
		connectionProgress.setIndeterminate(true);
		JPanel progressBarPanel = new JPanel();
		progressBarPanel.add(connectionProgress);
		progressBarPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 3, 5));
		connectingToMyoDialog.add(progressBarPanel, BorderLayout.PAGE_END);
		connectingToMyoDialog.setUndecorated(true);
		connectingToMyoDialog.pack();
		connectingToMyoDialog.setLocationRelativeTo(null);
		
		mainFrame.setVisible(true);
		connectingToMyoDialog.setVisible(true);
	}
	
	public static void main(String[] args) {
		
		final Myo myo = new Myo();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if(myo.isInitialized()) {
					myo.cleanup();
				}
			}
		});
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				constructAndShowUI();
			}
		});
	}

}
