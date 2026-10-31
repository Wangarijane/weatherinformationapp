/*
 * Weather Information App
 * Course: CS 1103 - Programming 1
 *
 * Description:
 * A Java Swing application that retrieves current weather information
 * and a short-term forecast from the OpenWeatherMap API.
 *
 * Features:
 * - Current temperature, humidity, wind speed, and conditions
 * - Weather icons
 * - Short-term forecast
 * - Celsius/Fahrenheit and km/h/mph conversions
 * - Search history with timestamps
 * - Background colours based on local time
 * - Input validation and API error handling
 *
 * The API key is read from the OPENWEATHER_API_KEY environment variable.
 */

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main class for the Weather Information App.
 * Static fields allow the event handlers to update the GUI components.
 */
public class WeatherInformationApp {

    // Reusable HTTP client for communicating with the weather API.
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    // Format used to display the date and time in the search history.
    private static final DateTimeFormatter HISTORY_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Store the original measurements so changing units does not lose accuracy.
    private static double temperatureCelsius;
    private static double windSpeedMetresPerSecond;
    private static boolean weatherLoaded;

    // Main window and panel used to build the graphical interface.
    private static JFrame window;
    private static JPanel mainPanel;

    // Labels used to display the current weather information.
    private static JLabel cityLabel;
    private static JLabel iconLabel;
    private static JLabel conditionLabel;
    private static JLabel temperatureLabel;
    private static JLabel humidityLabel;
    private static JLabel windLabel;
    private static JLabel statusLabel;

    // Input field, search button, and measurement-unit selectors.
    private static JTextField cityField;
    private static JButton searchButton;
    private static JComboBox<String> temperatureUnitSelector;
    private static JComboBox<String> windUnitSelector;

    // Text areas used to display forecast information and search history.
    private static JTextArea forecastArea;
    private static JTextArea historyArea;

    /**
     * Program entry point.
     * Schedules GUI creation on Swing's Event Dispatch Thread.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(WeatherInformationApp::createWindow);
    }

    /**
     * Creates the main application window and its GUI components.
     */
    private static void createWindow() {
        window = new JFrame("Weather Information App");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(820, 760);
        window.setMinimumSize(new Dimension(700, 650));
        window.setLocationRelativeTo(null);

        // Main panel uses BorderLayout to organise the interface.
        mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(new EmptyBorder(18, 18, 18, 18));

        // Choose a background colour based on the current local time.
        updateBackgroundForTime();

        // Application heading and subtitle.
        JLabel heading = new JLabel(
                "Weather Information App", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 27));
        heading.setForeground(new Color(30, 65, 105));

        JLabel subtitle = new JLabel(
                "Search for current weather and a short-term forecast",
                SwingConstants.CENTER);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        headerPanel.setOpaque(false);
        headerPanel.add(heading);
        headerPanel.add(subtitle);

        // City input field where the user enters a city name.
        cityField = new JTextField();
        cityField.setFont(new Font("Arial", Font.PLAIN, 15));
        cityField.setToolTipText("Enter a city, for example Nairobi");

