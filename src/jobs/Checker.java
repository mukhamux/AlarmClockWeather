package jobs;

import controllers.Controller;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.quartz.*;
import web.TypeRequest;
import web.Web;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Checker implements Job {
    private static MediaPlayer mediaPlayer;

    /* то что будет исполнятся по расписанию */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        Date meetingTime = null;
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        Scheduler scheduler = (Scheduler) jobDataMap.get("scheduler");
        String moment = jobExecutionContext.getJobDetail().getKey().getName().substring(0, 19);
        String city = jobExecutionContext.getJobDetail().getKey().getName().substring(19);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

        try {
            meetingTime = formatter.parse(moment);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        /* если выбранный момент ещё не наступил */
        if (new Date().before(meetingTime)) {
            try {
                String response = Web.requestWeather(TypeRequest.Forecast, city);
                JSONObject json = Controller.parse(response);
                JSONArray list = (JSONArray) json.get("list");
                /* проходимся по всему списку и смотрим, если переданное в расписание время совпало с перебираемым */
                /* и в ответе существует ключ rain то включаем музычку и убираем эту job из расписания выполнения */
                for (Object object : list) {
                    if (((JSONObject) object).get("dt_txt").equals(moment) && ((JSONObject) object).containsKey("rain")) {
                        Media song = new Media(Paths.get("src/music/TakeMeFaster.mp3").toUri().toString());
                        mediaPlayer = new MediaPlayer(song);
                        mediaPlayer.setStopTime(Duration.seconds(15));
                        mediaPlayer.play();

                        scheduler.unscheduleJob(jobExecutionContext.getTrigger().getKey());
                        break;
                    }
                }
            } catch (IOException | ParseException | SchedulerException e) {
                e.printStackTrace();
            }
        }
    }
}
