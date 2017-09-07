import article.summary.ArticleSummary;
import article.summary.LsaArticleSummary;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Created by yuyang on 29/8/17.
 */
public class LsaArticleSummaryTest extends TestCase {
    @Test
    public void testLsa() {
        String url = "http://news.163.com/17/0827/00/CSQ8QP5F0001885B.html";

        ArticleSummary as = new LsaArticleSummary();
        String[] topSentences = as.getKeySentence(url, 5);
        for (String s: topSentences) {
            System.out.println(s);
        }
    }
}