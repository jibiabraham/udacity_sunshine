package com.nn.studio.sunshine;

/**
 * Created by jibi on 26/7/14.
 */
public class WeatherModel {
    public String day;
    public String desc;
    public double tempHigh;
    public double tempLow;

    public WeatherModel(String day, String desc, double tempHigh, double tempLow) {
        this.day = day;
        this.desc = desc;
        this.tempHigh = tempHigh;
        this.tempLow = tempLow;
    }

    private static String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    @Override
    public String toString() {
        return day + " - " + desc + " - " + formatHighLows(tempHigh, tempLow);
    }

    private double toFarenheit(double temp){
        return (temp * 1.8) + 32;
    }

    public String toString(Boolean imperial) {
        if(!imperial){
            return this.toString();
        }
        return day + " - " + desc + " - " + formatHighLows(toFarenheit(tempHigh), toFarenheit(tempLow));
    }
}
