package article.summary;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.international.pennchinese.ChineseGrammaticalStructure;

import java.util.Collection;

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


    public static void main(String[] args) {
        String text = "他们服务质量很好";
        dependencyParing(text);
    }



}
