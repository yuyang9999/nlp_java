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
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by yuyang on 27/11/17.
 */
public class Utility {
    private static final Pattern splitSentencePat = Pattern.compile("[、，。？；;,.?\"\t\n]");
    static private final String modelPath = "edu/stanford/nlp/models/lexparser/chinesePCFG.ser.gz";
    static private final LexicalizedParser lp = LexicalizedParser.loadModel(modelPath);

    public static String[] splitSenteces(String text) {
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

        int rootIdx = root.index();
        if (!natures.get(rootIdx - 1).startsWith("a")) {
            return false;
        }

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
        String text = "1、装修味道很浓，闻得头晕；2、进门没有让我们用洗手液，工作人员不是很尽责；3、小朋友玩了一个半小时袜子就黑了；4、出门时工作人员在门口扫地，会都扫到家长鞋子里了。有部分家长鞋子没有放到隔板里，看到工作人员用满是灰尘的扫帚推鞋子。\n" +
                "应该不会再来了。";
        System.out.println(splitSenteces(text));

        System.out.println(isSentenceNounAdjStructure("装修味道很浓"));

    }

}
