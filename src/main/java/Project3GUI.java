import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

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
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        add(resultPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "Connect" -> connectToDatabase();
            case "Disconnect" -> disconnectFromDatabase();
            case "Execute" -> {
                // Placeholder for executing the SQL command using JDBC
                String sql = sqlCommandArea.getText();
                resultArea.setText("Executing SQL command:\n" + sql);
            }
            case "Clear" -> {
                sqlCommandArea.setText("");
                resultArea.setText("");
            }
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
             c = DriverManager.getConnection(
                    url, username, password);

            resultArea.setText("Connected as " + username);
            System.out.println("Connected as " + username + " to " + url + "\n");

        }
        catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found: "
                    + e.getMessage());
            resultArea.setText("JDBC Driver not found.");
        }
        catch (SQLException e) {
            System.err.println("SQL Error: "
                    + e.getMessage());
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


    public static void main(String[] args) {
        SwingUtilities.invokeLater(Project3GUI::new);
    }
}
