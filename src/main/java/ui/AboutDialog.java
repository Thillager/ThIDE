package ui;

import update.UpdateManager;
import config.LanguageManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AboutDialog {

	private final JFrame parent;
	private final String appVersion;
	private final String githubRepo;
	private final UpdateManager updateManager;

	public AboutDialog(JFrame parent, String appVersion, String githubRepo, UpdateManager updateManager) {
		this.parent        = parent;
		this.appVersion    = appVersion;
		this.githubRepo    = githubRepo;
		this.updateManager = updateManager;
	}

	public void show() {
		JDialog dialog = new JDialog(parent, LanguageManager.t("AboutDialog.header"), true);
		dialog.setSize(420, 280);
		dialog.setLocationRelativeTo(parent);
		dialog.setLayout(new BorderLayout(10, 10));

		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
		infoPanel.setBorder(new EmptyBorder(20, 30, 10, 30));
		infoPanel.setBackground(new Color(43, 45, 48));

		JLabel titleLabel = new JLabel(LanguageManager.t("AboutDialog.headline"));
		titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel versionLabel = new JLabel("Version " + appVersion);
		versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		versionLabel.setForeground(new Color(180, 180, 180));
		versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel repoLabel = new JLabel("github.com/" + githubRepo);
		repoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		repoLabel.setForeground(new Color(100, 150, 255));
		repoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel descLabel = new JLabel(LanguageManager.t("AboutDialog.slogan"));
		descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		descLabel.setForeground(new Color(160, 160, 160));
		descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		infoPanel.add(titleLabel);
		infoPanel.add(Box.createVerticalStrut(5));
		infoPanel.add(versionLabel);
		infoPanel.add(Box.createVerticalStrut(5));
		infoPanel.add(repoLabel);
		infoPanel.add(Box.createVerticalStrut(15));
		infoPanel.add(descLabel);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
		btnPanel.setBackground(new Color(43, 45, 48));

		JButton btnUpdate = new JButton(LanguageManager.t("searchUpdates"));
		JButton btnClose  = new JButton(LanguageManager.t("close"));

		btnUpdate.setForeground(new Color(80, 200, 120));
		btnUpdate.addActionListener(e -> {
				dialog.dispose();
				updateManager.checkForUpdates();
			});
		btnClose.addActionListener(e -> dialog.dispose());

		btnPanel.add(btnUpdate);
		btnPanel.add(btnClose);

		dialog.add(infoPanel,  BorderLayout.CENTER);
		dialog.add(btnPanel,   BorderLayout.SOUTH);
		dialog.getContentPane().setBackground(new Color(43, 45, 48));
		dialog.setVisible(true);
	}
}
