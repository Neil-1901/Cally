package gui;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Helper class for custom-styled Swing components, now with the new aesthetic.
 */
public class ModernComponents {

    // New Color Palette (derived from the provided image)
    public static final Color ACCENT_BLUE = new Color(0xC7DAE2); // Light blue
    public static final Color ACCENT_PEACH = new Color(0xF7E2D8); // Light peach/salmon
    public static final Color BASE_BEIGE = new Color(0xFBF2E0); // Main background (lighter cream)
    public static final Color DARK_BROWN_TEXT = new Color(0x605041); // For text and outlines
    public static final Color LIGHT_GRAY_BG = new Color(0xF0F0F0); // For subtle backgrounds/highlights
    public static final Color WHITE = Color.WHITE;

    /**
     * A custom-painted button with rounded corners, matching the new palette.
     */
    public static class ProButton extends JButton {
        private Color primaryColor;
        private Color secondaryColor; // Darker shade for pressed/hover

        public ProButton(String text, Color primary, Color secondary) {
            super(text);
            this.primaryColor = primary;
            this.secondaryColor = secondary;
            setForeground(DARK_BROWN_TEXT);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setFont(new Font("Serif", Font.BOLD, 14)); // Changed font for elegance
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color c;
            if (getModel().isPressed()) {
                c = secondaryColor;
            } else if (getModel().isRollover()) {
                c = primaryColor.darker(); // Slightly darker for rollover
            } else {
                c = primaryColor;
            }
            
            g2.setColor(c);
            int arc = 12; // Rounded corners
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            
            // Draw a subtle border for definition
            g2.setColor(DARK_BROWN_TEXT.brighter());
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

            // Let the default painter draw the text
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    /**
     * A modern, minimal scrollbar UI, updated with the new color palette.
     */
    public static class ModernScrollBarUI extends BasicScrollBarUI {
        private final Color thumbColor = DARK_BROWN_TEXT.brighter().brighter(); // Softer thumb color
        private final Color trackColor = new Color(255, 255, 255, 0); // Transparent track

        @Override
        protected void configureScrollBarColors() {
            this.thumbColor.getClass(); // NOP to satisfy sonar
            this.trackColor.getClass(); // NOP to satisfy sonar
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            Dimension zeroDim = new Dimension(0, 0);
            button.setPreferredSize(zeroDim);
            button.setMinimumSize(zeroDim);
            button.setMaximumSize(zeroDim);
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(trackColor);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                             thumbBounds.width - 4, thumbBounds.height - 4, 
                             8, 8);
            g2.dispose();
        }
    }

    /**
     * A custom border that draws scalloped edges.
     */
    public static class ScallopedBorder extends AbstractBorder {
        private final Color borderColor;
        private final int scallopSize;
        private final int thickness;

        public ScallopedBorder(Color borderColor, int scallopSize, int thickness) {
            this.borderColor = borderColor;
            this.scallopSize = scallopSize;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(thickness));

            // Draw outer rectangle for clean edge
            g2.drawRect(x, y, width - 1, height - 1);

            // Draw scalloped edges
            drawScallops(g2, x, y, width, height);

            g2.dispose();
        }
        
        // Helper method to draw scallops around the rectangle
        private void drawScallops(Graphics2D g2, int x, int y, int width, int height) {
            Path2D path = new Path2D.Float();

            // Top edge
            path.moveTo(x, y);
            for (int i = 0; i < width; i += scallopSize) {
                path.quadTo(x + i + scallopSize / 2, y + scallopSize, x + i + scallopSize, y);
            }
            
            // Right edge
            path.moveTo(x + width, y);
            for (int i = 0; i < height; i += scallopSize) {
                path.quadTo(x + width - scallopSize, y + i + scallopSize / 2, x + width, y + i + scallopSize);
            }

            // Bottom edge
            path.moveTo(x + width, y + height);
            for (int i = 0; i < width; i += scallopSize) {
                path.quadTo(x + width - i - scallopSize / 2, y + height - scallopSize, x + width - i - scallopSize, y + height);
            }

            // Left edge
            path.moveTo(x, y + height);
            for (int i = 0; i < height; i += scallopSize) {
                path.quadTo(x + scallopSize, y + height - i - scallopSize / 2, x, y + height - i - scallopSize);
            }

            g2.draw(path);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(scallopSize + thickness, scallopSize + thickness, scallopSize + thickness, scallopSize + thickness);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = scallopSize + thickness;
            return insets;
        }
    }
}