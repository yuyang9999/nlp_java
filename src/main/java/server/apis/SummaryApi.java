package server.apis;

import article.summary.WeightedArticleSummary;
import lombok.Data;
import org.springframework.web.bind.annotation.*;


@Data
class SummaryInput {
    private String url;
    private int extractNum;
}

@Data
class SummaryResponse {
    private String[] topSentences;
    private String[] allSentences;
    private String title;
    private int extraNum;

    public SummaryResponse(String title, String[] allSentences, int extraNum, String[] topSentences) {
        this.title = title;
        this.allSentences = allSentences;
        this.extraNum = extraNum;
        this.topSentences = topSentences;
    }
}

/*
the following code corresponding the following rest service

        const body = JSON.stringify({
                url: inputUrl,
                extractNum: sentenceCnt
                });

                let options = {
                headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
                },

                method: "POST",
                body: body,
                };

                fetch("http://localhost:8080/api1.0/test", options).then((resp)=>{
                if (resp.ok) {
                resp.json().then((jsonResp)=> {
                this.handleResponse(jsonResp);
                });
                } else {
                console.log('failed');
                }
                });
*/

/**
 * Created by yuyang on 13/9/17.
 */
@RestController
public class SummaryApi {
    private static final String version = "/api1.0/";

    @RequestMapping(version + "summary")
    public String getArticleSummary() {
        return "test";
    }


    @CrossOrigin(origins = "http://localhost:3000", methods = {RequestMethod.POST})
    @RequestMapping(value = version + "test",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"})
    public SummaryResponse test(@RequestBody SummaryInput url) {
        WeightedArticleSummary summary = new WeightedArticleSummary();
        String[] topSentences = summary.getKeySentence(url.getUrl(), url.getExtractNum());
        return new SummaryResponse(summary.getTitle(), summary.getSentences(), url.getExtractNum(), topSentences);
    }

}
