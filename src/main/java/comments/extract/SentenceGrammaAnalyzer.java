package comments.extract;


import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.international.pennchinese.ChineseGrammaticalStructure;
import edu.stanford.nlp.util.Pair;
import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.library.DicLibrary;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.jsoup.helper.StringUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by yuyang on 1/11/17.
 */
class Utility {
    static private final String grammars = "edu/stanford/nlp/models/lexparser/chinesePCFG.ser.gz";
    static private final LexicalizedParser lp = LexicalizedParser.loadModel(grammars);

    public static Pair<Collection<TypedDependency>, Map<Integer, Term>> getGrammaticalDependency(String s) {
        Result result = ToAnalysis.parse(s);
        List<String> terms = new ArrayList<String>();

        Map<Integer, Term> termsMap = new HashMap<Integer, Term>();

        for (int i = 0; i < result.getTerms().size(); i++) {
            Term t = result.getTerms().get(i);

            terms.add(t.getName());
            termsMap.put(i+1, t);
        }

        String segTermsStr = StringUtil.join(terms, " ");
        Tree tree = lp.parse(segTermsStr);

        ChineseGrammaticalStructure gs = new ChineseGrammaticalStructure(tree);
        Collection<TypedDependency> tdl = gs.typedDependenciesCollapsed();

        return new Pair<Collection<TypedDependency>, Map<Integer, Term>>(tdl, termsMap);
    }

    public static IndexedWord getRootIndexedWord(Collection<TypedDependency> dependencies) {
        IndexedWord ret = null;
        for (TypedDependency d: dependencies) {
            if (d.reln() == GrammaticalRelation.ROOT) {
                ret = d.dep();
                break;
            }
        }

        return ret;
    }

    public static IndexedWord getIndexedWordWithName(Collection<TypedDependency> dependencies, String word) {
        IndexedWord ret = null;

        for (TypedDependency d: dependencies) {
            IndexedWord w = d.dep();
            if (w.value().equals(word)) {
                ret = d.dep();
                break;
            }
        }

        return ret;
    }

    public static List<IndexedWord> getDependentIndexWords(Collection<TypedDependency> dependencies, IndexedWord govWord) {
        List<IndexedWord> ret = new ArrayList<IndexedWord>();

        for (TypedDependency d: dependencies) {
            if (d.gov().index() == govWord.index()) {
                ret.add(d.dep());
            }
        }

        return ret;
    }
}


public class SentenceGrammaAnalyzer {
    static {
        DicLibrary.insert(DicLibrary.DEFAULT, "亮点", "n", 1000);
        DicLibrary.insert(DicLibrary.DEFAULT, "提拉米苏", "n", 1000);
        DicLibrary.insert(DicLibrary.DEFAULT, "生煎", "n", 1000);
        DicLibrary.insert(DicLibrary.DEFAULT, "小笼包", "n", 1000);
    }


    private String mSentence;
    private Collection<TypedDependency> mDependencies;
    private Map<Integer, Term> mTermIndexMap;
    private boolean debug;

    public SentenceGrammaAnalyzer(String s, boolean debug) {
        assert(!StringUtil.isBlank(s));

        mSentence = s;
        Pair<Collection<TypedDependency>, Map<Integer, Term>> dependency = Utility.getGrammaticalDependency(s);
        mDependencies = dependency.first();
        mTermIndexMap = dependency.second();

        this.debug = debug;

        if (debug) {
            System.out.println(mDependencies);
            System.out.println(mTermIndexMap);
        }
    }

