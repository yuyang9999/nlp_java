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
        String url = "http://tech.163.com/17/0911/11/CU2305CK00097U7T.html";

        ArticleSummary as = new WeightedArticleSummary();
        String[] topSentences = as.getKeySentence(url, 5);
        for (String s: topSentences) {
            System.out.println(s);
        }
    }
}