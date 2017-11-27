package python_bridge;

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

import java.io.*;
import java.util.*;

import org.ansj.library.DicLibrary;

/**
 * Created by yuyang on 7/11/17.
 */
public class SentFeatureExtractor {
    static private final String modelPath = "edu/stanford/nlp/models/lexparser/chinesePCFG.ser.gz";
    static private final LexicalizedParser lp = LexicalizedParser.loadModel(modelPath);

    static private List<String> sDishes;

    static {
        sDishes = new ArrayList<String>();
        try {
            InputStream is = SentFeatureExtractor.class.getResourceAsStream("/dish_name.dic");

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                if (StringUtil.isBlank(line)) {
                    continue;
                }

                if (line.contains(" ")) {
                    continue;
                }

                //add dish name as n to the ansj
                DicLibrary.insert(DicLibrary.DEFAULT, line, "n", 1000);
            }

            is = SentFeatureExtractor.class.getResourceAsStream("/ansj.dic");
            reader = new BufferedReader(new InputStreamReader(is));
            while ((line = reader.readLine()) != null) {
                if (StringUtil.isBlank(line)) {
                    continue;
                }

                String[] parts = line.split(" ");
                if (parts.length != 2) {
                    System.out.println("format error, check line " + line);
                    continue;
                }
                String name =  parts[0];
                String pos = parts[1];
                DicLibrary.insert(DicLibrary.DEFAULT, name, pos, 1000);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void test() {
        System.out.println("this is python_bridge api");
    }

    public static Map<String, String> testPythonCalled() {
        Map<String, String> ret = new HashMap<String, String>();
        ret.put("key", "value");
        return ret;
    }

    public static int testPythonCalledIntegerVariable(int input) {
        return input + 1;
    }

    public static Collection<TypedDependency> getSentenceDependency(String sentence) {
        Result result = ToAnalysis.parse(sentence);
        List<String> segs = new ArrayList<String>();

        for (Term t: result.getTerms()) {
            segs.add(t.getName());
        }

        String splitSentence = StringUtil.join(segs, " ");
        Tree tree = lp.parse(splitSentence);

        ChineseGrammaticalStructure gs = new ChineseGrammaticalStructure(tree);
        Collection<TypedDependency> tdl = gs.typedDependenciesCollapsed();

        System.out.println(tdl);

        return tdl;
    }


    public static Object[] getSentenceFeatures(String[] sentence) {
        List<String> segs = new ArrayList<String>();


        for (String s: sentence) {
            segs.add(s);
        }

        String splitSentence = StringUtil.join(segs, " ");
        Tree tree = lp.parse(splitSentence);

        ChineseGrammaticalStructure gs = new ChineseGrammaticalStructure(tree);
        Collection<TypedDependency> tdl = gs.typedDependenciesCollapsed();

        //find the root
        IndexedWord root = null;
        for (TypedDependency d: tdl) {
            if (d.reln() == GrammaticalRelation.ROOT) {
                root = d.dep();
            }
        }

        assert(root != null);
        int rootIdx = root.index();

        //find the items depend on the root
        List<IndexedWord> rootDeps = new ArrayList<IndexedWord>();
        for (TypedDependency d: tdl) {
            if (d.gov().index() == rootIdx) {
                rootDeps.add(d.dep());
            }
        }

        //return the whole sentence seg size, root index, root dep index
        List<Object> ret = new ArrayList<Object>();
        ret.add(Integer.toString(rootIdx));
        for (IndexedWord w: rootDeps) {
            int tIdx=  w.index();

            ret.add(Integer.toString(tIdx));
        }

        return ret.toArray();
    }


    public static Object[] calculateSentenceFeatures(String sentence) {
        Result result = ToAnalysis.parse(sentence);

        List<String> segs = new ArrayList<String>();

        for (Term t: result.getTerms()) {
            segs.add(t.getName());
        }

        String splitSentence = StringUtil.join(segs, " ");
        Tree tree = lp.parse(splitSentence);

        ChineseGrammaticalStructure gs = new ChineseGrammaticalStructure(tree);
        Collection<TypedDependency> tdl = gs.typedDependenciesCollapsed();

        //find the root
        IndexedWord root = null;
        for (TypedDependency d: tdl) {
            if (d.reln() == GrammaticalRelation.ROOT) {
                root = d.dep();
            }
        }

        assert(root != null);
        int rootIdx = root.index();

        //find the items depend on the root
        List<IndexedWord> rootDeps = new ArrayList<IndexedWord>();
        for (TypedDependency d: tdl) {
            if (d.gov().index() == rootIdx) {
                rootDeps.add(d.dep());
            }
        }

        //return the whole sentence seg size, root index, root dep index, already add offset 1
        List<Object> ret = new ArrayList<Object>();

        ret.add(sentence);

        List<Integer> indexes = new ArrayList<Integer>();
        indexes.add(rootIdx-1);
        for (IndexedWord w: rootDeps) {
            int tIdx = w.index();

            indexes.add(tIdx-1);
        }

        ret.add(indexes.toArray());

        List<String> posSegs = new ArrayList<String>();
        for (Term t: result) {
            posSegs.add(t.getName() + ':' + t.getNatureStr());
        }

        ret.add(posSegs.toArray());

        return ret.toArray();
    }


    public static void testInputArray(String[] args) {
        System.out.println(args.length);
        for (String s: args) {
            System.out.println(s);
        }
    }


    public static void main(String[] args) {
        String text = "台式三杯鸡饭蛮好吃的";
        Result r = ToAnalysis.parse(text);
        System.out.println(r);


        System.out.println(calculateSentenceFeatures(text));
    }
}