        // Button that starts a weather search.
        searchButton = new JButton("Search");

        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(cityField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        // Selectors for temperature and wind-speed units.
        temperatureUnitSelector = new JComboBox<>(
                new String[]{"Celsius (°C)", "Fahrenheit (°F)"});

        windUnitSelector = new JComboBox<>(
                new String[]{
                        "Kilometres/hour (km/h)",
                        "Miles/hour (mph)"
                });

        JPanel unitPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 12, 4));
        unitPanel.setOpaque(false);
        unitPanel.add(new JLabel("Temperature:"));
        unitPanel.add(temperatureUnitSelector);
        unitPanel.add(new JLabel("Wind:"));
        unitPanel.add(windUnitSelector);

        // Group the heading, search controls, and unit selectors.
        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.setOpaque(false);
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);
        topPanel.add(unitPanel, BorderLayout.SOUTH);

        // Components used by the current weather section.
        iconLabel = new JLabel("🌤", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));

        cityLabel = new JLabel("Enter a city to begin");
        cityLabel.setFont(new Font("Arial", Font.BOLD, 22));

        conditionLabel = new JLabel(
                "Weather conditions will appear here");
        conditionLabel.setFont(new Font("Arial", Font.PLAIN, 15));

        temperatureLabel = new JLabel("-- °C");
        temperatureLabel.setFont(new Font("Arial", Font.BOLD, 36));

        humidityLabel = new JLabel("Humidity: --");
        windLabel = new JLabel("Wind speed: --");

        // Arrange the weather details vertically.
        JPanel detailsPanel = new JPanel(new GridLayout(4, 1, 4, 4));
        detailsPanel.setOpaque(false);
        detailsPanel.add(cityLabel);
        detailsPanel.add(conditionLabel);
        detailsPanel.add(humidityLabel);
        detailsPanel.add(windLabel);

        // Current weather panel contains the icon, temperature, and details.
        JPanel currentWeatherPanel = new JPanel(
                new BorderLayout(12, 8));
        currentWeatherPanel.setBorder(
                BorderFactory.createTitledBorder("Current Weather"));
        currentWeatherPanel.setBackground(Color.WHITE);
        currentWeatherPanel.add(iconLabel, BorderLayout.WEST);
        currentWeatherPanel.add(temperatureLabel, BorderLayout.CENTER);
        currentWeatherPanel.add(detailsPanel, BorderLayout.SOUTH);

        // Forecast area displays the upcoming forecast entries.
        forecastArea = new JTextArea(
                "Your short-term forecast will appear here after a search.");
        forecastArea.setEditable(false);
        forecastArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        forecastArea.setLineWrap(true);
        forecastArea.setWrapStyleWord(true);
        forecastArea.setRows(6);

        JScrollPane forecastScroll = new JScrollPane(forecastArea);
        forecastScroll.setBorder(
                BorderFactory.createTitledBorder("Short-Term Forecast"));

        // History area records successful city searches and timestamps.
        historyArea = new JTextArea("Search history will appear here.");
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);

        JScrollPane historyScroll = new JScrollPane(historyArea);
        historyScroll.setBorder(
                BorderFactory.createTitledBorder("Search History"));
        historyScroll.setPreferredSize(new Dimension(300, 120));

        // Stack the current weather, forecast, and history sections.
        JPanel contentPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        contentPanel.setOpaque(false);
        contentPanel.add(currentWeatherPanel);
        contentPanel.add(forecastScroll);
        contentPanel.add(historyScroll);

        // Status label provides feedback while requests are processed.
        statusLabel = new JLabel("Enter a city to retrieve live weather.");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(statusLabel, BorderLayout.CENTER);

        // Connect buttons and selectors to their event handlers.
        searchButton.addActionListener(event -> searchWeather());

        // Pressing Enter in the city field also starts a search.
        cityField.addActionListener(event -> searchButton.doClick());

        // Refresh the displayed measurements when units change.
        temperatureUnitSelector.addActionListener(
                event -> displayWeather());
        windUnitSelector.addActionListener(event -> displayWeather());

        // Add the main sections to the window.
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        window.setContentPane(mainPanel);
        window.setVisible(true);
    }

    /**
     * Validates the city and API key, then requests current weather data.
     * SwingWorker keeps the network request from blocking the GUI.
     */
    private static void searchWeather() {
        String city = cityField.getText().trim();

        // Reject an empty city name before sending a request.
        if (city.isEmpty()) {
            showError("Please enter a city name.");
            return;
        }

        // Read the API key from the environment rather than hardcoding it.
        String apiKey = System.getenv("OPENWEATHER_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            showError("The API key was not found. Load OPENWEATHER_API_KEY "
                    + "in this PowerShell terminal, then run the app from it.");
            return;
        }

        // Disable repeated searches while the request is in progress.
        searchButton.setEnabled(false);
        statusLabel.setText("Loading weather for " + city + "...");

        SwingWorker<String, Void> worker = new SwingWorker<>() {

            /**
             * Runs the HTTP request in the background.
             */
            @Override
            protected String doInBackground() throws Exception {
                String encodedCity = URLEncoder.encode(
                        city, StandardCharsets.UTF_8);

                String url =
                        "https://api.openweathermap.org/data/2.5/weather"
                        + "?q=" + encodedCity
                        + "&appid=" + apiKey
                        + "&units=metric";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(
                        request, HttpResponse.BodyHandlers.ofString());

                // Give a helpful message for common API errors.
                if (response.statusCode() == 404) {
                    throw new IllegalArgumentException(
                            "City not found. Check the spelling and try again.");
                }

                if (response.statusCode() == 401) {
                    throw new IllegalArgumentException(
                            "The API key was rejected. Check that it is active.");
                }

                if (response.statusCode() != 200) {
                    throw new IllegalArgumentException(
                            "Weather service returned HTTP "
                                    + response.statusCode() + ".");
                }

                return response.body();
            }

            /**
             * Processes the response and updates the interface.
             * This method runs after the background task completes.
             */
            @Override
            protected void done() {
                searchButton.setEnabled(true);

                try {
                    String json = get();

                    // Extract the required values from the API response.
                    String returnedCity = extractString(json, "name");
                    String condition = extractString(json, "description");
                    String iconCode = extractString(json, "icon");

                    temperatureCelsius = extractNumber(json, "temp");
                    double humidity = extractNumber(json, "humidity");
                    windSpeedMetresPerSecond =
                            extractNumber(json, "speed");

                    // Display the current weather details.
                    cityLabel.setText(returnedCity);
                    conditionLabel.setText(
                            "Conditions: " + capitalize(condition));
                    humidityLabel.setText(
                            String.format("Humidity: %.0f%%", humidity));
                    iconLabel.setText(weatherEmoji(iconCode));

                    // Allow unit selectors to display the retrieved values.
                    weatherLoaded = true;
                    displayWeather();

                    // Record the successful search and refresh the background.
                    addSearchHistory(returnedCity);
                    updateBackgroundForTime();

                    statusLabel.setText(
                            "Live weather loaded. Loading forecast...");

                    // Retrieve the short-term forecast separately.
                    loadForecast(returnedCity, apiKey);

                } catch (Exception ex) {
                    showError(getUsefulMessage(ex));
                }
            }
        };

        worker.execute();
    }

    /**
     * Retrieves and displays up to five forecast entries for the city.
     */
    private static void loadForecast(String city, String apiKey) {
        SwingWorker<String, Void> worker = new SwingWorker<>() {

            /**
             * Sends the forecast request in the background.
             */
            @Override
            protected String doInBackground() throws Exception {
                String encodedCity = URLEncoder.encode(
                        city, StandardCharsets.UTF_8);

                String url =
                        "https://api.openweathermap.org/data/2.5/forecast"
                        + "?q=" + encodedCity
                        + "&appid=" + apiKey
                        + "&units=metric";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(
                        request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new IllegalArgumentException(
                            "Forecast request failed with HTTP "
                                    + response.statusCode() + ".");
                }

                return response.body();
            }

            /**
             * Reads the forecast response and fills the forecast text area.
             */
            @Override
            protected void done() {
                try {
                    String json = get();
                    StringBuilder forecast = new StringBuilder();

                    // Forecast entries are provided at three-hour intervals.
                    // This pattern identifies each entry and its date/time.
                    Pattern entryPattern = Pattern.compile(
                            "\\{\\s*\"dt\"\\s*:\\s*\\d+.*?\"dt_txt\"\\s*:\\s*\"([^\"]+)\"",
                            Pattern.DOTALL);

                    Matcher entryMatcher = entryPattern.matcher(json);
                    int count = 0;

                    // Display no more than five forecast entries.
                    while (entryMatcher.find() && count < 5) {
                        String entry = entryMatcher.group();
                        String dateTime = entryMatcher.group(1);
                        double temp = extractNumber(entry, "temp");

                        String description;

                        // Use a fallback description if it cannot be read.
                        try {
                            description = extractString(entry, "description");
                        } catch (IllegalArgumentException ex) {
                            description = "conditions unavailable";
                        }

                        forecast.append(dateTime)
                                .append("  |  ")
                                .append(String.format("%.1f °C", temp))
                                .append("  |  ")
                                .append(capitalize(description))
                                .append('\n');

                        count++;
                    }

                    // Show a message if the response contains no readable entries.
                    if (count == 0) {
                        forecastArea.setText(
                                "Forecast data was received, but no entries could be read.");
                    } else {
                        forecastArea.setText(forecast.toString());
                    }

                    statusLabel.setText(
                            "Weather and forecast loaded successfully.");

                } catch (Exception ex) {
                    // Keep the current weather visible if only the forecast fails.
                    forecastArea.setText(
                            "Unable to load the forecast. "
                                    + getUsefulMessage(ex));
                    statusLabel.setText(
                            "Current weather loaded; forecast unavailable.");
                }
            }
        };

        worker.execute();
    }

    /**
     * Displays temperature and wind speed in the selected measurement units.
     * Original values are retained to avoid repeated conversion errors.
     */
    private static void displayWeather() {
        // Do nothing until current weather data has been loaded.
        if (!weatherLoaded) {
            return;
        }

        // Convert Celsius to Fahrenheit if the user selected Fahrenheit.
        boolean useFahrenheit =
                "Fahrenheit (°F)".equals(
                        temperatureUnitSelector.getSelectedItem());

        double displayedTemperature = useFahrenheit
                ? temperatureCelsius * 9.0 / 5.0 + 32
                : temperatureCelsius;

        String temperatureUnit = useFahrenheit ? "°F" : "°C";

        temperatureLabel.setText(String.format(
                "%.1f %s", displayedTemperature, temperatureUnit));

        // Convert metres per second to mph or km/h.
        boolean useMph =
                "Miles/hour (mph)".equals(
                        windUnitSelector.getSelectedItem());

        double displayedWind = useMph
                ? windSpeedMetresPerSecond * 2.236936
                : windSpeedMetresPerSecond * 3.6;

        String windUnit = useMph ? "mph" : "km/h";

        windLabel.setText(String.format(
                "Wind speed: %.1f %s", displayedWind, windUnit));
    }

    /**
     * Adds a successful city search and its timestamp to the history area.
     */
    private static void addSearchHistory(String city) {
        String timestamp = LocalDateTime.now().format(HISTORY_TIME);
        String previous = historyArea.getText();

        // Remove the initial placeholder when the first search succeeds.
        if (previous.equals("Search history will appear here.")) {
            previous = "";
        }

        historyArea.setText(
                previous + timestamp + "  |  " + city + "\n");

        // Scroll to the newest history entry.
        historyArea.setCaretPosition(
                historyArea.getDocument().getLength());
    }

    /**
     * Selects a background colour based on the computer's local time.
     */
    private static void updateBackgroundForTime() {
        int hour = LocalDateTime.now().getHour();
        Color background;

        // Use a different colour for each time period.
        if (hour >= 6 && hour < 12) {
            background = new Color(232, 244, 255); // Morning
        } else if (hour >= 12 && hour < 18) {
            background = new Color(255, 245, 220); // Afternoon
        } else if (hour >= 18 && hour < 21) {
            background = new Color(238, 230, 250); // Evening
        } else {
            background = new Color(220, 228, 240); // Night
        }

        // Update the panel if the GUI has already been created.
        if (mainPanel != null) {
            mainPanel.setBackground(background);
            mainPanel.repaint();
        }
    }

    /**
     * Converts the weather icon code into a suitable emoji.
     */
    private static String weatherEmoji(String iconCode) {
        if (iconCode == null) {
            return "🌤";
        }

        if (iconCode.startsWith("01")) {
            return iconCode.endsWith("n") ? "🌙" : "☀";
        }

        if (iconCode.startsWith("02")) {
            return "🌤";
        }

        if (iconCode.startsWith("03") || iconCode.startsWith("04")) {
            return "☁";
        }

        if (iconCode.startsWith("09") || iconCode.startsWith("10")) {
            return "🌧";
        }

        if (iconCode.startsWith("11")) {
            return "⛈";
        }

        if (iconCode.startsWith("13")) {
            return "❄";
        }

        if (iconCode.startsWith("50")) {
            return "🌫";
        }

        // Use a partly cloudy icon if the code is not recognised.
        return "🌤";
    }

    /**
     * Extracts a quoted string value from the API's JSON response.
     *
     * @param json  JSON response text
     * @param field name of the field to find
     * @return the extracted string value
     */
    private static String extractString(String json, String field) {
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"");

        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new IllegalArgumentException(
                "Could not find the weather field: " + field);
    }

    /**
     * Extracts a numeric value from the API's JSON response.
     *
     * @param json  JSON response text
     * @param field name of the numeric field to find
     * @return the extracted numeric value
     */
    private static double extractNumber(String json, String field) {
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(field)
                        + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");

        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }

        throw new IllegalArgumentException(
                "Could not find the numeric field: " + field);
    }

    /**
     * Capitalises the first letter of a text value for display.
     */
    private static String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    /**
     * Extracts a readable error message from an exception.
     */
    private static String getUsefulMessage(Exception ex) {
        Throwable cause = ex.getCause();

        String message = cause == null
                ? ex.getMessage()
                : cause.getMessage();

        return message == null || message.isBlank()
                ? "An unexpected error occurred. Please try again."
                : message;
    }

    /**
     * Displays an error in the status area and a dialog box.
     */
    private static void showError(String message) {
        statusLabel.setText("Could not load weather.");

        JOptionPane.showMessageDialog(
                window,
                message,
                "Weather Request Error",
                JOptionPane.ERROR_MESSAGE);
    }
}