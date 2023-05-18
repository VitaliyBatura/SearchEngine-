package searchengine.services.searching;

import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.Objects;

@Getter
@Setter
public class Match implements Comparable<Match> {

    private final int start;

    private final int end;

    private final String lemma;

    private final String word;

    public Match(int start, int end, String lemma, String word) {
        this.start = start;
        this.end = end;
        this.lemma = lemma;
        this.word = word;
    }

    @Override
    public int compareTo(Match o) {
        return Comparator.comparing(Match::getStart).compare(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Match match = (Match) obj;
        return start == match.start && end == match.end && lemma.equals(match.lemma);
    }
}
