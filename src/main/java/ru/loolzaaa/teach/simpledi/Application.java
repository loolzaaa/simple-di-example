package ru.loolzaaa.teach.simpledi;

import ru.loolzaaa.teach.simpledi.annotations.ElementScan;
import ru.loolzaaa.teach.simpledi.context.Context;
import ru.loolzaaa.teach.simpledi.elements.handlers.AnimalHandler;
import ru.loolzaaa.teach.simpledi.elements.handlers.VehicleHandler;

@ElementScan("ru.loolzaaa.teach.simpledi.elements")
public class Application {

    public static void main(String[] args) throws ReflectiveOperationException {
        Context context = new Context(Application.class);

        AnimalHandler animalHandler = (AnimalHandler)context.getBean("animalHandler");
        animalHandler.action();

        VehicleHandler vehicleHandler = (VehicleHandler)context.getBean("vehicleHandler");
        vehicleHandler.action();
    }
}
