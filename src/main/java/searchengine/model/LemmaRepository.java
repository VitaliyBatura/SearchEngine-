package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Long> {

    LemmaEntity findBySiteIdAndLemma(Long siteId, String lemma);

    List<LemmaEntity> findAllByLemmaInOrderByFrequencyAsc(Iterable<String> string);

    List<LemmaEntity> findAllByLemmaInAndSiteEqualsOrderByFrequencyAsc(Iterable<String> string, SiteEntity site);

}
