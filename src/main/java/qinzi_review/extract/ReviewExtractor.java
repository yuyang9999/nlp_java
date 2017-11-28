package qinzi_review.extract;

import CommonUtility.Utility;
import org.jsoup.helper.StringUtil;

import java.io.*;
import java.util.*;

/**
 * Created by yuyang on 28/11/17.
 */
public class ReviewExtractor {
    static private List<String> babyRefs;
    static private List<String> goodWords;
    static private List<String> badWords;
    static private final int maxSubsenCnt = 4;

    static {
        babyRefs = loadFileContentents("/baby_refs.txt");
        goodWords = loadFileContentents("/good_adj.txt");
        badWords = loadFileContentents("/bad_adj.txt");
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

    static private List<String> getReviews(String path) {
        List<String> ret = new ArrayList<String>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));
            int cnt = 0;
            String line = null;
            while ((line = reader.readLine()) != null) {
                cnt ++;
                if (cnt == 1) {
                    continue;
                }

                String[] parts = line.split("\t");
                String review = parts[8];
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
            for (List<String> ss: sentences) {
                if (ss.size() > maxSubsenCnt) {
                    continue;
                }

                String sentence = StringUtil.join(ss, ",");
                if (!isSentenceContainOneWordInArray(sentence, babyRefs)) {
                    continue;
                }
                if (isSentenceContainOneWordInArray(sentence, badWords)) {
                    continue;
                }
                if (!isSentenceContainOneWordInArray(sentence, goodWords)) {
                    continue;
                }

                ret.add(sentence);
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
//        List<String> reviews = Arrays.asList("两对情侣在泡泡池里热吻我又不好意思打扰 我就只能看啊 派对连杯酒都没有 连个帅哥也看不到这派对和小学生聚餐有啥区别说到小学生");
//        findCandidateSentences(reviews);


        //get reviews
        List<String> reviews = getReviews("/Users/yuyang/Downloads/d619d09157eb422491570ef4be5e4edf.csv");

        List<String> candidates = findCandidateSentences(reviews);

        writeCandidates(candidates, "/Users/yuyang/Desktop/3.txt");
        System.out.println(candidates.size());

    }


}
