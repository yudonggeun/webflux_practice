package com.pratice.webflux;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;

@RestController
public class Controller {

    @GetMapping("/hello")
    public Flux<String> hello(@RequestParam String name) {
        return Flux.just("hello", name);
    }

    @GetMapping("/test")
    public String test(){
        return "test";
    }

    @GetMapping("/map")
    public Object map(){
        HashMap<String, Object> result = new HashMap<>();
        result.put("key1", 10);
        result.put("key2", "hello");
        return result;
    }
}
