package gui;

import gui.ModernComponents.ProButton;
import gui.ModernComponents.ModernScrollBarUI;
import gui.ModernComponents.ScallopedBorder; // Import the new border
import model.Appointment;
import model.Deadline;
import model.Event;
import storage.CalendarStorage;
import storage.EventConflictException;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MainFrame extends JFrame {

    // --- Aesthetics (UPDATED TO MATCH IMAGE VIBE) ---
    private static final Color COLOR_BG_LIGHT = ModernComponents.BASE_BEIGE; // Overall background
    private static final Color COLOR_ACCENT_BLUE = ModernComponents.ACCENT_BLUE; // For buttons, highlights
    private static final Color COLOR_ACCENT_PEACH = ModernComponents.ACCENT_PEACH; // For secondary highlights, current date
    private static final Color COLOR_OUTLINE = ModernComponents.DARK_BROWN_TEXT; // Darker brown for outlines and text
    private static final Color COLOR_LIGHT_TEXT = ModernComponents.DARK_BROWN_TEXT.brighter(); // Lighter brown for subtle text
    private static final Color COLOR_CELL_BG = ModernComponents.WHITE; // Default cell background
    private static final Font FONT_HEADER = new Font("Serif", Font.BOLD, 28); // More elegant font
    private static final Font FONT_SUBHEADER = new Font("Serif", Font.BOLD, 18); // Subheaders
    private static final Font FONT_BODY_BOLD = new Font("SansSerif", Font.BOLD, 14); // General bold text
    private static final Font FONT_BODY_REG = new Font("SansSerif", Font.PLAIN, 12); // General regular text
    private static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11); // Small text for events
    
    // Borders
    private static final Border THIN_OUTLINE_BORDER = BorderFactory.createLineBorder(COLOR_OUTLINE.brighter(), 1); // Softer outline
    private static final Border CELL_PADDING_BORDER = new EmptyBorder(8, 8, 8, 8); // More padding
    private static final Border DAY_CELL_BORDER = new CompoundBorder(
        new ScallopedBorder(COLOR_OUTLINE.brighter(), 6, 1), // Scalloped edge
        new EmptyBorder(5, 5, 5, 5) // Inner padding
    );

    // --- State ---
    private final CalendarStorage storage;
    private LocalDate selectedDate;
    private YearMonth currentMiniCalMonth;
    private String currentView = "Month";

    // --- Components ---
    private JPanel viewContainer;
    private MiniCalendarPanel miniCalendarPanel;
    private JLabel currentViewLabel;

    public MainFrame() {
        this.storage = new CalendarStorage();
        this.selectedDate = LocalDate.now();
        this.currentMiniCalMonth = YearMonth.from(selectedDate);

        setTitle("My Planner");
        setSize(1200, 900);
        setMinimumSize(new Dimension(1000, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);

        JPanel mainPanel = createMainPanel();
        add(mainPanel, BorderLayout.CENTER);

        startReminderService();
        updateView();
    }

    private void startReminderService() {
        storage.startReminderService(event -> {
            // Ensure this runs on the Event Dispatch Thread
            SwingUtilities.invokeLater(() -> {
                String time = event.getStartTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                JOptionPane.showMessageDialog(this,
                        "Reminder: '" + event.getTitle() + "' at " + time,
                        "Event Reminder",
                        JOptionPane.INFORMATION_MESSAGE);
            });
        });
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 20));
        sidebar.setBackground(COLOR_BG_LIGHT);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_OUTLINE), // Right border
                new EmptyBorder(20, 15, 20, 15) // More padding
        ));

        JLabel headerLabel = new JLabel("My Planner", SwingConstants.CENTER);
        headerLabel.setFont(FONT_HEADER);
        headerLabel.setForeground(COLOR_OUTLINE);
        headerLabel.setBorder(new EmptyBorder(10, 0, 20, 0));
        sidebar.add(headerLabel, BorderLayout.NORTH);

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0); // Reduced horizontal insets
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;

        ProButton createEventBtn = new ProButton("Create Event", COLOR_ACCENT_BLUE.brighter(), COLOR_ACCENT_BLUE.darker());
        createEventBtn.addActionListener(e -> showEventDialog(null));
        controls.add(createEventBtn, gbc);

        gbc.gridy = 1;
        gbc.weighty = 1; // Make mini-calendar take remaining space
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.BOTH; // Fill both for mini-calendar
        miniCalendarPanel = new MiniCalendarPanel();
        controls.add(miniCalendarPanel, gbc);

        sidebar.add(controls, BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(COLOR_CELL_BG); // Changed to white/light for main content

        // --- Top Navigation Bar ---
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(COLOR_CELL_BG); // White background
        topBar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_OUTLINE.brighter()), // Softer bottom border
                new EmptyBorder(15, 20, 15, 20) // More padding
        ));

        // Prev/Today/Next controls
        JPanel navControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); // Spacing
        navControls.setOpaque(false);
        JButton prevButton = new JButton("<");
        JButton todayButton = new JButton("Today");
        JButton nextButton = new JButton(">");
        
        // Style nav buttons
        styleNavButton(prevButton);
        styleNavButton(todayButton);
        styleNavButton(nextButton);

        prevButton.addActionListener(e -> navigate(-1));
        todayButton.addActionListener(e -> navigate(0));
        nextButton.addActionListener(e -> navigate(1));
        navControls.add(prevButton);
        navControls.add(todayButton);
        navControls.add(nextButton);

        currentViewLabel = new JLabel();
        currentViewLabel.setFont(FONT_SUBHEADER);
        currentViewLabel.setForeground(COLOR_OUTLINE);
        navControls.add(currentViewLabel);
        
        topBar.add(navControls, BorderLayout.WEST);

        // View selection controls
        JPanel viewControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        viewControls.setOpaque(false);
        JToggleButton dayBtn = new JToggleButton("Day");
        JToggleButton weekBtn = new JToggleButton("Week");
        JToggleButton monthBtn = new JToggleButton("Month");
        JToggleButton yearBtn = new JToggleButton("Year");
        
        // Style toggle buttons
        styleToggleButton(dayBtn);
        styleToggleButton(weekBtn);
        styleToggleButton(monthBtn);
        styleToggleButton(yearBtn);

        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(dayBtn);
        viewGroup.add(weekBtn);
        viewGroup.add(monthBtn);
        viewGroup.add(yearBtn);

        monthBtn.setSelected(true); // Default view

        dayBtn.addActionListener(e -> setView("Day"));
        weekBtn.addActionListener(e -> setView("Week"));
        monthBtn.addActionListener(e -> setView("Month"));
        yearBtn.addActionListener(e -> setView("Year"));

        viewControls.add(dayBtn);
        viewControls.add(weekBtn);
        viewControls.add(monthBtn);
        viewControls.add(yearBtn);
        
        topBar.add(viewControls, BorderLayout.EAST);
        mainPanel.add(topBar, BorderLayout.NORTH);

        // --- View Container ---
        viewContainer = new JPanel(new BorderLayout());
        viewContainer.setBackground(COLOR_CELL_BG);
        mainPanel.add(viewContainer, BorderLayout.CENTER);

        return mainPanel;
    }

    private void styleNavButton(JButton button) {
        button.setFont(FONT_BODY_REG);
        button.setForeground(COLOR_OUTLINE);
        button.setBackground(COLOR_LIGHT_TEXT.brighter()); // A very light background
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(COLOR_OUTLINE.brighter(), 1)); // Subtle border
        button.setPreferredSize(new Dimension(button.getPreferredSize().width + 10, 30)); // Consistent height
        button.setOpaque(true); 
    }

    private void styleToggleButton(JToggleButton button) {
        button.setFont(FONT_BODY_REG);
        button.setForeground(COLOR_OUTLINE);
        button.setBackground(COLOR_ACCENT_BLUE.brighter());
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(COLOR_OUTLINE.brighter(), 1));
        button.setPreferredSize(new Dimension(button.getPreferredSize().width + 15, 30));
        button.setOpaque(true); 

        // Make selected state super obvious
        button.getModel().addChangeListener(e -> {
            ButtonModel model = (ButtonModel) e.getSource();
            if (model.isSelected()) {
                button.setBackground(COLOR_OUTLINE); // Dark brown
                button.setForeground(Color.WHITE); // White text
            } else {
                button.setBackground(COLOR_ACCENT_BLUE.brighter()); // Lighter blue
                button.setForeground(COLOR_OUTLINE); // Dark text
            }
        });
    }


    private void setView(String view) {
        this.currentView = view;
        updateView();
    }

    private void navigate(int direction) {
        if (direction == 0) { // Today
            selectedDate = LocalDate.now();
        } else {
            switch (currentView) {
                case "Day":
                    selectedDate = selectedDate.plusDays(direction);
                    break;
                case "Week":
                    selectedDate = selectedDate.plusWeeks(direction);
                    break;
                case "Month":
                    selectedDate = selectedDate.plusMonths(direction);
                    break;
                case "Year":
                    selectedDate = selectedDate.plusYears(direction);
                    break;
            }
        }
        currentMiniCalMonth = YearMonth.from(selectedDate);
        updateView();
    }

    private void updateView() {
        // Update main view label
        DateTimeFormatter formatter;
        switch (currentView) {
            case "Day":
                formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
                currentViewLabel.setText(selectedDate.format(formatter));
                break;
            case "Week":
                LocalDate startOfWeek = selectedDate.with(DayOfWeek.SUNDAY);
                LocalDate endOfWeek = startOfWeek.plusDays(6);
                formatter = DateTimeFormatter.ofPattern("MMM d");
                currentViewLabel.setText(startOfWeek.format(formatter) + " - " + endOfWeek.format(formatter) + ", " + startOfWeek.getYear());
                break;
            case "Month":
                formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
                currentViewLabel.setText(selectedDate.format(formatter));
                break;
            case "Year":
                formatter = DateTimeFormatter.ofPattern("yyyy");
                currentViewLabel.setText(selectedDate.format(formatter));
                break;
        }

        // Update main view content
        viewContainer.removeAll();
        JPanel newViewPanel = switch (currentView) {
            case "Day" -> new DayViewPanel();
            case "Week" -> new WeekViewPanel();
            case "Month" -> new MonthViewPanel();
            case "Year" -> new YearViewPanel();
            default -> new JPanel();
        };

        // Day and Week views are scrollable
        if (currentView.equals("Day") || currentView.equals("Week")) {
            JScrollPane scrollPane = new JScrollPane(newViewPanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            viewContainer.add(scrollPane, BorderLayout.CENTER);
        } else {
            viewContainer.add(newViewPanel, BorderLayout.CENTER);
        }
        
        viewContainer.revalidate();
        viewContainer.repaint();

        // Update sidebar
        miniCalendarPanel.buildGrid();
    }

    private void showEventDialog(Event eventToEdit) {
        EventDialog dialog = new EventDialog(this, eventToEdit);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            updateView();
        }
    }

    // --- Inner Class: MiniCalendarPanel (Sidebar) ---
    private class MiniCalendarPanel extends JPanel {
        private final JLabel monthLabel;
        private final JPanel daysGrid;

        MiniCalendarPanel() {
            setOpaque(false);
            setLayout(new BorderLayout(5, 5));
            // Apply scalloped border
            setBorder(new ScallopedBorder(COLOR_OUTLINE, 8, 1)); 

            // Header: < Month YYYY >
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            JButton prev = new JButton("<");
            JButton next = new JButton(">");
            
            // Style nav buttons
            styleMiniCalNavButton(prev);
            styleMiniCalNavButton(next);

            prev.addActionListener(e -> updateMonth(-1));
            next.addActionListener(e -> updateMonth(1));
            monthLabel = new JLabel("", SwingConstants.CENTER);
            monthLabel.setFont(FONT_BODY_BOLD);
            monthLabel.setForeground(COLOR_OUTLINE);
            header.add(prev, BorderLayout.WEST);
            header.add(monthLabel, BorderLayout.CENTER);
            header.add(next, BorderLayout.EAST);

            // Days Grid
            daysGrid = new JPanel(new GridLayout(0, 7));
            daysGrid.setOpaque(false);
            
            // Day of Week Headers
            String[] dayNames = {"S", "M", "T", "W", "T", "F", "S"};
            for (String day : dayNames) {
                JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
                dayLabel.setFont(FONT_BODY_REG);
                dayLabel.setForeground(COLOR_LIGHT_TEXT); // Softer color for day names
                daysGrid.add(dayLabel);
            }

            add(header, BorderLayout.NORTH);
            add(daysGrid, BorderLayout.CENTER);
            
            buildGrid();
        }

        private void styleMiniCalNavButton(JButton button) {
            button.setFont(FONT_BODY_REG.deriveFont(Font.BOLD));
            button.setForeground(COLOR_OUTLINE);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setContentAreaFilled(false); // Make transparent
            button.setOpaque(false);
            button.setPreferredSize(new Dimension(25, 25)); // Smaller size
        }

        void updateMonth(int delta) {
            currentMiniCalMonth = currentMiniCalMonth.plusMonths(delta);
            buildGrid();
        }

        void buildGrid() {
            monthLabel.setText(currentMiniCalMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")));

            // Remove old day cells (skip day-of-week headers)
            while (daysGrid.getComponentCount() > 7) {
                daysGrid.remove(7);
            }

            LocalDate firstDayOfMonth = currentMiniCalMonth.atDay(1);
            LocalDate firstDayOfGrid = firstDayOfMonth.with(DayOfWeek.SUNDAY);

            LocalDate d = firstDayOfGrid;
            for (int i = 0; i < 42; i++) { // 6 rows
                JLabel dayLabel = new JLabel(String.valueOf(d.getDayOfMonth()), SwingConstants.CENTER);
                dayLabel.setFont(FONT_BODY_REG);
                dayLabel.setOpaque(true);
                dayLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

                if (d.getMonth().equals(currentMiniCalMonth)) {
                    dayLabel.setForeground(COLOR_OUTLINE);
                } else {
                    dayLabel.setForeground(COLOR_LIGHT_TEXT); // Other months
                }

                // Make 'Today' and 'Selected' distinct
                if (d.equals(selectedDate)) {
                    dayLabel.setBackground(COLOR_ACCENT_PEACH); // Selected day is peach
                    dayLabel.setForeground(COLOR_OUTLINE);
                } else if (d.equals(LocalDate.now())) {
                    dayLabel.setBackground(COLOR_ACCENT_BLUE); // Today is blue
                    dayLabel.setForeground(Color.WHITE);
                } else {
                    dayLabel.setBackground(COLOR_CELL_BG); // Default
                }

                final LocalDate clickedDate = d;
                dayLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        selectedDate = clickedDate;
                        if (!clickedDate.getMonth().equals(currentMiniCalMonth)) {
                            currentMiniCalMonth = YearMonth.from(clickedDate);
                        }
                        updateView(); // This will trigger repaint of main frame and this panel
                    }
                });
                
                daysGrid.add(dayLabel);
                d = d.plusDays(1);
            }
            revalidate();
            repaint();
        }
    }

    // --- Inner Class: MonthViewPanel ---
    private class MonthViewPanel extends JPanel {
        MonthViewPanel() {
            setLayout(new GridLayout(0, 7)); // 7 columns, flexible rows
            setBackground(COLOR_CELL_BG);
            setBorder(new EmptyBorder(10, 10, 10, 10)); // More padding

            // 1. Day of Week Headers
            String[] dayNames = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
            for (int i = 0; i < dayNames.length; i++) {
                String day = dayNames[i];
                JLabel header = new JLabel(day, SwingConstants.CENTER);
                header.setFont(FONT_BODY_BOLD);
                header.setForeground(COLOR_OUTLINE);
                // Add vertical separators to match week view
                header.setBorder(new CompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, (i == 6 ? 0 : 1), COLOR_OUTLINE.brighter()), // Bottom and Right (except last)
                    CELL_PADDING_BORDER
                ));
                add(header);
            }

            // 2. Day Cells
            LocalDate firstDayOfMonth = selectedDate.withDayOfMonth(1);
            LocalDate firstDayOfGrid = firstDayOfMonth.with(DayOfWeek.SUNDAY);
            LocalDate lastDayOfMonth = selectedDate.with(TemporalAdjusters.lastDayOfMonth());

            LocalDate d = firstDayOfGrid;
            for (int i = 0; i < 42; i++) { // Max 6 weeks * 7 days
                JPanel cell = new JPanel(new BorderLayout());
                cell.setBorder(DAY_CELL_BORDER); // Scalloped border
                
                if (d.getMonth().equals(selectedDate.getMonth())) {
                    cell.setBackground(COLOR_CELL_BG);
                } else {
                    cell.setBackground(COLOR_LIGHT_TEXT.brighter().brighter()); // Very light gray for other months
                }

                // Day number
                JLabel dayNum = new JLabel(String.valueOf(d.getDayOfMonth()));
                dayNum.setFont(FONT_BODY_REG);
                dayNum.setForeground(COLOR_OUTLINE);
                dayNum.setBorder(new EmptyBorder(5, 8, 5, 0)); // More padding
                if (d.equals(LocalDate.now())) {
                    dayNum.setFont(FONT_BODY_BOLD);
                    dayNum.setForeground(COLOR_ACCENT_BLUE.darker()); // Use blue for 'Today'
                }
                cell.add(dayNum, BorderLayout.NORTH);

                // Events
                List<Event> events = storage.getEventsForDay(d);
                if (!events.isEmpty()) {
                    JPanel eventPanel = new JPanel();
                    eventPanel.setLayout(new BoxLayout(eventPanel, BoxLayout.Y_AXIS));
                    eventPanel.setOpaque(false);
                    eventPanel.setBorder(new EmptyBorder(0, 8, 5, 5)); // Padding for events
                    for (Event event : events) {
                        JLabel eventLabel = new JLabel("• " + event.getTitle());
                        eventLabel.setFont(FONT_SMALL); // Uses new 11pt font
                        eventLabel.setForeground(COLOR_OUTLINE);
                        eventPanel.add(eventLabel);
                    }
                    cell.add(eventPanel, BorderLayout.CENTER);
                }
                
                final LocalDate clickedDate = d;
                cell.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        selectedDate = clickedDate;
                        if (e.getClickCount() == 2) {
                            setView("Day"); // Double-click to go to day view
                        } else {
                            updateView(); // Single-click to select
                        }
                    }
                });

                add(cell);
                d = d.plusDays(1);
                if (i > 28 && d.getDayOfWeek() == DayOfWeek.SUNDAY && d.isAfter(lastDayOfMonth)) {
                    break; // Don't draw trailing empty weeks (if any)
                }
            }
        }
    }

    // --- Inner Class: WeekViewPanel ---
    private class WeekViewPanel extends JPanel {
        private final JPanel timeColumn;
        private final WeekGridPanel gridPanel;

        WeekViewPanel() {
            setLayout(new BorderLayout());
            setBackground(COLOR_CELL_BG);

            // 1. Time Column
            timeColumn = new JPanel(new GridLayout(24, 1));
            timeColumn.setBackground(COLOR_CELL_BG);
            timeColumn.setPreferredSize(new Dimension(60, 24 * 60)); // 60px height per hour
            for (int h = 0; h < 24; h++) {
                JLabel timeLabel = new JLabel(String.format("%02d:00", h), SwingConstants.CENTER);
                timeLabel.setFont(FONT_BODY_REG);
                timeLabel.setForeground(COLOR_OUTLINE);
                timeLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, COLOR_OUTLINE.brighter())); // Softer borders
                timeLabel.setPreferredSize(new Dimension(60, 60)); // Explicit height
                timeColumn.add(timeLabel);
            }
            
            // 2. Day Grid
            gridPanel = new WeekGridPanel();

            // We put the time column in the main panel...
            JPanel headerSpacer = new JPanel(); // Spacer for top-left corner
            headerSpacer.setPreferredSize(new Dimension(60, 50)); // Match time column width and day header height
            headerSpacer.setBackground(COLOR_CELL_BG);
            headerSpacer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, COLOR_OUTLINE.brighter())); // Softer border
            
            // ...and the day headers in the grid panel, handled by paintComponent
            JPanel mainContent = new JPanel(new BorderLayout());
            mainContent.add(timeColumn, BorderLayout.WEST);
            mainContent.add(gridPanel, BorderLayout.CENTER);

            // Day Header Panel
            JPanel dayHeaderPanel = new JPanel(new GridLayout(1, 7));
            dayHeaderPanel.setPreferredSize(new Dimension(0, 50));
            LocalDate startOfWeek = selectedDate.with(DayOfWeek.SUNDAY);
            for(int i = 0; i < 7; i++) {
                LocalDate day = startOfWeek.plusDays(i);
                String headerText = String.format("%s %d/%d", 
                    day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.US),
                    day.getMonthValue(), day.getDayOfMonth());
                JLabel dayLabel = new JLabel(headerText, SwingConstants.CENTER);
                dayLabel.setFont(FONT_BODY_BOLD);
                dayLabel.setForeground(COLOR_OUTLINE);
                dayLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, (i==6 ? 0 : 1), COLOR_OUTLINE.brighter())); // Softer border
                
                // Add background highlight for Today
                if (day.equals(LocalDate.now())) {
                    dayLabel.setOpaque(true);
                    dayLabel.setBackground(COLOR_ACCENT_PEACH);
                }
                
                dayHeaderPanel.add(dayLabel);
            }
            
            JPanel northPanel = new JPanel(new BorderLayout());
            northPanel.add(headerSpacer, BorderLayout.WEST);
            northPanel.add(dayHeaderPanel, BorderLayout.CENTER);

            add(northPanel, BorderLayout.NORTH);
            add(mainContent, BorderLayout.CENTER);
            
            // Set preferred size for scrolling
            gridPanel.setPreferredSize(new Dimension(800, 24 * 60)); // 1 pixel per minute
        }

        private class WeekGridPanel extends JPanel {
            private static final int HOUR_HEIGHT = 60; // 1 pixel per minute

            WeekGridPanel() {
                setBackground(COLOR_CELL_BG);
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            // Check if we clicked on an existing event
                            Optional<Event> clickedEvent = getEventAtPoint(e.getPoint());
                            
                            if (clickedEvent.isPresent()) {
                                // Open dialog to EDIT/DELETE the clicked event
                                showEventDialog(clickedEvent.get());
                            } else {
                                // No event clicked, create a NEW one
                                int colWidth = getWidth() / 7;
                                int dayIndex = e.getX() / colWidth;
                                LocalDate day = selectedDate.with(DayOfWeek.SUNDAY).plusDays(dayIndex);
                                
                                int minute = e.getY();
                                LocalTime time = LocalTime.of(minute / 60, minute % 60);

                                Event stub = new Appointment(null, "", "", LocalDateTime.of(day, time), 60, "");
                                showEventDialog(stub);
                            }
                        }
                    }
                });
            }

            /**
             * Helper: Finds which event, if any, exists at the clicked point.
             */
            private Optional<Event> getEventAtPoint(Point p) {
                LocalDate startOfWeek = selectedDate.with(DayOfWeek.SUNDAY);
                List<Event> events = storage.getEventsForWeek(startOfWeek);
                int colWidth = getWidth() / 7;

                for (Event event : events) {
                    LocalDateTime start = event.getStartTime();
                    int dayIndex = (int) ChronoUnit.DAYS.between(startOfWeek, start.toLocalDate());

                    if (dayIndex >= 0 && dayIndex < 7) {
                        int x = dayIndex * colWidth;
                        int y = start.getHour() * HOUR_HEIGHT + start.getMinute();
                        int eventHeight = (int) event.getDurationMinutes();
                        
                        // This rectangle MUST match the one in paintComponent
                        Rectangle eventBounds = new Rectangle(x + 5, y + 2, colWidth - 10, eventHeight - 4);
                        
                        if (eventBounds.contains(p)) {
                            return Optional.of(event);
                        }
                    }
                }
                return Optional.empty();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();
                int colWidth = width / 7;

                // Draw horizontal hour lines
                g2.setColor(COLOR_LIGHT_TEXT.brighter()); // Very light gray
                for (int h = 0; h < 24; h++) {
                    int y = h * HOUR_HEIGHT;
                    g2.drawLine(0, y, width, y);
                }

                // Draw vertical day lines
                g2.setColor(COLOR_OUTLINE.brighter()); // Softer outline
                for (int d = 1; d < 7; d++) {
                    int x = d * colWidth;
                    g2.drawLine(x, 0, x, height);
                }

                // Draw Events
                LocalDate startOfWeek = selectedDate.with(DayOfWeek.SUNDAY);
                List<Event> events = storage.getEventsForWeek(startOfWeek);
                
                for (Event event : events) {
                    LocalDateTime start = event.getStartTime();
                    int dayIndex = (int) ChronoUnit.DAYS.between(startOfWeek, start.toLocalDate());
                    
                    if (dayIndex >= 0 && dayIndex < 7) {
                        int x = dayIndex * colWidth;
                        int y = start.getHour() * HOUR_HEIGHT + start.getMinute();
                        int eventHeight = (int) event.getDurationMinutes();

                        if (y + eventHeight > height) eventHeight = height - y;
                        if (eventHeight < 4) eventHeight = 4; // Min height

                        // Draw event box
                        g2.setColor(COLOR_ACCENT_BLUE.brighter()); // Light blue event background
                        g2.fillRoundRect(x + 5, y + 2, colWidth - 10, eventHeight - 4, 10, 10);
                        g2.setColor(COLOR_OUTLINE.brighter()); // Softer outline
                        g2.drawRoundRect(x + 5, y + 2, colWidth - 10, eventHeight - 4, 10, 10);

                        // Draw text with hierarchy
                        g2.setColor(COLOR_OUTLINE);
                        Rectangle oldClip = g2.getClipBounds();
                        g2.setClip(x + 8, y + 5, colWidth - 16, eventHeight - 10);
                        
                        g2.setFont(FONT_BODY_REG.deriveFont(Font.BOLD)); // Bold title
                        g2.drawString(event.getTitle(), x + 10, y + 20);
                        
                        if (eventHeight > 35) { // Only draw if space
                            g2.setFont(FONT_BODY_REG); // Regular time
                            g2.drawString(start.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")), x + 10, y + 35);
                        }
                        g2.setClip(oldClip); // Restore original clip
                    }
                }
                g2.dispose();
            }
        }
    }

    // --- Inner Class: DayViewPanel ---
    private class DayViewPanel extends JPanel {
        private final JPanel timeColumn;
        private final DayGridPanel gridPanel;
        
        DayViewPanel() {
            setLayout(new BorderLayout());
            setBackground(COLOR_CELL_BG);

            // 1. Time Column
            timeColumn = new JPanel(new GridLayout(24, 1));
            timeColumn.setBackground(COLOR_CELL_BG);
            timeColumn.setPreferredSize(new Dimension(60, 24 * 60)); // 60px height per hour
            for (int h = 0; h < 24; h++) {
                JLabel timeLabel = new JLabel(String.format("%02d:00", h), SwingConstants.CENTER);
                timeLabel.setFont(FONT_BODY_REG);
                timeLabel.setForeground(COLOR_OUTLINE);
                timeLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, COLOR_OUTLINE.brighter()));
                timeLabel.setPreferredSize(new Dimension(60, 60)); // Explicit height
                timeColumn.add(timeLabel);
            }
            
            // 2. Day Grid
            gridPanel = new DayGridPanel();

            // Add time column and grid
            JPanel mainContent = new JPanel(new BorderLayout());
            mainContent.add(timeColumn, BorderLayout.WEST);
            mainContent.add(gridPanel, BorderLayout.CENTER);

            // Day Header Panel (just one day)
            JPanel dayHeaderPanel = new JPanel(new GridLayout(1, 1));
            dayHeaderPanel.setPreferredSize(new Dimension(0, 50));
            String headerText = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
            JLabel dayLabel = new JLabel(headerText, SwingConstants.CENTER);
            dayLabel.setFont(FONT_SUBHEADER);
            dayLabel.setForeground(COLOR_OUTLINE);
            dayLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_OUTLINE.brighter()));
            if (selectedDate.equals(LocalDate.now())) {
                dayLabel.setOpaque(true);
                dayLabel.setBackground(COLOR_ACCENT_PEACH);
            }
            dayHeaderPanel.add(dayLabel);
            
            // Spacer for top-left
            JPanel headerSpacer = new JPanel();
            headerSpacer.setPreferredSize(new Dimension(60, 50));
            headerSpacer.setBackground(COLOR_CELL_BG);
            headerSpacer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, COLOR_OUTLINE.brighter()));

            JPanel northPanel = new JPanel(new BorderLayout());
            northPanel.add(headerSpacer, BorderLayout.WEST);
            northPanel.add(dayHeaderPanel, BorderLayout.CENTER);

            add(northPanel, BorderLayout.NORTH);
            add(mainContent, BorderLayout.CENTER);
            
            // Set preferred size for scrolling
            gridPanel.setPreferredSize(new Dimension(800, 24 * 60)); // 1 pixel per minute
        }

        private class DayGridPanel extends JPanel {
            private static final int HOUR_HEIGHT = 60; // 1 pixel per minute

            DayGridPanel() {
                setBackground(COLOR_CELL_BG);
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            // Check if we clicked on an existing event
                            Optional<Event> clickedEvent = getEventAtPoint(e.getPoint());
                            
                            if (clickedEvent.isPresent()) {
                                // Open dialog to EDIT/DELETE the clicked event
                                showEventDialog(clickedEvent.get());
                            } else {
                                // No event clicked, create a NEW one
                                int minute = e.getY();
                                LocalTime time = LocalTime.of(minute / 60, minute % 60);
                                Event stub = new Appointment(null, "", "", LocalDateTime.of(selectedDate, time), 60, "");
                                showEventDialog(stub);
                            }
                        }
                    }
                });
            }

            /**
             * Helper: Finds which event, if any, exists at the clicked point.
             */
            private Optional<Event> getEventAtPoint(Point p) {
                List<Event> events = storage.getEventsForDay(selectedDate);
                int width = getWidth();
                
                for (Event event : events) {
                    int x = 15;
                    int y = event.getStartTime().getHour() * HOUR_HEIGHT + event.getStartTime().getMinute();
                    int eventHeight = (int) event.getDurationMinutes();
                    int eventWidth = width - 30;
                    
                    // This rectangle MUST match the one in paintComponent
                    Rectangle eventBounds = new Rectangle(x, y + 2, eventWidth, eventHeight - 4);
                    
                    if (eventBounds.contains(p)) {
                        return Optional.of(event);
                    }
                }
                return Optional.empty();
            }


            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                // Draw horizontal hour lines
                g2.setColor(COLOR_LIGHT_TEXT.brighter()); // Very light gray
                for (int h = 0; h < 24; h++) {
                    int y = h * HOUR_HEIGHT;
                    g2.drawLine(0, y, width, y);
                }

                // Draw Events
                List<Event> events = storage.getEventsForDay(selectedDate);
                
                for (Event event : events) {
                    LocalDateTime start = event.getStartTime();
                    
                    int x = 15; // More padding from left edge
                    int y = start.getHour() * HOUR_HEIGHT + start.getMinute();
                    int eventHeight = (int) event.getDurationMinutes();
                    int eventWidth = width - 30; // More padding total

                    if (y + eventHeight > height) eventHeight = height - y;
                    if (eventHeight < 4) eventHeight = 4; // Min height

                    // Draw event box
                    g2.setColor(COLOR_ACCENT_BLUE.brighter());
                    g2.fillRoundRect(x, y + 2, eventWidth, eventHeight - 4, 10, 10);
                    g2.setColor(COLOR_OUTLINE.brighter());
                    g2.drawRoundRect(x, y + 2, eventWidth, eventHeight - 4, 10, 10);

                    // Draw text with hierarchy
                    g2.setColor(COLOR_OUTLINE);
                    Rectangle oldClip = g2.getClipBounds();
                    g2.setClip(x + 5, y + 5, eventWidth - 10, eventHeight - 10);
                    
                    g2.setFont(FONT_BODY_REG.deriveFont(Font.BOLD)); // Bold title
                    g2.drawString(event.getTitle(), x + 8, y + 20);
                    
                    if (eventHeight > 35) { // Only draw if space
                        g2.setFont(FONT_BODY_REG); // Regular time
                        g2.drawString(start.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) + " (" + event.getDurationMinutes() + "m)", x + 8, y + 35);
                    }
                    g2.setClip(oldClip);
                }
                g2.dispose();
            }
        }
    }

    // --- Inner Class: YearViewPanel ---
    private class YearViewPanel extends JPanel {
        YearViewPanel() {
            setLayout(new GridLayout(3, 4, 15, 15)); // More spacing
            setBackground(COLOR_BG_LIGHT); // Use overall light background
            setBorder(new EmptyBorder(20, 20, 20, 20));

            int year = selectedDate.getYear();
            for (int m = 1; m <= 12; m++) {
                YearMonth month = YearMonth.of(year, m);
                JPanel miniCal = new YearMiniCalendar(month);
                miniCal.setBorder(new CompoundBorder(
                    new ScallopedBorder(COLOR_OUTLINE.brighter(), 6, 1), // Scalloped border for mini-cal
                    BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(), // No inner line for title
                        month.getMonth().getDisplayName(TextStyle.FULL, Locale.US),
                        javax.swing.border.TitledBorder.CENTER,
                        javax.swing.border.TitledBorder.TOP,
                        FONT_BODY_BOLD,
                        COLOR_OUTLINE
                    )
                ));
                add(miniCal);
            }
        }

        // A read-only mini calendar for the year view
        private class YearMiniCalendar extends JPanel {
            YearMiniCalendar(YearMonth month) {
                setLayout(new GridLayout(0, 7)); // 7 days + header
                setBackground(COLOR_CELL_BG); // White background for inner calendar
                setBorder(new EmptyBorder(5, 5, 5, 5)); // Inner padding

                // Day of Week Headers
                String[] dayNames = {"S", "M", "T", "W", "T", "F", "S"};
                for (String day : dayNames) {
                    JLabel dayLabel = new JLabel(day, SwingConstants.CENTER);
                    dayLabel.setFont(FONT_SMALL);
                    dayLabel.setForeground(COLOR_LIGHT_TEXT);
                    add(dayLabel);
                }

                // Day Cells
                LocalDate firstDayOfMonth = month.atDay(1);
                LocalDate firstDayOfGrid = firstDayOfMonth.with(DayOfWeek.SUNDAY);
                
                LocalDate d = firstDayOfGrid;
                for (int i = 0; i < 42; i++) {
                    JLabel dayLabel = new JLabel(String.valueOf(d.getDayOfMonth()), SwingConstants.CENTER);
                    dayLabel.setFont(FONT_SMALL);
                    dayLabel.setForeground(COLOR_OUTLINE); // Default text color
                    
                    if (!d.getMonth().equals(month)) {
                        dayLabel.setForeground(COLOR_LIGHT_TEXT); // Lighter for other months
                    } else if (d.equals(LocalDate.now())) {
                        dayLabel.setForeground(COLOR_ACCENT_BLUE.darker()); // Highlight today
                        dayLabel.setFont(FONT_BODY_REG); // Slightly bolder for today
                    }
                    
                    add(dayLabel);
                    d = d.plusDays(1);
                    if (i > 28 && d.getDayOfWeek() == DayOfWeek.SUNDAY && d.getMonthValue() != month.getMonthValue()) {
                        break; // Stop after 5 or 6 rows
                    }
                }

                final YearMonth clickedMonth = month;
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        selectedDate = clickedMonth.atDay(1);
                        setView("Month");
                    }
                });
            }
        }
    }

    // --- Inner Class: EventDialog ---
    private class EventDialog extends JDialog {
        private boolean saved = false;
        private final Event eventToEdit;

        private final JTextField titleField = new JTextField(20);
        private final JTextField dateField = new JTextField(10); // yyyy-MM-dd
        private final JTextField timeField = new JTextField(5); // HH:mm
        private final JTextField durationField = new JTextField(5); // minutes
        private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Appointment", "Deadline"});
        private final JTextField detailField = new JTextField(20); // Location/Course
        private final JTextArea descriptionArea = new JTextArea(5, 20);
        private final JLabel detailLabel = new JLabel("Location:");

        EventDialog(JFrame parent, Event event) {
            super(parent, (event == null ? "Create Event" : "Edit Event"), true);
            this.eventToEdit = event;

            setLayout(new BorderLayout(15, 15)); // More spacing
            setBackground(COLOR_BG_LIGHT);

            // Header
            JPanel header = new JPanel();
            header.setBackground(COLOR_ACCENT_BLUE); // Blue header
            header.setBorder(new EmptyBorder(15, 15, 15, 15));
            JLabel headerLabel = new JLabel((event == null ? "Create New Event" : "Edit Event"), SwingConstants.LEFT);
            headerLabel.setFont(FONT_SUBHEADER.deriveFont(Font.BOLD));
            headerLabel.setForeground(Color.WHITE); // White text on blue header
            header.add(headerLabel);
            add(header, BorderLayout.NORTH);

            // Form Panel
            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(COLOR_CELL_BG); // White background
            formPanel.setBorder(new EmptyBorder(15, 15, 15, 15)); // More padding
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8); // More spacing
            gbc.anchor = GridBagConstraints.WEST;

            // Apply consistent styling to all labels
            applyLabelStyle(new JLabel("Title:"), formPanel, gbc, 0, 0);
            gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(titleField, gbc);

            applyLabelStyle(new JLabel("Date (yyyy-MM-dd):"), formPanel, gbc, 0, 1);
            gbc.gridx = 1; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
            formPanel.add(dateField, gbc);

            applyLabelStyle(new JLabel("Time (HH:mm):"), formPanel, gbc, 2, 1);
            gbc.gridx = 3;
            formPanel.add(timeField, gbc);

            applyLabelStyle(new JLabel("Duration (mins):"), formPanel, gbc, 0, 2);
            gbc.gridx = 1;
            formPanel.add(durationField, gbc);

            applyLabelStyle(new JLabel("Type:"), formPanel, gbc, 2, 2);
            gbc.gridx = 3;
            formPanel.add(typeCombo, gbc);

            // Detail (Location/Course)
            applyLabelStyle(detailLabel, formPanel, gbc, 0, 3);
            gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
            formPanel.add(detailField, gbc);

            applyLabelStyle(new JLabel("Description:"), formPanel, gbc, 0, 4);
            gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
            JScrollPane scrollPane = new JScrollPane(descriptionArea);
            scrollPane.setBorder(BorderFactory.createLineBorder(COLOR_OUTLINE.brighter(), 1)); // Subtle border for text area
            formPanel.add(scrollPane, gbc);
            
            // Apply consistent styling to text fields and combo box
            styleInputField(titleField);
            styleInputField(dateField);
            styleInputField(timeField);
            styleInputField(durationField);
            styleInputField(detailField);
            styleComboBox(typeCombo);
            descriptionArea.setFont(FONT_BODY_REG);
            descriptionArea.setForeground(COLOR_OUTLINE);
            descriptionArea.setLineWrap(true);
            descriptionArea.setWrapStyleWord(true);


            add(formPanel, BorderLayout.CENTER);

            // Listen to type combo box
            typeCombo.addActionListener(e -> 
                detailLabel.setText("Appointment".equals(typeCombo.getSelectedItem()) ? "Location:" : "Course:")
            );

            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            buttonPanel.setBackground(COLOR_CELL_BG);
            buttonPanel.setBorder(new EmptyBorder(0, 15, 15, 15));
            
            // Show Delete button only when editing
            if (eventToEdit != null) {
                JButton deleteButton = new ProButton("Delete", COLOR_ACCENT_PEACH, COLOR_ACCENT_PEACH.darker());
                deleteButton.addActionListener(e -> deleteEvent());
                buttonPanel.add(deleteButton);
            }

            JButton cancelButton = new JButton("Cancel");
            styleNavButton(cancelButton); // Re-use nav button style for cancel
            cancelButton.addActionListener(e -> dispose());
            ProButton saveButton = new ProButton("Save", COLOR_ACCENT_BLUE, COLOR_ACCENT_BLUE.darker());
            saveButton.addActionListener(e -> saveEvent());
            buttonPanel.add(cancelButton);
            buttonPanel.add(saveButton);
            
            add(buttonPanel, BorderLayout.SOUTH);

            // Populate fields if editing
            populateFields();

            pack();
            setLocationRelativeTo(parent);
        }

        private void applyLabelStyle(JLabel label, JPanel panel, GridBagConstraints gbc, int x, int y) {
            gbc.gridx = x; gbc.gridy = y; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
            label.setFont(FONT_BODY_BOLD);
            label.setForeground(COLOR_OUTLINE);
            panel.add(label, gbc);
        }

        private void styleInputField(JTextField field) {
            field.setFont(FONT_BODY_REG);
            field.setForeground(COLOR_OUTLINE);
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_OUTLINE.brighter(), 1), // Subtle border
                new EmptyBorder(5, 8, 5, 8) // Inner padding
            ));
            field.setBackground(COLOR_LIGHT_TEXT.brighter().brighter()); // Very light background for fields
        }

        private void styleComboBox(JComboBox<String> comboBox) {
            comboBox.setFont(FONT_BODY_REG);
            comboBox.setForeground(COLOR_OUTLINE);
            comboBox.setBackground(COLOR_LIGHT_TEXT.brighter().brighter());
            ((JLabel)comboBox.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
            comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_OUTLINE.brighter(), 1),
                new EmptyBorder(5, 5, 5, 5)
            ));
        }


        private void populateFields() {
            LocalDate dateToShow;
            LocalTime timeToSHow;

            if (eventToEdit != null) {
                // Use event's info (for editing or stub)
                dateToShow = eventToEdit.getStartTime().toLocalDate();
                timeToSHow = eventToEdit.getStartTime().toLocalTime();
                
                titleField.setText(eventToEdit.getTitle());
                // Handle 0 duration for stubs
                int duration = eventToEdit.getDurationMinutes();
                durationField.setText(duration > 0 ? String.valueOf(duration) : "60");
                typeCombo.setSelectedItem(eventToEdit.getType());
                detailField.setText(eventToEdit.getDetail());
                descriptionArea.setText(eventToEdit.getDescription());
            } else {
                // **FIX**: Use current date/time for new event
                dateToShow = LocalDate.now(); 
                timeToSHow = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
                
                durationField.setText("60");
            }
            
            dateField.setText(dateToShow.format(DateTimeFormatter.ISO_LOCAL_DATE));
            timeField.setText(timeToSHow.format(DateTimeFormatter.ofPattern("HH:mm")));
            
            // Set label based on current/loaded type
            detailLabel.setText("Appointment".equals(typeCombo.getSelectedItem()) ? "Location:" : "Course:") ; 
        }

        private void saveEvent() {
            try {
                // 1. Parse fields
                String title = titleField.getText();
                LocalDate date = LocalDate.parse(dateField.getText(), DateTimeFormatter.ISO_LOCAL_DATE);
                LocalTime time = LocalTime.parse(timeField.getText(), DateTimeFormatter.ofPattern("HH:mm"));
                LocalDateTime startTime = LocalDateTime.of(date, time);
                int duration = Integer.parseInt(durationField.getText());
                String type = (String) typeCombo.getSelectedItem();
                String detail = detailField.getText();
                String description = descriptionArea.getText();

                // 2. Get ID (new or existing)
                String id = (eventToEdit != null) ? eventToEdit.getEventId() : UUID.randomUUID().toString();

                // 3. Create Event object
                Event newEvent;
                if ("Appointment".equals(type)) {
                    newEvent = new Appointment(id, title, description, startTime, duration, detail);
                } else {
                    newEvent = new Deadline(id, title, description, startTime, duration, detail);
                }

                // 4. Call updateEvent if editing, addEvent if new
                // **FIX**: Check if it's a stub (which has a blank title) or a real edit
                if (eventToEdit == null || (eventToEdit != null && eventToEdit.getTitle().isEmpty())) {
                    storage.addEvent(newEvent);
                } else {
                    storage.updateEvent(newEvent);
                }

                saved = true;
                dispose();

            } catch (EventConflictException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Conflict Detected", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                // Catch-all for parsing errors (DateTimeParseException, NumberFormatException)
                // or validation errors (IllegalArgumentException from Event constructor)
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        // This is the logic for the delete button
        private void deleteEvent() {
            if (eventToEdit == null) return;
            
            int choice = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete '" + eventToEdit.getTitle() + "'?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                
            if (choice == JOptionPane.YES_OPTION) {
                storage.deleteEvent(eventToEdit.getEventId());
                saved = true;
                dispose();
            }
        }

        public boolean isSaved() {
            return saved;
        }
    }

    // --- Main Method ---
    public static void main(String[] args) {
        // Set anti-aliasing for text
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Run on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}