package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.support.PagedListHolder;
import org.springframework.stereotype.Service;
import searchengine.dto.searching.SearchData;
import searchengine.dto.searching.SearchingResponse;
import searchengine.model.*;
import searchengine.services.searching.SnippetParser;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchingServiceImpl implements SearchingService {

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;

    private final SiteRepository siteRepository;

    private final LemmaFinder lemmaFinder;

    @Override
    public SearchingResponse search(String query, Integer offset, Integer limit) {
        Set<String> lemmaSet = lemmaFinder.getLemmasSet(query);
        List<LemmaEntity> lemmas = lemmaRepository.findAllByLemmaInOrderByFrequencyAsc(lemmaSet);
        List<SearchData> searchDataList = getSearchPages(lemmas, lemmaSet);
        return new SearchingResponse(true, searchDataList.size(), pageOfList(searchDataList, offset, limit));
    }

    @Override
    public SearchingResponse search(String query, String site, Integer offset, Integer limit) {
        Set<String> lemmaSet = lemmaFinder.getLemmasSet(query);
        SiteEntity siteEntity = siteRepository.findByUrlEquals(site);
        List<LemmaEntity> lemmas = lemmaRepository
                .findAllByLemmaInAndSiteEqualsOrderByFrequencyAsc(lemmaSet, siteEntity);
        List<SearchData> searchDataList = getSearchPages(lemmas, lemmaSet);
        return new SearchingResponse(true, searchDataList.size(), pageOfList(searchDataList, offset, limit));
    }

    private List<SearchData> getSearchPages(List<LemmaEntity> lemmas, Set<String> lemmaSet) {
        TreeSet<SearchData> searchDataTreeSet = new TreeSet<>(Comparator.comparing(SearchData::getRelevance)
                .thenComparing(SearchData::getSnippet).reversed());
        HashMap<SiteEntity, Set<PageEntity>> pagesMap = new HashMap<>();
        lemmas.forEach(lemma -> {
            if (pagesMap.containsKey(lemma.getSite())) {
                pagesMap.get(lemma.getSite()).retainAll(lemma.getIndexes().stream().map(IndexEntity::getPage)
                        .collect(Collectors.toList()));
            } else {
                Set<PageEntity> list = lemma.getIndexes().stream().map(IndexEntity::getPage)
                        .collect(Collectors.toSet());
                pagesMap.put(lemma.getSite(), list);
            }
        });
        AtomicReference<Float> maxRelevance = new AtomicReference<>(0F);
        pagesMap.forEach((site, pages) -> {
            for (PageEntity page : pages) {
                Document document = Jsoup.parse(page.getContent());
                SnippetParser snippetParser = new SnippetParser(document, lemmaFinder, lemmaSet);
                SearchData searchData = new SearchData();
                searchData.setSite(site.getUrl());
                searchData.setSiteName(site.getName());
                searchData.setUri(page.getPath().substring(1));
                searchData.setTitle(document.title());
                searchData.setSnippet(snippetParser.getSnippet());
                float relevance = indexRepository.findAllByPageAndLemmaIn(page, lemmas).stream()
                        .map(IndexEntity::getRank).reduce(0F, Float::sum);
                if (maxRelevance.get() < relevance) {
                    maxRelevance.set(relevance);
                }
                searchData.setRelevance(relevance);
                searchDataTreeSet.add(searchData);
            }
        });
        searchDataTreeSet.forEach(searchData -> searchData.setRelevance(maxRelevance.get()
                / searchData.getRelevance()));
        return new ArrayList<>(searchDataTreeSet);
    }

    private <T> List<T> pageOfList(List<T> searchDataList, Integer offset, Integer limit) {
        PagedListHolder<T> pages = new PagedListHolder<>(searchDataList);
        pages.setPageSize(limit);
        pages.setPage(offset);
        return pages.getPageList();
    }
}
