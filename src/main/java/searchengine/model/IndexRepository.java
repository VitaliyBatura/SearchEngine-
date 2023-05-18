package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Long> {

    List<IndexEntity> findAllByPageAndLemmaIn(PageEntity page, Iterable<LemmaEntity> lemmaEntities);
}
