package com.kcl.osc.imageprocessor;

import java.io.File;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.Random;

import javax.imageio.ImageIO;


public class TestApplication extends Application{
    private static ArrayList<Image> STImage = new ArrayList<>();
    private static ArrayList<Image> MTImage = new ArrayList<>();

    public static void main(String[] args){
        launch(args);
    }

    private static Color[][] getPixelData(Image image) {
		PixelReader pr = image.getPixelReader();
		Color[][] pixels = new Color[(int) image.getWidth()][(int) image.getHeight()];
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				pixels[i][j] = pr.getColor(i, j);
			}
		}

		return pixels;
	}

    @Override
    public void start(Stage stage) throws Exception{
        Boolean isIdentical = true;

        STImage.add(new Image("file:low.png"));
        MTImage.add(new Image("file:low.png_filtered.png"));

        for (int i = 0; i < STImage.size(); i++) {
            Color[][] imageST = getPixelData(STImage.get(i));
            Color[][] imageMT = getPixelData(MTImage.get(i));

            for (int j = 0; j < imageST.length; j++) {
                for (int k = 0; k < imageST[0].length; k++) {
                    if (!imageST[j][k].equals(imageMT[j][k])) {
                        System.out.println("Pixel at (" + j + ", " + k + ") is not identical.");
                        isIdentical = false;
                    }
                }
            }
            System.out.println(imageST.length + "x" + imageST[0].length);
        }


        if (isIdentical) {
            System.out.println("Identical");
        } else {
            System.out.println("Not Identical");
        }

    Platform.exit();
    }
}
