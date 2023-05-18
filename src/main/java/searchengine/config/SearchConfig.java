package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import searchengine.services.searching.Snippet;
import searchengine.services.searching.SnippetParser;

import javax.annotation.PostConstruct;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "search-settings")
public class SearchConfig {

    public Integer snippetLength;

    @PostConstruct
    private void init() {
        SnippetParser.setMaxSnippetLength(snippetLength);
        Snippet.setMaxSnippetLength(snippetLength);
    }
}
