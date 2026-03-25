package com.steganography.ui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.File;

public class EncodePanel extends JPanel {

    private static final Color BLUE = new Color(0x1172E4);
    private static final Color BLACK = Color.BLACK;
    private static final Color WHITE = Color.WHITE;
    private static final Color GRAY_TAB = new Color(0xDDDDDD);
    private static final Color GRAY_HINT = new Color(0x676767);
    private static final Color RADIO_BORDER = new Color(0xD9D9D9);

    private static final int STEGOKEY_H = 10 + 26 + 10 + 195;

    private JRadioButton radioText, radioFile, radioSeq, radioRandom;
    private JTextArea textMessageField;
    private JButton chooseFileMessage;
    private JLabel stegokeyLabel;
    private JScrollPane stegokeyScroll;
    private JButton encodeBtn;
    private Runnable resizeCallback;

    private int encodeBtnBaseY;
    private final int W = 860;

    public EncodePanel() {
        setLayout(null);
        setBackground(WHITE);
        buildAll();
    }

    public void setResizeCallback(Runnable cb) {
        this.resizeCallback = cb;
    }

    private void buildAll() {
        int y = addTabBar(0, 0, W);
        y += 15;
        y = addEncodeContent(0, y, W);
        setPreferredSize(new Dimension(W, y + 40));
    }

    private int addTabBar(int x, int y, int w) {
        JPanel tabBar = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GRAY_TAB);
                g2.setStroke(new BasicStroke(1));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        tabBar.setBackground(WHITE);
        tabBar.setBounds(x, y, w, 44);

