package it.drone.mesh.roles.common;

import java.util.ArrayList;

import it.drone.mesh.models.Device;

public class RoutingTable {

    private ArrayList<Device> devices;


    public RoutingTable() {
        devices = new ArrayList<>();
    }

    public void addDevice(Device device) {
        this.devices.add(device);
    }

    // TODO: 21/11/18 bisogna creare un meccanismo di sottoscrizione per quando trova altri device in modo che la grafica possa aggiornarsi, senza tale meccanismo qualsiasi aggiunta successiva NON verrà visualizzata

    public ArrayList<Device> getDevices() {
        return devices;
    }
}