    public boolean isSentenceGoodCandidate() {
        if (mDependencies == null) {
            return false;
        }

        boolean ret = false;
        //get the root indexed word
        IndexedWord root = Utility.getRootIndexedWord(mDependencies);
        List<IndexedWord> dependWords = Utility.getDependentIndexWords(mDependencies, root);

        //核心词为形容词或者名词，并且必须有名词主语依赖，则认为是好的句子
        String rootTag = mTermIndexMap.get(root.index()).getNatureStr();
        if (rootTag.startsWith("a") ) {
            boolean hasNoun = false;
            for (IndexedWord w : dependWords) {
                //并且两个词也不能相连
                if (mTermIndexMap.get(w.index()).getNatureStr().startsWith("n") && w.index() < root.index()) {

                    hasNoun = true;
                    break;
                }
            }

            if (hasNoun) {
                ret = true;
            }
        }

        return ret;
    }


    public String extractNERForKeyword(String keyword) {
        if (keyword == null || !mSentence.contains(keyword)) {
            return "";
        }

        IndexedWord word = Utility.getIndexedWordWithName(mDependencies, keyword);
        if (word == null) {
            return "";
        }

        String ret = "";

        //找到root
        IndexedWord root = Utility.getRootIndexedWord(mDependencies);
        List<IndexedWord> rootDepends = Utility.getDependentIndexWords(mDependencies, root);

        IndexedWord target = null;
        if (rootDepends.contains(word) || root.index() == word.index()) {
            //关键词也依赖于root或就是root
            if (word.index() > root.index()) {
                //找root前面的名词
                for (IndexedWord w: rootDepends) {
                    if (w.index() < root.index() && getPos(w.index()).startsWith("n")) {
                        target = w;
                        break;
                    }
                }
            } else {
                //找root后面的名词
                for (IndexedWord w: rootDepends) {
                    if (w.index() > root.index() && getPos(w.index()).startsWith("n")) {
                        target = w;
                        break;
                    }
                }
            }
        }

        if (target != null) {
            //找到target依赖的名词并拼接
            List<IndexedWord> targetDepends = Utility.getDependentIndexWords(mDependencies, target);
            for (IndexedWord w: targetDepends) {
                if (w.index() < target.index() && getPos(w.index()).startsWith("n")) {
                    ret += w.value();
                }
            }
            ret += target.value();
        }

        return ret;
    }

    private String getPos(int idx) {
        Term term = mTermIndexMap.get(idx);
        if (term == null) {
            return "";
        }

        return term.getNatureStr();
    }

