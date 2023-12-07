package com.kcl.osc.imageprocessor;

import javafx.scene.paint.Color;

public class ImageProcessorMT implements Runnable {

	private Color[][] pixels;
	private Color[][] filteredPixels;
	private String filterType;
	private int row;
	private Thread.State state;

	/**
	 * Constructor.
	 * @param pixels the image pixels
	 * @param filter The filter to use.
	 * @param save Whether to save the new image or not.
	 * @param row The row of the image to process.
	 */
	public ImageProcessorMT(Color[][] pixels, String filter, int row) {
		this.pixels = pixels;
		this.filterType = filter;
		this.row = row;
	}

	/**
	 * Runs this image processor.
	 */
	@Override
	public void run() {
		state = Thread.State.RUNNABLE;
		this.filter();
		state = Thread.State.TERMINATED;
	}

	/**
	 * get the row of the image to process.
	 * 
	 * @return the row of the image.
	 */
	public int getRow() {
		return row;
	}

	/**
	 * This method get the current state of the thread.
	 * 
	 * @return the current state of the thread.
	 */
	public Thread.State getState() {
		return state;
	}

	/**
	 * This method returns weather the thread is alive or not.
	 * 
	 * @return true if the thread is alive, false otherwise.
	 */
	public final boolean isAlive() {
		if (state == Thread.State.RUNNABLE) {
			return true;
		} else {
			return false;
		}
	};

	/**
	 * This method will join the thread.
	 * 
	 * @throws InterruptedException
	 * 		   if the thread is interrupted.
	 */
	public final void join() throws InterruptedException {
        join(0);
    }

	/**
	 * This method will join the thread.
	 * @param millis the time to wait for the thread to join.
	 * @throws InterruptedException
	 */
	public final synchronized void join(long millis)
    throws InterruptedException {
        long base = System.currentTimeMillis();
        long now = 0;

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (millis == 0) {
            while (isAlive()) {
                wait(0);
            }
        } else {
            while (isAlive()) {
                long delay = millis - now;
                if (delay <= 0) {
                    break;
                }
                wait(delay);
                now = System.currentTimeMillis() - base;
            }
        }
    }

	/**
	 * This method decides whether a filter needs to be applied or not and then
	 * calls appropriate methods to create the new, filtered pixel data.
	 * @return the filtered pixel data.
	 */
	private Color[][] filterImage() {

		if (filterType.equals("GREY")) {
			return applyGreyscale();
		}

		float[][] filter = createFilter(filterType);

		Color[][] filteredImage = applyFilter(pixels, filter);

		return filteredImage;
	}


	/**
	 * Applies the greyscale operation.
	 * @return the new pixel data.
	 */
	private Color[][] applyGreyscale() {

		Color[][] inputPixels = pixels;
		Color[][] outputPixels = new Color[inputPixels.length - 2][inputPixels[0].length - 2];

		for (int i = 1; i < inputPixels.length - 1; i++) {
			for (int j = 1; j < inputPixels[i].length - 1; j++) {

				double red = inputPixels[i][j].getRed();
				double green = inputPixels[i][j].getGreen();
				double blue = inputPixels[i][j].getBlue();

				double newRGB = (red + green + blue) / 3;
				newRGB = clampRGB(newRGB);

				Color newPixel = new Color(newRGB, newRGB, newRGB, 1.0);                
				outputPixels[i - 1][j - 1] = newPixel;
			}
		}

		return outputPixels;
	}

	/**
	 * Applies the required filter to the input pixel data.
	 * @param pixels The input pixel data.
	 * @param filter The filter.
	 * @return The new, filtered pixel data.
	 */
	private Color[][] applyFilter(Color[][] pixels, float[][] filter) {

		Color[][] finalImage = new Color[pixels.length - 2][pixels[0].length - 2];

		for (int i = 1; i < pixels.length -1; i++) {
			for (int j = 1; j < pixels[i].length -1; j++) {

				double red = 0.0;
				double green = 0.0;
				double blue = 0.0;

				for (int k = -1; k < filter.length - 1; k++) {
					for (int l = -1; l < filter[0].length - 1; l++) {
						red += pixels[i + k][j + l].getRed() * filter[1 + k][1 + l];
						green += pixels[i + k][j + l].getGreen() * filter[1 + k][1 + l];
						blue += pixels[i + k][j + l].getBlue() * filter[1 + k][1 + l];
					}
				}

				red = clampRGB(red);
				green = clampRGB(green);
				blue = clampRGB(blue);
				finalImage[i - 1][j - 1] = new Color(red,green,blue,1.0);
			}
		}
		
		return finalImage;
	}

	private void filter() {
		filteredPixels = filterImage();
	}

	/**
	 * Creates the filter.
	 * @param filterType The type of filter required.
	 * @return The filter.
	 */
	private float[][] createFilter(String filterType) {
		filterType = filterType.toUpperCase();
		
		if (filterType.equals("IDENTITY")) {
			return (new float[][] {{0,0,0},{0,1,0},{0,0,0}});
		} else if (filterType.equals("BLUR")) {
			return (new float[][] {{0.0625f,0.125f,0.0625f},{0.125f,0.25f,0.125f},{0.0625f,0.125f,0.0625f}});
		} else if (filterType.equals("SHARPEN")) {
			return (new float[][] {{0,-1,0},{-1,5,-1},{0,-1,0}});
		} else if (filterType.equals("EDGE")) {
			return (new float[][] {{-1,-1,-1},{-1,8,-1},{-1,-1,-1}});
		} else if (filterType.equals("EMBOSS")) {
			return (new float[][] {{-2,-1,0},{-1,0,1},{0,1,2}});
		}
		return null;
	}

	/**
	 * This method ensures that the computations on color values have not 
	 * strayed outside of the range [0,1].
	 * @param RGBValue the value to clamp.
	 * @return The clamped value.
	 */
	protected static double clampRGB(double RGBValue) {
		if (RGBValue < 0.0) {
			return 0.0;
		} else if (RGBValue > 1.0) {
			return 1.0;
		} else {
			return RGBValue;
		}
	}

	/**
	 * Gets the pixel data from the image and remove borders.
	 * @return The pixel data.
	 */
	public Color[][] getFilteredPixels() {
		return filteredPixels;
	}
}
