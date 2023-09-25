package com.pratice.webflux.practice;

import com.jayway.jsonpath.JsonPath;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.*;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.test.StepVerifierOptions;
import reactor.test.publisher.PublisherProbe;
import reactor.test.publisher.TestPublisher;
import reactor.test.scheduler.VirtualTimeScheduler;
import reactor.util.context.Context;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.stream.IntStream;

import static java.lang.Thread.sleep;

/**
 * Learn how to create Flux instances.
 *
 * @author Sebastien Deleuze
 * @see <a href="https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html">Flux Javadoc</a>
 */
@Slf4j
public class Part01Flux {


    @DisplayName("hello reactor 출력")
    @Test
    public void test1() {
        Flux<String> sequence = Flux.just("Hello", "Reactor");
        sequence.map(data -> data.toLowerCase())
                .subscribe(data -> System.out.println(data));
    }

    @DisplayName("mono : 출력")
    @Test
    public void test2() {
        Mono.just("hello")
                .subscribe(System.out::println);
    }

    @DisplayName("mono handle")
    @Test
    public void test3() {
        Mono.empty()
                .subscribe(
                        none -> System.out.println("# emitted onNext signal"),
                        error -> {
                        },
                        () -> System.out.println("emitted onComplete signal")
                );
    }

    @DisplayName("Mono sample 3")
    @Test
    public void test4() {
        URI uri = UriComponentsBuilder.newInstance().scheme("http")
                .host("worldtimeapi.org")
                .port(80)
                .path("/api/timezone/Asia/Seoul")
                .build()
                .encode()
                .toUri();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Mono.just(
                restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<String>(httpHeaders), String.class)
        ).map(response -> {
            var jsonContext = JsonPath.parse(response.getBody());
            String dateTime = jsonContext.read("$.datetime");
            return dateTime;
        }).subscribe(
                data -> System.out.println("emitted data : " + data),
                error -> {
                    System.out.println(error);
                },
                () -> System.out.println("complete")
        );
    }

    @Test
    public void test5() {
        Flux.just(6, 9, 12)
                .map(num -> num % 2)
                .subscribe(System.out::println);
    }

    @Test
    public void test6() {
        Flux.fromArray(new Integer[]{3, 6, 19, 18})
                .filter(num -> num > 6)
                .map(num -> num * 2)
                .subscribe(System.out::println);
    }

    @Test
    public void test7() {
        Mono.justOrEmpty("steve")
                .concatWith(Mono.justOrEmpty("job"))
                .subscribe(System.out::println);
    }

    @Test
    public void test8() {
        Flux.concat(
                        Flux.just("a", "b", "c"),
                        Flux.just("ac", "bc", "cc"),
                        Flux.just("a1", "b1", "c1")
                ).collectList()
                .subscribe(System.out::println);
    }

    @Test
    public void test9() throws InterruptedException {
        Flux<String> cold = Flux.just("korea", "japan", "chinese")
                .map(String::toLowerCase);

        cold.subscribe(country -> System.out.println("sub 1: " + country));
        System.out.println("----------------------------");
        cold.subscribe(country -> System.out.println("sub 2: " + country));
    }

    @Test
    public void test10() throws InterruptedException {
        String[] singers = {"a", "b", "c", "d"};

        System.out.println("begin");

        Flux<String> flux = Flux.fromArray(singers)
                .delayElements(Duration.ofSeconds(1))
                .share();

        System.out.println("sub1 begin");
        flux.subscribe(singer -> System.out.println("sub 1 : " + singer));

        Thread.sleep(2500L);

        System.out.println("sub2 begin");
        flux.subscribe(singer -> System.out.println("sub 2 : " + singer));

        Thread.sleep(4000L);
    }

    @Test
    public void test11() throws InterruptedException {
        URI uri = UriComponentsBuilder.newInstance().scheme("http")
                .host("worldtimeapi.org")
                .port(80)
                .path("/api/timezone/Asia/Seoul")
                .build()
                .encode()
                .toUri();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Mono<String> mono = getWorldTime(uri);
        mono.subscribe(dateTime -> System.out.println("date time1 : " + dateTime));
        Thread.sleep(2000);
        mono.subscribe(dateTime -> System.out.println("date time2 : " + dateTime));
        Thread.sleep(2000);
    }

    private Mono<String> getWorldTime(URI uri) {
        return WebClient.create()
                .get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    var jsonContext = JsonPath.parse(response);
                    return jsonContext.read("$.datetime");
                });
    }

    @Test
    public void test12() throws InterruptedException {
        URI uri = UriComponentsBuilder.newInstance().scheme("http")
                .host("worldtimeapi.org")
                .port(80)
                .path("/api/timezone/Asia/Seoul")
                .build()
                .encode()
                .toUri();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Mono<String> mono = getWorldTime(uri).cache();
        mono.subscribe(dateTime -> System.out.println("date time1 : " + dateTime));
        Thread.sleep(2000);
        mono.subscribe(dateTime -> System.out.println("date time2 : " + dateTime));
        Thread.sleep(2000);
    }

    @Test
    public void test13() {
        Flux.range(1, 5)
                .doOnRequest(data -> System.out.println("do request : " + data))
                .subscribe(new BaseSubscriber<Integer>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        request(1);
                        super.hookOnSubscribe(subscription);
                    }

                    @Override
                    protected void hookOnNext(Integer value) {
                        try {
                            Thread.sleep(200L);
                            System.out.println("hook on next : " + value);
                            request(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }

    @Test
    public void test14() throws InterruptedException {
        Flux.interval(Duration.ofMillis(1l))
                .onBackpressureError()
                .doOnNext(data -> System.out.println("do on next : " + data))
                .publishOn(Schedulers.parallel())
                .subscribe(data -> {
                            try {
                                sleep(5L);
                            } catch (InterruptedException e) {
                            }
                            System.out.println("on next : " + data);
                        },
                        error -> System.out.println("on error" + error.getMessage())
                );
        Thread.sleep(2000L);
    }

    @Test
    public void test15() throws InterruptedException {
        Flux.interval(Duration.ofMillis(1l))
                .onBackpressureDrop(drop -> System.out.println("drop : " + drop))
                .publishOn(Schedulers.parallel())
                .subscribe(data -> {
                            try {
                                sleep(5L);
                            } catch (InterruptedException e) {
                            }
                            System.out.println("on next : " + data);
                        },
                        error -> System.out.println("on error" + error.getMessage())
                );
        Thread.sleep(2000L);
    }

    @Test
    public void test16() throws InterruptedException {
        Flux.interval(Duration.ofMillis(1l))
                .onBackpressureLatest()
                .publishOn(Schedulers.parallel())
                .subscribe(data -> {
                            try {
                                sleep(5L);
                            } catch (InterruptedException e) {
                            }
                            System.out.println("on next : " + data);
                        },
                        error -> System.out.println("on error" + error.getMessage())
                );
        Thread.sleep(2000L);
    }

    @Test
    public void test17() throws InterruptedException {
        Flux.interval(Duration.ofMillis(300l))
                .doOnNext(data -> System.out.println("emit by original flux : " + data))
                .onBackpressureBuffer(2,
                        drop -> System.out.println("overflow : " + drop),
                        BufferOverflowStrategy.DROP_LATEST
                )
                .doOnNext(data -> System.out.println("emit by buffer : " + data))
                .publishOn(Schedulers.parallel(), false, 1)
                .subscribe(data -> {
                            try {
                                sleep(1000L);
                            } catch (InterruptedException e) {
                            }
                            System.out.println("on next : " + data);
                        },
                        error -> System.out.println("on error" + error.getMessage())
                );
        Thread.sleep(3000L);
    }

    @Test
    public void test18() throws InterruptedException {
        Flux.interval(Duration.ofMillis(300l))
                .doOnNext(data -> System.out.println("emit by original flux : " + data))
                .onBackpressureBuffer(2,
                        drop -> System.out.println("overflow : " + drop),
                        BufferOverflowStrategy.DROP_OLDEST
                )
                .doOnNext(data -> System.out.println("emit by buffer : " + data))
                .publishOn(Schedulers.parallel(), false, 1)
                .subscribe(data -> {
                            try {
                                sleep(1000L);
                            } catch (InterruptedException e) {
                            }
                            System.out.println("on next : " + data);
                        },
                        error -> System.out.println("on error" + error.getMessage())
                );
        Thread.sleep(3000L);
    }

    @Test
    public void test19() throws InterruptedException {
        int tasks = 6;
        Flux.create((FluxSink<String> sink) -> {
                    IntStream.range(1, tasks)
                            .forEach(n -> sink.next(doTasks(n)));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(n -> log.info("# create(): {}", n))
                .publishOn(Schedulers.parallel())
                .map(result -> result + " success")
                .doOnNext(n -> log.info("# map() : {}", n))
                .publishOn(Schedulers.parallel())
                .subscribe(data -> log.info("# onNext: {}", data));
        Thread.sleep(500L);
    }

    private String doTasks(int taskNumber) {
        return "task + " + taskNumber + " result";
    }

    @Test
    public void test20() throws InterruptedException {
        int tasks = 6;

        Sinks.Many<String> unicastSink = Sinks.many().unicast()
                .onBackpressureBuffer();

        Flux<String> flux = unicastSink.asFlux();
        IntStream.range(1, tasks)
                .forEach(n -> {
                    try {
                        new Thread(() -> {
                            unicastSink.emitNext(doTasks(n), Sinks.EmitFailureHandler.FAIL_FAST);
                            log.info("# emitted : {}", n);
                        }).start();
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage());
                    }
                });

        flux.publishOn(Schedulers.parallel())
                .map(result -> result + " success!")
                .doOnNext(n -> log.info("# map: {}", n))
                .publishOn(Schedulers.parallel())
                .subscribe(data -> log.info("$ onNext: {}", data));

        Thread.sleep(200L);
    }

    @Test
    public void test21() {
        Sinks.One<String> sinkOne = Sinks.one();
        Mono<String> mono = sinkOne.asMono();

        sinkOne.emitValue("hello reactor", Sinks.EmitFailureHandler.FAIL_FAST);
        sinkOne.emitValue("hi reactor", Sinks.EmitFailureHandler.FAIL_FAST);

        mono.subscribe(data -> log.info("# sub1 : {}", data));
        mono.subscribe(data -> log.info("# sub2 : {}", data));
    }

    @Test
    public void example9_8() {
        Sinks.Many<Integer> unicastSink = Sinks.many().unicast().onBackpressureBuffer();

        Flux<Integer> fluxView = unicastSink.asFlux();

        unicastSink.emitNext(1, Sinks.EmitFailureHandler.FAIL_FAST);
        unicastSink.emitNext(2, Sinks.EmitFailureHandler.FAIL_FAST);

        fluxView.subscribe(data -> log.info("# sub1: {}", data));
        unicastSink.emitNext(3, Sinks.EmitFailureHandler.FAIL_FAST);
//        fluxView.subscribe(data -> log.info("# sub2: {}", data));
    }

    @Test
    public void example9_9() {
        Sinks.Many<Integer> multicastSink = Sinks.many().multicast().onBackpressureBuffer();

        Flux<Integer> fluxView = multicastSink.asFlux();

        multicastSink.emitNext(1, Sinks.EmitFailureHandler.FAIL_FAST);
        multicastSink.emitNext(2, Sinks.EmitFailureHandler.FAIL_FAST);

        fluxView.subscribe(data -> log.info("# sub1: {}", data));
        fluxView.subscribe(data -> log.info("# sub2: {}", data));

        multicastSink.emitNext(3, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    @Test
    public void example9_10() {
        Sinks.Many<Integer> replaySink = Sinks.many().replay().limit(2);

        Flux<Integer> fluxView = replaySink.asFlux();

        replaySink.emitNext(1, Sinks.EmitFailureHandler.FAIL_FAST);
        replaySink.emitNext(2, Sinks.EmitFailureHandler.FAIL_FAST);
        replaySink.emitNext(3, Sinks.EmitFailureHandler.FAIL_FAST);

        fluxView.subscribe(data -> log.info("# sub1: {}", data));
        replaySink.emitNext(4, Sinks.EmitFailureHandler.FAIL_FAST);
        fluxView.subscribe(data -> log.info("# sub2: {}", data));
    }

    @Test
    public void example10_1() throws InterruptedException {
        Flux.just(1, 3, 5, 7)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(data -> log.info("# doOnNext: {}", data))
                .doOnSubscribe(subscription -> log.info("# doOnSubscribe"))
                .subscribe(data -> log.info("# onNext: {}", data));

        Thread.sleep(500L);
    }

    @Test
    public void example10_2() throws InterruptedException {
        Flux.just(1, 3, 5, 7)
                .doOnNext(data -> log.info("# doOnNext: {}", data))
                .doOnSubscribe(subscription -> log.info("# doOnSubscribe"))
                .publishOn(Schedulers.parallel())
                .subscribe(data -> log.info("# onNext: {}", data));

        Thread.sleep(500L);
    }

    @Test
    public void example10_3() throws InterruptedException {
        Flux.range(1, 100)
                .parallel()
                .runOn(Schedulers.parallel())
                .doOnSubscribe(subscription -> log.info("# doOnSubscribe"))
                .subscribe(data -> log.info("# onNext: {}", data));

        Thread.sleep(100L);
    }

    @Test
    public void example10_4() throws InterruptedException {
        Flux.range(1, 100)
                .parallel(5)
                .runOn(Schedulers.parallel())
                .doOnSubscribe(subscription -> log.info("# doOnSubscribe"))
                .subscribe(data -> log.info("# onNext: {}", data));

        Thread.sleep(100L);
    }

    @Test
    public void example10_5() {
        Flux
                .range(1, 10)
                .doOnNext(data -> log.info("# doOnNext input {}", data))
                .filter(data -> data > 3)
                .doOnNext(data -> log.info("# doOnNext filter {}", data))
                .map(data -> data * 10)
                .doOnNext(data -> log.info("# doOnNext map {}", data))
                .subscribe(data -> log.info("# onNext: {}", data));
    }

    @Test
    public void example10_6() {
        Flux
                .range(1, 10)
                .doOnNext(data -> log.info("# doOnNext input {}", data))
                .publishOn(Schedulers.parallel())
                .filter(data -> data > 3)
                .doOnNext(data -> log.info("# doOnNext filter {}", data))
                .map(data -> data * 10)
                .doOnNext(data -> log.info("# doOnNext map {}", data))
                .subscribe(data -> log.info("# onNext: {}", data));
    }

    @Test
    public void example10_7() {
        Flux
                .range(1, 10)
                .doOnNext(data -> log.info("# doOnNext input {}", data))
                .publishOn(Schedulers.parallel())
                .filter(data -> data > 3)
                .doOnNext(data -> log.info("# doOnNext filter {}", data))
                .publishOn(Schedulers.parallel())
                .map(data -> data * 10)
                .doOnNext(data -> log.info("# doOnNext map {}", data))
                .subscribe(data -> log.info("# onNext: {}", data));
    }

    @Test
    public void example10_8() {
        Flux
                .range(1, 7)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(data -> log.info("# doOnNext input {}", data))
                .filter(data -> data > 3)
                .doOnNext(data -> log.info("# doOnNext filter {}", data))
                .publishOn(Schedulers.parallel())
                .map(data -> data * 10)
                .doOnNext(data -> log.info("# doOnNext map {}", data))
                .subscribe(data -> log.info("# onNext: {}", data));
    }

    /**
     * Schedulers.immediate() 이전의 쓰레드 이어서 사용
     */
    @Test
    public void example10_9() {
        Flux
                .range(1, 7)
                .publishOn(Schedulers.parallel())
                .filter(data -> data > 3)
                .doOnNext(data -> log.info("# doOnNext filter {}", data))
                .publishOn(Schedulers.immediate())
                .map(data -> data * 10)
                .doOnNext(data -> log.info("# doOnNext map {}", data))
                .subscribe(data -> log.info("# onNext: {}", data));
    }

    /**
     * Schedulers.single() 스레드 하나만 생성해서 스케줄러 제거되기 전까지 재사용
     * 지연 시간이 짧은 작업을 처리하는 것에 효과적
     */
    @Test
    public void example10_10() {
        doTask("task1", Schedulers.single())
                .subscribe(data -> log.info("# onNext: {}", data));
        doTask("task2", Schedulers.single())
                .subscribe(data -> log.info("# onNext: {}", data));
    }

    private Flux<Integer> doTask(String taskName, Scheduler scheduler) {
        return Flux.just(1, 3, 5, 7)
                .publishOn(scheduler)
                .filter(data -> data > 3)
                .doOnNext(data -> log.info("#{} doOnNext filter {}", taskName, data))
                .map(data -> data * 10)
                .doOnNext(data -> log.info("#{} doOnNext map {}", taskName, data));
    }

    /**
     * Schedulers.newSingle() 호출할 때마다 매번 새로운 스레드 생성
     * 첫번째 파라미터는 이름 두번째는 데몬 스레드 여부
     */
    @Test
    public void example10_11() {
        doTask("task1", Schedulers.newSingle("new-single", true))
                .subscribe(data -> log.info("# onNext: {}", data));
        doTask("task2", Schedulers.newSingle("new-single", true))
                .subscribe(data -> log.info("# onNext: {}", data));
    }

    /**
     * Schedulers.boundedElastic()
     * ExecutorService 기반의 스레드 풀을 생성한 후, 그 안에서 정해진 수만큼의 스레드를 사용하여 작업을 처리하고 작업이 종료된 스레드는 반납하여 재사용
     * Blocking I/O 작업에 적합
     */

    /**
     * Schedulers.fromExecutorService()
     * 기존에 사용하고 있는 ExecutorService 가 있다면 그것으로 부터 스케줄러를 생성하는 방식 직접 서비스를 생성하는 것은 권장하지 않음
     */

    /**
     * Schedulers.newXXXXX()
     * 새로운 스케줄러 인스턴스를 생성할 수 있음, 즉 스레드 이름, 생성 가능한 디폴트 스레드의 개수, 스레드의 유휴 시간, 데몬 스레드 동작 여부
     * 등을 직접 지정해서 커스텀 스레드 풀을 새로 생성할 수 있다.
     */

    /**
     * 쓸때는 Context 읽을 때는 ContextView
     */
    @Test
    public void example11_1() throws InterruptedException {
        Mono.deferContextual(ctx ->
                        Mono.just("hello" + " " + ctx.get("firstName"))
                                .doOnNext(data -> log.info("# just doOnNext : {}", data))
                )
                .subscribeOn(Schedulers.boundedElastic())
                .publishOn(Schedulers.parallel())
                .transformDeferredContextual(
                        (mono, ctx) -> mono.map(data -> data + " " + ctx.get("lastName"))
                )
                .contextWrite(context -> context.put("lastName", "jobs"))
                .contextWrite(context -> context.put("firstName", "steve"))
                .subscribe(data -> log.info("# onNext: {}", data));

        Thread.sleep(100l);
    }

    @Test
    public void example11_3() {
        final String key1 = "company";
        final String key2 = "firstName";
        final String key3 = "lastName";

        Mono.deferContextual(ctx ->
                        Mono.just(ctx.get(key1) + ", " + ctx.get(key2) + " " + ctx.get(key3))
                )
                .publishOn(Schedulers.parallel())
                .contextWrite(ctx -> ctx.putAll(Context.of(key2, "steve", key3, "Jobs").readOnly()))
                .contextWrite(ctx -> ctx.put(key1, "Apple"))
                .subscribe(data -> log.info("# onNext : {}", data));
    }

    @Test
    public void example11_4() {
        final String key1 = "company";
        final String key2 = "firstName";
        final String key3 = "lastName";

        Mono.deferContextual(ctx ->
                        Mono.just(ctx.get(key1) + ", " +
                                ctx.getOrEmpty(key2).orElse("no firstName") + " " +
                                ctx.getOrDefault(key3, "no lastName"))
                )
                .publishOn(Schedulers.parallel())
                .contextWrite(ctx -> ctx.put(key1, "Apple"))
                .subscribe(data -> log.info("# onNext : {}", data));
    }

    /**
     * 동일한 키에 대한 값을 중복해서 저장하면 Operator 체인에서 가장 위쪽에 위치한 contextWrite()이 저장한 값으로 덮어슨다.
     */
    @Test
    public void example11_5() {
        final String key1 = "company";

        Mono<String> mono = Mono.deferContextual(ctx ->
                        Mono.just("Company : " + ctx.get(key1)))
                .publishOn(Schedulers.parallel());

        mono.contextWrite(context -> context.put(key1, "Apple"))
                .subscribe(data -> log.info("#sub1 onNext : {}", data));
        mono
                .contextWrite(context -> context.put(key1, "Microsoftsss"))
                .contextWrite(context -> context.put(key1, "Microsoft"))
                .subscribe(data -> log.info("#sub2 onNext : {}", data));
    }

    /**
     * context 는 operator 체인의 아래에서 위로 전파된다.
     * Operator 에서 Context에 저장된 데이터를 읽을 수 있도록 contextWrite()를 Operator 체인의 맨 마지막에 둡니다.
     */
    @Test
    public void example11_6() {
        String key1 = "company";
        String key2 = "name";

        Mono
                .deferContextual(ctx ->
                        Mono.just(ctx.get(key1))
                )
                .publishOn(Schedulers.parallel())
                .contextWrite(context -> context.put(key2, "Bill"))
                .transformDeferredContextual((mono, ctx) -> mono.map(data -> data + ", " + ctx.getOrDefault(key2, "steve")))
                .contextWrite(context -> context.put(key1, "Apple"))
                .subscribe(data -> log.info("#onNext : {}", data));
    }

    /**
     * 내부에서는 외부의 context 에 저장된 데이터를 읽을 수 있다.
     * 외부에서는 내부의 context 에 저장된 데이터를 읽을 수 없다.
     */
    @Test
    public void example11_7() {
        String key1 = "company";

        Mono
                .just("steve")
//                .transformDeferredContextual(((stringMono, contextView) -> contextView.get("role")))
                .flatMap(name ->
                        Mono.deferContextual(ctx ->
                                Mono
                                        .just(ctx.get(key1) + ", " + name)
                                        .transformDeferredContextual((mono, innerCtx) -> mono.map(data -> data + ", " + innerCtx.get("role")))
                                        .contextWrite(context -> context.put("role", "CEO"))
                        )
                )
                .publishOn(Schedulers.parallel())
                .contextWrite(context -> context.put(key1, "Apple"))
                .subscribe(data -> log.info("#onNext : {}", data));
    }

    /**
     * context 는 인증 정보 같은 직교성을 가지는 정보를 전송학는데 적합하다.
     */
    @Test
    public void example11_8() {
        Mono<String> mono = postBook(Mono.just(
                new Book(
                        "test",
                        "name",
                        "person"
                ))
        )
                .contextWrite(Context.of(HEADER_AUTH_TOKEN, "brabra"));

        mono.subscribe(data -> log.info("# onNext : {}", data));
    }

    private static final String HEADER_AUTH_TOKEN = "authToken";

    @Data
    @AllArgsConstructor
    class Book {
        private String isbn;
        private String bookName;
        private String author;
    }

    private Mono<String> postBook(Mono<Book> book) {
        return Mono.zip(book, Mono.deferContextual(ctx ->
                Mono.just(ctx.get(HEADER_AUTH_TOKEN)))
        ).flatMap(tuple -> {
            String response = "POST the book(" + tuple.getT1().getBookName() + "," +
                    tuple.getT1().getAuthor() + ") with token: " +
                    tuple.getT2();
            return Mono.just(response);
        });
    }

    @Test
    public void example12_1() {
        var fruits = new HashMap<String, String>();
        fruits.put("banana", "바나나");
        fruits.put("apple", "사과");
        fruits.put("pear", "배");
        fruits.put("grape", "포도");

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
    }

    @Test
    public void example12_2() {
        Flux.just(2, 4, 6, 8)
                .zipWith(Flux.just(1, 2, 3, 0), (x, y) -> x / y)
                .map(num -> num + 2)
                .checkpoint()
                .subscribe(
                        data -> log.info("# onNext : {}", data),
                        error -> error.printStackTrace()
                );
    }

    @Test
    public void example12_3() {
        Flux.just(2, 4, 6, 8)
                .zipWith(Flux.just(1, 2, 3, 0), (x, y) -> x / y)
                .checkpoint()
                .map(num -> num + 2)
                .checkpoint()
                .subscribe(
                        data -> log.info("# onNext : {}", data),
                        error -> error.printStackTrace()
                );
    }

    @Test
    public void example12_4() {
        Flux.just(2, 4, 6, 8)
                .zipWith(Flux.just(1, 2, 3, 0), (x, y) -> x / y)
                .checkpoint("zipwith check point")
                .map(num -> num + 2)
                .checkpoint("map check point")
                .subscribe(
                        data -> log.info("# onNext : {}", data),
                        error -> error.printStackTrace()
                );
    }

    @Test
    public void example12_5() {
        Flux.just(2, 4, 6, 8)
                .zipWith(Flux.just(1, 2, 3, 0), (x, y) -> x / y)
                .checkpoint("zipwith check point", true)
                .map(num -> num + 2)
                .checkpoint("map check point", true)
                .subscribe(
                        data -> log.info("# onNext : {}", data),
                        error -> error.printStackTrace()
                );
    }

    @Test
    public void example12_6() {
        Flux<Integer> source = Flux.just(2, 4, 6, 8);
        Flux<Integer> other = Flux.just(1, 2, 3, 0);

        Flux<Integer> multiplySource = multiply(source, other).checkpoint();
        Flux<Integer> plusSource = plus(multiplySource).checkpoint();

        plusSource.subscribe(
                data -> log.info("# onNext: {}", data),
                error -> error.printStackTrace()
        );
    }

    private Flux<Integer> plus(Flux<Integer> input) {
        return input.map(data -> data + 2);
    }

    private Flux<Integer> multiply(Flux<Integer> source, Flux<Integer> other) {
        return source.zipWith(other, (o1, o2) -> o1 / o2);
    }

    @Test
    public void example12_7() {
        Map<String, String> fruits = new HashMap<String, String>();

        fruits.put("banana", "바나나");
        fruits.put("apple", "사과");
        fruits.put("pear", "배");
        fruits.put("grape", "포도");

        Flux
                .just("BANANAS", "APPLES", "PEARS", "MELONS")
                .map(String::toLowerCase)
                .map(fruit -> fruit.substring(0, fruit.length() - 1))
//                .log("Fruit.Substring", Level.FINE)
                .log()
                .map(fruits::get)
                .subscribe(
                        log::info,
                        error -> {
                            log.error("# onError : " + error);
                            error.printStackTrace();
                        }
                );
    }

    @Test
    public void example13_1() {
        StepVerifier
                .create(Mono.just("Hello Reactor"))
                .expectNext("Hello Reactor")
                .expectComplete()
                .verify();
    }

    @Test
    public void example13_3() {
        StepVerifier
                .create(GeneralTestExample.sayHello())
                .expectSubscription()
                .as("#expect subscription")
                .expectNext("hi")
                .as("#expect hi")
                .expectNext("reactor")
                .as("#expect reactor")
                .verifyComplete();
    }

    @Test
    public void example13_4() {
        Flux<Integer> source = Flux.just(2, 4, 6, 8, 10);
        StepVerifier.create(GeneralTestExample.divideByTwo(source))
                .expectSubscription()
//                .expectNext(1)
//                .expectNext(2)
//                .expectNext(3)
//                .expectNext(4)
                .expectNext(1, 2, 3, 4)
                .expectError()
                .verify();
    }

    @Test
    public void exampel13_5() {
        var source = Flux.range(0, 1000);
        StepVerifier
                .create(GeneralTestExample.takeNumber(source, 500),
                        StepVerifierOptions.create().scenarioName("Verify from 0 to 499")
                )
                .expectSubscription()
                .expectNext(0)
                .expectNextCount(498)
                .expectNext(500)
                .expectComplete()
                .verify();
    }

    @DisplayName("1시간에 하나 take 하는 것을 가상의 스케줄러로 test")
    @Test
    public void example13_7() {
        StepVerifier
                .withVirtualTime(() -> TimeBasedTestExample.getCOVID19Count(
                                Flux.interval(Duration.ofHours(1)).take(1)
                        )
                )
                .expectSubscription()
                .then(() -> VirtualTimeScheduler
                        .get()
                        .advanceTimeBy(Duration.ofHours(1)))
                .expectNextCount(11)
                .expectComplete()
                .verify();
    }

    @DisplayName("3초안에 실행되야한다.")
    @Test
    public void example13_8() {
        StepVerifier
                .create(TimeBasedTestExample.getCOVID19Count(
                        Flux.interval(Duration.ofMinutes(1)).take(1)
                ))
                .expectSubscription()
                .expectNextCount(11)
                .expectComplete()
                .verify(Duration.ofSeconds(3));
    }

    /**
     * expectNoEvent() 의 파라미터로 시간을 지정하면 지정한 시간 동안 어떤 이벤트(signal) 도 발생하지 않을 것이라고 기대
     * 지정한 시간만큼 시간을 앞당긴다.
     */
    @Test
    public void example13_9() {
        StepVerifier
                .withVirtualTime(() -> TimeBasedTestExample.getVoteCount(
                        Flux.interval(Duration.ofMinutes(1))
                ))
                .expectSubscription()
                .expectNoEvent(Duration.ofMinutes(1))
                .expectNoEvent(Duration.ofMinutes(1))
                .expectNoEvent(Duration.ofMinutes(1))
                .expectNoEvent(Duration.ofMinutes(1))
                .expectNoEvent(Duration.ofMinutes(1))
                .expectNextCount(5)
                .expectComplete()
                .verify();
    }

    /**
     * 데이터의 요청 개수를 1로 지정해서 오버플로가 발생
     * thenComsumeWhile() 메서드에서 데이터를 소비하지만 예상한 것보다 더 많은 데이터를 수신함으로써 결국에는 오버플로
     */
    @Test
    public void example13_11() {
        StepVerifier
                .create(BackpressureTestExample.generateNumber(), 1L)
                .thenConsumeWhile(num -> num >= 1)
                .verifyComplete();
    }

    @Test
    public void example13_12() {
        StepVerifier
                .create(BackpressureTestExample.generateNumber(), 1L)
                .thenConsumeWhile(num -> num >= 1)
                .expectError()
                .verifyThenAssertThat()
                .hasDroppedElements();
    }

    @Test
    public void example13_14() {
        Mono<String> source = Mono.just("hello");

        StepVerifier
                .create(ContextTestExample.getSecretMessage(source)
                        .contextWrite(context -> context.put("secretMessage", "Hello, Reactor"))
                        .contextWrite(context -> context.put("secretKey", "hello"))
                )
                .expectSubscription()
                .expectAccessibleContext()
                .hasKey("secretKey")
                .hasKey("secretMessage")
                .then()
                .expectNext("Hello, Reactor")
                .expectComplete()
                .verify();
    }

    /**
     * recordWith 를 이용하면 emit 되는 데이터에 대한 세밀한 테스트가 가능
     */
    @Test
    public void example13_16() {
        StepVerifier
                .create(RecordTestExample.getCapitalizedCountry(
                        Flux.just("korea", "england", "canada", "india")
                ))
                .expectSubscription()
                .recordWith(ArrayList::new)
                .thenConsumeWhile(country -> !country.isEmpty())
                .consumeRecordedWith(countries -> {
                    Assertions.assertThat(
                            countries.stream()
                                    .allMatch(country ->
                                            Character.isUpperCase(country.charAt(0)))
                    ).isTrue();
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void example13_17() {
        StepVerifier
                .create(RecordTestExample.getCapitalizedCountry(
                        Flux.just("korea", "england", "canada", "india")
                ))
                .expectSubscription()
                .recordWith(ArrayList::new)
                .thenConsumeWhile(country -> !country.isEmpty())
                .expectRecordedMatches(countries ->
                        countries
                                .stream()
                                .allMatch(country ->
                                        Character.isUpperCase(country.charAt(0)))
                )
                .expectComplete()
                .verify();
    }

    /**
     * TestPulisher 를 사용하면, 개발자가 직접 프로그래밍 방식으로 signal을 발생시키면서 원하는 상황을 미세하게 재연하며 테스트를 진행할 수 있다!!
     * TestPulisher가 발생시키는 Signal 유형
     * 1. next(T) 또는 next(T...) : 1개 이상의 onNext Signal을 발생시킨다.
     * 2. emit(T...) 1개 이상의 signal을 발생시킨 후, onComplete signal을 발생
     * 3. complete() onComplete signal 발생
     * 4. error(Throwable) : onError signal 발생
     */
    @Test
    public void example13_18() {
        TestPublisher<Integer> source = TestPublisher.create();

        StepVerifier
                .create(GeneralTestExample.divideByTwo(source.flux()))
                .expectSubscription()
                .then(() -> source.emit(2, 4, 6, 8, 10))
                .expectNext(1, 2, 3, 4)
                .expectError()
                .verify();
    }

    /**
     * 오동작하는 (Misbehaving) TestPublisher를 생성하기 위한 위반 조건
     * ALLOW_NULL : 전송할 데이터가 null 이어도 NullPointerException을 발생시키지 않고 다음 호출을 진행할 수 있도록 한다.
     * CLEANUP_ON_TERMINATE : onComplete, onError, emit 같은 Terminal signal을 연달아 여러 번 보낼 수 있도록 한다.
     * DEFER_CANCELLATION : cancel signal을 무시하고 계속해서 signal을 emit할 수 있게 한다.
     * REQUEST_OVERFLOW : 요청 개수보다 더 많은 signal이 발생하더라도 IllegalStateException을 발생시키지 않고 다음 호출을 진행 할 수 있도록 한다.
     */
    @Test
    public void example13_19() {
//        TestPublisher<Integer> source = TestPublisher.create();
        TestPublisher<Integer> source = TestPublisher.createNoncompliant(TestPublisher.Violation.ALLOW_NULL);

        StepVerifier
                .create(GeneralTestExample.divideByTwo(source.flux()))
                .expectSubscription()
                .then(() -> {
                    getDataSource().stream()
                            .forEach(data -> source.next(data));
                    source.complete();
                })
                .expectNext(1, 2, 3, 4, 5)
                .expectComplete()
                .verify();
    }

    private List<Integer> getDataSource() {
        return Arrays.asList(2, 4, 6, 8, null);
    }

    @Test
    public void example13_21() {
        PublisherProbe<String> probe = PublisherProbe.of(Mono.just("# supply standby power"));

        StepVerifier
                .create(PublisherProbeTestExample.processTask(
                        Mono.empty(),
                        probe.mono())
                )
                .expectNextCount(1)
                .verifyComplete();

        probe.assertWasSubscribed();
        probe.assertWasRequested();
        probe.assertWasNotCancelled();
    }
}