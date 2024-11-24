package com.sinuo.imagetagger;

public class TaggedObject {
    private String combinedName;
    // of the picture
    private int height;
    private int width;
    // origin from left-top
    private int x1, y1, x2, y2;
    private double x1Ratio, y1Ratio, x2Ratio, y2Ratio;
    private double tagRatioX, tagRatioY;

    public TaggedObject(String combinedName, int x1, int y1, int x2, int y2, int height, int width) {
        this.combinedName = combinedName;
        this.height = height;
        this.width = width;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;

        // Calculate ratios
        calculateRatios();
    }

    private void calculateRatios() {
        // Ensure we don't divide by zero
        if (width > 0 && height > 0) {
            x1Ratio = (double) x1 / width;
            y1Ratio = (double) y1 / height;
            x2Ratio = (double) x2 / width;
            y2Ratio = (double) y2 / height;
            tagRatioX = (x1Ratio + x2Ratio) / 2;
            tagRatioY = (y1Ratio + y2Ratio) / 2;
        }
    }



    public void removePrefix() {
        if (combinedName.matches("^\\d+\\..*")) {
            combinedName = combinedName.replaceFirst("^\\d+\\.", "");
        }
    }

    // Original getters
    public String getCombinedName() { return combinedName; }
    public int getHeight() { return height; }
    public int getWidth() { return width; }

    // Ratio getters

    public double getTagRatioX() { return tagRatioX;}
    public double getTagRatioY() { return tagRatioY;}


}