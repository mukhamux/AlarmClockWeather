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
import javafx.scene.text.Font;
import javafx.stage.Stage;
import jobs.Checker;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import web.Loader;
import web.TypeRequest;
import web.Web;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.*;

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

    protected static final DecimalFormat decimalFormat = new DecimalFormat("0.0");
    private int extraCells = 0;
    private static final String colorForChosenCells = "#ff7d7d;";
    private final String[] times = {"00:00", "03:00", "06:00", "09:00", "12:00", "15:00", "18:00", "21:00"};

    @FXML
    void initialize() {
        /* заполняем столбцы со временем */
        for (int i = 1; i < 9; i++) {
            Label label = new Label(times[i-1]);
            label.setFont(Font.font(14));
            GridPane.setHalignment(label, HPos.CENTER);
            gridPane.add(label, 0, i);
        }

        /* если нажимаем на поле для ввода города, красим его рамку в стандартный цвет */
        cityNameTextField.setOnMouseClicked(mouseEvent -> cityNameTextField.setStyle("-fx-border-color: default;"));

        /* при нажатии на кнопку "Текущая погода" */
        currentWeatherButton.setOnAction(value -> {
            /* получаем информацию с weatherMapAPI */
            String information = null;
            try {
                information = Web.requestWeather(TypeRequest.Current, cityNameTextField.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }

            /* если запрос успешен, отображаем маленькое окно с текущей погодой */
            if (information != null) {
                FXMLLoader page = new FXMLLoader(getClass().getResource("..\\fxml\\currentForecast.fxml"));
                Parent root;
                CurrentForecastController controller;
                try {
                    root = page.load();
                    controller = page.getController();
                    controller.setInformation(information);
                    Stage stage = new Stage();
                    stage.setTitle("Погода в городе " + cityNameTextField.getText());
                    stage.setScene(new Scene(root));
                    stage.showAndWait();
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
                /* если запрос вернул null, значит либо неправильно введено название города, либо поле пустое, либо нет интеренета */
            } else {
                setEmptySearchField();
            }
        });

        /* при нажатии на кнопку "Поставить будильник" */
        setAlarmButton.setOnAction(actionEvent -> {
            List<String> times = new ArrayList<>();
            String tmpTime;
            Calendar scheduleTime = null;
            Date currentTime = null;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            /* проходим по сетке и выбираем ячейки, которые выделены цветом */
            for (Node node : gridPane.getChildren()) {
                if (node.getStyle().contains("-fx-background-color: " + colorForChosenCells)) {
                    /* возвращаем выделенной ячейке её родной цвет */
                    node.setStyle("-fx-background-color: default");
                    tmpTime = timeFromGridPane(node);

                    currentTime = new Date();
                    scheduleTime = new GregorianCalendar();

                    scheduleTime.roll(Calendar.DAY_OF_MONTH, GridPane.getColumnIndex(node)-1);

                    scheduleTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tmpTime.substring(0, 2)));
                    scheduleTime.set(Calendar.MINUTE, Integer.parseInt(tmpTime.substring(3, 5)));
                    scheduleTime.set(Calendar.SECOND, 0);
                    /* сравниваем выбранное время в ячейке и текущее, если текущее время больше чем выбранное, то добавляем выбранное в массив*/
                    if (scheduleTime.getTime().after(currentTime)) {
                        times.add(dateFormat.format(scheduleTime.getTime()));
                    }
                }
            }
            /* если массив времени не пустой */
            if (!times.isEmpty()) {
                /* создаём расписание, которое будет чекать дождь на выбранное время */
                SchedulerFactory schedulerFactory = new StdSchedulerFactory();
                Scheduler scheduler;
                long diffInMillies;
                long diff;
                SimpleTrigger trigger;
                try {
                    /* указываем расписанию откуда брать код для выполнения */
                    JobBuilder jobBuilder = JobBuilder.newJob(Checker.class);
                    for (String time : times) {
                        scheduler = schedulerFactory.getScheduler();
                        scheduler.start();
                        String scheduleName = time + cityNameTextField.getText().replace(' ', '+');
                        JobDetail job = jobBuilder.withIdentity(scheduleName, "all jobs").build();
                        job.getJobDataMap().put("scheduler", scheduler);
                        /* вычисляем, сколько часов проходит между текущим моментом и выбранной датой */
                        diffInMillies = Math.abs(currentTime.getTime() - scheduleTime.getTime().getTime());
                        diff = diffInMillies / 3600000;

                        trigger = newTrigger()
                                .withIdentity(scheduleName)
                                .startNow()
                                /* говорим расписанию, сколько надо будет выполнить раз код и с каким диапозоном */
                                .withSchedule(SimpleScheduleBuilder.repeatHourlyForTotalCount((int) diff/3, 3))
                                .build();
                                /* запускаем выполнение */
                        scheduler.scheduleJob(job, trigger);
                    }
                } catch (SchedulerException e) {
                    e.printStackTrace();
                }
            }
        });

        /* при нажатии на кнопку "Прогноз" */
        forecastWeatherButton.setOnAction(value -> {
            String information = null;
            try {
                information = Web.requestWeather(TypeRequest.Current,cityNameTextField.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (information != null) {
                try {
                    String response = Web.requestWeather(TypeRequest.Forecast, cityNameTextField.getText());
                    fillTable(response);
                } catch (IOException | ParseException | InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                setEmptySearchField();
            }
        });
    }

    /* если в поле для ввода города нет текста, то красим его рамку в красный цвет и пишем информативное сообщение */
    private void setEmptySearchField() {
        cityNameTextField.setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
        cityNameTextField.setText("");
        cityNameTextField.setPromptText("Введите город!");
    }

    public static JSONObject parse(String information) throws ParseException {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(information);
    }

    private void fillTable(String information) throws ParseException, IOException, InterruptedException {
        /* отчищаем таблицу от старых значений */
        gridPane.getChildren().removeIf(node -> GridPane.getColumnIndex(node) != null && GridPane.getColumnIndex(node) != 0);
        JSONObject json = parse(information);
        JSONArray list = (JSONArray) json.get("list");
        /* счётчик по элементам JSONArray list */
        int nextHandlingElement = 0;

        /* заполняем столбы сетки, каждый раз будем возвращать индекс элемента в JSONArray list, на котором мы остановились */
        for (int i = 1; i < 6; ++i) {
            nextHandlingElement = fillColumn(i, nextHandlingElement, list);
        }
    }

    private int fillColumn(int columnIndex, int handlingElement, JSONArray information) throws IOException, InterruptedException {
        int startRow;
        double temperature;
        DateTimeFormatter formatterRead = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatterWrite = DateTimeFormatter.ofPattern("EEEE, dd MMMM");
        JSONObject period = (JSONObject) information.get(handlingElement++);

        /* если эта первая колонка, то возможна ситуация, что начнём заполнять её не с начала */
        if (columnIndex == 1) {
            String dtText = (String) period.get("dt_txt");
            LocalDateTime dateTime = LocalDateTime.parse(dtText, formatterRead);
            /* считаем индекс, с которого надо надо заполнять сетку */
            startRow = dateTime.getHour() / 3;
            extraCells = startRow;
        } else {
            startRow = 0;
        }

        /* ставим дату в самую верхнюю ячейку столбца */
        String dtText = (String) period.get("dt_txt");
        LocalDateTime dateTime = LocalDateTime.parse(dtText, formatterRead);
        Label label = new Label(dateTime.format(formatterWrite));
        label.setFont(Font.font(14));
        GridPane.setHalignment(label, HPos.CENTER);
        gridPane.add(label, columnIndex, 0);

        /* проходимся по всему стобику и заполняем его значениями температуры и картиночками */
        for (int i = startRow + 1; i < 9; ++i) {
            try {
                temperature = (long) ((JSONObject) period.get("main")).get("temp");
            } catch (ClassCastException e)  {
                temperature = (double) ((JSONObject) period.get("main")).get("temp");
            }
            Label text = new Label(decimalFormat.format(temperature - 273) + " °C");
            text.setFont(Font.font(14));
            text.setMaxWidth(Double.MAX_VALUE);
            text.setMaxHeight(Double.MAX_VALUE);
            text.setAlignment(Pos.CENTER);
            JSONArray weather = (JSONArray) period.get("weather");
            JSONObject elemInWeather = (JSONObject) weather.get(0);
            String imageName = elemInWeather.get("icon") + "@2x.png";

            ImageView imageView = Loader.loadImage(imageName);
            imageView.setFitHeight(45);
            imageView.setFitWidth(45);
            text.setGraphic(imageView);
            /* устанавливаем изменения на вхождение и выход курсора из ячейки, клик по ячейке и чтобы появлялась ручка при наведении на ячейку */
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

        return handlingElement - 1;
    }

    /* по ячейки в сетке, получаем время, к которому она относится */
    private String timeFromGridPane(Node node) {
        return ((Label) gridPane.getChildren().get(GridPane.getRowIndex(node))).getText();
    }
}

