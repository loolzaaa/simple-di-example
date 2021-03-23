package ru.loolzaaa.teach.simpledi.elements;

import ru.loolzaaa.teach.simpledi.annotations.Element;

@Element
public class Worm implements Animal {

    public Worm() {
    }

    @Override
    public String getName() {
        return "Jim";
    }
}
