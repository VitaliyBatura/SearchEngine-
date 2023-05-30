package searchengine.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Long> {

    boolean existsByPath(String path);

    PageEntity findByPath(String path);
}