        JPanel encodeTab = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE);
                RoundRectangle2D rr = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight() + 10, 20, 20);
                g2.fill(rr);
                g2.setColor(GRAY_TAB);
                g2.setStroke(new BasicStroke(1));
                g2.draw(rr);
                g2.setColor(WHITE);
                g2.fillRect(0, getHeight() - 2, getWidth(), 4);
                g2.dispose();
            }
        };
        encodeTab.setOpaque(false);
        encodeTab.setBounds(0, 0, 111, 44);
        encodeTab.setLayout(new BorderLayout());
        JLabel encodeLabel = new JLabel("Encode", SwingConstants.CENTER);
        encodeLabel.setFont(new Font("Inter", Font.PLAIN, 20));
        encodeLabel.setForeground(BLACK);
        encodeTab.add(encodeLabel, BorderLayout.CENTER);
        tabBar.add(encodeTab);

        JLabel decodeLabel = new JLabel("Decode", SwingConstants.CENTER);
        decodeLabel.setFont(new Font("Inter", Font.PLAIN, 20));
        decodeLabel.setForeground(BLACK);
        decodeLabel.setBounds(111, 0, 111, 43);
        tabBar.add(decodeLabel);

        add(tabBar);
        return y + 44;
    }

    private int addEncodeContent(int x, int y, int w) {
        y = addLabel("Select an AVI Cover Video to upload", BLACK, Font.PLAIN, 20, x, y, w);
        y += 10;

        JButton chooseCoverBtn = makeBlueButtonRaw("Choose File", 241, 42);
        chooseCoverBtn.setBounds(x, y, 241, 42);
        add(chooseCoverBtn);

        JLabel coverFileLabel = makeHintLabel("", x + 251, y + 11, w - 251);
        add(coverFileLabel);

        chooseCoverBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("AVI Video Files (*.avi)", "avi"));
            fc.setAcceptAllFileFilterUsed(false);
            int result = fc.showOpenDialog(EncodePanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                coverFileLabel.setText(f.getName());
            }
        });

        y += 42 + 30;

        y = addLabel("Enter your message", BLACK, Font.PLAIN, 20, x, y, w);
        y += 10;

        ButtonGroup msgGroup = new ButtonGroup();
        radioText = makeRadioButton("Text", msgGroup);
        radioFile = makeRadioButton("File", msgGroup);
        radioText.setSelected(true);

        radioText.setBounds(x, y, 120, 26);
        add(radioText);
        y += 36;

        textMessageField = new JTextArea();
        JScrollPane textScroll = makeTextField(textMessageField, w, 195);
        textScroll.setBounds(x, y, w, 195);
        add(textScroll);
        y += 205;

        radioFile.setBounds(x, y, 120, 26);
        add(radioFile);
        y += 36;

        chooseFileMessage = makeBlueButtonRaw("Choose File", 241, 42);
        chooseFileMessage.setBounds(x, y, 241, 42);
        chooseFileMessage.setEnabled(false);
        add(chooseFileMessage);

        JLabel msgFileLabel = makeHintLabel("", x + 251, y + 11, w - 251);
        add(msgFileLabel);

        chooseFileMessage.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter(
                    "Supported Files (txt, pdf, docx, png, jpg, exe)",
                    "txt", "pdf", "docx", "png", "jpg", "exe"
            ));
            fc.setAcceptAllFileFilterUsed(true);
            int result = fc.showOpenDialog(EncodePanel.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                msgFileLabel.setText(f.getName());
            }
        });

        y += 52;

        y = addLabel("txt, pdf, docx, png, jpg, and exe formats", GRAY_HINT, Font.PLAIN, 20, x, y, w);
        y += 30;

        radioText.addActionListener(e -> { textMessageField.setEnabled(true); chooseFileMessage.setEnabled(false); repaintRadios(); });
        radioFile.addActionListener(e -> { textMessageField.setEnabled(false); chooseFileMessage.setEnabled(true); repaintRadios(); });

        y = addLabel("Enter password (optional)", BLACK, Font.PLAIN, 20, x, y, w);
        y += 10;
        JScrollPane passwordScroll = makeTextField(new JTextArea(), w, 195);
        passwordScroll.setBounds(x, y, w, 195);
        add(passwordScroll);
        y += 205;

        y = addLabel("To encrypt with A5/1 method", GRAY_HINT, Font.PLAIN, 20, x, y, w);
        y += 30;

        y = addLabel("Choose method", BLACK, Font.PLAIN, 20, x, y, w);
        y += 10;

        ButtonGroup methodGroup = new ButtonGroup();
        radioSeq = makeRadioButton("Sequential", methodGroup);
        radioRandom = makeRadioButton("Random", methodGroup);
        radioSeq.setSelected(true);

        radioSeq.setBounds(x, y, 180, 26);
        add(radioSeq);
        y += 36;

        radioRandom.setBounds(x, y, 160, 26);
        add(radioRandom);
        y += 26;

        int afterRadioY = y;

        stegokeyLabel = new JLabel("Insert stego-key");
        stegokeyLabel.setFont(new Font("Inter", Font.PLAIN, 20));
        stegokeyLabel.setForeground(BLACK);
        stegokeyLabel.setBounds(x, afterRadioY + 10, w, 26);
        stegokeyLabel.setVisible(false);
        add(stegokeyLabel);

        stegokeyScroll = makeTextField(new JTextArea(), w, 195);
        stegokeyScroll.setBounds(x, afterRadioY + 10 + 26 + 10, w, 195);
        stegokeyScroll.setVisible(false);
        add(stegokeyScroll);

        encodeBtnBaseY = afterRadioY + 30;

        encodeBtn = new JButton("Encode") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        encodeBtn.setFont(new Font("Inter", Font.PLAIN, 20));
        encodeBtn.setForeground(WHITE);
        encodeBtn.setContentAreaFilled(false);
        encodeBtn.setBorderPainted(false);
        encodeBtn.setFocusPainted(false);
        encodeBtn.setOpaque(false);
        encodeBtn.setBounds(x, encodeBtnBaseY, w, 42);
        add(encodeBtn);

        radioSeq.addActionListener(e -> {
            stegokeyLabel.setVisible(false);
            stegokeyScroll.setVisible(false);
            encodeBtn.setBounds(x, encodeBtnBaseY, w, 42);
            updatePanelHeight(encodeBtnBaseY + 42);
        });

        radioRandom.addActionListener(e -> {
            stegokeyLabel.setVisible(true);
            stegokeyScroll.setVisible(true);
            int btnY = encodeBtnBaseY + STEGOKEY_H + 30;
            encodeBtn.setBounds(x, btnY, w, 42);
            updatePanelHeight(btnY + 42);
        });

        return encodeBtnBaseY + 42;
    }

    private void updatePanelHeight(int contentBottom) {
        int newH = contentBottom + 40;
        setPreferredSize(new Dimension(W, newH));
        if (getParent() != null) {
            Rectangle b = getBounds();
            setBounds(b.x, b.y, W, newH);
            getParent().revalidate();
            getParent().repaint();
        }
        if (resizeCallback != null) resizeCallback.run();
    }

    private int addLabel(String text, Color color, int style, int size, int x, int y, int w) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Inter", style, size));
        label.setForeground(color);
        label.setBounds(x, y, w, 26);
        add(label);
        return y + 26;
    }

    private JLabel makeHintLabel(String text, int x, int y, int w) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Inter", Font.PLAIN, 16));
        label.setForeground(GRAY_HINT);
        label.setBounds(x, y, w, 20);
        return label;
    }

    private int addBlueButton(String text, int bw, int bh, int x, int y) {
        JButton btn = makeBlueButtonRaw(text, bw, bh);
        btn.setBounds(x, y, bw, bh);
        add(btn);
        return y + bh;
    }

    private JButton makeBlueButtonRaw(String text, int bw, int bh) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() ? BLUE : GRAY_TAB);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Inter", Font.PLAIN, 20));
        btn.setForeground(WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        return btn;
    }

    private JScrollPane makeTextField(JTextArea area, int w, int h) {
        area.setFont(new Font("Inter", Font.PLAIN, 16));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(new AbstractBorder() {
            @Override
            public void paintBorder(Component c, Graphics g, int bx, int by, int bw2, int bh2) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BLUE);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(bx, by, bw2 - 1, bh2 - 1, 20, 20);
                g2.dispose();
            }
            @Override
            public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
        });
        return scroll;
    }

    private JRadioButton makeRadioButton(String text, ButtonGroup group) {
        JRadioButton radio = new JRadioButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int r = 18;
                int ry = (getHeight() - r) / 2;
                if (isSelected()) {
                    g2.setColor(BLUE);
                    g2.fillOval(0, ry, r, r);
                } else {
                    g2.setColor(WHITE);
                    g2.fillOval(0, ry, r, r);
                    g2.setColor(RADIO_BORDER);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawOval(0, ry, r - 1, r - 1);
                }
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.drawString(getText(), r + 8, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        radio.setFont(new Font("Inter", Font.PLAIN, 20));
        radio.setForeground(BLACK);
        radio.setOpaque(false);
        radio.setBorderPainted(false);
        radio.setContentAreaFilled(false);
        radio.setFocusPainted(false);
        group.add(radio);
        return radio;
    }

    private void repaintRadios() {
        if (radioText != null) radioText.repaint();
        if (radioFile != null) radioFile.repaint();
        if (radioSeq != null) radioSeq.repaint();
        if (radioRandom != null) radioRandom.repaint();
    }
}