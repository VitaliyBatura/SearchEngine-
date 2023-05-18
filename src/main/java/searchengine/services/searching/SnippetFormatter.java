package searchengine.services.searching;

import java.util.Arrays;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SnippetFormatter {

    private final static Pattern patternSentence = Pattern.compile("[\n?!.]+");

    private final static Pattern patternUpWord = Pattern.compile("(\\b[А-Я])");

    private final Snippet snippet;

    private final int suffixLength = 100;
    private final String text;

    public SnippetFormatter(Snippet snippet, String text) {
        this.snippet = snippet;
        this.text = text;
    }

    @Override
    public String toString() {
        List<Match> matches = snippet.getMatches();
        StringBuilder stringBuilder = new StringBuilder();
        Match first = matches.get(0);
        Match last = matches.get(matches.size() - 1);
        int start = Math.max(first.getStart() - suffixLength, 0);
        int end = Math.min(last.getEnd() + suffixLength, text.length());
        String newPrefix = trimPrefix(text.substring(start, first.getStart()));
        String newPostfix = trimPostfix(text.substring(last.getEnd(), end));
        stringBuilder.append(newPrefix);
        if (first.equals(last)) {
            addBoldWord(text, first, stringBuilder);
        } else {
            addMatchesToSuffix(matches, stringBuilder);
        }
        stringBuilder.append(newPostfix);
        if (!stringBuilder.substring(stringBuilder.length() - 2).equals("\n")) {
            stringBuilder.append(" ... ");
        }
        return stringBuilder.toString();
    }

    private String trimPrefix(String part) {
        String[] sentenceSplit = part.split(patternSentence.pattern());
        if (sentenceSplit.length > 1) {
            return sentenceSplit[sentenceSplit.length - 1];
        }
        List<MatchResult> matches = patternUpWord.matcher(part).results().collect(Collectors.toList());
        if (!matches.isEmpty()) {
            MatchResult matchResult = matches.get(matches.size() - 1);
            return part.substring(matchResult.start());
        }
        String[] spaceSplit = part.split("\\s+");
        if (spaceSplit.length > 1) {
            return Arrays.stream(spaceSplit, 1, sentenceSplit.length)
                    .collect(Collectors.joining(" ")).concat(" ");
        }
        return part;
    }

    private String trimPostfix(String part) {
        String[] sentenceSplit = part.split(patternSentence.pattern());
        if (sentenceSplit.length > 1) {
            return sentenceSplit[0];
        }
        List<MatchResult> matches = patternUpWord.matcher(part).results().collect(Collectors.toList());
        if (!matches.isEmpty()) {
            MatchResult matchResult = matches.get(0);
            return part.substring(0, matchResult.start());
        }
        String[] spaceSplit = part.split("\\s+");
        if (spaceSplit.length > 1) {
            return " ".concat(Arrays.stream(spaceSplit, 0, spaceSplit.length - 1)
                    .collect(Collectors.joining(" ")));
        }
        return part;
    }

    private void addMatchesToSuffix(Iterable<Match> matches, StringBuilder stringBuilder) {
        Match prevMatch = null;
        for (Match match : matches) {
            if (prevMatch != null) {
                stringBuilder.append(text, prevMatch.getEnd(), match.getStart());
            }
            addBoldWord(text, match, stringBuilder);
            prevMatch = match;
        }
    }

    private void addBoldWord(String text, Match match, StringBuilder stringBuilder) {
        stringBuilder.append("<b>");
        stringBuilder.append(text, match.getStart(), match.getEnd());
        stringBuilder.append("</b>");
    }
}
