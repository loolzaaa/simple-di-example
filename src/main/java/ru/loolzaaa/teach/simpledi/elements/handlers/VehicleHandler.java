package ru.loolzaaa.teach.simpledi.elements.handlers;

import ru.loolzaaa.teach.simpledi.annotations.Element;
import ru.loolzaaa.teach.simpledi.annotations.ElementWire;
import ru.loolzaaa.teach.simpledi.elements.Vehicle;

@Element
public class VehicleHandler implements Handler {

    private Vehicle vehicle;

    public VehicleHandler() {}

    @ElementWire
    public VehicleHandler(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    @Override
    public void action() {
        System.out.println("Vehicle power: " + vehicle.getPower());
    }
}
