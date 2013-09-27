package com.yavalek.ontraffic.model;

public class NearLog implements Comparable<NearLog> {
    private LogRecord logRecord;
    private double distance;

    public LogRecord getLogRecord() {
        return logRecord;
    }

    public void setLogRecord(LogRecord logRecord) {
        this.logRecord = logRecord;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    @Override
    public int compareTo(NearLog o) {
        return Double.compare(distance, o.distance);
    }
}
