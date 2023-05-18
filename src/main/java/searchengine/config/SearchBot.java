package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bot")
public class SearchBot {

    private String userAgent;

    private String referrer;

    private Integer timeout;

    public Document getJsoupDocument(String url) throws IOException {
        return Jsoup.connect(url).userAgent(getUserAgent()).referrer(getReferrer()).timeout(getTimeout())
                .method(Connection.Method.GET).execute().parse();
    }
}
