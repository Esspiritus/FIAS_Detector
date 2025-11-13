import javax.swing.;
import javax.swing.border.EmptyBorder;
import java.awt.;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AddressValidator extends JFrame {
private static final String TOKEN = "your_token";
private static final String API_URL = "api_url";
private static final String VERSION = "1.0.0";
private static final String DEVELOPER = "DEVELOPER";

private JTextField addressField;
private JTextArea resultArea;
private JButton searchButton;
private JButton clearButton;
private JButton aboutButton;
private JProgressBar progressBar;

public AddressValidator() {
    initializeUI();
}

private void initializeUI() {
    setTitle("ФИАС Детектор " + VERSION);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(600, 500);
    setLocationRelativeTo(null);
    setIconImage(createIcon().getImage());
    
    JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
    mainPanel.setBackground(Color.WHITE);
    
    JPanel inputPanel = createInputPanel();
    mainPanel.add(inputPanel, BorderLayout.NORTH);
    
    JPanel resultPanel = createResultPanel();
    mainPanel.add(resultPanel, BorderLayout.CENTER);
    
    JPanel buttonPanel = createButtonPanel();
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    add(mainPanel);
}

private JPanel createInputPanel() {
    JPanel panel = new JPanel(new BorderLayout(10, 5));
    panel.setBackground(Color.WHITE);

    JLabel titleLabel = new JLabel("Поиск корректного FIAS ID");
    titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
    titleLabel.setForeground(new Color(0, 82, 155));

    JLabel instructionLabel = new JLabel("Введите адрес для проверки:");
    instructionLabel.setFont(new Font("Arial", Font.PLAIN, 12));

    addressField = new JTextField();
    addressField.setFont(new Font("Arial", Font.PLAIN, 14));
    addressField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
    ));

    JPanel titlePanel = new JPanel(new BorderLayout());
    titlePanel.setBackground(Color.WHITE);
    titlePanel.add(titleLabel, BorderLayout.NORTH);
    titlePanel.add(instructionLabel, BorderLayout.SOUTH);

    panel.add(titlePanel, BorderLayout.NORTH);
    panel.add(addressField, BorderLayout.CENTER);
    
    addressField.addActionListener(e -> searchAddress());

    return panel;
}

private JPanel createResultPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(Color.WHITE);

    JLabel resultLabel = new JLabel("Результаты поиска:");
    resultLabel.setFont(new Font("Arial", Font.BOLD, 14));
    resultLabel.setBorder(new EmptyBorder(5, 0, 5, 0));

    resultArea = new JTextArea();
    resultArea.setFont(new Font("Consolas", Font.PLAIN, 12));
    resultArea.setEditable(false);
    resultArea.setLineWrap(true);
    resultArea.setWrapStyleWord(true);
    resultArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
    ));

    JScrollPane scrollPane = new JScrollPane(resultArea);
    scrollPane.setBorder(BorderFactory.createTitledBorder("Информация об адресе"));

    progressBar = new JProgressBar();
    progressBar.setVisible(false);
    progressBar.setIndeterminate(true);

    panel.add(resultLabel, BorderLayout.NORTH);
    panel.add(scrollPane, BorderLayout.CENTER);
    panel.add(progressBar, BorderLayout.SOUTH);

    return panel;
}

private JPanel createButtonPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
    panel.setBackground(Color.WHITE);
    
    searchButton = new JButton("Найти адрес");
    clearButton = new JButton("Очистить");
    aboutButton = new JButton("О программе");
    
    styleButton(searchButton, new Color(76, 175, 80));
    styleButton(clearButton, new Color(244, 67, 54));
    styleButton(aboutButton, new Color(33, 150, 243));

    searchButton.addActionListener(e -> searchAddress());
    clearButton.addActionListener(e -> clearFields());
    aboutButton.addActionListener(e -> showAboutDialog());

    panel.add(aboutButton);
    panel.add(clearButton);
    panel.add(searchButton);

    return panel;
}

private void styleButton(JButton button, Color color) {
    button.setFont(new Font("Arial", Font.BOLD, 12));
    button.setBackground(color);
    button.setForeground(Color.WHITE);
    button.setFocusPainted(false);
    button.setBorderPainted(false);
    button.setOpaque(true);
    button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
    
    button.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseEntered(java.awt.event.MouseEvent evt) {
            button.setBackground(color.darker());
        }
        public void mouseExited(java.awt.event.MouseEvent evt) {
            button.setBackground(color);
        }
    });
}

private void searchAddress() {
    String address = addressField.getText().trim();
    if (address.isEmpty()) {
        JOptionPane.showMessageDialog(this,
                "Пожалуйста, введите адрес для поиска",
                "Внимание",
                JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    progressBar.setVisible(true);
    searchButton.setEnabled(false);
   
    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
        private String result;

        @Override
        protected Void doInBackground() throws Exception {
            String modifiedAddress = modifyAddress(address);
            result = findAddress(modifiedAddress);
            return null;
        }

        @Override
        protected void done() {
            progressBar.setVisible(false);
            searchButton.setEnabled(true);
            resultArea.setText(result);
        }
    };

    worker.execute();
}

private String findAddress(String address) {
    try {
        HttpClient client = HttpClient.newHttpClient();
        String jsonBody = String.format("{\"query\": \"%s\"}", address.replace("\"", "\\\""));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Token " + TOKEN)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return processResponse(response.body());
        } else {
            return " Ошибка запроса: " + response.statusCode() + "\n" +
                    "Проверьте подключение к интернету и API ключ";
        }

    } catch (Exception e) {
        return " Произошла ошибка: " + e.getMessage() + "\n" +
                "Проверьте подключение к интернету";
    }
}

