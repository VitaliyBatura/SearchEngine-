package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Long> {

    @Transactional
    void deleteAllByName(String name);

    SiteEntity findByName(String name);

    SiteEntity findByUrlEquals(String url);
}
