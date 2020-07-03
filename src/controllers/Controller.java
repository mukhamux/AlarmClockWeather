package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
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
import java.util.ArrayList;
import java.util.List;

public class Controller {
    @FXML
    private TextField cityName;

    @FXML
    private Button searchButton;

    @FXML
    private GridPane grid;

    @FXML
    private Button forecastButton;

    protected static final DecimalFormat df = new DecimalFormat("0.0");
    protected final static String apiCode = "67ff01ef1bd158284e098eba0512fb5d";
    protected final static String weekForecastHeadSearch = "http://api.openweathermap.org/data/2.5/forecast?q=";
    private int extraCells = 0;
    private String[] times = {"00:00", "03:00", "06:00", "09:00", "12:00", "15:00", "18:00", "21:00"};

    @FXML
    void initialize() {
        for (int i = 1; i < 9; i++) {
            Label label = new Label(times[i-1]);
            label.setFont(Font.font(14));
            GridPane.setHalignment(label, HPos.CENTER);
            grid.add(label, 0, i);
        }
        cityName.setOnMouseClicked(mouseEvent -> cityName.setStyle("-fx-border-color: default;"));

        searchButton.setOnAction(value -> {
            if (!cityName.getText().isEmpty()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("..\\fxml\\currentForecast.fxml"));
                Parent root = null;
                CurrentForecastController controller;
                try {
                    root = loader.load();
                    controller = loader.getController();
                    controller.setCity(cityName.getText().replace(' ', '+'));
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }

                Stage stage = new Stage();
                stage.setTitle("Погода в городе " + cityName.getText());
                stage.setScene(new Scene(root));
                stage.showAndWait();
            }
        });

        List<String> times = new ArrayList<>();
        forecastButton.setOnAction(value -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy");
            if (!cityName.getText().isEmpty()) {
//                String tmpTime;
//                Calendar scheduleTime;
//                Date currentTime;
//
//                for (Node node : grid.getChildren()) {
//                    if (node.getStyle().contains("-fx-fill")) {
//                        tmpTime = timeFromGridPane(node);
//
//                        currentTime = new Date();
//                        scheduleTime = new GregorianCalendar();
//
//                        if (grid.getColumnIndex(node) == 2) {
//                            scheduleTime.roll(Calendar.DAY_OF_MONTH, 1);
//                        }
//
//                        scheduleTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tmpTime.substring(0, 2)));
//                        scheduleTime.set(Calendar.MINUTE, Integer.parseInt(tmpTime.substring(3, 5)));
//                        scheduleTime.set(Calendar.SECOND, 0);
//                        if (scheduleTime.getTime().after(currentTime)) {
//                            times.add(dateFormat.format(scheduleTime.getTime()));
//                        }
//                    }
//                }
                try {
                    String response = sendGET(weekForecastHeadSearch + cityName.getText().replace(' ', '+') + "&appid=" + apiCode);
                    fillTable(response);
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
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

    protected static String sendGET(String path) throws IOException {
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

    protected static JSONObject parse(String information) throws ParseException {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(information);
    }

    private void fillTable(String information) throws ParseException {
        grid.getChildren().removeIf(node -> GridPane.getColumnIndex(node) != null && GridPane.getColumnIndex(node) != 0);
        JSONObject json = parse(information);
        JSONArray list = (JSONArray) json.get("list");
        int nextHandlingElement = 0;

        for (int i = 1; i < 6; ++i) {
            nextHandlingElement = fillColumn(i, nextHandlingElement, list);
        }
    }

    private int fillColumn(int columnIndex, int handlingElement, JSONArray information) {
        int startRow;
        double temperature;
        JSONObject period = null;
        DateTimeFormatter formatterRead = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatterWrite = DateTimeFormatter.ofPattern("EEEE, dd MMMM");

        if (columnIndex == 1) {
            period = (JSONObject) information.get(handlingElement++);
            String dtText = (String) period.get("dt_txt");
            LocalDateTime dateTime = LocalDateTime.parse(dtText, formatterRead);
            startRow = dateTime.getHour() / 3;
            extraCells = startRow;
        } else {
            startRow = 0;
            period = (JSONObject) information.get(handlingElement++);
        }

        String dtText = (String) period.get("dt_txt");
        LocalDateTime dateTime = LocalDateTime.parse(dtText, formatterRead);
        Label label = new Label(dateTime.format(formatterWrite));
        label.setFont(Font.font(14));
        GridPane.setHalignment(label, HPos.CENTER);
        grid.add(label, columnIndex, 0);

        for (int i = startRow + 1; i < 9; ++i) {
            try {
                temperature = (long) ((JSONObject) period.get("main")).get("temp");
            } catch (ClassCastException e)  {
                temperature = (double) ((JSONObject) period.get("main")).get("temp");
            }
            Label text = new Label(df.format(temperature - 273));
            text.setFont(Font.font(14));
            text.setMaxWidth(Double.MAX_VALUE);
            text.setMaxHeight(Double.MAX_VALUE);
            text.setAlignment(Pos.CENTER);

            text.setOnMouseEntered(mouseEvent -> {
                int row = GridPane.getRowIndex(text);
                int column = GridPane.getColumnIndex(text);
                grid.getChildren().get(row).setStyle("-fx-border-color: red");
                if (column == 1) {
                    grid.getChildren().get(column*9).setStyle("-fx-border-color: red");
                } else {
                    grid.getChildren().get(column*9 - extraCells).setStyle("-fx-border-color: red");
                }
            });

            text.setOnMouseExited(mouseEvent -> {
            int row = GridPane.getRowIndex(text);
            int column = GridPane.getColumnIndex(text);
            grid.getChildren().get(row).setStyle("-fx-border-color: default");
                if (column == 1) {
                    grid.getChildren().get(column*9).setStyle("-fx-border-color: default");
                } else {
                    grid.getChildren().get(column*9 - extraCells).setStyle("-fx-border-color: default");
                }
        });


            if (handlingElement != 40) {
                period = (JSONObject) information.get(handlingElement++);
            }
            grid.add(text, columnIndex, i);
        }

        return handlingElement;
    }
}

