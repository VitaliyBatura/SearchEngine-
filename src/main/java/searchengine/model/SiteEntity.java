package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "site")
@Getter
@Setter
@NoArgsConstructor
public class SiteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    @Enumerated(EnumType.STRING)
    Status status;

    @UpdateTimestamp
    @Column(name = "status_time", columnDefinition = "DATETIME", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    Date statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    String name;

    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    List<PageEntity> pages;

    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    List<LemmaEntity> lemmas;

    public SiteEntity(String name, String url, Status status, String lastError) {
        this.name = name;
        this.url = url;
        this.status = status;
        this.lastError = lastError;
    }
}
