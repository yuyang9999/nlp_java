package comments.extract;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLWord;
import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by yuyang on 27/10/17.
 */
public class CommentsExtractor {
    private List<List<String>> mAllShopComments;
    private List<List<String>> mExtractPhrases;

    static private Set<String> mBadWords;
    static private final Pattern mSplitPatten = Pattern.compile("[。！？，\t]");

    static {
        mBadWords = new HashSet<String>();
        InputStream badWordsStream = CommentsExtractor.class.getResourceAsStream("/bad_words.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(badWordsStream));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || "".equals(line)) {
                    continue;
                }
                mBadWords.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CommentsExtractor() {
        getCommentsList();
        extractUserfulPhrase();
    }

    private void getCommentsList() {
        String path = "/Users/yuyang/Desktop/recommend_generation/comments.csv";

        List<List<String>> ret = new ArrayList<List<String>>();

        BufferedReader br;

        try {
            br = new BufferedReader(new FileReader(path));
            String line;
            int cnt = 0;
            while ((line = br.readLine()) != null) {
                List<String> oneShopComments = new ArrayList<String>();

                cnt += 1;
                if (cnt == 1) {
                    continue;
                }
                String[] parts = line.split("\t");
                String comments= parts[1];
                comments = comments.substring(2, comments.length() - 2);
                for (String s: comments.split("\",\"")) {
                    oneShopComments.add(s);
                }

                ret.add(oneShopComments);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mAllShopComments = ret;
    }

    private void extractUserfulPhrase() {
        List<List<String>> ret = new ArrayList<List<String>>();

        for (List<String> l: mAllShopComments) {
            List<String> oneShop = new ArrayList<String>();

            for (String s: l) {
                for (String ss: mSplitPatten.split(s)) {
                    if (ss.length() == 0) {
                        continue;
                    }

                    if (shouldFilterSentence(ss)) {
                        continue;
                    }

                    if (isGoodSubSentence(ss)) {
                        System.out.println("choose phrase " + ss);
                        oneShop.add(s);
                    }
                }
            }

            ret.add(oneShop);
        }

        mExtractPhrases = ret;
    }

    private boolean shouldFilterSentence(String sentence) {
        boolean ret = false;
        for (String w: mBadWords) {
            if (sentence.contains(w)) {
                ret = true;
                break;
            }
        }

        Result result = ToAnalysis.parse(sentence);
        boolean hasNoun = false, hasAdj = false;

        for (Term t: result.getTerms()) {
            if (t.getNatureStr().startsWith("n")) {
                hasNoun = true;
            }
            if (t.getNatureStr().startsWith("a")) {
                hasAdj = true;
            }
            if (hasNoun && hasAdj) {
                break;
            }
        }
        if (!hasAdj || !hasNoun) {
            ret = true;
        }

        return ret;
    }

    private boolean isGoodSubSentence(String text) {
        CoNLLSentence sentence = HanLP.parseDependency(text);

        CoNLLWord[] wordArray = sentence.getWordArray();

        boolean hasZhuWei = false;
        for (CoNLLWord w: wordArray) {
            if ("主谓关系".equals(w.DEPREL) &&
                    "核心关系".equals(w.HEAD.DEPREL)) {
                hasZhuWei = true;
            }
        }
        CoNLLWord lastWord = wordArray[wordArray.length - 1];
        if ("核心关系".equals(lastWord.DEPREL) &&
                "a".equals(lastWord.POSTAG) &&
                hasZhuWei) {
            return true;
        }

        return false;
    }


    public static void main(String[] args) {
        CommentsExtractor extractor = new CommentsExtractor();
        System.out.println(extractor.mExtractPhrases);
    }
}
