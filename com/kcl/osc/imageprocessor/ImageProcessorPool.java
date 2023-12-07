package com.kcl.osc.imageprocessor;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.paint.Color;

public class ImageProcessorPool implements Runnable {

    private int poolSize;
    private ImageProcessorMT[] coreProcessThreads;
    private ArrayList<ImageProcessorMT> imageProcessorQueue;
    private Thread.State state;
    private Map<Integer, Color[][]> filteredPixels;

    /**
     * Constructor
     * 
     * @param s the maximum number of threads to be used in the pool
     */
    public ImageProcessorPool(int s) {
        this.poolSize = s;
        coreProcessThreads = new ImageProcessorMT[poolSize];
        imageProcessorQueue = new ArrayList<ImageProcessorMT>();
        filteredPixels = new HashMap<Integer, Color[][]>();
        state = Thread.State.NEW;
        for (int i = 0; i < poolSize; i++) {
            coreProcessThreads[i] = null;
        }
    }
    
    /**
     * This method will start the pool of threads.
     * 
     */
    public void start(){
        state = Thread.State.RUNNABLE;
        for (int i = 0; i < coreProcessThreads.length && imageProcessorQueue.size() > 0; i++) {
            coreProcessThreads[i] = imageProcessorQueue.get(0);
            imageProcessorQueue.remove(0);
            coreProcessThreads[i].run();
        }
    }

    /**
     * This method will let the thread pool running
     */
    @Override
    public void run(){
        while (state == Thread.State.RUNNABLE) {
            for(int i = 0; i < poolSize; i++){
                if (imageProcessorQueue.isEmpty()){
                    state = Thread.State.TERMINATED;
                    break;
                }
                if(coreProcessThreads[i].getState() == Thread.State.TERMINATED){
                    filteredPixels.put(coreProcessThreads[i].getRow(), coreProcessThreads[i].getFilteredPixels());
                    coreProcessThreads[i] = imageProcessorQueue.get(0);
                    imageProcessorQueue.remove(0);
                    coreProcessThreads[i].run();
                }
            }
        }
        for(int i = 0; i < poolSize; i++){
            if(coreProcessThreads[i] != null){
                filteredPixels.put(coreProcessThreads[i].getRow(), coreProcessThreads[i].getFilteredPixels());
            }
        }
    }

    /**
     * This method will add a new image processor to the thread pool
     * 
     * @param imageProcessor the image processor to be added
     */
    public void submit(ImageProcessorMT imageProcessorMT){
        imageProcessorQueue.add(imageProcessorMT);
    }
    
    /**
     * This method will join all the threads in the thread pool
     */
    public void join(){
        for (int i = 0; i < poolSize; i++) {
            if (coreProcessThreads[i] != null) {
                try {
                    coreProcessThreads[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * This method will let the thread pool terminate
     */
    public void quit(){
        for(int i = 0; i < poolSize; i++){
            coreProcessThreads[i] = null;
            filteredPixels.clear();
        }
    }

    /**
     * This method will get the filtered pixels
     * 
     * @return Map of batch of filtered pixels
     */
    public Map<Integer, Color[][]> getFilteredPixels(){
        return filteredPixels;
    }
}