package searchengine.services.searching;

import lombok.Setter;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.services.JsoupUtil;
import searchengine.services.LemmaFinder;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class SnippetParser {

    @Setter
    private static int maxSnippetLength;

    private final LemmaFinder lemmaFinder;

    private final Set<String> lemmas;

    private final Set<Snippet> snippetSet = new TreeSet<>();

    private final String text;

    public SnippetParser(Document document, LemmaFinder lemmaFinder, Set<String> lemmas) {
        this.lemmaFinder = lemmaFinder;
        this.lemmas = new HashSet<>(lemmas);
        Elements elements = JsoupUtil.documentContentSelector(document);
        this.text = String.join("\n", elements.eachText());
        findSnippets();
    }

    private void findSnippets() {
        List<Match> matches = new ArrayList<>();
        Pattern.compile("[А-Яа-я]+").matcher(text).results().forEach(match -> {
            lemmas.stream().filter(l -> lemmaFinder.isLemmaApplyWord(l, match.group().toLowerCase(Locale.ROOT)))
                    .findFirst().ifPresent(lemma -> {
                        matches.add(new Match(match.start(), match.end(), lemma, match.group()));
                    });
        });
        IntStream.range(0, matches.size()).forEach(iArray -> {
            Snippet snippet = new Snippet();
            snippetSet.add(snippet);
            int offset = 0;
            boolean isAdded;
            do {
                Match match = matches.get(iArray + offset);
                isAdded = snippet.addMatch(match);
            } while (isAdded && iArray + ++offset < matches.size() - 1);
        });
    }

    public String getSnippet() {
        StringBuilder stringBuilder = new StringBuilder();
        snippetSet.stream().takeWhile(i -> !lemmas.isEmpty() || stringBuilder.length() < maxSnippetLength)
                .filter(i -> i.getLemmaSet().stream().anyMatch(lemmas::contains) &&
                        stringBuilder.length() + i.getSnippetLength() < maxSnippetLength)
                .map(snippet -> {
                    lemmas.removeAll(snippet.getLemmaSet());
                    String string = new SnippetFormatter(snippet, text).toString();
                    snippet.setText(string);
                    return string;
                }).forEach(stringBuilder::append);
        if (!lemmas.isEmpty()) {
            stringBuilder.append("<br/>Не найдено: <s>");
            stringBuilder.append(String.join("</s>, <s>", lemmas));
            stringBuilder.append(".");
        }
        return stringBuilder.toString().replace("\n", " * ");
    }
}
