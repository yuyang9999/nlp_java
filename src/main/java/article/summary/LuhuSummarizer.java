package article.summary;

import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.JResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.helper.StringUtil;

import java.util.*;

/**
 * Created by yuyang on 30/8/17.
 */
@Slf4j
public class LuhuSummarizer implements ArticleSummary {
    private TextParser textParser;

    static private final float significant_percentage = 1.0f;

    @Override
    public String[] getKeySentence(String title, String article, int topCnt) {
        String[] sentences = textParser.splitSentences(article);
        String[] allWords = textParser.getArticleWords(article);

        TfDocumentModel model = new TfDocumentModel(allWords);
        int best_words_cnt = (int)(allWords.length * significant_percentage);
        String[] best_words = model.getMostFrequestTerms(best_words_cnt);

        return getBestSentences(sentences, best_words, topCnt);
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
            System.out.println(text);
            System.out.println(title);

            if (!StringUtil.isBlank(text) && !StringUtil.isBlank(title)) {
                return getKeySentence(title, text, topCnt);
            }
        }

        return new String[0];
    }

    public LuhuSummarizer() {
        textParser = new TextParser();
    }

    private String[] getBestSentences(String[] sentences, String[] bestWords, int count) {
        List<Map<String, Object>> sentenceList = new ArrayList<Map<String, Object>>();

        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i];

            //skip sentence ends with a question mark
            if (sentence.endsWith("？")) {
                continue;
            }

            float rating = getSentenceRating(sentence, bestWords);

            Map<String, Object> map = new HashMap<String, Object>();
            map.put("sentence", sentence);
            map.put("rating", rating);
            map.put("order", i);
            sentenceList.add(map);
        }

        //sort the sentence list according to the rating
        Collections.sort(sentenceList, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                Float rate1 = (Float)o1.get("rating");
                Float rate2 = (Float)o2.get("rating");
                return -rate1.compareTo(rate2);
            }
        });

        List<Map<String, Object>> topSentenceList = sentenceList.subList(0, Math.min(count, sentenceList.size()));
        //sort the top sentence list according to the sentence order in the article
        Collections.sort(topSentenceList, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                Integer order1 = (Integer)o1.get("order");
                Integer order2 = (Integer)o2.get("order");

                return order1.compareTo(order2);
            }
        });

        List<String> retSentences = new ArrayList<String>();
        for (Map<String, Object>m: topSentenceList) {
            String sentence = (String)m.get("sentence");
            retSentences.add(sentence);
        }

        return retSentences.toArray(new String[retSentences.size()]);
    }

    private float getSentenceRating(String sentence, String[] bestWords) {
        List<List<Integer>> chunks = new ArrayList<List<Integer>>();

        Set<String> bestWordSet = new HashSet<String>();
        bestWordSet.addAll(Arrays.asList(bestWords));

        String[] sentenceWords = textParser.splitWords(sentence);

        boolean inChunk = false;

        for (String w: sentenceWords) {
            if (bestWordSet.contains(w) && !inChunk) {
                inChunk = true;
                chunks.add(new ArrayList<Integer>());
            } else if (inChunk) {
                boolean isSignificant = bestWordSet.contains(w);
                List<Integer> lastChunk = chunks.get(chunks.size() - 1);
                lastChunk.add(isSignificant ? 1 : 0);
            }
        }

        return getSentenceTrunkRating(chunks);
    }

    private float getSentenceTrunkRating(List<List<Integer>> chunks) {
        float maxChunkRating = 0.0f;
        for (List<Integer> chunk: chunks) {
            //remove the trailing 0
            for (int i = chunk.size() - 1; i >= 0; i--) {
                int value = chunk.get(i);
                if (value == 0) {
                    chunk.remove(i);
                } else {
                    break;
                }
            }

            if (chunk.size() == 0) {
                continue;
            }

            int sum = 0;
            for (int i: chunk) {
                sum += i;
            }

            float value = sum * sum / (chunk.size() * 1.0f);
            maxChunkRating = Math.max(value, maxChunkRating);
        }

        return maxChunkRating;
    }
}


class TfDocumentModel {
    private int maxFrequency;
    private Map<String, Integer> termFreqMap;
    private List<String> significentWords;

    public TfDocumentModel(String[] words) {
        termFreqMap = new HashMap<String, Integer>();
        for (String w: words) {
            String w1 = w.trim().toLowerCase();
            if (!termFreqMap.containsKey(w1)) {
                termFreqMap.put(w1, 0);
            }
            int prevCnt = termFreqMap.get(w1);
            termFreqMap.put(w1, prevCnt + 1);
        }

        int maxFreq = 0;
        for (Map.Entry<String,Integer>entry: termFreqMap.entrySet()) {
            maxFreq = Math.max(maxFreq, entry.getValue());
        }
        maxFrequency = maxFreq;

        List<Map<String, Object>> sigWords = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Integer>entry: termFreqMap.entrySet()) {
            if (entry.getValue() > 1) {
                Map<String, Object> wordCntMap = new HashMap<String, Object>();
                wordCntMap.put("name", entry.getKey());
                wordCntMap.put("count", entry.getValue());
                sigWords.add(wordCntMap);
            }
        }

        Collections.sort(sigWords, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                Integer i1 = (Integer)o1.get("count");
                Integer i2 = (Integer)o2.get("count");
                return -i1.compareTo(i2);
            }
        });

        significentWords = new ArrayList<String>();
        for (Map<String, Object>map: sigWords) {
            significentWords.add((String)map.get("name"));
        }
    }

    public String[] getMostFrequestTerms(int count) {
        int end = Math.min(count, significentWords.size());
        return significentWords.subList(0, end).toArray(new String[end]);
    }

    public int getTermFrequency(String term) {
        if (!termFreqMap.containsKey(term)) {
            return 0;
        }

        return termFreqMap.get(term);
    }

    public float getNormalizedTermFrequency(String term, float smooth) {
        int freq = getTermFrequency(term);

        float normFreq = freq / (maxFrequency * 1.0f);

        return smooth + (1.0f - smooth) * normFreq;
    }
}