package com.kcl.osc.imageprocessor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ImageProcessorApplicationMT extends Application {

	/**
	 * Change this constant to change the filtering operation. Options are
	 * IDENTITY, EDGE, BLUR, SHARPEN, EMBOSS, EDGE, GREY
	 */
	private static final String filter = "BLUR";

	/**
	 * Set this boolean to false if you do NOT wish the new images to be
	 * saved after processing.
	 */
	private static final boolean saveNewImages = true;

	private static final Integer MAXIMUM_NUMBER_OF_THREADS = 20;

	private static final Integer SLICE_HEIGHT = 30;

	ImageProcessorPool imageProcessorPool;

	@Override
	public void start(Stage stage) throws Exception {
		// instantiate the image processor pool
		imageProcessorPool = new ImageProcessorPool(MAXIMUM_NUMBER_OF_THREADS);

		// gets the images from the 'img' folder.
		ArrayList<ImageInfo> images = findImages();

		System.out.println("Working.");

		for (int i = 0; i < images.size(); i++) {
			Image imageToProcess = images.get(i).getImage();
			ArrayList<Color[][]> imagesToProcess = sliceImage(getPixelDataExtended(imageToProcess), SLICE_HEIGHT);

			// start time for processing this image
			long startTime = System.nanoTime();

			// for each image slice creates and runs an ImageProcessor to process the image.
			for (int j = 0; j < imagesToProcess.size(); j++) {
				imageProcessorPool.submit(new ImageProcessorMT(imagesToProcess.get(j), filter, j));
			}

			imageProcessorPool.start();
			imageProcessorPool.run();
			imageProcessorPool.join();

			// end time for processing this image
			long endTime = System.nanoTime();
			System.out.println(images.get(i).getFilename() + ": " + (endTime - startTime) / 1000000 + "ms");

			if (saveNewImages) {
				// get the filtered slices from the ImageProcessorPool
				Map<Integer, Color[][]> filteredPixels = imageProcessorPool.getFilteredPixels();

				WritableImage filteredImage = new WritableImage((int) imageToProcess.getHeight(),
						(int) imageToProcess.getHeight());
				PixelWriter pixelWriter = filteredImage.getPixelWriter();

				// write the filtered slices to the new image
				Integer rowIndex = 0;
				for (int j = 0; j < filteredPixels.size(); j++) {
					Color[][] filteredPixelsArray = filteredPixels.get(j);
					for (int l = 0; l < filteredPixelsArray[0].length; l++) {
						for (int k = 0; k < filteredPixelsArray.length; k++) {
							pixelWriter.setColor(k, rowIndex, filteredPixelsArray[k][l]);
						}
						rowIndex++;
					}
				}

				// save the new image
				saveImage(filteredImage, images.get(i).getFilename() + "_filtered.png");
			}
			imageProcessorPool.quit();
		}

		System.out.println("Done.");

		// Kill this application
		Platform.exit();
	}

	/**
	 * Slice the image into smaller images.
	 * 
	 * @param image       The image to slice.
	 * @param sliceHeight The pixel height of each slice.
	 * 
	 * @return An ArrayList of Color[][] arrays.
	 */
	private ArrayList<Color[][]> sliceImage(Color[][] image, Integer sliceHeight) {
		ArrayList<Color[][]> slicedImages = new ArrayList<Color[][]>();

		if (sliceHeight > 3 && image.length - 2 > sliceHeight) {

			Integer height = (int) image.length;
			Integer numberOfSlices = Math.floorDiv(height, sliceHeight - 2) - 1;
			Integer width = (int) image[0].length;

			for (int h = 0; h < numberOfSlices; h++) {
				Color[][] slicedImage = new Color[width][sliceHeight];
				for (int i = 0; i < width; i++) {
					for (int j = 0; j < sliceHeight; j++) {
						slicedImage[i][j] = image[i][h * (sliceHeight - 2) + j];
					}
				}
				slicedImages.add(slicedImage);
			}
			if (height % sliceHeight != 0) {
				Color[][] slicedImage = new Color[width][height - numberOfSlices * (sliceHeight - 2)];
				for (int h = 0; h < width; h++) {
					for (int j = 0; j < height - numberOfSlices * (sliceHeight - 2); j++) {
						slicedImage[h][j] = image[h][numberOfSlices * (sliceHeight - 2) + j];
					}
				}
				slicedImages.add(slicedImage);
			}
			return slicedImages;
		} else {
			System.out.println("Slice height is too small or too large for this image");
			slicedImages.add(image);
			return slicedImages;
		}
	}

	/**
	 * This method expects all of the images that are to be processed to
	 * be in a folder called img that is in the current working directory.
	 * In Eclipse, for example, this means the img folder should be in the project
	 * folder (alongside src and bin).
	 * 
	 * @return Info about the images found in the folder.
	 */
	private ArrayList<ImageInfo> findImages() {
		ArrayList<ImageInfo> images = new ArrayList<ImageInfo>();
		Collection<File> files = listFileTree(new File("img"));
		for (File f : files) {
			if (f.getName().startsWith(".")) {
				continue;
			}
			Image img = new Image("file:" + f.getPath());
			ImageInfo info = new ImageInfo(img, f.getName());
			images.add(info);
		}
		return images;
	}

	private static Collection<File> listFileTree(File dir) {
		Set<File> fileTree = new HashSet<File>();
		if (dir.listFiles() == null)
			return fileTree;
		for (File entry : dir.listFiles()) {
			if (entry.isFile())
				fileTree.add(entry) /* */;
			else
				fileTree.addAll(listFileTree(entry));
		}
		return fileTree;
	}

	/**
	 * Gets the pixel data from the image but with a one-pixel border added.
	 * 
	 * @param image the image to get the pixel data from
	 * @return The pixel data.
	 */
	private Color[][] getPixelDataExtended(Image image) {
		Color[][] extended = new Color[(int) image.getWidth() + 2][(int) image.getHeight() + 2];

		Color borderColor = new Color(0.5, 0.5, 0.5, 1.0);

		PixelReader pr = image.getPixelReader();

		for (int i = 0; i < (int) image.getWidth(); i++) {
			for (int j = 0; j < (int) image.getHeight(); j++) {
				extended[i + 1][j + 1] = pr.getColor(i, j);
			}
		}

		// for top and bottom of the image, add a one-pixel border
		for (int x = 0; x < image.getWidth() + 2; x++) {
			extended[x][0] = borderColor;
			extended[x][(int) image.getHeight() + 1] = borderColor;
		}
		// for left and right of the image, add a one-pixel border
		for (int y = 0; y < image.getHeight() + 2; y++) {
			extended[0][y] = borderColor;
			extended[(int) image.getWidth() + 1][y] = borderColor;
		}
		return extended;
	}

	/**
	 * Save the image to a file.
	 * 
	 * @param image    the image to save
	 * @param fileName the name of the file to save to
	 */
	private void saveImage(Image image, String fileName) {
		File newFile = new File(fileName);

		try {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", newFile);
		} catch (Exception s) {
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

	/**
	 * Simply class to hold an Image and its filename.
	 * 
	 * @author iankenny
	 *
	 */
	private static class ImageInfo {
		private Image image;
		private String filename;

		public ImageInfo(Image image, String filename) {
			this.image = image;
			this.filename = filename;
		}

		public Image getImage() {
			return image;
		}

		public String getFilename() {
			return filename;
		}
	}
}
