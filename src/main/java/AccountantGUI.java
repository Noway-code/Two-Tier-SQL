/*
 Name: Camilo Alvarez-Velez
 Course: CNT 4714 Spring 2025
 Assignment title: Project 3 – A Specialized Accountant Application
 Date: March 14, 2025
 Class: AccountantGUI.java
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

public class AccountantGUI extends JFrame implements ActionListener {
    // Connection Panel components
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton connectButton;
    private final JButton disconnectButton;

    // Command Panel components
    private final JTextArea sqlCommandArea;
    private final JButton executeButton;
    private final JButton clearButton;

    // Results Panel components (non-editable JTable)
    private final JTable resultTable;
    private final JScrollPane resultScrollPane;
    private final JButton exitButton; // Exit button

    // Panel for connection info (above results)
    private final JLabel connectionInfoLabel;

    // JDBC connection
    public Connection c;

    // Fixed accountant properties file (all properties in one file)
    private final String accountantPropertiesFile = "config/ops/theaccountant.properties";

    public AccountantGUI() {
        // Updated window title
        setTitle("Project 3 - Accountant Client");
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Build Connection Panel using GridBagLayout (2 rows: Credentials, Connect/Disconnect)
        JPanel connectionPanel = new JPanel(new GridBagLayout());
        connectionPanel.setBorder(BorderFactory.createTitledBorder("Database Connection"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Row 1: Credentials
        gbc.gridx = 0;
        gbc.gridy = 0;
        connectionPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(10);
        connectionPanel.add(usernameField, gbc);

        gbc.gridx = 2;
        connectionPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 3;
        passwordField = new JPasswordField(10);
        connectionPanel.add(passwordField, gbc);

        // Row 2: Connect / Disconnect buttons
        gbc.gridx = 0;
        gbc.gridy = 1;
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
        commandPanel.setBorder(BorderFactory.createTitledBorder("SQL Command (SELECT queries only)"));
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

        // Bottom panel to hold connection info, results, and exit button.
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
     * Connects to the operations log database using settings from the accountant properties file.
     * Verifies that the entered username and password match the properties.
     */
    public void connectToDatabase() {
        Properties accProps = new Properties();
        File accPropFile = new File(accountantPropertiesFile);
        if (!accPropFile.exists()) {
            updateConnectionStatus("Accountant properties file not found", Color.RED);
            return;
        }
        try (FileInputStream fis = new FileInputStream(accPropFile)) {
            accProps.load(fis);
        } catch (IOException ex) {
            updateConnectionStatus("Failed to load accountant properties", Color.RED);
            return;
        }

        String driver = accProps.getProperty("driver", "com.mysql.cj.jdbc.Driver");
        String urlFromProps = accProps.getProperty("url", "jdbc:mysql://localhost:3306/operationslog");
        String propUsername = accProps.getProperty("username", "");
        String propPassword = accProps.getProperty("password", "");

        // Use text field values if provided; otherwise, default to properties
        String guiUsername = usernameField.getText().trim();
        String guiPassword = new String(passwordField.getPassword()).trim();

        // Verify credentials: the GUI input must match exactly the properties.
        if (!guiUsername.equals(propUsername) || !guiPassword.equals(propPassword)) {
            updateConnectionStatus("Credential mismatch", Color.RED);
            JOptionPane.showMessageDialog(this, "Credential mismatch. Entered credentials do not match.", "Error", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "JDBC Driver not found.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            updateConnectionStatus("Connection failed", Color.RED);
            JOptionPane.showMessageDialog(this, "SQL Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void disconnectFromDatabase() {
        try {
            if (c != null && !c.isClosed()) {
                c.close();
                updateConnectionStatus("Not connected", Color.LIGHT_GRAY);
                c = null;
            } else {
                JOptionPane.showMessageDialog(this, "No active connection to disconnect.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            updateConnectionStatus("Failed to disconnect", Color.RED);
            JOptionPane.showMessageDialog(this, "SQL Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Executes an SQL command.
     * For the accountant application, only SELECT queries are allowed.
     */
    public void executeSQLCommand() {
        String sql = sqlCommandArea.getText().trim();
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an SQL command.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Enforce that only SELECT queries are allowed
        if (!sql.toLowerCase().startsWith("select")) {
            JOptionPane.showMessageDialog(this, "Only SELECT queries are allowed in the accountant application.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (c == null) {
            JOptionPane.showMessageDialog(this, "No active connection. Please connect to the database first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try (PreparedStatement pstmt = c.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            DefaultTableModel model = buildTableModel(rs);
            resultTable.setModel(model);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "SQL Error: " + ex.getMessage(), "SQL Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("SQL Error: " + ex.getMessage());
        }
    }

    /**
     * Builds a non-editable DefaultTableModel from the given ResultSet.
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AccountantGUI::new);
    }
}
