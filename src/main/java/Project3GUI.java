/*
 Name: Camilo Alvarez-Velez
 Course: CNT 4714 Spring 2025
 Assignment title: Project 3 – A Two-tier Client-Server Application
 Date: March 14, 2025
 Class: Project3GUI.java
*/

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
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

    // Results Panel components (spreadsheet-style JTable)
    private final JTable resultTable;
    private final JScrollPane resultScrollPane;
    private final JButton exitButton; // Exit button

    // Panel for connection info (above results)
    private final JLabel connectionInfoLabel;

    // Default values in case properties files do not override them
    String defaultUrl = "jdbc:mysql://localhost:3306/project3";

    // JDBC connection
    public Connection c;

    // Store the current logged-in user (from the properties file) for logging operations
    private String currentLoggedInUser;

    public Project3GUI() {
        // Updated window title
        setTitle("Project 3 - Two-Tier Client Application");
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
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

        // Build Results Panel with Exit Button (in the south)
        JPanel resultsContainer = new JPanel(new BorderLayout());
        resultsContainer.setBorder(BorderFactory.createTitledBorder("SQL Result"));
        // Create a non-editable JTable for spreadsheet-style display
        resultTable = new JTable(new DefaultTableModel()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable.setFillsViewportHeight(true);
        resultScrollPane = new JScrollPane(resultTable);
        resultsContainer.add(resultScrollPane, BorderLayout.CENTER);

        // Create a panel for connection info (above results)
        JPanel connectionInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connectionInfoLabel = new JLabel("Not connected");
        connectionInfoPanel.add(new JLabel("Current Connection URL: "));
        connectionInfoPanel.add(connectionInfoLabel);

        // Bottom panel to hold connection info, results panel, and exit button.
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(connectionInfoPanel, BorderLayout.NORTH);
        bottomPanel.add(resultsContainer, BorderLayout.CENTER);

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
            }
        } else {
            userPropertiesCombo.addItem("root.properties");
            userPropertiesCombo.addItem("client1.properties");
            userPropertiesCombo.addItem("client2.properties");
            userPropertiesCombo.addItem("project3app.properties");
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
                ((DefaultTableModel) resultTable.getModel()).setRowCount(0);
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
            currentLoggedInUser = propUsername;
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
        try {
            if (sql.toLowerCase().startsWith("select")) {
                try (PreparedStatement pstmt = c.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    DefaultTableModel model = buildTableModel(rs);
                    resultTable.setModel(model);
                    // Log SELECT queries (unless executed by the accountant)
                    if (!currentLoggedInUser.equalsIgnoreCase("theaccountant")) {
                        logOperation(currentLoggedInUser, "query");
                    }
                }
            } else {
                try (PreparedStatement pstmt = c.prepareStatement(sql)) {
                    int updateCount = pstmt.executeUpdate();
                    String message = "Command executed successfully. Rows affected: " + updateCount;
                    String lowerSql = sql.toLowerCase();
                    if (lowerSql.startsWith("insert")) {
                        JOptionPane.showMessageDialog(this, message, "Insert Success", JOptionPane.INFORMATION_MESSAGE);
                    } else if (lowerSql.startsWith("update") || lowerSql.startsWith("delete")) {
                        JOptionPane.showMessageDialog(this, message, "Update Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                    resultTable.setModel(new DefaultTableModel());
                    // Log update operation if not executed by accountant
                    if (!currentLoggedInUser.equalsIgnoreCase("theaccountant")) {
                        logOperation(currentLoggedInUser, "update");
                    }
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "SQL Error: " + ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("SQL Error: " + ex.getMessage());
        }
    }

    /**
     * Builds a DefaultTableModel from the given ResultSet.
     */
    private DefaultTableModel buildTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (int i = 1; i <= colCount; i++) {
            model.addColumn(meta.getColumnLabel(i));
        }

        while (rs.next()) {
            Object[] rowData = new Object[colCount];
            for (int i = 1; i <= colCount; i++) {
                rowData[i - 1] = rs.getObject(i);
            }
            model.addRow(rowData);
        }
        return model;
    }

    /**
     * Logs the operation to the operations log database.
     * For SELECT queries, operationType should be "query".
     * For non-select commands (insert, update, delete), operationType should be "update".
     * Operations by "theaccountant" are not logged.
     */
    private void logOperation(String loginUsername, String operationType) {
        // Append "@localhost" if it's not already included
        if (!loginUsername.contains("@")) {
            loginUsername = loginUsername + "@localhost";
        }

        // Load operations log properties from "operationslog.properties" in config/ops
        Properties opProps = new Properties();
        File opPropFile = new File("config/ops", "operationslog.properties");
        if (!opPropFile.exists()) {
            System.err.println("Operations log properties file not found.");
            return;
        }
        try (FileInputStream fis = new FileInputStream(opPropFile)) {
            opProps.load(fis);
        } catch (IOException ex) {
            System.err.println("Failed to load operations log properties: " + ex.getMessage());
            return;
        }

        String driver = opProps.getProperty("driver", "com.mysql.cj.jdbc.Driver");
        String url = opProps.getProperty("url", "jdbc:mysql://localhost:3306/operationslog");
        String opUsername = opProps.getProperty("username", "project3app");
        String opPassword = opProps.getProperty("password", "project3app");

        try {
            Class.forName(driver);
            try (Connection opConn = DriverManager.getConnection(url, opUsername, opPassword)) {
                String sql = null;
                if (operationType.equalsIgnoreCase("query")) {
                    sql = "UPDATE operationscount SET num_queries = num_queries + 1 WHERE login_username = ?";
                } else if (operationType.equalsIgnoreCase("update")) {
                    sql = "UPDATE operationscount SET num_updates = num_updates + 1 WHERE login_username = ?";
                }
                if (sql != null) {
                    try (PreparedStatement pstmt = opConn.prepareStatement(sql)) {
                        pstmt.setString(1, loginUsername);
                        int rows = pstmt.executeUpdate();
                        if (rows == 0) {
                            String insertSql = "INSERT INTO operationscount (login_username, num_queries, num_updates) VALUES (?, ?, ?)";
                            try (PreparedStatement ipstmt = opConn.prepareStatement(insertSql)) {
                                ipstmt.setString(1, loginUsername);
                                ipstmt.setInt(2, operationType.equalsIgnoreCase("query") ? 1 : 0);
                                ipstmt.setInt(3, operationType.equalsIgnoreCase("update") ? 1 : 0);
                                ipstmt.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Error logging operation: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Project3GUI::new);
    }
}
