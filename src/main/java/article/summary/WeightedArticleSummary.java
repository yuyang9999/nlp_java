package article.summary;

import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.JResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.helper.StringUtil;

import java.util.*;

/**
 * Created by yuyang on 28/8/17.
 */
@Slf4j
public class WeightedArticleSummary implements ArticleSummary {
    private TextParser textParser;

    static private final float idealLength = 20.0f;
    static private final int topKeyWordCnt = 10;

    static private final String sentKeyTotalScore=  "totalScore";
    static private final String sentKeySentence = "sentence";
    static private final String sentKeyOrder = "order";

    private String mArticle;
    private String mTitle;
    private String[] mSentences;

    public WeightedArticleSummary() {
        textParser = new TextParser();
    }

    public String[] getKeySentence(String title, String article, int topCnt) {
        if ("".equals(article) || "".equals(title)) {
            return new String[0];
        }

//        CorefResolution corefResolution = new CorefResolution(article);
//        String[] sentences = corefResolution.decorefSentences();
//        System.out.println("begin print coref resolution");
//        for (String s: sentences) {
//            System.out.println(s);
//        }
//        System.out.println("end print coref resolution");

        this.mArticle = article;
        this.mTitle = title;

        String[] sentences = textParser.splitSentences(article);

        this.mSentences = sentences;

        String cleanTitle = textParser.removePunctations(title);
        String[] titleWords = textParser.splitText(cleanTitle);
        TextParser.KeyWords kws = textParser.getKeywords(article);

        List<Map<String, Object>> keywords = kws.getKeyWords();

        List<Map<String, Object>> topKeyWords = keywords.subList(0, Math.min(topKeyWordCnt, keywords.size()));

        List<Map<String, Object>> sentenceScores = computeScore(sentences, titleWords, topKeyWords);

        String[] topSentences = sortScore(sentenceScores, topCnt);

        return topSentences;
    }


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
            System.out.println("text " + text);
            System.out.println("title " + title);

