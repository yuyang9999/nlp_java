package comments.extract;

import edu.stanford.nlp.ling.CoreLabel;
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

/**
 * Created by yuyang on 31/10/17.
 */
public class StanfordNlpTest {
    static private final String grammars = "edu/stanford/nlp/models/lexparser/chinesePCFG.ser.gz";
    static private final LexicalizedParser lp = LexicalizedParser.loadModel(grammars);

    static private String appendIndexWordDependentNoun(Collection<TypedDependency> dependencies, IndexedWord target) {
        String targetString = "";

        //find other indexed word depend on the target
        for (TypedDependency d: dependencies) {
            if (d.gov().index() == target.index()) {
                CoreLabel label = d.dep().backingLabel();
                if (label.tag().startsWith("N")) {
                    targetString += d.dep().value();
                }
            }
        }
        targetString += target.value();

        return targetString;
    }

    static private String extractPhrase(String sentence, String keyword) {

    }

    static private void testDependencies(Collection<TypedDependency> dependencies, String keyWord) {
        //find root indexed word
        IndexedWord rootWord = null;
        for (TypedDependency d: dependencies) {
            if (d.reln() == GrammaticalRelation.ROOT) {
                rootWord = d.dep();
            }
        }

        assert(rootWord != null);
        System.out.println(rootWord);
        List<IndexedWord> rootWordDependents = new ArrayList<IndexedWord>();

        //find indexed word depend the root word
        for (TypedDependency d: dependencies) {
            //compare pointer is not enough
            if (d.gov().index() == rootWord.index()) {
                rootWordDependents.add(d.dep());
            }
        }
        System.out.println(rootWordDependents);

        //find the keyword indexed word
        IndexedWord indexedKeyWord = null;
        for (TypedDependency d: dependencies) {
            if (keyWord.equals(d.dep().value())) {
                indexedKeyWord = d.dep();
            }
        }
        if (indexedKeyWord == null) {
            System.out.println("can't find matched keyword");
            return;
        }
        System.out.println(indexedKeyWord);

        if (indexedKeyWord.index() > rootWord.index()) {
            System.out.println("动宾短语");
            //把主语找到, 主语必须为名词
            IndexedWord target = null;
            for (IndexedWord w: rootWordDependents) {
                CoreLabel label = w.backingLabel();
                //tag 即为词性
                if (label.tag().startsWith("N")) {
                    target = w;
                    break;
                }
            }
            if (target != null) {
                String targetString = appendIndexWordDependentNoun(dependencies, target);
                System.out.println("主语为 " + targetString);
            }
        } else if (indexedKeyWord.index() < rootWord.index()) {
            //特征词在主语，要找宾语
            IndexedWord target = null;
            for (IndexedWord w: rootWordDependents) {
                if (w.index() > rootWord.index()) {
                    target = w;
                    break;
                }
            }

            if (target != null) {
                String targetString = appendIndexWordDependentNoun(dependencies, target);
                System.out.println("宾语为 " + targetString);
            }
        } else if (indexedKeyWord.index() == rootWord.index()) {
            IndexedWord target = null;

            //keyword is also root, find the rel word before it
            for (IndexedWord w: rootWordDependents) {
                if (w.index() < rootWord.index()) {
                    if (w.backingLabel().tag().startsWith("N")) {
                        target = w;
                        break;
                    }
                }
            }

            if (target != null) {
                String targetString = appendIndexWordDependentNoun(dependencies, target);
                System.out.println("宾语为 " + targetString);
            }
        }
    }


    public static void printParseTree(String text, String keyword) {
        if (StringUtil.isBlank(text)) {
            return;
        }

        Tree tree = lp.parse(text);

//        List<Tree> leaves = tree.getLeaves();
//
//        for (Tree t: leaves) {
//            System.out.println(t.value());
//            System.out.println(t.depth());
//        }

        ChineseGrammaticalStructure gs = new ChineseGrammaticalStructure(tree);
        Collection<TypedDependency> tdl = gs.typedDependenciesCollapsed();

        testDependencies(tdl, keyword);
        System.out.println(tdl);
//
//        for (TypedDependency dependency: tdl) {
//            IndexedWord govIdx= dependency.gov();
//            IndexedWord depIdx = dependency.dep();
//            System.out.println(govIdx);
//            System.out.println(depIdx);
//        }
//
//        tree.indentedListPrint();
    }

    public static void main(String[] args) {
        String keyword = "特色";
        String text = "酱汁有特色";
        text = "特色是吊的黄酒";
//        text = "酱汁出众";
        Result result = ToAnalysis.parse(text);
        System.out.println(result);

        List<String> terms = new ArrayList<String>();
        for (Term t: result.getTerms()) {
            terms.add(t.getName());
        }

        String segText = StringUtil.join(terms, " ");

        printParseTree(segText, keyword);
    }
}
