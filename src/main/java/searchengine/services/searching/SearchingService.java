package searchengine.services.searching;

import searchengine.dto.searching.SearchingResponse;

public interface SearchingService {

    SearchingResponse search(String query, Integer offset, Integer limit);

    SearchingResponse search(String query, String site, Integer offset, Integer limit);
}
