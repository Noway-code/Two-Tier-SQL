import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Project3GUI extends JFrame implements ActionListener {
    // Connection Panel components
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton connectButton;
    private final JButton disconnectButton;

    // Command Panel components
    private final JTextArea sqlCommandArea;
    private final JButton executeButton;
    private final JButton clearButton;

    // Result Panel components
    private final JTextArea resultArea;
    private final JButton exitButton; // Exit button

    String url = "jdbc:mysql://localhost:3306/project3";

    public Project3GUI() {
        setTitle("Project 3 - Client Application");
        setSize(1000, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Build Connection Panel
        JPanel connectionPanel = new JPanel(new FlowLayout());
        connectionPanel.setBorder(BorderFactory.createTitledBorder("Database Connection"));
        connectionPanel.add(new JLabel("Username:"));
        usernameField = new JTextField(10);
        connectionPanel.add(usernameField);
        connectionPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField(10);
        connectionPanel.add(passwordField);

        connectButton = new JButton("Connect");
        connectButton.addActionListener(this);
        connectionPanel.add(connectButton);

        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(this);
        connectionPanel.add(disconnectButton);

        add(connectionPanel, BorderLayout.NORTH);

        // Build Command Panel
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

        // Build Result Panel
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder("SQL Result"));
        resultArea = new JTextArea(10, 50);
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultArea.setLineWrap(false);
        resultPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        // Create a bottom panel to hold the result panel and the Exit button
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(resultPanel, BorderLayout.CENTER);

        exitButton = new JButton("Exit");
        exitButton.addActionListener(this);
        // Add some padding so the exit button is not flush against the border
        JPanel exitPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        exitPanel.add(exitButton);
        bottomPanel.add(exitPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
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

    public Connection c;

    public void connectToDatabase() {
        // Establish JDBC Connection
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            if (c != null && !c.isClosed()) {
                resultArea.append("\nAlready connected.");
                return;
            }
            c = DriverManager.getConnection(url, username, password);
            resultArea.setText("Connected as " + username);
            System.out.println("Connected as " + username + " to " + url + "\n");
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found: " + e.getMessage());
            resultArea.setText("JDBC Driver not found.");
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            resultArea.setText("SQL Error: " + e.getMessage());
        }
    }

    public void disconnectFromDatabase() {
        try {
            if (c != null && !c.isClosed()) {
                c.close();
                resultArea.setText("Disconnected from database.");
                c = null;
            } else {
                resultArea.setText("No active connection to disconnect.");
            }
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }
    }

    public void executeSQLCommand() {
        String sql = sqlCommandArea.getText().trim();
        if (sql.isEmpty()) {
            resultArea.setText("Please enter an SQL command.");
            return;
        }
        if (c == null) {
            resultArea.setText("No active connection. Please connect to the database first.");
            return;
        }
        try (Statement stmt = c.createStatement()) {
            // Check if the SQL starts with "select" (case-insensitive)
            if (sql.toLowerCase().startsWith("select")) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    String tableOutput = resultSetToTableString(rs);
                    resultArea.setText(tableOutput);
                }
            } else {
                int updateCount = stmt.executeUpdate(sql);
                resultArea.setText("Command executed successfully. Rows affected: " + updateCount);
            }
        } catch (SQLException ex) {
            resultArea.setText("SQL Error: " + ex.getMessage());
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
