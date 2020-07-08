package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import jobs.SimpleJob;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.*;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Controller {
    @FXML
    private TextField cityNameTextField;

    @FXML
    private Button currentWeatherButton;

    @FXML
    private GridPane gridPane;

    @FXML
    private Button setAlarmButton;

    @FXML
    private Button forecastWeatherButton;

    protected static final DecimalFormat df = new DecimalFormat("0.0");
    protected final static String apiCode = "67ff01ef1bd158284e098eba0512fb5d";
    protected final static String weekForecastHeadSearch = "http://api.openweathermap.org/data/2.5/forecast?q=";
    private int extraCells = 0;
    private static String colorForChosenCells = "#ff7d7d;";
    private String[] times = {"00:00", "03:00", "06:00", "09:00", "12:00", "15:00", "18:00", "21:00"};
    MediaPlayer mediaPlayer;


    @FXML
    void initialize() {
        for (int i = 1; i < 9; i++) {
            Label label = new Label(times[i-1]);
            label.setFont(Font.font(14));
            GridPane.setHalignment(label, HPos.CENTER);
            gridPane.add(label, 0, i);
        }

        cityNameTextField.setOnMouseClicked(mouseEvent -> cityNameTextField.setStyle("-fx-border-color: default;"));

        currentWeatherButton.setOnAction(value -> {
            if (!cityNameTextField.getText().isEmpty()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("..\\fxml\\currentForecast.fxml"));
                Parent root = null;
                CurrentForecastController controller;
                try {
                    root = loader.load();
                    controller = loader.getController();
                    controller.setCity(cityNameTextField.getText().replace(' ', '+'));
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }

                Stage stage = new Stage();
                stage.setTitle("Погода в городе " + cityNameTextField.getText());
                stage.setScene(new Scene(root));
                stage.showAndWait();
            } else {
                setEmptySearchField();
            }
        });

        setAlarmButton.setOnAction(actionEvent -> {
            Media song = new Media(Paths.get("src/music/TakeMeFaster.mp3").toUri().toString());
            mediaPlayer = new MediaPlayer(song);
            mediaPlayer.play();

            List<String> times = new ArrayList<>();
            String tmpTime;
            Calendar scheduleTime;
            Date currentTime;
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy");

            for (Node node : gridPane.getChildren()) {
                if (node.getStyle().contains("-fx-background-color: " + colorForChosenCells)) {
                    tmpTime = timeFromGridPane(node);

                    currentTime = new Date();
                    scheduleTime = new GregorianCalendar();

                    scheduleTime.roll(Calendar.DAY_OF_MONTH, GridPane.getColumnIndex(node)-1);

                    scheduleTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tmpTime.substring(0, 2)));
                    scheduleTime.set(Calendar.MINUTE, Integer.parseInt(tmpTime.substring(3, 5)));
                    scheduleTime.set(Calendar.SECOND, 0);

                    if (scheduleTime.getTime().after(currentTime)) {
                        times.add(dateFormat.format(scheduleTime.getTime()));
                    }
                }
            }
            System.out.println(times);
            SchedulerFactory schedFact = new StdSchedulerFactory();
            try {
                Scheduler sched = schedFact.getScheduler();
                sched.start();
                JobBuilder jobBuilder = JobBuilder.newJob(SimpleJob.class);

                JobDetail job = jobBuilder.withIdentity("SimpleJob", "group jobs").build();
                SimpleTrigger trigger = newTrigger().withIdentity("test trigger")
                                                    .startNow()
                                                    .withSchedule(simpleSchedule().withIntervalInSeconds(3).repeatForever()).build();
                sched.scheduleJob(job, trigger);

            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        });

        forecastWeatherButton.setOnAction(value -> {
            if (!cityNameTextField.getText().isEmpty()) {
                try {
                    String response = sendGET(weekForecastHeadSearch + cityNameTextField.getText().replace(' ', '+') + "&appid=" + apiCode);
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
        cityNameTextField.setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
        cityNameTextField.setPromptText("Введите город!");
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

    private void fillTable(String information) throws ParseException, FileNotFoundException {
        gridPane.getChildren().removeIf(node -> GridPane.getColumnIndex(node) != null && GridPane.getColumnIndex(node) != 0);
        JSONObject json = parse(information);
        JSONArray list = (JSONArray) json.get("list");
        int nextHandlingElement = 0;

        for (int i = 1; i < 6; ++i) {
            nextHandlingElement = fillColumn(i, nextHandlingElement, list);
        }
    }

    private int fillColumn(int columnIndex, int handlingElement, JSONArray information) throws FileNotFoundException {
        final String imageURL = "http://openweathermap.org/img/wn/";
        int startRow;
        double temperature;
        DateTimeFormatter formatterRead = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatterWrite = DateTimeFormatter.ofPattern("EEEE, dd MMMM");
        JSONObject period = (JSONObject) information.get(handlingElement++);

        if (columnIndex == 1) {
            String dtText = (String) period.get("dt_txt");
            LocalDateTime dateTime = LocalDateTime.parse(dtText, formatterRead);
            startRow = dateTime.getHour() / 3;
            extraCells = startRow;
        } else {
            startRow = 0;
        }

        String dtText = (String) period.get("dt_txt");
        LocalDateTime dateTime = LocalDateTime.parse(dtText, formatterRead);
        Label label = new Label(dateTime.format(formatterWrite));
        label.setFont(Font.font(14));
        GridPane.setHalignment(label, HPos.CENTER);
        gridPane.add(label, columnIndex, 0);

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
            JSONArray weather = (JSONArray) period.get("weather");
            JSONObject elemInWeather = (JSONObject) weather.get(0);
            ImageView imageView = new ImageView(imageURL + elemInWeather.get("icon") + "@2x.png");
            imageView.setFitHeight(45);
            imageView.setFitWidth(45);
            text.setGraphic(imageView);

            text.setOnMouseEntered(mouseEvent -> {
                int row = GridPane.getRowIndex(text);
                int column = GridPane.getColumnIndex(text);
                gridPane.getChildren().get(row).setStyle("-fx-border-color: red");
                if (column == 1) {
                    gridPane.getChildren().get(column*9).setStyle("-fx-border-color: red");
                } else {
                    gridPane.getChildren().get(column*9 - extraCells).setStyle("-fx-border-color: red");
                }
            });
            text.setOnMouseExited(mouseEvent -> {
            int row = GridPane.getRowIndex(text);
            int column = GridPane.getColumnIndex(text);
            gridPane.getChildren().get(row).setStyle("-fx-border-color: default");
                if (column == 1) {
                    gridPane.getChildren().get(column*9).setStyle("-fx-border-color: default");
                } else {
                    gridPane.getChildren().get(column*9 - extraCells).setStyle("-fx-border-color: default");
                }
            });
            text.setCursor(Cursor.HAND);
            text.setOnMouseClicked(mouseEvent -> {
                if (text.getStyle().contains(colorForChosenCells)) {
                    text.setStyle("-fx-background-color: default");
                } else {
                    text.setStyle("-fx-background-color: " + colorForChosenCells);
                }
            });

            period = (JSONObject) information.get(handlingElement++);
            gridPane.add(text, columnIndex, i);
        }

        return handlingElement-1;
    }

    private String timeFromGridPane(Node node) {
        return ((Label) gridPane.getChildren().get(GridPane.getRowIndex(node))).getText();
    }
}

