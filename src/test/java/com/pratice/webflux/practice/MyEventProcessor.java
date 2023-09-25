package com.pratice.webflux.practice;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class MyEventProcessor {

    List<MyEventListener<String>> observers = new LinkedList<>();
    public void setListener(MyEventListener<String> myEventListener) {
        observers.add(myEventListener);
    }

    public void flowInto() {
        for (MyEventListener<String> observer : observers) {
            observer.onDataChunk(List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        }
    }

    public void complete() {
        for (MyEventListener<String> observer : observers) {
            observer.processComplete();
        }
    }
}
