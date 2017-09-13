import article.summary.ArticleSummary;
import article.summary.WeightedArticleSummary;
import com.hankcs.hanlp.HanLP;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.List;

/**
 * Created by yuyang on 28/8/17.
 */
public class WeightedArticleSummaryTest extends TestCase {
    @Test
    public void testURLArticleSummary() {
        String url = "http://tech.163.com/17/0911/07/CU1NO3MP00097U81.html";

        ArticleSummary as = new WeightedArticleSummary();
        String[] topSentences = as.getKeySentence(url, 5);
        for (String s: topSentences) {
            System.out.println(s);
        }
    }
}