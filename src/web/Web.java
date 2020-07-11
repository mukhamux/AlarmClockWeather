package web;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;




public class Web {
    public final static String apiCode = "67ff01ef1bd158284e098eba0512fb5d";
    final public static String imageURL = "http://openweathermap.org/img/wn/";
    public final static String currentForecastHeadSearch = "http://api.openweathermap.org/data/2.5/weather?q=";
    public final static String weekForecastHeadSearch = "http://api.openweathermap.org/data/2.5/forecast?q=";

    public static void saveImage(String imageUrl, String destinationFile) throws IOException {
        URL url = new URL(imageUrl);
        InputStream input = url.openStream();
        OutputStream output = new FileOutputStream(destinationFile);

        byte[] b = new byte[2048];
        int length;

        while ((length = input.read(b)) != -1) {
            output.write(b, 0, length);
        }

        input.close();
        output.close();
    }

    public static String requestWeather(TypeRequest type, String city) throws IOException {
        String  mainStringRequest = null;
        if (type == TypeRequest.Current) {
            mainStringRequest = currentForecastHeadSearch;
        } else if (type == TypeRequest.Forecast) {
            mainStringRequest = weekForecastHeadSearch;
        }

        URL request = new URL(mainStringRequest + city.replace(' ', '+') + "&appid=" + apiCode);
        HttpURLConnection connection = (HttpURLConnection) request.openConnection();
        connection.setRequestMethod("GET");

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = input.readLine();
            input.close();
            return response;
        } else {
            return null;
        }
    }
}
