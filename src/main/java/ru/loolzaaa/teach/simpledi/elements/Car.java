package ru.loolzaaa.teach.simpledi.elements;

import ru.loolzaaa.teach.simpledi.annotations.Element;

@Element
public class Car implements Vehicle {

    public Car() {
    }

    @Override
    public long getPower() {
        return 124;
    }
}
