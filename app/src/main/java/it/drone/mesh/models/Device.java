package it.drone.mesh.models;

import java.util.Objects;

public class Device {

    private String id;
    private StringBuffer input;
    private StringBuffer output;


    public Device(String id) {
        this.id = id;
        this.input = new StringBuffer();
        this.output = new StringBuffer();
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public StringBuffer getInput() {
        return input;
    }

    public void writeInput(String input) {
        this.input.append(input).append("\n");
    }

    public StringBuffer getOutput() {
        return output;
    }

    public void writeOutput(String output) {
        this.output.append(output).append("\n");
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
