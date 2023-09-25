package com.pratice.webflux.practice;

import reactor.core.publisher.Mono;

public class ContextTestExample {
    public static Mono<String> getSecretMessage(Mono<String> keySource) {
        return keySource.zipWith(Mono.deferContextual(ctx ->
                        Mono.just((String) ctx.get("secretKey"))
                ))
                .filter(tp -> tp.getT1().equals(tp.getT2()))
                .transformDeferredContextual(
                        (mono, ctx) -> mono.map(notUse -> ctx.get("secretMessage"))
                );
    }
}
