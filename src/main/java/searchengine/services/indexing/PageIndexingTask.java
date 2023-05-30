package searchengine.services.indexing;

import org.jsoup.HttpStatusException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import searchengine.config.SearchBot;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.services.JsoupUtil;
import searchengine.services.LemmaFinder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class PageIndexingTask extends AbstractIndexingTask {

    private final IndexingServiceImpl indexingService;
    private final SearchBot searchBot;
    private final SiteEntity site;
    private final AtomicBoolean run;
    private final URL url;
    private final LemmaFinder lemmaFinder;

    public PageIndexingTask(URL url, SiteEntity site, SearchBot searchBot, IndexingServiceImpl indexingService,
                            LemmaFinder lemmaFinder) {
        this.url = url;
        this.indexingService = indexingService;
        this.searchBot = searchBot;
        this.site = site;
        this.run = new AtomicBoolean(true);
        this.lemmaFinder = lemmaFinder;
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
        if (!run.get()) {
            return false;
        }
        PageEntity page = new PageEntity();
        page.setPath(url.toString());
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
            return true;
        } catch (UnsupportedMimeTypeException | MalformedURLException e) {
            return false;
        } catch (IOException e) {
            site.setStatus(Status.FAILED);
            site.setLastError(e.getClass().getName() + ":" + e.getMessage());
            indexingService.saveSite(site);
            return false;
        }
        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(JsoupUtil.documentContentSelector(document).text());
        synchronized (PageIndexingTask.class) {
            indexingService.saveLemmasIndexes(page, lemmas);
        }
        return true;
    }
}
