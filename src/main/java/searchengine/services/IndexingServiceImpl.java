package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import searchengine.config.SearchBot;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.SiteData;
import searchengine.model.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SearchBot searchBot;
    private final ForkJoinPool forkJoinPool;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final Set<AbstractIndexingTask> runningIndexingTasks = new CopyOnWriteArraySet<>();
    private final Map<AbstractIndexingTask, Future<Void>> mapTaskThread = new Hashtable<>();

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
            SiteIndexingTask siteIndexingTask = new SiteIndexingTask(siteData, siteEntity, searchBot, this);
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
}
