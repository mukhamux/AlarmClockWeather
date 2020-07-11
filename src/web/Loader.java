package web;

import javafx.scene.image.ImageView;

import java.io.File;
import java.io.IOException;

public class Loader {
    /* кэшируем картинку */
    public static ImageView loadImage(String imageName) throws IOException {
        /* если такой картинки ещё нет, скачиваем её */
        if (!new File("src\\images\\" + imageName).exists()) {
            Web.saveImage(Web.imageURL + imageName, "src\\images\\" + imageName);
        }

        File pathToImage = new File("src\\images\\" + imageName);
        return new ImageView(pathToImage.toURI().toString());
    }
}
