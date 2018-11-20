package it.drone.mesh.models;


import android.content.Context;
import android.os.SystemClock;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import it.drone.mesh.R;

public class Device {
    private String id;

    private long lastTime;
    // per il momento sono stringhe diventeranno qualcosa di decente a breve
    private String input = "";
    private String output = "";
    private String signalPower;

    public Device(String id, long lastTime, String signalPower) {
        this.id = id;
        this.lastTime = lastTime;
        this.signalPower = signalPower;
    }

    /**
     * Takes in a number of nanoseconds and returns a human-readable string giving a vague
     * description of how long ago that was.
     */
    private String getTimeSinceString(Context context) {
        String lastSeenText = context.getResources().getString(R.string.last_seen) + " ";

        long timeSince = SystemClock.elapsedRealtimeNanos() - lastTime;
        long secondsSince = TimeUnit.SECONDS.convert(timeSince, TimeUnit.NANOSECONDS);

        if (secondsSince < 5) {
            lastSeenText += context.getResources().getString(R.string.just_now);
        } else if (secondsSince < 60) {
            lastSeenText += secondsSince + " " + context.getResources()
                    .getString(R.string.seconds_ago);
        } else {
            long minutesSince = TimeUnit.MINUTES.convert(secondsSince, TimeUnit.SECONDS);
            if (minutesSince < 60) {
                if (minutesSince == 1) {
                    lastSeenText += minutesSince + " " + context.getResources()
                            .getString(R.string.minute_ago);
                } else {
                    lastSeenText += minutesSince + " " + context.getResources()
                            .getString(R.string.minutes_ago);
                }
            } else {
                long hoursSince = TimeUnit.HOURS.convert(minutesSince, TimeUnit.MINUTES);
                if (hoursSince == 1) {
                    lastSeenText += hoursSince + " " + context.getResources()
                            .getString(R.string.hour_ago);
                } else {
                    lastSeenText += hoursSince + " " + context.getResources()
                            .getString(R.string.hours_ago);
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

    public String getInput() {
        return input;
    }

    public void writeInput(String input) {
        this.input += input + "\n";
    }

    public String getOutput() {
        return output;
    }

    public void writeOutput(String output) {
        this.output += output + "\n";
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
