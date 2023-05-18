package searchengine.services.indexing;

import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import searchengine.config.SearchBot;
import searchengine.dto.indexing.SiteData;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.services.IndexingServiceImpl;
import searchengine.services.JsoupUtil;
import searchengine.services.LemmaFinder;


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
    private final String uriHost;
    private final AtomicBoolean run;
    private final URL url;
    private final LemmaFinder lemmaFinder;

    public SiteIndexingTask(SiteData siteData, SiteEntity site, SearchBot searchBot,
                            IndexingServiceImpl indexingService, LemmaFinder lemmaFinder) {
        this.siteData = siteData;
        this.url = siteData.getUrl();
        String uriHost = this.url.getHost();
        this.uriHost = uriHost.startsWith("www.") ? uriHost.substring(4) : uriHost;
        this.indexingService = indexingService;
        this.searchBot = searchBot;
        this.site = site;
        this.runningUrls = new CopyOnWriteArraySet<>();
        this.run = new AtomicBoolean(true);
        this.lemmaFinder = lemmaFinder;
    }

    private SiteIndexingTask(URL url, SiteIndexingTask siteIndexingTask) {
        this.siteData = siteIndexingTask.siteData;
        this.url = url;
        this.uriHost = siteIndexingTask.uriHost;
        this.indexingService = siteIndexingTask.indexingService;
        this.searchBot = siteIndexingTask.searchBot;
        this.site = siteIndexingTask.site;
        this.runningUrls = siteIndexingTask.runningUrls;
        this.run = siteIndexingTask.run;
        this.lemmaFinder = siteIndexingTask.lemmaFinder;
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
        UrlType uriType = validateUrl();
        if (!(uriType == UrlType.SITE_PAGE)) {
            return true;
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
            Map<String, Integer> lemmas = lemmaFinder.collectLemmas(JsoupUtil.documentContentSelector(document).text());
            synchronized (SiteIndexingTask.class) {
                indexingService.saveLemmasIndexes(page, lemmas);
            }
            List<SiteIndexingTask> taskList = walkSiteLinks(document);
            return taskList.stream().allMatch(ForkJoinTask::join);
        }
        return true;
    }

    private UrlType validateUrl() {
        if (!(url.getHost().equals(uriHost) || url.getHost().endsWith(".".concat(uriHost)))) {
            return UrlType.OTHER_SITE;
        } else if (url.getPath().contains(".") && !url.getPath().endsWith(".html")) {
            return UrlType.SITE_FILE;
        } else if (indexingService.isPageExistByPath(url.toString())) {
            return UrlType.PAGE_IN_TABLE;
        } else {
            return UrlType.SITE_PAGE;
        }
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
