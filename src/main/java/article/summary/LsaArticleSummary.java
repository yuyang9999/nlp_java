package article.summary;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.JResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.helper.StringUtil;

import java.util.*;

/**
 * Created by yuyang on 29/8/17.
 */
@Slf4j
public class LsaArticleSummary implements ArticleSummary {
    private TextParser textParser;

    static private final float smooth = 0.4f;
    static private final int min_dimensions = 3;

    @Override
    public String[] getKeySentence(String title, String article, int topCnt) {
        Map<String, Integer> dictionary = createDictionary(article);

        String[] sentences = textParser.splitSentences(article);
        Matrix wordFreqMatrix = createMatrix(sentences, dictionary);
        Matrix normFreqMatrix = computeTermFrequency(wordFreqMatrix);
        List<Integer> sentenceRankIndex = getRankIndexes(normFreqMatrix);
        return getTopRankSentence(sentences, sentenceRankIndex, topCnt);
    }

    @Override
    public String[] getKeySentence(String url, int topCnt) {
        HtmlFetcher fetcher = new HtmlFetcher();
        JResult res = null;
        try {
            res = fetcher.fetchAndExtract(url, 1000, false);
        } catch (Exception e) {
            log.error("{}", e);
        }

        if (res != null) {
            String text = res.getText();
            String title = res.getTitle();

            if (!StringUtil.isBlank(text) && !StringUtil.isBlank(title)) {
                return getKeySentence(title, text, topCnt);
            }
        }

        return new String[0];
    }

    public LsaArticleSummary() {
        textParser = new TextParser();
    }

    /***
     * create word - index dictionary from text
     * @param text input text
     * @return word - index dictionary
     */
    private Map<String, Integer> createDictionary(String text) {
        Map<String, Integer> ret = new HashMap<String, Integer>();

        Set<String> wordSet = new HashSet<String>();

        List<Map<String, Object>> allWords = textParser.getKeywords(text).getKeyWords();
        for (Map<String, Object>m: allWords) {
            wordSet.add((String)m.get(TextParser.KeyWords.nameKey));
        }

        int idx = 0;
        for (String s: wordSet) {
            ret.put(s, idx++);
        }

        return ret;
    }

    /***
     * create the word - sentence matrix
     * @param sentences input sentences
     * @param dictionary word - index dictionary
     * @return word - sentence matrix
     */
    private Matrix createMatrix(String[] sentences, Map<String, Integer> dictionary) {
        int sentenceCnt = sentences.length;
        int wordCnt = dictionary.size();

        Matrix matrix = new Matrix(wordCnt, sentenceCnt, 0.0);
        for (int col = 0; col < sentenceCnt; col++) {
            String sentence = sentences[col];
            String[] words = textParser.splitWords(sentence);
            for (String w: words) {
                if (dictionary.containsKey(w)) {
                    Integer wordIdx = dictionary.get(w);
                    Double prevVal = matrix.get(wordIdx, col);
                    matrix.set(wordIdx, col, prevVal + 1);
                }
            }
        }

        return matrix;
    }

    /***
     * normalized the frequency matrix
     * @param matrix word - sentence matrix
     * @return normalized frequency matrix
     */
    private Matrix computeTermFrequency(Matrix matrix) {
        Matrix ret = new Matrix(matrix.getRowDimension(), matrix.getColumnDimension(), 0.0);
        for (int j = 0; j < matrix.getColumnDimension(); j++) {
            Double maxVal = 0.0;
            for (int i = 0; i < matrix.getRowDimension(); i++) {
                Double item = matrix.get(i, j);
                maxVal = Math.max(maxVal, item);
            }
            if (maxVal != 0.0) {
                for (int i = 0; i < matrix.getRowDimension(); i++) {
                    Double item = matrix.get(i, j);
                    if (item > 0.0) {
                        Double freq = item / maxVal;
                        Double smoothed = smooth + (1.0 - smooth) * freq;
                        ret.set(i, j, smoothed);
                    }
                }
            }
        }

        return ret;
    }

    /***
     * get the rank index for sentences
     * @param matrix word - sentence matrix
     * @return rank indexes for each sentence
     */
    private List<Integer> getRankIndexes(Matrix matrix) {
        SingularValueDecomposition svd = new SingularValueDecomposition(matrix);

        double[] ss = svd.getSingularValues();
        int dimensions = Math.max(min_dimensions, ss.length);

        for (int i = 0; i < ss.length; i++) {
            if (i < dimensions) {
                ss[i] = ss[i] * ss[i];
            } else {
                ss[i] = 0.0;
            }
        }

        List<Double> ranks = new LinkedList<Double>();
        Matrix mv = svd.getV().transpose();
        for (int i = 0; i < mv.getRowDimension(); i++) {
            double totalScore = 0.0;
            for (int j = 0; j < mv.getColumnDimension(); j++) {
                double item = mv.get(i, j);
                totalScore += item * item * ss[j];
            }
            totalScore = Math.sqrt(totalScore);

            ranks.add(totalScore);
        }

        List<Map<String, Object>> rankList = new LinkedList<Map<String, Object>>();
        for (int i = 0; i < ranks.size(); i++) {
            double rank = ranks.get(i);
            Map<String, Object> obj = new HashMap<String, Object>();
            obj.put("index", i);
            obj.put("rank", rank);
            rankList.add(obj);
        }

        Collections.sort(rankList, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                Double d1 = (Double)o1.get("rank");
                Double d2 = (Double)o2.get("rank");
                return - d1.compareTo(d2);
            }
        });

        List<Integer> sortRankIdxes = new LinkedList<Integer>();
        for (Map<String, Object>m: rankList) {
            Integer index = (Integer)m.get("index");
            sortRankIdxes.add(index);
        }

        return sortRankIdxes;
    }

    /***
     * get the sentences with top N ranks
     * @param sentences input sentences
     * @param rankIndexes rank indexes
     * @param N the returned sentences count
     * @return N sentences
     */
    private String[] getTopRankSentence(String[] sentences, List<Integer> rankIndexes, int N) {
        //filter sentences ends with question mark
        List<Integer> filterIndex = new LinkedList<Integer>();
        for (Integer idx: rankIndexes) {
            String sentence = sentences[idx];
            if (!sentence.endsWith("？")) {
                filterIndex.add(idx);
            }
        }

        List<Integer> topRanks = filterIndex.subList(0, Math.min(N, filterIndex.size()));

        Collections.sort(topRanks);

        List<String> ret = new LinkedList<String>();
        for (Integer idx: topRanks) {
            ret.add(sentences[idx]);
        }

        return ret.toArray(new String[ret.size()]);
    }
}
