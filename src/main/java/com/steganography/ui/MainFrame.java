package com.steganography.ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("LSB Encoder & Decoder");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(buildContent());
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        root.add(scrollPane, BorderLayout.CENTER);

        setContentPane(root);
        setVisible(true);
    }

    private JPanel buildContent() {
        JPanel content = new JPanel();
        content.setBackground(Color.WHITE);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setPreferredSize(new Dimension(1000, 1536));
        content.setBorder(BorderFactory.createEmptyBorder(40, 70, 40, 70));

        JLabel title = new JLabel("LSB Encoder & Decoder", SwingConstants.CENTER);
        title.setFont(loadInterFont(Font.BOLD, 40));
        title.setForeground(new Color(0x1172E4));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setMaximumSize(new Dimension(860, 50));
        content.add(title);

        content.add(Box.createVerticalStrut(30));

        JLabel subtitle = new JLabel("Embed and Extract Hidden Data from Least Significant Bits for an AVI Video File", SwingConstants.CENTER);
        subtitle.setFont(loadInterFont(Font.PLAIN, 20));
        subtitle.setForeground(Color.BLACK);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setMaximumSize(new Dimension(860, 30));
        content.add(subtitle);

        content.add(Box.createVerticalStrut(30));

        EncodePanel encodePanel = new EncodePanel();
        encodePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(encodePanel);

        return content;
    }

    private Font loadInterFont(int style, int size) {
        return new Font("Inter", style, size);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::new);
    }
}