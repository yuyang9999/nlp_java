import article.summary.ArticleSummary;
import article.summary.WeightedArticleSummary;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Created by yuyang on 28/8/17.
 */
public class WeightedArticleSummaryTest extends TestCase {
    @Test
    public void testURLArticleSummary() {
        String url = "http://news.163.com/17/0827/00/CSQ8QP5F0001885B.html";

        ArticleSummary as = new WeightedArticleSummary();
        String[] topSentences = as.getKeySentence(url, 5);
        for (String s: topSentences) {
            System.out.println(s);
        }
    }
}