package searchengine.services;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class JsoupUtil {

    public static Elements documentContentSelector(Document document) {
        return document.getAllElements().not("nav,aside,header,footer,[class*=menu]")
                .select("h1,h2,h3,h4,h5,h6,p,ul,ol,span");
    }
}
