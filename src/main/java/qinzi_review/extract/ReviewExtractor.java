package qinzi_review.extract;

import CommonUtility.Utility;
import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.jsoup.helper.StringUtil;

import java.io.*;
import java.util.*;

/**
 * Created by yuyang on 28/11/17.
 */
public class ReviewExtractor {
    private static class FilterTemplate {
        private List<String> nouns;
        private List<String> adjs;

        public FilterTemplate(String nounsStr, String adjsStr, String seperator) {
            String[] ns = nounsStr.split(seperator);
            String[] ajs = adjsStr.split(seperator);

            nouns = new ArrayList<String>();
            adjs = new ArrayList<String>();

            for (String n: ns) {
                n = n.trim();
                if (!StringUtil.isBlank(n)) {
                    nouns.add(n);
                }
            }
            for (String a: ajs) {
                a = a.trim();
                if (!StringUtil.isBlank(a)) {
                    adjs.add(a);
                }
            }
        }

        public boolean isSentenceMatchTemplate(String sentence) {
            if (StringUtil.isBlank(sentence)) {
                return false;
            }

            boolean ret = false;
            boolean hasNoun = false, hasAdj = false;
            for (String s: nouns) {
                if (sentence.contains(s)) {
                    hasNoun = true;
                    break;
                }
            }
            for (String s: adjs) {
                if (sentence.contains(s)) {
                    hasAdj = true;
                    break;
                }
            }

            ret = hasNoun & hasAdj;
            return ret;
        }
    }

    static private List<FilterTemplate> templates;


//    static private List<String> babyRefs;
//    static private List<String> goodWords;
    static private List<String> badWords;
    static private final int maxSubsenCnt = 4;

    static {
        templates = loadTemlates("/extractor_templates.txt", ";", " ");
        badWords = loadFileContentents("/bad_adj.txt");
    }

    /**
     * load templates
     * @param filePath template file path
     * @param sep1 separator between nouns and adjs
     * @param sep2 separator between nouns or between adjs
     * @return a list of FilterTemplates
     */
    static private List<FilterTemplate> loadTemlates(String filePath, String sep1, String sep2) {
        List<FilterTemplate> ret = new ArrayList<FilterTemplate>();
        InputStream is = ReviewExtractor.class.getResourceAsStream(filePath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (!StringUtil.isBlank(line)) {
                    String[] parts = line.split(sep1);
                    assert(parts.length == 2);
                    FilterTemplate t = new FilterTemplate(parts[0], parts[1], sep2);
                    ret.add(t);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    static private List<String> loadFileContentents(String path) {
        List<String> ret = new ArrayList<String>();

        InputStream is = ReviewExtractor.class.getResourceAsStream(path);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (!StringUtil.isBlank(line)) {
                    ret.add(line);
                }
            }
            reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    static private List<String> getReviews(String path, boolean skipHead) {
        List<String> ret = new ArrayList<String>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));
            int cnt = 0;
            String line = null;
            while ((line = reader.readLine()) != null) {
                cnt ++;
                if (cnt == 1 && skipHead) {
                    continue;
                }

                String[] parts = line.split("\t");
                String review = parts[2];
                review = review.substring(1, review.length() -  2);
                review = review.replaceAll("\\\\n", "\n");
                review = review.replaceAll("\\\\r", "\n");

                ret.add(review);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }


    static private boolean isSentenceContainOneWordInArray(String sentence, List<String> words) {
        boolean ret = false;

        if (StringUtil.isBlank(sentence)) {
            return false;
        }

        for (String w: words) {
            if (sentence.contains(w)) {
                ret = true;
                break;
            }
        }

        return ret;
    }


    static private List<String> findCandidateSentences(List<String> reviews) {
        List<String> ret = new ArrayList<String>();

        for (String review: reviews) {
            List<List<String>> sentences = Utility.splitParagraph(review);

            List<String> goodSentences = new ArrayList<String>();

            for (List<String> ss: sentences) {
                if (ss.size() > maxSubsenCnt) {
                    continue;
                }

                String sentence = StringUtil.join(ss, ",");
                if (isSentenceContainOneWordInArray(sentence, badWords)) {
                    continue;
                }

                boolean matchTemplate = false;
                for (FilterTemplate t: templates) {
                    if (t.isSentenceMatchTemplate(sentence)) {
                        matchTemplate = true;
                        break;
                    }
                }

                if (matchTemplate) {
                    goodSentences.add(sentence);
                }
            }

            if (goodSentences.size() > 0) {
                String flattenReview = review.replace("\n", "。");
                ret.add("[" + flattenReview + "]" + "\t" + StringUtil.join(goodSentences, ";"));
            }

        }

        return ret;
    }

    static private void writeCandidates(List<String> candidates, String writePath) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(writePath));
            for (String s: candidates) {
                writer.write(s + "\n");
            }

            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //get reviews
        List<String> reviews = getReviews("/Users/yuyang/Desktop/temp/qinzi_examples.txt", false);

        List<String> candidates = findCandidateSentences(reviews);

        writeCandidates(candidates, "/Users/yuyang/Desktop/3.txt");
        System.out.println(candidates.size());


//
//        String a = "a\\nb\\nc\\n";
//        System.out.println(a);
//
//        Result result = ToAnalysis.parse("虽然是亲子游戏,但是大宝宝们玩的还是很嗨大");
//        List<Term> terms = result.getTerms();
//        System.out.println(terms);
    }


}
