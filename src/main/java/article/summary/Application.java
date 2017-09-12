package article.summary;

/**
 * Created by yuyang on 12/9/17.
 */
public class Application {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("please input the url");
            return;
        }

        String url = args[0];

        ArticleSummary as = new WeightedArticleSummary();
        String[] topSentences = as.getKeySentence(url, 5);
        for (String s: topSentences) {
            System.out.println(s);
        }
    }
}
