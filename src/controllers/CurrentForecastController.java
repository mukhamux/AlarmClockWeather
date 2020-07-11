package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import web.Loader;

import java.io.IOException;

public class CurrentForecastController {
    @FXML
    private VBox vbox;

    @FXML
    private Label labelForImage;

    private static String information;

    @FXML
    void initialize() {}

    void getInformation() throws IOException, ParseException {
        JSONObject json = Controller.parse(information);
        JSONObject main = (JSONObject) json.get("main");
        JSONArray weather = (JSONArray) json.get("weather");
        JSONObject wind = (JSONObject) json.get("wind");

        double temperature = (double) main.get("temp") - 273;
        double feelsLike = (double) main.get("feels_like") - 273;
        JSONObject commonWeather = (JSONObject) weather.get(0);
        String summary = (String) commonWeather.get("description");
        double speed;

        try {
            speed = (long) wind.get("speed");
        } catch (ClassCastException e) {
            speed = (double) wind.get("speed");
        }

        /* загружаем картинку и устанавливаем в 4 поля соответствующие значения */
        String imageName = commonWeather.get("icon") + "@2x.png";
        ImageView image = Loader.loadImage(imageName);
        labelForImage.setGraphic(image);

        ((Text) (vbox.getChildren().get(0))).setText(Controller.decimalFormat.format(temperature) + " °C");
        ((Text) (vbox.getChildren().get(1))).setText(Controller.decimalFormat.format(feelsLike) + " °C");
        ((Text) (vbox.getChildren().get(2))).setText(Controller.decimalFormat.format(speed) + " м/с");
        ((Text) (vbox.getChildren().get(3))).setText(summary);
    }

    public void setInformation(String information) throws IOException, ParseException {
        CurrentForecastController.information = information;
        getInformation();
    }
}
