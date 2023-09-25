package com.pratice.webflux.practice;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Debug {
    public static Map<String, String> fruits = new HashMap<String, String>();

    static {
        fruits.put("banana", "바나나");
        fruits.put("apple", "사과");
        fruits.put("pear", "배");
        fruits.put("grape", "포도");
    }

    public static void main(String[] args) throws InterruptedException {

        Hooks.onOperatorDebug();

        Flux
                .just("BANANAS", "APPLES", "PEARS", "MELONS")
                .subscribeOn(Schedulers.boundedElastic())
                .publishOn(Schedulers.parallel())
                .map(String::toLowerCase)
                .map(fruit -> fruit.substring(0, fruit.length() - 1))
                .map(fruits::get)
                .map(translated -> "맛있는 " + translated)
                .subscribe(
                        log::info,
                        error -> {
                            log.error("# onError : " + error);
                            error.printStackTrace();
                        }
                );

        Thread.sleep(100L);
    }
}
