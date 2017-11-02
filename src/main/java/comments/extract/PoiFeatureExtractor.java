package comments.extract;

import org.jsoup.helper.StringUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by yuyang on 2/11/17.
 */
public class PoiFeatureExtractor {
    private static final Pattern sSentenceSplitReg = Pattern.compile("[。，！？,!?\\s~]");
    private static Set<String> sFeatureWords;
    private static Set<String> sNegWords;
    private Map<String, List<String>> mFeatSenMap;

    static {
        String[] fws = {"特色", "新意", "亮点", "出众", "独有"};
        String[] ngws = {"没", "不", "缺少", "无", "失望", "一般", "少", "小", "但是", "淡", "慢", "普通", "环境", "菜"};

        sFeatureWords = new HashSet<String>();
        sFeatureWords.addAll(Arrays.asList(fws));

        sNegWords = new HashSet<String>();
        sNegWords.addAll(Arrays.asList(ngws));
    }

    private boolean isSentenceAllChinese(String s) {
        if (s.length() == 0) {
            return false;
        }

        boolean ret = true;
        for (int i = 0; i < s.length(); i++) {
            int codePoint = s.codePointAt(i);
            if (Character.UnicodeScript.of(codePoint) != Character.UnicodeScript.HAN) {
                ret = false;
                break;
            }
        }

        return ret;
    }


    private Map<String, List<String>> findSubSentenceWithFeatureWords(List<String> comments, Set<String> featureWords, Set<String> negWords) {
        Map<String, List<String>> featCommMap = new HashMap<String, List<String>>();

        for (String s: comments) {
            String[] subSs = sSentenceSplitReg.split(s);
            for (String ss: subSs) {
                if (!isSentenceAllChinese(ss)) {
                    continue;
                }

                //check if the sentence has feature word in it
                String featureWord = null;
                for (String fw: featureWords) {
                    if (ss.contains(fw)) {
                        featureWord = fw;
                        break;
                    }
                }
                if (featureWord == null) {
                    continue;
                }

                //check if it has neg word
                boolean hasNeg = false;
                for (String neg: negWords) {
                    if (ss.contains(neg)) {
                        hasNeg = true;
                    }
                }
                if (hasNeg) {
                    continue;
                }

                //check if the sentence has ner in it
                SentenceGrammaAnalyzer analyzer = new SentenceGrammaAnalyzer(ss, false);
                if (StringUtil.isBlank(analyzer.extractNERForKeyword(featureWord))) {
                    continue;
                }

                if (!featCommMap.containsKey(featureWord)) {
                    featCommMap.put(featureWord, new ArrayList<String>());
                }
                featCommMap.get(featureWord).add(ss);
            }
        }

        return featCommMap;
    }

    public PoiFeatureExtractor(List<String> comments) {
        if (comments.size() > 0) {
           mFeatSenMap = findSubSentenceWithFeatureWords(comments, sFeatureWords, sNegWords);
        }
    }

    public List<String> getFeatureSentences() {
        List<String> ret = new ArrayList<String>();
        if (mFeatSenMap.size() > 0) {
            for (Map.Entry<String, List<String>> entry: mFeatSenMap.entrySet()) {
                ret.addAll(entry.getValue());
            }
        }

        return ret;
    }


    public static void main(String[] args) {
        String filePath = "/Users/yuyang/Desktop/recommend_generation/comments.csv";
        int numOfShop = 10;

        List<String> shopComments = new ArrayList<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            int cnt = -1;
            String line;
            while ((line = reader.readLine()) != null) {
                cnt += 1;
                if (cnt == 0) {
                    //skip the first line
                    continue;
                }

                shopComments.add(line.split("\t")[1]);

                if (cnt >= numOfShop) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<List<String>> shopFeatureSentences = new ArrayList<List<String>>();

        for (String commentsStr: shopComments) {
            String[] comments = commentsStr.substring(2, commentsStr.length() - 2).split("\",\"");
            PoiFeatureExtractor extractor = new PoiFeatureExtractor(Arrays.asList(comments));
            List<String> sentences = extractor.getFeatureSentences();
            if (sentences.size() > 0) {
                shopFeatureSentences.add(sentences);
            }
        }

        System.out.println(shopFeatureSentences);
    }

}
