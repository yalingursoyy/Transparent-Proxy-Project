 import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetSocketAddress;

public class ProxyGUI extends JFrame {
    private ProxyServer server;

    public ProxyGUI() {
        setTitle("Transparent Proxy");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem startMenuItem = new JMenuItem("Start");
        JMenuItem stopMenuItem = new JMenuItem("Stop");
        JMenuItem reportMenuItem = new JMenuItem("Report");
        JMenuItem addHostMenuItem = new JMenuItem("Add host to filter");
        JMenuItem displayFilterMenuItem = new JMenuItem("Display current filtered hosts");
        JMenuItem exitMenuItem = new JMenuItem("Exit");

        fileMenu.add(startMenuItem);
        fileMenu.add(stopMenuItem);
        fileMenu.add(reportMenuItem);
        fileMenu.add(addHostMenuItem);
        fileMenu.add(displayFilterMenuItem);
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);

        setJMenuBar(menuBar);

        JLabel statusLabel = new JLabel("Proxy Server is Stopped...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Serif", Font.BOLD, 20));
        add(statusLabel, BorderLayout.CENTER);

        startMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    server = new ProxyServer(80);
                    server.start();
                    statusLabel.setText("Proxy Server is Running...");
                } catch (IOException ex) {
                    statusLabel.setText("Error starting proxy: " + ex.getMessage());
                }
            }
        });

        stopMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (server != null) {
                    server.stop();
                    statusLabel.setText("Proxy Server is Stopped...");
                }
            }
        });

        reportMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                displayReport();
            }
        });

        addHostMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addHostToFilter();
            }
        });

        displayFilterMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                displayFilterList();
            }
        });

        exitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
    }

    private void displayReport() {
        try {
            File file = new File("proxy_logs.txt");
            if (!file.exists()) {
                JOptionPane.showMessageDialog(this, "No logs available.", "Report", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            BufferedReader br = new BufferedReader(new FileReader(file));
            JTextArea textArea = new JTextArea();
            textArea.read(br, null);
            br.close();
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(500, 400));
            JOptionPane.showMessageDialog(this, scrollPane, "Client Logs", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading logs: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addHostToFilter() {
        String host = JOptionPane.showInputDialog(this, "Enter host to filter:");
        if (host != null && !host.trim().isEmpty()) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter("blocked_hosts.txt", true))) {
                bw.write(host);
                bw.newLine();
                JOptionPane.showMessageDialog(this, "Host added to filter list.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error adding host to filter: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void displayFilterList() {
        try {
            File file = new File("blocked_hosts.txt");
            if (!file.exists()) {
                JOptionPane.showMessageDialog(this, "No hosts are currently filtered.", "Filter List", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            BufferedReader br = new BufferedReader(new FileReader(file));
            JTextArea textArea = new JTextArea();
            textArea.read(br, null);
            br.close();
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(500, 400));
            JOptionPane.showMessageDialog(this, scrollPane, "Filtered Hosts", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading filter list: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ProxyGUI().setVisible(true);
            }
        });
    }
}