    /*
    //below is for testing
    private static final Pattern sentenceSplitReg = Pattern.compile("[。，！？,!?\\s]");

    private static String cleanOneSentence(String s, String kw) {
        int idx = s.indexOf(kw);
        //从idx 往前往后找，直到非中文字符
        int before = idx - 1;
        while (before >= 0) {
            int codePoint = s.codePointAt(before);
            if (Character.UnicodeScript.of(codePoint) != Character.UnicodeScript.HAN) {
                break;
            }
            before -= 1;
        }
        before += 1;
        int after = idx + 1;
        while (after < s.length()) {
            int codePoint = s.codePointAt(after);
            if (Character.UnicodeScript.of(codePoint) != Character.UnicodeScript.HAN) {
                break;
            }
            after += 1;
        }

        return s.substring(before, after);
    }

    private static List<String> findSubSentenceWithWord(List<String> comments, Set<String> featureWords, Set<String> negWords) {
        List<String> ret = new ArrayList<String>();

        List<String> filterSentences = new ArrayList<String>();
        for (String s: comments) {
            String[] subSs = sentenceSplitReg.split(s);
            for (String ss: subSs) {
                for (String fw: featureWords) {
                    if (ss.contains(fw)) {
                        filterSentences.add(cleanOneSentence(ss, fw));
                    }
                }
            }
        }

        for (String s: filterSentences) {
            boolean hasNegWord = false;
            for (String neg: negWords) {
                if (s.contains(neg)) {
                    hasNegWord = true;
                    break;
                }
            }

            if (!hasNegWord) {
                ret.add(s);
            }
        }

        return ret;
    }

    private static Map<String, List<String>> findNerSubsentence(List<String> comments, Set<String> ners, Set<String> negWords) {
        Map<String, List<String>> nerCommentsMap = new HashMap<String, List<String>>();

        for (String s: comments) {
            String[] subs = sentenceSplitReg.split(s);
            for (String ss: subs) {
                String valid_ner = null;
                for (String ner: ners) {
                    if (ss.contains(ner)) {
                        valid_ner = ner;
                        break;
                    }
                }

                if (valid_ner != null) {
                    boolean hasNeg = false;
                    for (String neg: negWords) {
                        if (ss.contains(neg)) {
                            hasNeg = true;
                            break;
                        }
                    }

                    if (!hasNeg) {
                        if (!nerCommentsMap.containsKey(valid_ner)) {
                            nerCommentsMap.put(valid_ner, new ArrayList<String>());
                        }
                        nerCommentsMap.get(valid_ner).add(cleanOneSentence(ss, valid_ner));
                    }
                }
            }
        }
        return nerCommentsMap;
    }


    private static String getRecommendSentence(Map<String, List<String>> nerCommentsMap) {
        List<String> selectComments = new ArrayList<String>();
        for (Map.Entry<String, List<String>> entry: nerCommentsMap.entrySet()) {
            //find max size entry
            if (entry.getValue().size() > selectComments.size()) {
                selectComments = entry.getValue();
            }
        }

        for (String s: selectComments) {
            SentenceGrammaAnalyzer analyzer = new SentenceGrammaAnalyzer(s, false);
            if (analyzer.isSentenceGoodCandidate()) {
                return s;
            }
        }

        return "";
    }


    private static String parseOneShopComments(String commentsStr, Set<String> featureWords, Set<String> negWords) {
        String[] comments = commentsStr.substring(2, commentsStr.length() - 2).split("\",\"");

        List<String> featureSentences = findSubSentenceWithWord(Arrays.asList(comments), featureWords, negWords);

        List<String> featureEntities = new ArrayList<String>();

        for (String s: featureSentences) {
            for (String fw: featureWords) {
                if (s.contains(fw)) {
                    SentenceGrammaAnalyzer analyzer = new SentenceGrammaAnalyzer(s, false);
                    String ner = analyzer.extractNERForKeyword(fw);
                    if (!StringUtil.isBlank(ner)) {
                        featureEntities.add(ner);
                    }
                    break;
                }
            }
        }

        Map<String, List<String>> candidates = findNerSubsentence(Arrays.asList(comments), new HashSet(featureEntities), negWords);


        return getRecommendSentence(candidates);
    }


    public static void test(int numOfShop, String filePath) {
        List<String> shopComments = new ArrayList<String>();

        String[] fws = {"特色", "新意", "亮点", "出众", "独有"};
        Set<String> featureWords = new HashSet<String>();
        featureWords.addAll(Arrays.asList(fws));

        String[] ngws = {"没", "不", "缺少", "无", "失望", "一般", "少", "小", "但是", "淡", "慢", "普通", "环境", "菜"};
        Set<String> negWords = new HashSet<String>();
        negWords.addAll(Arrays.asList(ngws));

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

        List<String> extractSentences = new ArrayList<String>();
        for (String s: shopComments) {
            extractSentences.add(parseOneShopComments(s, featureWords, negWords));
        }

        System.out.println(extractSentences);
    }

    public static void main(String[] args) {
//        test(10, "/Users/yuyang/Desktop/recommend_generation/comments.csv");

        String text = "A区壁画是特色";

        SentenceGrammaAnalyzer analyzer = new SentenceGrammaAnalyzer(text, true);
        System.out.println(analyzer.isSentenceGoodCandidate());
        System.out.println(analyzer.extractNERForKeyword("特色"));
    }
    */

    public static void main(String[] args) {
        String ss = "面里的龙虾非常有新意";
        SentenceGrammaAnalyzer analyzer = new SentenceGrammaAnalyzer(ss, true);
        System.out.println(analyzer.extractNERForKeyword("新意"));
    }
}


