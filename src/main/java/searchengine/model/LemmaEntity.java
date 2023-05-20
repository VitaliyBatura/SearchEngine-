package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lemma", uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "lemma"}))
public class LemmaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private SiteEntity site;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private Integer frequency;

    @OneToMany(mappedBy = "lemma", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<IndexEntity> indexes;

    public LemmaEntity(SiteEntity site, String lemma, Integer frequency) {
        this.site = site;
        this.lemma = lemma;
        this.frequency = frequency;
    }
}
