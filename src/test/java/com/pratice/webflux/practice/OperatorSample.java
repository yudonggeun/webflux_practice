package com.pratice.webflux.practice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class OperatorSample {

    @Test
    public void justOrEmptyNull() {
        Mono.justOrEmpty(null)
                .subscribe(
                        data -> {
                        },
                        error -> {
                        },
                        () -> log.info("on complete")
                );
    }

    @Test
    public void justOrEmpty() {
        Mono.justOrEmpty("test")
                .subscribe(
                        data -> {
                        },
                        error -> {
                        },
                        () -> log.info("on complete")
                );
    }

    @Test
    public void fromIterable() {
        Flux
                .fromIterable(List.of(1, 2, 3, 4, 5))
                .subscribe(data -> log.info("데이터 출력 !! " + data));
    }

    @Test
    public void fromStream() {
        Flux
                .fromStream(List.of(1, 2, 3, 4, 5).stream())
                .subscribe(data -> log.info("데이터 출력 !! " + data));
    }

    @Test
    public void range() {
        Flux.range(11, 10)
                .subscribe(data -> log.info("data {}", data));
    }

    @Test
    public void defer() throws InterruptedException {
        Mono<LocalDateTime> justMono = Mono.just(LocalDateTime.now());
        Mono<LocalDateTime> deferMono = Mono.defer(() -> Mono.just(LocalDateTime.now()));

        Thread.sleep(2000l);

        justMono.subscribe(data -> log.info("just1 {}", data));
        deferMono.subscribe(data -> log.info("defer1 {}", data));
        Thread.sleep(2000l);

        justMono.subscribe(data -> log.info("just2 {}", data));
        deferMono.subscribe(data -> log.info("defer2 {}", data));
    }

    @Test
    public void example14_7() throws InterruptedException {
        Mono.just("hello")
                .delayElement(Duration.ofSeconds(3))
                .switchIfEmpty(Mono.defer(() -> sayDefault()))
                .subscribe(data -> log.info("data {}", data));

        Thread.sleep(3500);
    }

    private Mono<String> sayDefault() {
        log.info("call method");
        return Mono.just("hi");
    }

    /**
     * reactive try-with-resource
     */
    @Test
    public void using() {
        List<Integer> data = List.of(1, 2, 2, 3, 34, 3, 4, 1, 23, 4, 23, 42341234, 12);
        Flux
                .using(
                        () -> {
                            log.info("input resource");
                            return data;
                        },
                        (input) -> {
                            log.info("try : create flux");
                            return Flux.fromIterable(input);
                        },
                        (input) -> {
                            log.info("finally : input close process {}", input);
                        }
                ).subscribe(d -> log.info("{}", d));
    }

    /**
     * 초기값을 주고
     * sink를 생성하여 emit 특정 상태가 될 때까지(sink.complete()) keep going
     * 동기적
     */
    @Test
    public void generate() {
        Flux
                .generate(
                        () -> 0,
                        (state, sink) -> {
                            sink.next("상태는 " + state);
                            if (state == 10) sink.complete();
                            return ++state;
                        }
                )
                .subscribe(data -> log.info("onNext : {}", data));
    }

    @Test
    public void generateComplete() {
        Flux.generate(
                        () -> Tuples.of(3, 1),
                        (status, sink) -> {
                            Integer dan = status.getT1();
                            Integer level = status.getT2();
                            if (level == 10) sink.complete();
                            sink.next(dan + " * " + level + " = " + (dan * level));
                            return Tuples.of(dan, level + 1);
                        },
                        (status) -> log.info(status.getT1() + "종료")
                )
                .subscribe(data -> log.info("onNext : {}", data));
    }

    static int size = 0;
    static int count = -1;
    final static List<Integer> dataSource = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    @Test
    public void create() {

        log.info("# start");
        Flux.create((sink) -> {
                    sink.onRequest(n -> {
                        try {
                            Thread.sleep(1000L);
                            for (int i = 0; i < n; i++) {
                                if (count >= 9) {
                                    sink.complete();
                                } else {
                                    count++;
                                    sink.next(dataSource.get(count));
                                }
                            }
                        } catch (InterruptedException e) {
                        }
                    });
                    sink.onDispose(() -> log.info("# clean up"));
                })
                .subscribe(new BaseSubscriber<>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        request(2);
                    }

                    @Override
                    protected void hookOnComplete() {
                        log.info("# onComplete");
                    }

                    @Override
                    protected void hookOnNext(Object value) {
                        size++;
                        log.info("# onNext: {}", value);
                        if (size == 2) {
                            request(2);
                            size = 0;
                        }
                    }
                });
    }

    @Test
    public void example14_13() throws InterruptedException {

        var emitter = new MyEventProcessor();
        Flux.create(sink -> {
                    emitter.setListener(new MyEventListener<String>() {
                        @Override
                        public void onDataChunk(List<String> chunk) {
                            chunk.stream().forEach(data -> {
                                sink.next(data);
                            });
                        }

                        @Override
                        public void processComplete() {
                            sink.complete();
                        }
                    });
                })
                .publishOn(Schedulers.parallel())
                .subscribe(
                        data -> log.info("onNext : {}", data),
                        error -> {
                        },
                        () -> log.info("onComplete")
                );

        Thread.sleep(3000l);

        emitter.flowInto();

        Thread.sleep(2000L);
        emitter.complete();
    }

    @Test
    public void filter() {
        Flux
                .range(1, 20)
                .filter(num -> num % 2 == 0)
                .subscribe(data -> log.info("# onNext : {}", data));
    }

    /**
     * 비동기적으로 필터링을 수행하는 filterWhen
     */
    @Test
    public void filterWhen() {
        Flux
                .range(1, 20)
                .filterWhen(num -> Mono
                        .just(num % 2 == 0)
                        .publishOn(Schedulers.parallel()))
                .subscribe(data -> log.info("# onNext : {}", data));
    }

    @Test
    public void skip() throws InterruptedException {
        Flux
                .interval(Duration.ofSeconds(1))
                .skip(2)
                .subscribe(data -> log.info("data : {}", data));

        Thread.sleep(5500l);
    }

    @Test
    public void skipTime() throws InterruptedException {
        Flux
                .interval(Duration.ofMillis(300))
                .skip(Duration.ofSeconds(1))
                .subscribe(data -> log.info("data : {}", data));

        Thread.sleep(2000l);
    }

    @Test
    public void take() throws InterruptedException {
        Flux
                .interval(Duration.ofSeconds(1))
                .take(2)
                .subscribe(data -> log.info("data : {}", data));

        Thread.sleep(4000l);
    }

    @Test
    public void takeTime() throws InterruptedException {
        Flux
                .interval(Duration.ofMillis(300))
                .take(Duration.ofSeconds(1))
                .subscribe(data -> log.info("data : {}", data));

        Thread.sleep(2000l);
    }

    @Test
    public void takeLast() {
        Flux
                .just(1, 2, 3, 4, 5)
                .takeLast(2)
                .subscribe(data -> log.info("data {}", data));
    }

    /**
     * takeUntil 이 true가 될때까지 take
     * 평가할 때 사용한 데이터가 포함됨을 주의
     */
    @Test
    public void takeUntil() {
        Flux
                .range(1, 100)
                .takeUntil(num -> num > 5)
                .subscribe(data -> log.info("data {}", data));
    }

    /**
     * takeUntil과 완전히 반대되는 개념
     */
    @Test
    public void takeWhile() {
        Flux.range(1, 100)
                .takeWhile(num -> num < 5)
                .subscribe(data -> log.info("data {}", data));
    }

    @Test
    public void next() {
        Mono<Integer> mono = Flux
                .range(1, 100)
                .next();

        mono.subscribe(data -> log.info("data {}", data));
    }

    @Test
    public void map() {
        Flux.range(1, 10)
                .map(num -> num * -1)
                .subscribe(data -> log.info("data {}", data));
    }

    /**
     * 비동적으로 sequence 의 요소를 flat map 처리
     */
    @Test
    public void flatMap() {
        Flux.range(1, 5)
                .flatMap(num ->
                        Flux.just("key", "value")
                                .publishOn(Schedulers.parallel())
                                .map(data -> data + num)
                )
                .subscribe(data -> log.info("onNext {}", data));
    }

    /**
     * 순차적 합침
     */
    @Test
    public void concat() {
        Flux
                .concat(
                        Flux.just(1, 2, 3),
                        Flux.just(4, 5, 6)
                )
                .subscribe(data -> log.info("data : {}", data));
    }

    @Test
    public void concatDiff() {
        Flux
                .concat(
                        Flux.just(1, 2, 3),
                        Flux.just("4")
                )
                .subscribe(data -> log.info("data : {}", data));
    }

    /**
     * 순자적이지 않고 인터리빙 방식으로 합침
     * 동시에 데이터 리소스 사용
     */
    @Test
    public void merge() throws InterruptedException {
        Flux
                .merge(
                        Flux.range(1, 100)
                                .delayElements(Duration.ofMillis(3)),
                        Flux.range(101, 100)
                                .delayElements(Duration.ofMillis(5))
                )
                .subscribe(data -> log.info("data : {}", data));

        Thread.sleep(1000L);
    }

    @Test
    public void zip() {
        Flux
                .zip(
                        Flux.just(1, 2, 3),
                        Flux.just("h", "d", "c")
                )
                .subscribe(data -> log.info("data : {} {}", data.getT1(), data.getT2()));
    }

    @Test
    public void zipCal() {
        Flux
                .zip(
                        Flux.just(1, 2, 3),
                        Flux.just(1, 2, 3),
                        (x, y) -> x * y
                )
                .subscribe(data -> log.info("data {}", data));
    }

    @DisplayName("최대 8개의 sequence zip 가능")
    @Test
    public void zip2() {
        Flux.zip(
                Flux.just(1, 2, 3),
                Flux.just(1, 2, 3),
                Flux.just(1, 2, 3),
                Flux.just(1, 2, 3),
                Flux.just(1, 2, 3),
                Flux.just(1, 2, 3),
                Flux.just(1, 2, 3),
                Flux.just(1, 2, 3)
        ).subscribe(data -> log.info("data {}", data.getT8()));
    }

    /**
     * and 는 모든 sequence 가 종료된 이후에 complete 메시지만 전달하는 역할
     * 모든 작업이 끝난 시점에 최종적으로 후처리 작업을 수행하기 적합한 operator
     */
    @Test
    public void and() throws InterruptedException {
        Mono
                .just("task 1")
                .delayElement(Duration.ofMillis(10))
                .doOnNext(data -> log.info("# Mono doOnNext {}", data))
                .and(
                        Flux
                                .just("task 2", "task 3")
                                .delayElements(Duration.ofMillis(10))
                                .doOnNext(data -> log.info("# Flux doOnNext {}", data))
                )
                .subscribe(
                        data -> log.info("on next {}", data),
                        error -> log.error("on error: ", error),
                        () -> log.info("complete")
                );

        Thread.sleep(500);
    }

    @Test
    public void collectList() {
        Flux
                .just(1, 2, 3, 4)
                .collectList()
                .subscribe(list -> log.info("data {}", list));
    }

    @Test
    public void collectMap() {
        Flux
                .range(1, 10)
                .collectMap(key -> key,
                        value -> value + 1)
                .subscribe(map -> log.info("data {}", map));
    }

    @Test
    public void doXXX() {
        Flux.range(1, 5)
                .doFinally(signalType -> log.info("# doFinally 1: {}", signalType))
                .doFinally(signalType -> log.info("# doFinally 2: {}", signalType))
                .doOnNext(data -> log.info("# range > doOnNext(): {}", data))
                .doOnRequest(data -> log.info("# doOnRequest(): {}", data))
                .doOnSubscribe(subscription -> log.info("# doOnSubscribe 1"))
                .doFirst(() -> log.info("# doFirst()"))
                .filter(num -> num % 2 == 1)
                .doOnNext(data -> log.info("# filter > doOnNext(): {}", data))
                .doOnComplete(() -> log.info("# doOnComplete()"))
                .subscribe(new BaseSubscriber<Integer>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        request(1);
                    }

                    @Override
                    protected void hookOnNext(Integer value) {
                        log.info("# hookOnNext: {}", value);
                        request(1);
                    }
                });
    }

    @Test
    public void error() {
        Flux
                .range(1, 5)
                .flatMap(num -> {
                            if (num < 3) return Mono.just(num);
                            else return Mono.error(new IllegalArgumentException("test"));
                        }
                ).subscribe(
                        data -> log.info("data {}", data),
                        error -> log.error("error ", error)
                );
    }

    @Test
    public void errorReturn() {
        Flux
                .range(1, 5)
                .flatMap(num -> {
                            if (num < 3) return Mono.just(num);
                            else return Mono.error(new IllegalArgumentException("test"));
                        }
                )
                .onErrorReturn(100)
                .subscribe(
                        data -> log.info("data {}", data),
                        error -> log.error("error ", error)
                );
    }

    /**
     * sequence 리턴하여 대체하는 것
     */
    @Test
    public void errorResume() {
        Flux
                .range(1, 5)
                .flatMap(num -> {
                            if (num != 3) return Mono.just(num);
                            else return Mono.error(new IllegalArgumentException("test"));
                        }
                )
                .onErrorResume(error -> Flux.just(100, 101, 102))
                .subscribe(
                        data -> log.info("data {}", data),
                        error -> log.error("error ", error)
                );
    }

    @Test
    public void errorContinue() {
        Flux
                .range(1, 5)
                .flatMap(num -> {
                            if (num != 3) return Mono.just(num);
                            else return Mono.error(new IllegalArgumentException("test"));
                        }
                )
                .onErrorContinue((error, data) -> log.error("onErrorContinue {}, {}", error.getMessage(), data))
                .subscribe(
                        data -> log.info("data {}", data),
                        error -> log.error("error ", error)
                );
    }

    @Test
    public void reply() {
        AtomicInteger count = new AtomicInteger(0);
        Flux.range(1, 5)
                .flatMap(data -> {
                    if (data == 3 && count.intValue() == 0) {
                        count.getAndIncrement();
                        return Mono.error(new RuntimeException("text"));
                    }
                    return Mono.just(data);
                })
                .doOnSubscribe((subscription) -> log.info("sub {}", subscription))
                .retry(2)
                .subscribe(data -> log.info("data {}", data));
    }

    /**
     * emit된 데이터 사이의 경과 시간을 측정해서 Tuple 형태로 Downstream 에 emit
     */
    @Test
    public void elapsed() throws InterruptedException {
        Flux.range(1, 5)
                .delayElements(Duration.ofMillis(10))
                .elapsed()
                .subscribe(data -> log.info("data {}, time {}", data.getT2(), data.getT1()));

        Thread.sleep(1000);
    }

    /**
     * flatMap 과 반대로 sequence로 분할
     */
    @Test
    public void window() {
        Flux
                .range(1, 100)
                .window(3)
                .flatMap(flux -> {
                    log.info("--------------------------");
                    return flux;
                })
                .subscribe(data -> log.info("data {}", data));
    }

    /**
     * list 로 분할
     */
    @Test
    public void buffer() {
        Flux
                .range(1, 105)
                .buffer(10)
                .subscribe(data -> log.info("data {}", data));
    }

    /**
     * 특정 시간 동안 버퍼 유지 + 버퍼 최대사이즈 지정 기능 합침
     */
    @Test
    public void bufferTimeout() throws InterruptedException {
        Flux
                .range(1, 55)
                .delayElements(Duration.ofMillis(21))
                .bufferTimeout(10, Duration.ofMillis(100))
                .subscribe(data -> log.info("data {}", data));

        Thread.sleep(3000L);
    }

    @Data
    @AllArgsConstructor
    @ToString
    class Book {
        private String isbn;
        private String bookName;
        private String author;
    }

    private List<Book> sample() {
        return List.of(
                new Book(UUID.randomUUID().toString(), "bookname6", "kim"),
                new Book(UUID.randomUUID().toString(), "bookname4", "jung"),
                new Book(UUID.randomUUID().toString(), "bookname5", "kim"),
                new Book(UUID.randomUUID().toString(), "bookname2", "kan"),
                new Book(UUID.randomUUID().toString(), "bookname1", "kan"),
                new Book(UUID.randomUUID().toString(), "bookname3", "jung")
        );
    }

    @Test
    public void groupBy() {

        Flux
                .fromIterable(sample())
                .groupBy(book -> book.getAuthor())
                .flatMap(group -> group
                        .map(book -> book.getAuthor())
                        .collectList())
                .subscribe(data -> log.info("data {}", data));
    }

    @Test
    public void publish() throws InterruptedException {
        ConnectableFlux<Integer> flux = Flux
                .range(1, 5)
                .delayElements(Duration.ofMillis(3L))
                .publish();

        Thread.sleep(5L);
        flux.subscribe(data -> log.info("# sub1 {}", data));

        Thread.sleep(2L);
        flux.subscribe(data -> log.info("#          sub2 {}", data));

        flux.connect(); // emit 시작

        Thread.sleep(10L);
        flux.subscribe(data -> log.info("#                      sub3 {}", data));

        Thread.sleep(2000L);
    }

    /**
     * 지정한 숫자의 구독이 발생하면 자동으로 connect 가되는 publish
     */
    @Test
    public void autoConnect() throws InterruptedException {
        Flux<Integer> flux = Flux
                .range(1, 5)
                .delayElements(Duration.ofMillis(3L))
                .publish()
                .autoConnect();// default minSubscriber 2

        Thread.sleep(5L);
        flux.subscribe(data -> log.info("# sub1 {}", data));

        Thread.sleep(2L);
        flux.subscribe(data -> log.info("#          sub2 {}", data));

        Thread.sleep(10L);
        flux.subscribe(data -> log.info("#                      sub3 {}", data));

        Thread.sleep(2000L);
    }

    /**
     * 모든 구독이 끊기면 처음 부터 다시 시작(upstream 소스에 다시 연결)
     */
    @Test
    public void refCount() throws InterruptedException {
        Flux<Long> flux = Flux
                .interval(Duration.ofMillis(5L))
                .publish()
//                .autoConnect(1);
                .refCount(1);

        Disposable disposable = flux.subscribe(data -> log.info("# sub1 {}", data));

        Thread.sleep(21L);

        disposable.dispose();

        flux.subscribe(data -> log.info("#          sub2 {}", data));

        Thread.sleep(250L);
    }

    @Test
    public void refCount2() throws InterruptedException {
        Flux<Integer> flux = Flux
                .range(1, 10)
                .publish()
                .refCount(1);

        flux.subscribe(data -> log.info("# sub1 {}", data));
        Thread.sleep(100L);

        flux.subscribe(data -> log.info("#          sub2 {}", data));
        Thread.sleep(250L);
    }

    @Test
    public void concatMap() {

        Flux.range(1, 100)
                .concatMap(num -> Flux.just("hi", num))
                .subscribe(data -> log.info("data {}", data));
    }
}
