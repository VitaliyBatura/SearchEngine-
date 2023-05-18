package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SearchBot;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.SiteData;
import searchengine.model.*;
import searchengine.services.indexing.AbstractIndexingTask;
import searchengine.services.indexing.PageIndexingTask;
import searchengine.services.indexing.SiteIndexingTask;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;

    private final PageRepository pageRepository;

    private final SiteRepository siteRepository;

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;

    private final SearchBot searchBot;

    private final ForkJoinPool forkJoinPool;

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private final Set<AbstractIndexingTask> runningIndexingTasks = new CopyOnWriteArraySet<>();

    private final Map<AbstractIndexingTask, Future<Void>> mapTaskThread = new Hashtable<>();

    private final LemmaFinder lemmaFinder;

    @Override
    public IndexingResponse startSitesIndexing() throws IndexingServiceException {
        List<Site> sitesList = sites.getSites();
        if (!runningIndexingTasks.isEmpty()) {
            throw new IndexingServiceException("Индексация запущена!");
        } else if (sitesList.isEmpty()) {
            throw new IndexingServiceException("В конфигурационном файле не указаны сайты для индексации.");
        }
        List<SiteData> sitesData = getSiteConfigs(sitesList);
        for (SiteData siteData : sitesData) {
            SiteEntity siteEntity = new SiteEntity(siteData.getName(), siteData.getUrl().toString(),
                    Status.INDEXING, "");
            SiteIndexingTask siteIndexingTask = new SiteIndexingTask(siteData, siteEntity, searchBot, this,
                    lemmaFinder);
            mapTaskThread.put(siteIndexingTask, threadPoolTaskExecutor.submit(() -> threadSiteIndexing(siteIndexingTask,
                    siteEntity, siteData)));
        }
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopSitesIndexing() throws IndexingServiceException {
        if (isIndexing()) {
            throw new IndexingServiceException("Индексация не запущена.");
        }
        runningIndexingTasks.forEach(AbstractIndexingTask::stopCompute);
        new ArrayList<>(runningIndexingTasks).forEach(task -> {
            Future<Void> future = mapTaskThread.get(task);
            if (future != null) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse pageIndexing(String urlString) throws IndexingServiceException {
        if (!isIndexing()) {
            throw new IndexingServiceException("Индексация уже запущена.");
        }
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new IndexingServiceException("Плохой url: " + urlString);
        }
        Site site = isUrlInConfig(url);
        if (site == null) {
            throw new IndexingServiceException("Данная страница находится за пределами сайтов" +
                    ", указанных в конфигурационном файле");
        }
        SiteEntity newSite = siteRepository.findByName(site.getName());
        SiteEntity siteEntity = newSite != null ? newSite : new SiteEntity(site.getName(),
                site.getUrl(), Status.INDEXING, "");
        PageEntity pageEntity = pageRepository.findByPath(url.toString());
        if (pageEntity != null) {
            deletePage(pageEntity);
        }
        PageIndexingTask pageIndexingTask = new PageIndexingTask(url, siteEntity, searchBot, this,
                lemmaFinder);
        mapTaskThread.put(pageIndexingTask, threadPoolTaskExecutor.submit(() ->
                threadPageIndexing(pageIndexingTask, siteEntity)));
        return new IndexingResponse(true);
    }

    @Override
    public boolean isIndexing() {
        return mapTaskThread.isEmpty();
    }

    public Void threadSiteIndexing(SiteIndexingTask siteIndexingTask, SiteEntity siteEntity, SiteData siteData) {
        runningIndexingTasks.add(siteIndexingTask);
        try {
            siteRepository.deleteAllByName(siteData.getName());
            saveSite(siteEntity);
            Boolean inv = forkJoinPool.invoke(siteIndexingTask);
            if (inv) {
                siteEntity.setStatus(Status.INDEXED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            siteEntity.setLastError(e.getClass().getName());
            siteEntity.setStatus(Status.FAILED);
        }
        saveSite(siteEntity);
        runningIndexingTasks.remove(siteIndexingTask);
        mapTaskThread.remove(siteIndexingTask);
        return null;
    }

    private Void threadPageIndexing(PageIndexingTask pageIndexingTask, SiteEntity siteEntity) {
        runningIndexingTasks.add(pageIndexingTask);
        try {
            Boolean inv = forkJoinPool.invoke(pageIndexingTask);
            if (inv) {
                siteEntity.setStatus(Status.INDEXED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            siteEntity.setLastError(e.getClass().getName());
            siteEntity.setStatus(Status.FAILED);
        }
        saveSite(siteEntity);
        runningIndexingTasks.remove(pageIndexingTask);
        mapTaskThread.remove(pageIndexingTask);
        return null;
    }

    public void saveSite(SiteEntity siteEntity) {
        try {
            siteRepository.save(siteEntity);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void savePage(PageEntity pageEntity) {
        pageRepository.save(pageEntity);
    }

    public boolean isPageExistByPath(String path) {
        return pageRepository.existsByPath(path);
    }

    public void saveLemmasIndexes(PageEntity page, Map<String, Integer> lemmas) {
        List<IndexEntity> indexEntities = new ArrayList<>();
        List<LemmaEntity> lemmaEntities = lemmas.entrySet().stream().map(item -> {
            LemmaEntity lemma = lemmaRepository.findBySiteIdAndLemma(page.getSite().getId(), item.getKey());
            if (lemma == null) {
                lemma = new LemmaEntity(page.getSite(), item.getKey(), 1);
            } else {
                lemma.setFrequency(lemma.getFrequency() + 1);
            }
            indexEntities.add(new IndexEntity(page, lemma, item.getValue().floatValue()));
            return lemma;
        }).collect(Collectors.toList());
        lemmaRepository.saveAll(lemmaEntities);
        indexRepository.saveAll(indexEntities);
    }

    @Transactional
    public void deletePage(PageEntity pageEntity) {
        List<IndexEntity> indexes = pageEntity.getIndexes();
        for (IndexEntity index : indexes) {
            LemmaEntity lemma = index.getLemma();
            if (lemma.getFrequency() <= 1) {
                lemmaRepository.delete(lemma);
            } else {
                lemma.setFrequency(lemma.getFrequency() - 1);
                lemmaRepository.save(lemma);
            }
            indexRepository.delete(index);
        }
        pageRepository.delete(pageEntity);
    }

    private List<SiteData> getSiteConfigs(List<Site> sitesList) throws IndexingServiceException {
        List<SiteData> sitesData = new ArrayList<>();
        for (Site site : sitesList) {
            SiteData siteData = new SiteData();
            siteData.setName(site.getName());
            try {
                URL url = new URL(site.getUrl());
                siteData.setUrl(url);
            } catch (MalformedURLException e) {
                throw new IndexingServiceException("Плохой url: " + site.getUrl());
            }
            sitesData.add(siteData);
        }
        return sitesData;
    }

    private Site isUrlInConfig(URL url) {
        String uriHost = url.getHost().startsWith("www.") ? url.getHost().substring(4) : url.getHost();
        List<Site> sitesList = sites.getSites();
        return sitesList.stream().filter(s -> s.getUrl().contains(uriHost)).findAny().orElse(null);
    }
}
