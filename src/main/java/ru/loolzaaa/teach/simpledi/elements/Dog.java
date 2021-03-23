package ru.loolzaaa.teach.simpledi.elements;

import ru.loolzaaa.teach.simpledi.annotations.Element;
import ru.loolzaaa.teach.simpledi.annotations.PrimaryElement;

@Element
@PrimaryElement
public class Dog implements Animal {

    public Dog() {
    }

    @Override
    public String getName() {
        return "Rex";
    }
}
