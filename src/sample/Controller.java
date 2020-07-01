package sample;

import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Controller {
    @FXML
    private GridPane grid;

    @FXML
    private Button searchButton;

    @FXML
    private Button setAlarm;

    @FXML
    private TextField cityName;

    @FXML
    private Text sensationTemperature;

    @FXML
    private AnchorPane workList;

    @FXML
    private Text windSpeed;

    @FXML
    private Text realTemperature;

    @FXML
    private Text commonState;

    @FXML
    private Text text1_0;

    @FXML
    private Text text1_1;

    @FXML
    private Text text1_2;

    @FXML
    private Text text1_3;

    @FXML
    private Text text1_4;

    @FXML
    private Text text1_5;

    @FXML
    private Text text1_6;

    @FXML
    private Text text1_7;

    @FXML
    private Text text2_0;

    @FXML
    private Text text2_1;

    @FXML
    private Text text2_2;

    @FXML
    private Text text2_3;

    @FXML
    private Text text2_4;

    @FXML
    private Text text2_5;

    @FXML
    private Text text2_6;

    @FXML
    private Text text2_7;

    private static final DecimalFormat df = new DecimalFormat("0.0");
    private static
    final String apiCode = "67ff01ef1bd158284e098eba0512fb5d";
    final String weekForecastHeadSearch = "http://api.openweathermap.org/data/2.5/forecast?q=";
    final String currentForecastHeadSearch = "http://api.openweathermap.org/data/2.5/weather?q=";
    private static int informationCounter = 0;
    private final List<List<Text>> tableOfText = new ArrayList<>();


    @FXML
    void initialize() {
        workList.setVisible(false);
        tableOfText.add(Arrays.asList(text1_0, text1_1, text1_2, text1_3, text1_4, text1_5, text1_6, text1_7));
        tableOfText.add(Arrays.asList(text2_0, text2_1, text2_2, text2_3, text2_4, text2_5, text2_6, text2_7));

        cityName.setOnMouseClicked(mouseEvent -> cityName.setStyle("-fx-border-color: default;"));

        searchButton.setOnAction(value -> {
            if (!cityName.getText().isEmpty()) {
                String city = cityName.getText().replace(' ', '+');
                try {
                    String currentWeatherResponse = sendGET(currentForecastHeadSearch + city + "&appid=" + apiCode);
                    String weekWeatherResponse = sendGET(weekForecastHeadSearch + city + "&appid=" + apiCode);

                    fillCurrentParameters(currentWeatherResponse);
                    fillForecastTable(weekWeatherResponse);

                    workList.setVisible(true);
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
            } else {
                setEmptySearchField();
            }
        });

        List<String> times = new ArrayList<>();
        setAlarm.setOnAction(value -> {
//            SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy");
            if (!cityName.getText().isEmpty()) {
                String tmpTime;
                Calendar scheduleTime;
                Date currentTime;

                for (Node node : grid.getChildren()) {
                    if (node.getStyle().contains("-fx-fill")) {
                        tmpTime = timeFromGridPane(node);

                        currentTime = new Date();
                        scheduleTime = new GregorianCalendar();

                        if (grid.getColumnIndex(node) == 2) {
                            scheduleTime.roll(Calendar.DAY_OF_MONTH, 1);
                        }

                        scheduleTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tmpTime.substring(0, 2)));
                        scheduleTime.set(Calendar.MINUTE, Integer.parseInt(tmpTime.substring(3, 5)));
                        scheduleTime.set(Calendar.SECOND, 0);
                        if (scheduleTime.getTime().after(currentTime)) {
                            times.add(dateFormat.format(scheduleTime.getTime()));
                        }
                    }
                }

            } else {
                setEmptySearchField();
            }
        });
    }

    private void setEmptySearchField() {
        cityName.setStyle("-fx-text-box-border: #B22222; -fx-focus-color: #B22222;");
        cityName.setPromptText("Необходимо ввести город!");
    }

    private static String sendGET(String path) throws IOException {
        URL request = new URL(path);
        HttpURLConnection connection = (HttpURLConnection) request.openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = input.readLine();
            input.close();
            return response;
        } else {
            return "GET request not worked";
        }
    }

    private void fillCurrentParameters(String information) throws ParseException {
        JSONObject json = parse(information);
        JSONObject main = (JSONObject) json.get("main");
        JSONArray weather = (JSONArray) json.get("weather");
        JSONObject wind = (JSONObject) json.get("wind");

        double realTemperatureValue = (double) main.get("temp") - 273;
        double sensationTemperatureValue = (double) main.get("feels_like") - 273;
        JSONObject commonWeather = (JSONObject) weather.get(0);
        String descriptionWeather = (String) commonWeather.get("description");
        double speed;
        try {
            speed = (long) wind.get("speed");
        } catch (ClassCastException e) {
            speed = (double) wind.get("speed");
        }

        realTemperature.setText(df.format(realTemperatureValue) + " °C");
        sensationTemperature.setText(df.format(sensationTemperatureValue) + " °C");
        commonState.setText(descriptionWeather);
        windSpeed.setText(df.format(speed) + " м/с");
    }

    private void fillForecastTable(String information) throws ParseException {
        grid.getChildren().removeIf(node -> grid.getColumnIndex(node) != null); // удаляем все ячейки кроме первого столбца, по дефолту null только у нулевого индекса.

        handleColumn(1, information);
        handleColumn(2, information);
        for (Node node : grid.getChildren()) {
            if (grid.getColumnIndex(node) != null){
                node.setOnMouseClicked(mouseEvent -> node.setStyle("-fx-background-color: #a460ff; -fx-fill: #a460ff"));
            }
        }
        informationCounter = 0;
    }

    JSONObject parse(String information) throws ParseException {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(information);
    }

    private void handleColumn(int columnIndex, String information) throws ParseException {
        JSONObject json = parse(information);
        JSONArray list = (JSONArray) json.get("list");

        JSONObject period = (JSONObject) list.get(informationCounter++);
        String dtText = (String) period.get("dt_txt");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(dtText, formatter);
        double temperature;
        int startRow = columnIndex == 2 ? 0 : dateTime.getHour() / 3;

        for (int i = startRow; i < 8; i++) {
            period = (JSONObject) list.get(informationCounter++);
            try {
                temperature = (long) ((JSONObject) period.get("main")).get("temp");
            } catch (ClassCastException e)  {
                temperature = (double) ((JSONObject) period.get("main")).get("temp");
            }
            tableOfText.get(columnIndex-1).get(i).setText(df.format(temperature-273));
            tableOfText.get(columnIndex-1).get(i).setCursor(Cursor.HAND);
            tableOfText.get(columnIndex-1).get(i).setWrappingWidth(35);
            tableOfText.get(columnIndex-1).get(i).setTextAlignment(TextAlignment.CENTER);
            grid.add(tableOfText.get(columnIndex-1).get(i), columnIndex, i);
        }
    }

    private String timeFromGridPane(Node node) {
        return ((Text) grid.getChildren().get(grid.getRowIndex(node) % 8)).getText();
    }

}

