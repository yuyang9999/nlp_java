package article.summary;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.recognition.impl.StopRecognition;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.jsoup.helper.StringUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created by yuyang on 25/8/17.
 */
@Slf4j
public class TextParser {
    static private Set<Character> punctuations;
    static private Set<Character> splitPuncs;
    static private StopRecognition stopFilter;
    static private Set<String> splitPuncStrs;

    static {
        stopFilter = new StopRecognition();
        punctuations = new HashSet<Character>();
        try {
            InputStream is = TextParser.class.getResourceAsStream("/stop_words.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replace("\n", "");
                if (!"".equals(line)) {
                    stopFilter.insertStopWords(line);
                }
            }

            is = TextParser.class.getResourceAsStream("/punctuations.txt");
            reader = new BufferedReader(new InputStreamReader(is));
            while ((line = reader.readLine()) != null) {
                line = line.replace("\n", "");
                if (!"".equals(line)) {
                    for (char ch : line.toCharArray()) {
                        punctuations.add(ch);
                    }
                }
            }
        } catch (Exception e) {
            log.error("{}", e);
        }

        splitPuncs = new HashSet<Character>();
        splitPuncs.add('。');
        splitPuncs.add('！');
        splitPuncs.add('？');

        splitPuncStrs = new HashSet<String>();
        splitPuncStrs.add("。");
        splitPuncStrs.add("！");
        splitPuncStrs.add("？");
    }

    /***
     * remove text punctuation
     * @param text input text
     * @return text without punctuation
     */
    public String removePunctations(String text) {
        String ret = "";
        if (text == null | "".equals(text)) {
            return ret;
        }

        for (char ch : text.toCharArray()) {
            if (!punctuations.contains(ch)) {
                ret += ch;
            }
        }

        return ret;
    }

    /***
     * split the text into sentences
     * @param text input text
     * @return sentences array
     */
    public String[] splitSentences(String text) {
        List<String> sentences = new LinkedList<String>();
        String[] paras = text.split("\n");
        for (String para : paras) {
            if ("".equals(para)) {
                continue;
            }

            List<String> paraSentences = splitOneParagraph(para);
            sentences.addAll(paraSentences);
        }

        return sentences.toArray(new String[sentences.size()]);
    }

    /***
     * split text into words by using ansj
     * @param text input text
     * @return words array
     */
    public String[] splitText(String text) {
        if (text == null || "".equals(text)) {
            return new String[0];
        }

        List<String> words = new LinkedList<String>();

        Result splitResult = ToAnalysis.parse(text).recognition(stopFilter);
        for (Term term : splitResult.getTerms()) {
            String word = term.getRealName();
            if (!StringUtil.isBlank(word)) {
                words.add(word.toLowerCase().trim());
            }
        }

        return words.toArray(new String[words.size()]);
    }

    public class KeyWords {
        public static final String nameKey = "name";
        public static final String countKey = "count";
        public static final String totalScoreKey = "totalScore";

        @Getter
        @Setter
        private List<Map<String, Object>> keyWords;

        @Getter
        @Setter
        private int wordCnt;
    }

    public String[] removePunctionsForWordsArray(String[] words) {
        List<String> ret = new ArrayList<String>();
        for (String w: words) {
            if (w.length() == 1) {
                if (punctuations.contains(w.charAt(0))) {
                    continue;
                }
            }

            ret.add(w);
        }

        return ret.toArray(new String[ret.size()]);
    }


    /***
     * split text into words and order the words according to the frequencies
     * @param text input text
     * @return KeyWords class instance which holds words weight
     */
    public KeyWords getKeywords(String text) {
        int wordCnt = 0;
        String[] sentences = splitSentences(text);
        Map<String, Integer> wordDic = new HashMap<String, Integer>();
        for (String s : sentences) {
            String[] words = splitText(s);
            words = removePunctionsForWordsArray(words);
            for (String w : words) {
                wordCnt += 1;
                if (!wordDic.containsKey(w)) {
                    wordDic.put(w, 0);
                }
                wordDic.put(w, wordDic.get(w) + 1);
            }
        }

        List<Map<String, Object>> allWords = new LinkedList<Map<String, Object>>();
        for (Map.Entry<String, Integer> entry : wordDic.entrySet()) {
            String word = entry.getKey();
            Integer cnt = entry.getValue();
            float score = 1.0f * cnt / wordCnt;

            Map<String, Object> wordProp = new HashMap<String, Object>();
            wordProp.put(KeyWords.nameKey, word);
            wordProp.put(KeyWords.countKey, cnt);
            wordProp.put(KeyWords.totalScoreKey, score);
            allWords.add(wordProp);
        }

        Collections.sort(allWords, new Comparator<Map<String, Object>>() {
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                Integer cnt1 = (Integer) o1.get(KeyWords.countKey);
                Integer cnt2 = (Integer) o2.get(KeyWords.countKey);
                return -cnt1.compareTo(cnt2);
            }
        });

        KeyWords kws = new KeyWords();
        kws.setKeyWords(allWords);
        kws.setWordCnt(wordCnt);
        return kws;
    }

    public String[] getArticleWords(String text) {
        List<String> ret = new ArrayList<String>();
        String[] sentences = splitSentences(text);
        for (String sentence: sentences) {
            for (String w: splitText(sentence)) {
                ret.add(w);
            }
        }

        return ret.toArray(new String[ret.size()]);
    }

    private List<String> splitOneParagraph(String para) {
        List<String> sentences = new LinkedList<String>();
        String collected = "";
        for (int i = 0; i < para.length(); i++) {
            collected += para.charAt(i);

            if (splitPuncs.contains(para.charAt(i))) {
                //check if the after character is "
                if (i < para.length() - 1 && para.charAt(i + 1) == '”') {
                    collected += para.charAt(i + 1);
                    i += 1;
                }
                if (!"".equals(collected)) {
                    sentences.add(collected);
                    collected = "";
                }
            }
            if (i == para.length() - 1 && !"".equals(collected)) {
                sentences.add(collected);
            }
        }
        return sentences;
    }


    static public String mergeAllTermsWithoutPunctions(String[] terms) {
        String ret = "";
        for (String term: terms) {
            if (term.length() != 1) {
                ret += term;
            } else {
                if (!punctuations.contains(term.charAt(0))) {
                    ret += term;
                }
            }
        }

        return ret;
    }
}
