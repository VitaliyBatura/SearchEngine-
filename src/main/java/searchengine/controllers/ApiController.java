package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.searching.SearchingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    private final IndexingService indexingService;

    private final SearchingService searchingService;

    @Autowired
    public ApiController(StatisticsService statisticsService, IndexingService indexingService,
                         SearchingServiceImpl searchingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchingService = searchingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() throws IndexingServiceException {
        return ResponseEntity.ok(indexingService.startSitesIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() throws IndexingServiceException {
        return ResponseEntity.ok(indexingService.stopSitesIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> pageIndexing(@RequestParam(name = "url") String url) throws
            IndexingServiceException {
        return ResponseEntity.ok(indexingService.pageIndexing(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchingResponse> search(@RequestParam(name = "query") String query,
                                                    @RequestParam(name = "site", required = false) String site,
                                                    @RequestParam(name = "offset", required = false) Integer offset,
                                                    @RequestParam(name = "limit", required = false) Integer limit) {
        offset = offset == null ? 0 : offset;
        limit = limit == null ? 20 : limit;
        if (site == null) {
            return ResponseEntity.ok(searchingService.search(query, offset, limit));
        } else {
            return ResponseEntity.ok(searchingService.search(query, site, offset, limit));
        }
    }
}
