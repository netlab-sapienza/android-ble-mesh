package it.drone.mesh.models;


import android.os.SystemClock;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Device {

    private String id;
    private long lastTime;
    private StringBuffer input;
    private StringBuffer output;
    private String signalPower;

    public Device(String id, long lastTime, String signalPower) {
        this.id = id;
        this.lastTime = lastTime;
        this.signalPower = signalPower;
        this.input = new StringBuffer();
        this.output = new StringBuffer();
    }

    /**
     * Takes in a number of nanoseconds and returns a human-readable string giving a vague
     * description of how long ago that was.
     */
    public String getTimeSinceString() {
        String lastSeenText = "";

        long timeSince = SystemClock.elapsedRealtimeNanos() - lastTime;
        long secondsSince = TimeUnit.SECONDS.convert(timeSince, TimeUnit.NANOSECONDS);

        if (secondsSince < 5) {
            lastSeenText += "just now";
        } else if (secondsSince < 60) {
            lastSeenText += secondsSince + " " + "seconds ago";
        } else {
            long minutesSince = TimeUnit.MINUTES.convert(secondsSince, TimeUnit.SECONDS);
            if (minutesSince < 60) {
                if (minutesSince == 1) {
                    lastSeenText += minutesSince + "minutes ago";
                } else {
                    lastSeenText += minutesSince + " " + "minutes ago";
                }
            } else {
                long hoursSince = TimeUnit.HOURS.convert(minutesSince, TimeUnit.MINUTES);
                if (hoursSince == 1) {
                    lastSeenText += hoursSince + " " + "hours ago";
                } else {
                    lastSeenText += hoursSince + " " + "hours";
                }
            }
        }

        return lastSeenText;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    public StringBuffer getInput() {
        return input;
    }

    public void writeInput(String input) {
        this.input.append(input);
    }

    public StringBuffer getOutput() {
        return output;
    }

    public void writeOutput(String output) {
        this.output.append(output);
    }

    public String getSignalPower() {
        return signalPower;
    }

    public void setSignalPower(String signalPower) {
        this.signalPower = signalPower;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Device device = (Device) o;
        return Objects.equals(id, device.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
