package test;
import org.junit.jupiter.api.Test;
import web.TypeRequest;
import web.Web;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MainTests {
    @Test
    public void checkForWrongWebRequest() throws IOException {
        assertNull(Web.requestWeather(TypeRequest.Current, "Night City"), "Проверка на несуществующий город пошла не по плану");
        assertNull(Web.requestWeather(TypeRequest.Current, "Markarth"), "Проверка на несуществующий город пошла не по плану");
        assertNull(Web.requestWeather(TypeRequest.Current, "Riften"), "Проверка на несуществующий город пошла не по плану");
        assertNull(Web.requestWeather(TypeRequest.Current, "Whiterun"), "Проверка на несуществующий город пошла не по плану");
    }

    @Test
    public void checkForCorrectWebRequest() throws IOException {
        assertNotNull(Web.requestWeather(TypeRequest.Current, "Solitude"), "Проверка на несуществующий город пошла не по плану");
        assertNotNull(Web.requestWeather(TypeRequest.Current, "Los Santos"), "Проверка на несуществующий город пошла не по плану");
        assertNotNull(Web.requestWeather(TypeRequest.Current, "Empire Bay"), "Проверка на несуществующий город пошла не по плану");
        assertNotNull(Web.requestWeather(TypeRequest.Current, "Великий Новгород"), "Проверка на существующий город пошла не по плану");
        assertNotNull(Web.requestWeather(TypeRequest.Current, "Moscow"), "Проверка на существующий город пошла не по плану");
        assertNotNull(Web.requestWeather(TypeRequest.Current, "Paris"), "Проверка на существующий город пошла не по плану");
        assertNotNull(Web.requestWeather(TypeRequest.Current, "Los Angeles"), "Проверка на существующий город пошла не по плану");
        assertNotNull(Web.requestWeather(TypeRequest.Current, "Санкт-Петербург"), "Проверка на существующий город пошла не по плану");
        assertNotNull(Web.requestWeather(TypeRequest.Current, "Североморск"), "Проверка на существующий город пошла не по плану");
    }

}