package com.pratice.webflux.text;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TextService {

    private final R2dbcEntityTemplate template;

    public Mono findRandomText() {

        return template.select(Text.class)
                .from("text")
                .count()
                .flatMap(count -> {
                    log.info("count : {}", count);
                    if (count == 0) return Mono.empty();
                    long target = new Random().nextLong(count);
                    return template.select(Text.class)
                            .from("text")
                            .all()
                            .skip(target)
                            .take(1)
                            .next();
                })
                .map(text -> {
                    log.info("text {}", text.getContent());
                    return text.getContent();
                });
    }

    public Mono findTextsBy(Mono<SearchCondition> condition) {
        return condition
                .flatMap(con -> {
                    return template.select(Text.class)
                            .from("text")
                            .matching(query(
                                            where("content").like("%" + con.getText() + "%")
                                    )
                            )
                            .all()
                            .collectList();
                })
                .onErrorReturn(List.of());
    }

    public Mono createText(String text) {
        return Mono.just(text)
                .map(t -> {
                    Text entity = new Text();
                    entity.setContent(t);
                    return entity;
                })
                .flatMap(entity -> template.insert(entity));
    }

    public Mono delete(String content) {
        return template.delete(Text.class)
                .from("text")
                .matching(query(where("content").is(content))
                ).all();
    }
}
