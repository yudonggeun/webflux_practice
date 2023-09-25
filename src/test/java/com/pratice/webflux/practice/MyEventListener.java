package com.pratice.webflux.practice;

import java.util.List;

interface MyEventListener<T> {
    void onDataChunk(List<T> chunk);
    void processComplete();
}