            if (!StringUtil.isBlank(text) && !StringUtil.isBlank(title)) {
                return getKeySentence(title, text, topCnt);
            }
        }

        return new String[0];
    }

    public String getArticle() {
        return this.mArticle;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public String[] getSentences() {
        return this.mSentences;
    }

    /***
     * get the top sentences with high score
     * @param sentProps the list of sentence property
     * @param N the sentences count will return
     * @return the top N sentences with highest scores and sorted by the order in the text
     */
    private String[] sortScore(List<Map<String, Object>> sentProps, int N) {
        //filter sentences ends with question mark
        List<Map<String, Object>> filterSentProps = new LinkedList<Map<String, Object>>();
        for (Map<String, Object> m: sentProps) {
            String sentence = (String)m.get(sentKeySentence);
            if (!sentence.endsWith("？")) {
                filterSentProps.add(m);
            }
        }

        List<Map<String, Object>> topN = filterSentProps.subList(0, Math.min(N, filterSentProps.size()));

        Collections.sort(topN, new Comparator<Map<String, Object>>() {
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                Integer idx1 = (Integer)o1.get(sentKeyOrder);
                Integer idx2 = (Integer)o2.get(sentKeyOrder);
                return idx1.compareTo(idx2);
            }
        });

        List<String> ret = new LinkedList<String>();
        for (Map<String, Object> map: topN) {
            String sentence = (String)map.get(sentKeySentence);
            ret.add(sentence);
        }

        return ret.toArray(new String[ret.size()]);
    }

    /***
     * calculate each sentence score and return sentences with decreasing score order
     * @param sentences sentences in the article
     * @param titleWords title words
     * @param topKWs top key words
     * @return the list of sentence map in the order of decreasing score
     */
    private List<Map<String, Object>> computeScore(String[] sentences, String[] titleWords, List<Map<String, Object>> topKWs) {
        //get top words list
        List<String> keywords = new LinkedList<String>();
        for (Map<String, Object> m: topKWs) {
            keywords.add((String)m.get(TextParser.KeyWords.nameKey));
        }

        List<Map<String, Object>> ret = new LinkedList<Map<String, Object>>();
        for (int i = 0; i < sentences.length; i++) {
            String origSentence = sentences[i];
            String[] words = textParser.splitText(origSentence);
            words = textParser.removePunctionsForWordsArray(words);

            float sbsScore = sbsFeature(words, keywords, topKWs);
            float dbsScore = dbsFeature(words, keywords, topKWs);

            float titleScore = getTitleScore(titleWords, words);
            float sentLenScore = getSentenceLengthScore(words);
            float sentPosScore = getSentencePositionScore(i, sentences.length);
            float keywordFreq = (sbsScore + dbsScore) / 2.0f * 10.0f;
            float totalScore = (titleScore * 1.5f + keywordFreq * 2.0f + sentLenScore * 0.5f + sentPosScore * 1.0f) / 4.0f;

            if (Float.isNaN(totalScore)) {
                log.error("score is NaN");
            }

            Map<String, Object> sentProp = new HashMap<String, Object>();
            sentProp.put(sentKeyTotalScore, totalScore);
            sentProp.put(sentKeySentence, origSentence);
            sentProp.put(sentKeyOrder, i);

            ret.add(sentProp);
        }

        //sort the sentences according to the sentence score
        Collections.sort(ret, new Comparator<Map<String, Object>>() {
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                Float score1 = (Float)o1.get(sentKeyTotalScore);
                Float score2 = (Float)o2.get(sentKeyTotalScore);

                return -score1.compareTo(score2);
            }
        });

        return ret;
    }

    /***
     * get the sentence title score
     * @param titleWords title words
     * @param sentenceWords sentence words
     * @return the title score for one sentence
     */
    private float getTitleScore(String[] titleWords, String[] sentenceWords) {
        if (titleWords.length == 0) {
            return 0.0f;
        }

        Set<String> twSet = new HashSet<String>();
        Set<String> swSet = new HashSet<String>();

        twSet.addAll(Arrays.asList(titleWords));

        swSet.addAll(Arrays.asList(sentenceWords));

        twSet.retainAll(swSet);
        return twSet.size() / (titleWords.length * 1.0f);
    }

    /***
     * get the sentence length score
     * @param words sentence
     * @return score with regard to the sentence length
     */
    private float getSentenceLengthScore(String[] words) {
        return (idealLength - Math.abs(idealLength - words.length)) / idealLength;
    }

    /***
     * get the sentence position score
     * @param i sentence index in the article
     * @param sentenceCount total sentence count in the article
     * @return score with regard to sentence position in the article
     */
    private float getSentencePositionScore(int i, int sentenceCount) {
        float normalized = i / (sentenceCount * 1.0f);

        if (normalized > 0 && normalized <= 0.1) {
            return 0.17f;
        } else if (normalized > 0.1 && normalized <= 0.2) {
            return 0.23f;
        } else if (normalized > 0.2 && normalized <= 0.3) {
            return 0.14f;
        } else if (normalized > 0.3 && normalized <= 0.4) {
            return 0.08f;
        } else if (normalized > 0.4 && normalized <= 0.5) {
            return 0.05f;
        } else if (normalized > 0.5 && normalized <= 0.6) {
            return 0.04f;
        } else if (normalized > 0.6 && normalized <= 0.7) {
            return 0.06f;
        } else if (normalized > 0.7 && normalized <= 0.8) {
            return 0.04f;
        } else if (normalized > 0.8 && normalized <= 0.9) {
            return 0.04f;
        } else if (normalized > 0.9 && normalized <= 1.0) {
            return 0.15f;
        } else {
            return 0;
        }
    }

    /***
     * get the sbs score
     * @param words sentence words
     * @param topKeyWords top keyword list
     * @param topKWList top keyword prop list
     * @return sbs score
     */
    private float sbsFeature(String[] words, List<String> topKeyWords, List<Map<String, Object>> topKWList) {
        float score = 0.0f;

        if (words.length == 0) {
            return 0.0f;
        }

        for (String word: words) {
            word = word.toLowerCase();
            int index = topKeyWords.indexOf(word);
            if (index != -1) {
                score += (Float)(topKWList.get(index).get(TextParser.KeyWords.totalScoreKey));
            }
        }

        return 1.0f / words.length * score;
    }

    /***
     * get the dbs score
     * @param words sentence words
     * @param topKeyWords top keyword list
     * @param topKWList top keyword prop list
     * @return dbs score
     */
    private float dbsFeature(String[] words, List<String> topKeyWords, List<Map<String, Object>> topKWList) {
        Set<String> wordsSet = new HashSet<String>();
        Set<String> keywordsSet = new HashSet<String>();

        wordsSet.addAll(Arrays.asList(words));
        keywordsSet.addAll(topKeyWords);

        wordsSet.retainAll(keywordsSet);
        int k = wordsSet.size() + 1;

        float sum = 0.0f;
        Map<String, Object> firstWord = new HashMap<String, Object>();
        Map<String, Object> secondWord = new HashMap<String, Object>();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            int index = topKeyWords.indexOf(word);
            if (index != -1) {
                if (firstWord.size() == 0) {
                    firstWord.put("i", i);
                    firstWord.put("score", topKWList.get(index).get(TextParser.KeyWords.totalScoreKey));
                } else {
                    secondWord.clear();
                    secondWord.putAll(firstWord);
                    firstWord.clear();
                    firstWord.put("i", i);
                    firstWord.put("score", topKWList.get(index).get(TextParser.KeyWords.totalScoreKey));
                    int distance = (Integer)firstWord.get("i") - (Integer)secondWord.get("i");

                    sum += ((Float)firstWord.get("score") *(Float)secondWord.get("score")) / (distance * distance);
                }
            }
        }

        return (1.0f / k * (k + 1.0f)) * sum;
    }
}
