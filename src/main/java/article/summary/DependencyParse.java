package article.summary;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.international.pennchinese.ChineseGrammaticalStructure;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by yuyang on 11/9/17.
 */
public class DependencyParse {

    public static void dependencyParing(String text) {
        String modelPath = "edu/stanford/nlp/models/lexparser/xinhuaFactoredSegmenting.ser.gz";
        LexicalizedParser lp = LexicalizedParser.loadModel(modelPath);
        Tree t = lp.parse(text);

        t.pennPrint();
//        ChineseGrammaticalStructure gs = new ChineseGrammaticalStructure(t);
//        Collection<TypedDependency> tdl = gs.typedDependenciesCollapsed();
//        for (int i = 0; i < tdl.size(); i++) {
//            TypedDependency td = (TypedDependency)tdl.toArray()[i];
//            System.out.println(td.dep().toString());
//        }
    }

    static void hannlpDependParser(String text) {
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
            System.out.println("this is a good sentence");
        }

//        for (CoNLLWord word: sentence) {
//            System.out.println(word);
//        }
        System.out.println(sentence);
    }

    private static String[] getComments() {
        return new String[0];
    }

    

    public static void main(String[] args) {
        String text = "下午茶随便点点";
        text = "这里的菜很好吃";
        text = "今年的年会吃好";
        text = "阳光明媚的午后";
        text = "特别喜欢在晴朗凉爽的午后";
        text = "朋友一起聚会的好地方";
        text = "门口看着挺干净清爽的";
        text = "脆皮乳鸽很好吃";
        text = "人类终于耳聪目明了";
        text = "从爱因斯坦预言引力波到一百年后科学家接力发展高精度探测技术";
        text = "我们也看到中国科学家跻身在列";
//        dependencyParing(text);
        text = "不过海鲜真的蛮诱人的扇贝肉很大一个";
        text = "以后做西餐的时候";
        text = "一碗饭吃得干净";
        hannlpDependParser(text);
    }
}
