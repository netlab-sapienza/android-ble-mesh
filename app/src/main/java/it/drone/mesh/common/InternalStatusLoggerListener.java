package it.drone.mesh.common;

public interface InternalStatusLoggerListener {
    void OnDebugMessage(String message);

    void OnErrorMessage(String message);
}
