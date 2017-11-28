package CommonUtility;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.international.pennchinese.ChineseGrammaticalStructure;
import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.jsoup.helper.StringUtil;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by yuyang on 27/11/17.
 */
public class Utility {
    private static final Pattern splitSentencePat = Pattern.compile("[、，。？；;,.?\"\t\n]");
    private static final Pattern splitLargeSentencePat = Pattern.compile("[。？；！\n?!~.]");
    private static final Pattern splitSubSentencePat = Pattern.compile("[，\t ]");

    static private final String modelPath = "edu/stanford/nlp/models/lexparser/chinesePCFG.ser.gz";
    static private final LexicalizedParser lp = LexicalizedParser.loadModel(modelPath);

    public static String[] splitSentences(String text) {
        String[] sentences = splitSentencePat.split(text);
        //filter the sentences with length zero

        List<String> ret = new ArrayList<String>();
        for (String s: sentences) {
            if (s.length() > 0) {
                ret.add(s);
            }
        }

        return ret.toArray(new String[ret.size()]);
    }

    public static List<List<String>> splitParagraph(String text) {
        List<List<String>> ret = new ArrayList<List<String>>();

        List<String> sentences =  Arrays.asList(splitLargeSentencePat.split(text));
        for (String sentence: sentences) {
            if (sentence.length() == 0) {
                continue;
            }

            String[] subsentences = splitSubSentencePat.split(sentence);
            List<String> subarr = new ArrayList<String>();
            for (String s: subsentences) {
                if (s.length() > 0) {
                    subarr.add(s);
                }
            }
            if (subarr.size() > 0) {
                ret.add(subarr);
            }
        }

        return ret;
    }

    public static boolean isSentenceNounAdjStructure(String text) {
        Result result = ToAnalysis.parse(text);
        List<String> segs = new ArrayList<String>();

        boolean hasNoun =false, hasAdj = false;
        for (Term t: result.getTerms()) {
            if (t.getNatureStr().startsWith("a")) {
                hasAdj = true;
            } else if (t.getNatureStr().startsWith("n")) {
                hasNoun = true;
            }
        }

        if (!hasAdj || !hasNoun) {
            return false;
        }

        List<String> natures = new ArrayList<String>();

        for (Term t: result.getTerms()) {
            segs.add(t.getName());
            natures.add(t.getNatureStr());
        }

        String segStr = StringUtil.join(segs, " ");
        Tree tree = lp.parse(segStr);
        ChineseGrammaticalStructure gs = new ChineseGrammaticalStructure(tree);
        Collection<TypedDependency> dependencies = gs.typedDependenciesCollapsed();

        IndexedWord root = getDependenciesRoot(dependencies);
        assert(root  != null);

//        int rootIdx = root.index();
//        if (!natures.get(rootIdx - 1).startsWith("a")) {
//            return false;
//        }

        for (TypedDependency d: dependencies) {
            if (d.gov().index() == root.index()) {
                if (d.reln().getShortName().equals("nsubj")) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isSentenceAllChinese(String s) {
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

    private static IndexedWord getDependenciesRoot(Collection<TypedDependency> dependencies) {
        IndexedWord ret = null;
        for (TypedDependency d: dependencies) {
            if (d.reln() == GrammaticalRelation.ROOT) {
                ret = d.dep();
                break;
            }
        }

        return ret;
    }

    private static List<IndexedWord> getRootDependencies(Collection<TypedDependency> dependencies, IndexedWord root) {
        int rootIdx = root.index();

        List<IndexedWord> ret = new ArrayList<IndexedWord>();
        for (TypedDependency d: dependencies) {
            if (d.gov().index() == rootIdx) {
                ret.add(d.dep());
            }
        }

        return ret;
    }

    public static void main(String[] args) {
//        String text = "1、装修味道很浓，闻得头晕；2、进门没有让我们用洗手液，工作人员不是很尽责；3、小朋友玩了一个半小时袜子就黑了；4、出门时工作人员在门口扫地，会都扫到家长鞋子里了。有部分家长鞋子没有放到隔板里，看到工作人员用满是灰尘的扫帚推鞋子。\n" +
//                "应该不会再来了。";
//        System.out.println(splitSentences(text));
//
//        System.out.println(isSentenceNounAdjStructure("蹦床种类蛮多的"));

        String text = "\uD83C\uDF89作为\uD83C\uDF1F第100条\uD83C\uDF1F点评，当然是要给开心的欢乐的满意的点评…念了N久的蹦床也算是跟闺蜜来找了回童年的乐趣，玩的很热也玩得很开心。\n" +
                "住在浦东光坐车和地铁7⃣️转3⃣️再到达这里就花了整整两小时，玩下来还是觉得值得的。好不容易找到了门口，得让走去东门，走到了东门上去又找了好久终于找到了电影院门口的电梯上四楼。\n" +
                "进去之后还挺大挺丰富的，前台签了字，30块/小时/人，袜子¥6/双，储物柜¥100/柜，柜子挺大的两人放包绰绰有余。换好袜子后让我们跟着电视做下热身，然后就可以自己进去随便耍啦～\n" +
                "❤️滑滑梯超级爽，还挺长的，上去前自己要拿着座垫，坐进坐垫后再滑下来，我比我闺蜜胖所以滑下来比她快冲击力也比较大哈哈哈哈，直接冲进了海洋球，而她每次都停止在了滑梯的下面这条头。\n" +
                "\uD83D\uDC9B海绵垫这一块跳进去真的超级软超级舒服，但是卡进去后爬出来真的是难，袜子都给扯歪了。中间休息的时候就是躺在这海绵垫里。\n" +
                "\uD83D\uDC9A篮球架有很多个，在蹦床上投篮真的是弹跳力\uD83D\uDCAFget！赞到不行～\n" +
                "\uD83D\uDC99入口处这边有个白色网的弹力超级的蹦床，但是摔下来并不舒服。\n" +
                "\uD83D\uDC9C绳索道那块应该比较刺激一点，越往上越赞，有各种的道自己看自己能力。几乎都走了一遍，觉得最晃悠的就是像铅笔一样的滚轴，因为它会转动，一不小心就要向后向前…挺好玩的，下去还是要原路返回的…\n" +
                "▪️总的是物超所值，玩得很开心！不管是亲子还是朋友都适合玩～推荐！";
        System.out.println(splitParagraph(text));

    }

}
