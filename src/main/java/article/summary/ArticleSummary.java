package article.summary;

/**
 * Created by yuyang on 28/8/17.
 */
public interface ArticleSummary {
    String[] getKeySentence(String title, String article, int topCnt);
    String[] getKeySentence(String url, int topCnt);
}
