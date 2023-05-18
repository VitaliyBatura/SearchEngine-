package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {

    IndexingResponse startSitesIndexing() throws IndexingServiceException;

    IndexingResponse stopSitesIndexing() throws IndexingServiceException;

    IndexingResponse pageIndexing(String url) throws IndexingServiceException;

    boolean isIndexing();
}
