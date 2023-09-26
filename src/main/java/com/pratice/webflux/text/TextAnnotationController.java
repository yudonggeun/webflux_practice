package com.pratice.webflux.text;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class TextAnnotationController {

    private final TextService textService;

    @GetMapping("/text")
    public Mono addText(){
        return textService.findRandomText();
    }

    @GetMapping("/text/all")
    public Mono getTextsByCondition(@ModelAttribute Mono<SearchCondition> condition){
        return textService.findTextsBy(condition);
    }

    @PostMapping("/text")
    public Mono addText(@RequestParam String text){
        return textService.createText(text);
    }

    @DeleteMapping("/text/{content}")
    public Mono deleteText(@PathVariable String content){
        return textService.delete(content);
    }
}
