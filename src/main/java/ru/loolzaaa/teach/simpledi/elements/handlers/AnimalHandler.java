package ru.loolzaaa.teach.simpledi.elements.handlers;

import ru.loolzaaa.teach.simpledi.annotations.Element;
import ru.loolzaaa.teach.simpledi.elements.Animal;

@Element
public class AnimalHandler implements Handler {

    private Animal animal;

    public AnimalHandler(Animal animal) {
        this.animal = animal;
    }

    @Override
    public void action() {
        System.out.println("Animal name: " + animal.getName());
    }
}
