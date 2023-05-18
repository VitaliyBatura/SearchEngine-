package searchengine.dto.searching;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchingResponse {

    private Boolean result;

    private Integer count;

    private List<SearchData> data;
}
