import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Project3GUI extends JFrame implements ActionListener {
    // Connection Panel components
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton connectButton;
    private final JButton disconnectButton;
    private final JComboBox<String> dbPropertiesCombo;   // Dropdown for DB properties
    private final JComboBox<String> userPropertiesCombo; // Dropdown for user credentials

    // Command Panel components
    private final JTextArea sqlCommandArea;
    private final JButton executeButton;
    private final JButton clearButton;

    // Result Panel components
    private final JTextArea resultArea;
    private final JButton exitButton; // Exit button

    // Panel for connection info (above results)
    private final JLabel connectionInfoLabel;

    // Default values in case properties files do not override them
    String defaultUrl = "jdbc:mysql://localhost:3306/project3";

    // JDBC connection
    public Connection c;

    public Project3GUI() {
        setTitle("Project 3 - Client Application");
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        // Use a BorderLayout for the frame.
        setLayout(new BorderLayout());

        // Build Connection Panel using GridBagLayout (3 rows: Properties, Credentials, Connect)
        JPanel connectionPanel = new JPanel(new GridBagLayout());
        connectionPanel.setBorder(BorderFactory.createTitledBorder("Database Connection"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Row 1: Properties dropdowns
        gbc.gridx = 0;
        gbc.gridy = 0;
        connectionPanel.add(new JLabel("DB Properties:"), gbc);

        gbc.gridx = 1;
        dbPropertiesCombo = new JComboBox<>();
        populateDBPropertiesDropdown();
        connectionPanel.add(dbPropertiesCombo, gbc);

        gbc.gridx = 2;
        connectionPanel.add(new JLabel("User Properties:"), gbc);

        gbc.gridx = 3;
        userPropertiesCombo = new JComboBox<>();
        populateUserPropertiesDropdown();
        connectionPanel.add(userPropertiesCombo, gbc);

        // Row 2: Credentials
        gbc.gridx = 0;
        gbc.gridy = 1;
        connectionPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(10);
        connectionPanel.add(usernameField, gbc);

        gbc.gridx = 2;
        connectionPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 3;
        passwordField = new JPasswordField(10);
        connectionPanel.add(passwordField, gbc);

        // Row 3: Connect / Disconnect buttons
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        connectButton = new JButton("Connect");
        connectButton.addActionListener(this);
        connectionPanel.add(connectButton, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(this);
        connectionPanel.add(disconnectButton, gbc);

        add(connectionPanel, BorderLayout.NORTH);

        // Build Command Panel (in the center)
        JPanel commandPanel = new JPanel(new BorderLayout());
        commandPanel.setBorder(BorderFactory.createTitledBorder("SQL Command"));
        sqlCommandArea = new JTextArea(5, 50);
        sqlCommandArea.setLineWrap(true);
        sqlCommandArea.setWrapStyleWord(true);
        commandPanel.add(new JScrollPane(sqlCommandArea), BorderLayout.CENTER);

        JPanel cmdButtonPanel = new JPanel();
        executeButton = new JButton("Execute");
        executeButton.addActionListener(this);
        cmdButtonPanel.add(executeButton);
        clearButton = new JButton("Clear");
        clearButton.addActionListener(this);
        cmdButtonPanel.add(clearButton);
        commandPanel.add(cmdButtonPanel, BorderLayout.SOUTH);

        add(commandPanel, BorderLayout.CENTER);

        // Build Result Panel with Exit Button (in the south)
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("SQL Result"));
        resultArea = new JTextArea(10, 50);
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultArea.setLineWrap(false);
        resultPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        // Create a panel for connection info (above results)
        JPanel connectionInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connectionInfoLabel = new JLabel("Not connected");
        connectionInfoPanel.add(new JLabel("Current Connection URL: "));
        connectionInfoPanel.add(connectionInfoLabel);

        // Bottom panel to hold connection info, result panel, and exit button.
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(connectionInfoPanel, BorderLayout.NORTH);
        bottomPanel.add(resultPanel, BorderLayout.CENTER);

        exitButton = new JButton("Exit");
        exitButton.addActionListener(this);
        JPanel exitPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        exitPanel.add(exitButton);
        bottomPanel.add(exitPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    /**
     * Updates the connection info label with the given message and background color.
     */
    private void updateConnectionStatus(String message, Color bgColor) {
        connectionInfoLabel.setText(message);
        connectionInfoLabel.setOpaque(true);
        connectionInfoLabel.setBackground(bgColor);
    }

    /**
     * Populates the database properties dropdown from the "config/db" folder.
     */
    private void populateDBPropertiesDropdown() {
        File dbConfigDir = new File("config/db");
        if (dbConfigDir.exists() && dbConfigDir.isDirectory()) {
            File[] files = dbConfigDir.listFiles((dir, name) -> name.endsWith(".properties"));
            if (files != null && files.length > 0) {
                for (File file : files) {
                    dbPropertiesCombo.addItem(file.getName());
                }
            } else {
                dbPropertiesCombo.addItem("project3.properties");
                dbPropertiesCombo.addItem("bikedb.properties");
            }
        } else {
            dbPropertiesCombo.addItem("project3.properties");
            dbPropertiesCombo.addItem("bikedb.properties");
        }
    }

    /**
     * Populates the user properties dropdown from the "config/user" folder.
     */
    private void populateUserPropertiesDropdown() {
        File userConfigDir = new File("config/user");
        if (userConfigDir.exists() && userConfigDir.isDirectory()) {
            File[] files = userConfigDir.listFiles((dir, name) -> name.endsWith(".properties"));
            if (files != null && files.length > 0) {
                for (File file : files) {
                    userPropertiesCombo.addItem(file.getName());
                }
            } else {
                userPropertiesCombo.addItem("root.properties");
                userPropertiesCombo.addItem("client1.properties");
                userPropertiesCombo.addItem("client2.properties");
                userPropertiesCombo.addItem("project3app.properties");
                userPropertiesCombo.addItem("theaccountant.properties");
            }
        } else {
            userPropertiesCombo.addItem("root.properties");
            userPropertiesCombo.addItem("client1.properties");
            userPropertiesCombo.addItem("client2.properties");
            userPropertiesCombo.addItem("project3app.properties");
            userPropertiesCombo.addItem("theaccountant.properties");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Connect" -> connectToDatabase();
            case "Disconnect" -> disconnectFromDatabase();
            case "Execute" -> executeSQLCommand();
            case "Clear" -> {
                sqlCommandArea.setText("");
                resultArea.setText("");
            }
            case "Exit" -> System.exit(0);
        }
    }

    /**
     * Connects to the database using settings from the selected DB properties file and user properties file.
     * Verifies that the entered username and password match the user properties.
     */
    public void connectToDatabase() {
        // Load DB properties from the selected file in config/db
        String selectedDBProperties = (String) dbPropertiesCombo.getSelectedItem();
        Properties dbProps = new Properties();
        File dbPropFile = new File("config/db", selectedDBProperties);
        if (dbPropFile.exists()) {
            try (FileInputStream fis = new FileInputStream(dbPropFile)) {
                dbProps.load(fis);
            } catch (IOException ex) {
                updateConnectionStatus("Failed to load DB properties", Color.RED);
                return;
            }
        } else {
            updateConnectionStatus("DB properties file not found", Color.RED);
            return;
        }

        // Load User properties from the selected file in config/user
        String selectedUserProperties = (String) userPropertiesCombo.getSelectedItem();
        Properties userProps = new Properties();
        File userPropFile = new File("config/user", selectedUserProperties);
        if (userPropFile.exists()) {
            try (FileInputStream fis = new FileInputStream(userPropFile)) {
                userProps.load(fis);
            } catch (IOException ex) {
                updateConnectionStatus("Failed to load user properties", Color.RED);
                return;
            }
        } else {
            updateConnectionStatus("User properties file not found", Color.RED);
            return;
        }

        // Use properties to override defaults
        String driver = dbProps.getProperty("driver", "com.mysql.cj.jdbc.Driver");
        String urlFromProps = dbProps.getProperty("url", defaultUrl);

        // Use text field values if provided; otherwise, default to properties
        String guiUsername = usernameField.getText().trim();
        String guiPassword = new String(passwordField.getPassword()).trim();
        String propUsername = userProps.getProperty("username", "");
        String propPassword = userProps.getProperty("password", "");

        // Verify credentials: the GUI input must match the properties file exactly.
        if (!guiUsername.equals(propUsername) || !guiPassword.equals(propPassword)) {
            updateConnectionStatus("Credential mismatch", Color.RED);
            return;
        }

        try {
            Class.forName(driver);
            if (c != null && !c.isClosed()) {
                updateConnectionStatus("Already connected", Color.ORANGE);
                return;
            }
            c = DriverManager.getConnection(urlFromProps, propUsername, propPassword);
            updateConnectionStatus("Connected: " + urlFromProps, Color.GREEN);
            System.out.println("Connected as " + propUsername + " to " + urlFromProps + "\n");
        } catch (ClassNotFoundException e) {
            updateConnectionStatus("JDBC Driver not found", Color.RED);
        } catch (SQLException e) {
            updateConnectionStatus("Connection failed", Color.RED);
        }
    }

    public void disconnectFromDatabase() {
        try {
            if (c != null && !c.isClosed()) {
                c.close();
                updateConnectionStatus("Not connected", Color.LIGHT_GRAY);
                c = null;
            }
        } catch (SQLException e) {
            updateConnectionStatus("Failed to disconnect", Color.RED);
        }
    }

    public void executeSQLCommand() {
        String sql = sqlCommandArea.getText().trim();
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an SQL command.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (c == null) {
            JOptionPane.showMessageDialog(this, "No active connection. Please connect to the database first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Use a PreparedStatement for all commands.
        try {
            if (sql.toLowerCase().startsWith("select")) {
                try (PreparedStatement pstmt = c.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    String tableOutput = resultSetToTableString(rs);
                    resultArea.setText(tableOutput);
                }
            } else {
                try (PreparedStatement pstmt = c.prepareStatement(sql)) {
                    int updateCount = pstmt.executeUpdate();
                    String message = "Command executed successfully. Rows affected: " + updateCount;
                    String lowerSql = sql.toLowerCase();
                    if (lowerSql.startsWith("insert")) {
                        JOptionPane.showMessageDialog(this, message, "Insert Success", JOptionPane.INFORMATION_MESSAGE);
                    } else if (lowerSql.startsWith("update")) {
                        JOptionPane.showMessageDialog(this, message, "Update Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                    // Clear the result area since it's for SELECT output only.
                    resultArea.setText("");
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "SQL Error: " + ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("SQL Error: " + ex.getMessage());
        }
    }

    /**
     * Converts a ResultSet into a formatted table string.
     * Calculates column widths based on header and cell lengths.
     */
    public String resultSetToTableString(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        List<String[]> rows = new ArrayList<>();
        String[] headers = new String[colCount];
        int[] maxWidths = new int[colCount];

        // Read headers and initialize maxWidths.
        for (int i = 0; i < colCount; i++) {
            headers[i] = meta.getColumnLabel(i + 1);
            maxWidths[i] = headers[i].length();
        }
        rows.add(headers);

        // Read data rows.
        while (rs.next()) {
            String[] row = new String[colCount];
            for (int i = 0; i < colCount; i++) {
                Object obj = rs.getObject(i + 1);
                row[i] = (obj == null ? "NULL" : obj.toString());
                if (row[i].length() > maxWidths[i]) {
                    maxWidths[i] = row[i].length();
                }
            }
            rows.add(row);
        }

        // Build the table string.
        StringBuilder sb = new StringBuilder();
        // Header row
        for (int i = 0; i < colCount; i++) {
            sb.append(String.format("%-" + maxWidths[i] + "s", rows.get(0)[i]));
            if (i < colCount - 1) sb.append(" | ");
        }
        sb.append("\n");

        // Separator line
        for (int i = 0; i < colCount; i++) {
            sb.append("-".repeat(maxWidths[i]));
            if (i < colCount - 1) sb.append("-+-");
        }
        sb.append("\n");

        // Data rows
        for (int r = 1; r < rows.size(); r++) {
            String[] row = rows.get(r);
            for (int i = 0; i < colCount; i++) {
                sb.append(String.format("%-" + maxWidths[i] + "s", row[i]));
                if (i < colCount - 1) sb.append(" | ");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Project3GUI::new);
    }
}
