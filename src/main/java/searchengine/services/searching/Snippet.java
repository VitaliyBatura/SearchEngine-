package searchengine.services.searching;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
public class Snippet implements Comparable<Snippet> {

    @Setter
    private static int maxSnippetLength;

    private final Set<String> lemmaSet = new HashSet<>();

    private final List<Match> matches = new ArrayList<>();

    private int countUpCaseLetter = 0;

    @Setter
    private String text = "";

    public List<Match> getMatches() {
        return new ArrayList<>(matches);
    }

    public boolean addMatch(Match match) {
        if (!matches.isEmpty() && match.getEnd() - matches.get(0).getStart() > maxSnippetLength) {
            return false;
        }
        if (Character.isUpperCase(match.getWord().charAt(0))) {
            countUpCaseLetter++;
        }
        String lemma = match.getLemma();
        lemmaSet.add(lemma);
        matches.add(match);
        return true;
    }

    public int getSnippetLength() {
        if (text.isEmpty()) {
            if (matches.isEmpty()) {
                return 0;
            } else if (matches.size() == 1) {
                return matches.get(0).getEnd() - matches.get(0).getStart();
            } else {
                return matches.get(matches.size() - 1).getEnd() - matches.get(0).getStart();
            }
        }
        return text.length();
    }

    @Override
    public int compareTo(Snippet o) {
        return Comparator.comparing(Snippet::getLemmaSet, Comparator.comparingInt(Set::size))
                .thenComparing(Snippet::getCountUpCaseLetter)
                .thenComparing(Snippet::getMatches, Comparator.comparing(List::size))
                .thenComparing(Snippet::getText, Comparator.comparingInt(String::length)).compare(o, this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matches);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Snippet)) return false;
        Snippet snippet = (Snippet) obj;
        return lemmaSet.equals(snippet.lemmaSet) && matches.equals(snippet.matches);
    }
}

