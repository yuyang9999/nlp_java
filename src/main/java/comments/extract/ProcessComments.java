package comments.extract;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLWord;
import lombok.Data;
import org.jsoup.helper.StringUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/**
 * Created by yuyang on 30/10/17.
 */
public class ProcessComments {
    @Data
    private class ShopComments {
        private String shopId;
        private List<String> comments;
        private List<String> extractComments;

        public ShopComments(String shopId, List<String> comments) {
            this.shopId = shopId;
            this.comments = comments;
            System.out.println("process shop with id " + shopId);
            this.extractComments = filterComments(comments);
        }

        private List<String> filterComments(List<String> comments) {
            Set<String> phrases = new HashSet<String>();

            for (String s: comments) {
                String phrase = extractPhrase(s);
                if (!StringUtil.isBlank(phrase)) {
                    phrases.add(phrase);
                }
            }

            List<String> ret = new ArrayList<String>();
            ret.addAll(phrases);

            return ret;
        }
    }

    private static Set<String> sFeatureWords;

    private List<ShopComments> mShopCommentsArr;

    static {
        sFeatureWords = new HashSet<String>();
        sFeatureWords.add("特色");
        sFeatureWords.add("新意");
        sFeatureWords.add("亮点");
        sFeatureWords.add("出众");
        sFeatureWords.add("独有");
    }

    public ProcessComments(String path) {
        mShopCommentsArr = new ArrayList<ShopComments>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            int cnt = -1;
            String shopId = "";
            List<String> comments;
            while ((line = br.readLine()) != null) {
                cnt++;
                if (cnt % 2 == 0) {
                    //read shop id
                    shopId = line;
                } else {
                    comments = Arrays.asList(line.split("##"));
                    ShopComments shopComments = new ShopComments(shopId, comments);
                    mShopCommentsArr.add(shopComments);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("");
    }

    public void dumpResultToFile(String outputFile) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
            for (ShopComments c: this.mShopCommentsArr) {
                bw.write(c.getShopId() + "\n");
                for (String s: c.getExtractComments()) {
                    bw.write(s + "\n");
                }
            }
            bw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static String extractPhrase(String s) {
        String ret = "";

        CoNLLSentence sentence = HanLP.parseDependency(s);
//        System.out.println(sentence);

        List<CoNLLWord> words = Arrays.asList(sentence.getWordArray());

        //get the index of feature word
        Map<String, Integer> map = new HashMap<String, Integer>();

        for (int i = 0; i < words.size(); i++) {
            CoNLLWord w = words.get(i);
            map.put(w.LEMMA, i);
        }

        int idx = -1;
        for (String w: sFeatureWords) {
            if (map.containsKey(w)) {
                idx = map.get(w);
                break;
            }
        }
        if (idx == -1) {
            return ret;
        }

        CoNLLWord fWord = words.get(idx);

        if ("核心关系".equals(words.get(idx).DEPREL)) {
            //find 主谓关系
            for (CoNLLWord w: words) {
                if (w.DEPREL == "主谓关系" &&
                        w.POSTAG.startsWith("n") &&
                        w.HEAD == fWord) {
                    ret = w.LEMMA;
                    break;
                }
            }
        } else if ("动宾关系".equals(fWord.DEPREL)) {
            //主谓加动宾
            CoNLLWord head = fWord.HEAD;
            CoNLLWord target = null;
            for (CoNLLWord w: words) {
                if (w.HEAD == head &&
                        w.POSTAG.startsWith("n") &&
                        "主谓关系".equals(w.DEPREL)) {
                    target = w;
                    break;
                }
            }
            //如果是定中关系也加进来
            if (target != null) {
                for (CoNLLWord w: words) {
                    if ("定中关系".equals(w.DEPREL) && w.HEAD == target && w.POSTAG.startsWith("n")) {
                        ret += w.LEMMA;
                    }
                }
                ret += target.LEMMA;
            }

        } else if ("定中关系".equals(fWord.DEPREL)) {
            //把定中关系后面所有短语拼接
            ret = "";
            for (int i = idx+1; i < words.size(); i++) {
                ret += words.get(i).LEMMA;
                if (words.get(i) == fWord.HEAD) {
                    break;
                }
            }
        } else if ("主谓关系".equals(fWord.DEPREL)) {
            //找核心关系的动宾关系
            CoNLLWord head = fWord.HEAD;
            if (head.POSTAG.startsWith("v")) {
                for (CoNLLWord w: words) {
                    if (w.HEAD == head &&
                            w.POSTAG.startsWith("n") &&
                            "动宾关系".equals(w.DEPREL)) {
                        ret = w.LEMMA;
                        break;
                    }
                }
            }
        }

        return ret;
    }

    public static void main(String[] args) {
//        String text = "她认为这家店的焦糖香草更有特色";
//        text = "区壁画很有特色";
//        text = "他们的意大利面还是有特色在里面的";
//        text = "酱汁很有特色";
//        text = "还有特色牛丸";
//        text = "特色是吊的黄酒";
//
//        System.out.println(extractPhrase(text));

        String inputFile = "/Users/yuyang/Desktop/recommend_generation/short_recommends.txt";
        String dumpFile = "/Users/yuyang/Desktop/recommend_generation/good_recommends.txt";

        ProcessComments p = new ProcessComments(inputFile);
        p.dumpResultToFile(dumpFile);
    }
}
