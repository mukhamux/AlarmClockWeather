package controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;

public class CurrentForecastController {
    @FXML
    private VBox vbox;
    private static String city;
    protected final static String currentForecastHeadSearch = "http://api.openweathermap.org/data/2.5/weather?q=";

    @FXML
    void initialize() {}

    void getInformation() throws IOException, ParseException {
        String response = Controller.sendGET(currentForecastHeadSearch + city + "&appid=" + Controller.apiCode);

        JSONObject json = Controller.parse(response);
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

        ((Text) (vbox.getChildren().get(0))).setText(Controller.df.format(temperature) + " °C");
        ((Text) (vbox.getChildren().get(1))).setText(Controller.df.format(feelsLike) + " °C");
        ((Text) (vbox.getChildren().get(2))).setText(Controller.df.format(speed) + " м/с");
        ((Text) (vbox.getChildren().get(3))).setText(summary);
    }

    public void setCity(String city) throws IOException, ParseException {
        CurrentForecastController.city = city;
        getInformation();
    }

}

