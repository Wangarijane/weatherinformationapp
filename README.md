# Weather Information App

## 1. Project Overview

The Weather Information App is a Java desktop application that provides users with current weather information and a short-term weather forecast for a selected city. The application uses Java Swing to create a graphical user interface and the OpenWeatherMap API to retrieve weather data.

## 2. Features

- Search for weather information by city name.
- Display the current temperature, humidity, wind speed, and weather conditions.
- Display an icon representing the current weather conditions.
- Show a short-term forecast with timestamps, temperatures, and conditions.
- Convert temperature between Celsius and Fahrenheit.
- Convert wind speed between kilometres per hour and miles per hour.
- Record recent successful searches with timestamps.
- Change the application background according to the computer's local time of day.
- Display error messages when a city cannot be found or a weather request fails.

## 3. Technologies Used

- Java
- Java Swing for the graphical user interface
- OpenWeatherMap API for weather data
- Java HTTP Client for sending API requests
- SwingWorker for performing network requests in the background

## 4. Requirements

- Java Development Kit (JDK) that supports the Java HTTP Client API.
- An active OpenWeatherMap API key.
- An internet connection.

## 5. API Key Configuration

The application reads the API key from an environment variable named `OPENWEATHER_API_KEY`. The key is not stored directly in the Java source code.

In Windows PowerShell, load the API key into the environment variable before running the application:

```powershell
$env:OPENWEATHER_API_KEY = "YOUR_API_KEY"
```

Replace `YOUR_API_KEY` with your own key. Do not share the actual key publicly or commit it to a repository.

## 6. How to Compile and Run

Open PowerShell in the project folder.

Compile the application:

```powershell
javac src\WeatherInformationApp.java
```

Run the application:

```powershell
java -cp src WeatherInformationApp
```

The API key must be available in the same PowerShell session used to launch the application.

## 7. How to Use the Application

1. Launch the application.
2. Enter a city name, such as Nairobi, in the search field.
3. Click the Search button.
4. View the current temperature, humidity, wind speed, weather conditions, and icon.
5. Review the short-term forecast displayed in the forecast panel.
6. Select Celsius or Fahrenheit to change the temperature unit.
7. Select kilometres per hour or miles per hour to change the wind speed unit.
8. Search for other cities and view the search history with timestamps.

## 8. Implementation Details

The application uses Java Swing components to display the interface. The Java HTTP Client sends requests to the OpenWeatherMap current-weather and forecast endpoints. The returned JSON data is processed to retrieve the relevant weather information.

SwingWorker performs network operations in the background to help keep the graphical interface responsive. Input validation and error handling provide feedback when a city name is missing, a city cannot be found, or a request fails.

The application uses conversion formulas to display temperature and wind speed in the selected units. It also records successful searches and changes the background colour according to the computer's local time.

## 9. Limitations

The application requires internet access and a valid OpenWeatherMap API key. Weather information depends on the data provided by the API and the availability and limits of the selected API service.

## 10. Conclusion

The Weather Information App demonstrates how Java Swing can be combined with a weather API to retrieve and display current weather information. It incorporates user input, data retrieval, unit conversion, forecast display, search history, and error handling in a desktop application.
