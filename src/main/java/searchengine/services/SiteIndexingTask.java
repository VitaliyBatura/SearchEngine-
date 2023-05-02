package searchengine.services;

import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import searchengine.config.SearchBot;
import searchengine.dto.indexing.SiteData;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class SiteIndexingTask extends AbstractIndexingTask {

    private final IndexingServiceImpl indexingService;
    private final SearchBot searchBot;
    private final SiteEntity site;
    private final SiteData siteData;
    private final Set<String> runningUrls;
    private final String urlHost;
    private final AtomicBoolean run;
    private final URL url;

    public SiteIndexingTask(SiteData siteData, SiteEntity site, SearchBot searchBot, IndexingServiceImpl indexingService) {
        this.siteData = siteData;
        this.url = siteData.getUrl();
        String urlHost = this.url.getHost();
        this.urlHost = urlHost.startsWith("www.") ? urlHost.substring(4) : urlHost;
        this.indexingService = indexingService;
        this.searchBot = searchBot;
        this.site = site;
        this.runningUrls = new CopyOnWriteArraySet<>();
        this.run = new AtomicBoolean(true);
    }

    private SiteIndexingTask(URL url, SiteIndexingTask siteIndexingTask) {
        this.siteData = siteIndexingTask.siteData;
        this.url = url;
        this.urlHost = siteIndexingTask.urlHost;
        this.indexingService = siteIndexingTask.indexingService;
        this.searchBot = siteIndexingTask.searchBot;
        this.site = siteIndexingTask.site;
        this.runningUrls = siteIndexingTask.runningUrls;
        this.run = siteIndexingTask.run;
    }

    @Override
    public void stopCompute() {
        if (!isDone()) {
            run.set(false);
            site.setStatus(Status.FAILED);
            site.setLastError("Индексация остановлена!");
            indexingService.saveSite(site);
        }
    }

    @Override
    protected Boolean compute() {
        if (!runningUrls.add(url.toString())) {
            return true;
        } else if (!run.get()) {
            return false;
        }
        if (url.toString().startsWith(site.getUrl()) && !url.toString().endsWith(".pdf")) {
            PageEntity page = new PageEntity();
            page.setPath(site.getUrl().endsWith("/") ? url.toString().replace(site.getUrl(), "/") :
                    url.toString().replace(site.getUrl(), ""));
            page.setSite(site);
            Document document;
            try {
                document = searchBot.getJsoupDocument(url.toString());
                page.setContent(document.outerHtml());
                page.setCode(document.connection().response().statusCode());
                indexingService.savePage(page);
                indexingService.saveSite(site);
            } catch (HttpStatusException e) {
                page.setContent(e.getMessage());
                page.setCode(e.getStatusCode());
                indexingService.savePage(page);
                indexingService.saveSite(site);
                return true;
            } catch (UnsupportedMimeTypeException | MalformedURLException e) {
                return true;
            } catch (IOException e) {
                run.set(false);
                site.setStatus(Status.FAILED);
                site.setLastError(e.getClass().getName() + ":" + e.getMessage());
                indexingService.saveSite(site);
                return false;
            }
            List<SiteIndexingTask> taskList = walkSiteLinks(document);
            return taskList.stream().allMatch(ForkJoinTask::join);
        }
        return true;
    }

    private List<SiteIndexingTask> walkSiteLinks(Document document) {
        List<SiteIndexingTask> taskList = new ArrayList<>();
        for (Element element : document.select("a")) {
            String urlLink = element.attr("abs:href");
            URL newUrl;
            try {
                newUrl = new URL(urlLink);
                newUrl = new URL(newUrl.getProtocol(), newUrl.getHost(), newUrl.getPath());
            } catch (MalformedURLException e) {
                continue;
            }
            SiteIndexingTask siteIndexingTask = new SiteIndexingTask(newUrl, this);
            taskList.add(siteIndexingTask);
            siteIndexingTask.fork();
        }
        return taskList;
    }
}