private String processResponse(String responseBody) {
    try {
        int suggestionsIndex = responseBody.indexOf("\"suggestions\"");
        if (suggestionsIndex == -1) {
            return " Адрес не найден в базе данных";
        }

        int arrayStart = responseBody.indexOf('[', suggestionsIndex);
        int arrayEnd = responseBody.indexOf(']', arrayStart);

        if (arrayStart == -1 || arrayEnd == -1) {
            return " Адрес не найден в базе данных";
        }

        String firstSuggestion = responseBody.substring(arrayStart + 1, arrayEnd);
        if (firstSuggestion.trim().isEmpty()) {
            return " Адрес не найден в базе данных";
        }

        String value = extractJsonField(firstSuggestion, "value");
        String fiasId = extractJsonField(firstSuggestion, "fias_id");
        String geoLat = extractJsonField(firstSuggestion, "geo_lat");
        String geoLon = extractJsonField(firstSuggestion, "geo_lon");

        if (value != null && !value.isEmpty()) {
            StringBuilder result = new StringBuilder();
            result.append("АДРЕС НАЙДЕН\n\n");
            result.append("Полный адрес:\n").append(value).append("\n\n");
            result.append("ФИАС ID:\n").append(fiasId != null ? fiasId : "не указан").append("\n\n");
            result.append("Координаты:\n");
            result.append("Широта: ").append(geoLat != null ? geoLat : "не указана").append("\n");
            result.append("Долгота: ").append(geoLon != null ? geoLon : "не указана").append("\n\n");

            return result.toString();
        } else {
            return " Адрес не найден в базе данных";
        }

    } catch (Exception e) {
        return " Ошибка при обработке ответа от сервера";
    }
}

private String extractJsonField(String json, String fieldName) {
    try {
        String searchKey = "\"" + fieldName + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;

        int valueStart = json.indexOf(':', keyIndex) + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) return null;

        char firstChar = json.charAt(valueStart);
        int valueEnd;

        if (firstChar == '"') {
            valueStart++;
            valueEnd = json.indexOf('"', valueStart);
        } else {
            valueEnd = json.indexOf(',', valueStart);
            if (valueEnd == -1) valueEnd = json.indexOf('}', valueStart);
            if (valueEnd == -1) valueEnd = json.length();
        }

        if (valueEnd == -1 || valueEnd <= valueStart) return null;

        return json.substring(valueStart, valueEnd).trim();

    } catch (Exception e) {
        return null;
    }
}

private String modifyAddress(String address) {
    String modified = address
            .replace("корпус", "")
            .replace("квартира", "")
            .replace("a", "")
            .trim();

    modified = modified.replace("дом №", "д.");

    if (modified.contains("квартира") || modified.contains("корпус")) {
        String[] parts = modified.split(",");
        if (parts.length > 1) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) result.append(",");
                result.append(parts[i]);
            }
            return result.toString().trim();
        }
    }

    return modified;
}

private void clearFields() {
    addressField.setText("");
    resultArea.setText("");
    addressField.requestFocus();
}

private void showAboutDialog() {
    String aboutText =
            "<html><body style='width: 300px; text-align: center;'>" +
                    "<h2>Поиск корректного FIAS ID</h2>" +
                    "<p><b>Версия:</b> " + VERSION + "</p>" +
                    "<p><b>Разработчик:</b> " + DEVELOPER + "</p>" +
                    "<hr>" +
                    "<p>Программа для проверки и поиска корректных FIAS ID</p>" +
                    "<p>Функции:</p>" +
                    "<ul style='text-align: left;'>" +
                    "<li>Поиск адресов по ФИАС</li>" +
                    "<li>Получение ФИАС ID</li>" +
                    "<li>Определение координат</li>" +
                    "<li>Валидация адресов</li>" +
                    "</ul>" +
                    "<p>© 2025 Все права защищены</p>" +
                    "</body></html>";

    JOptionPane.showMessageDialog(this, aboutText, "О программе", JOptionPane.INFORMATION_MESSAGE);
}

private ImageIcon createIcon() {
    // Создаем простую иконку программно
    java.awt.Image image = new java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = (Graphics2D) image.getGraphics();
    g2d.setColor(new Color(0, 82, 155));
    g2d.fillRect(0, 0, 32, 32);
    g2d.setColor(Color.WHITE);
    g2d.setFont(new Font("Arial", Font.BOLD, 16));
    g2d.drawString("D", 10, 22);
    g2d.dispose();
    return new ImageIcon(image);
}

public static void main(String[] args) {        
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
        e.printStackTrace();
    }

    SwingUtilities.invokeLater(() -> {
        new AddressValidator().setVisible(true);
    });
}
}
