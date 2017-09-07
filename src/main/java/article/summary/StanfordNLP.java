package article.summary;


import com.sun.tools.javac.comp.Annotate;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.Mention;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.trees.international.pennchinese.ChineseGrammaticalStructure;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;
import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.jsoup.helper.StringUtil;

import java.util.*;

/**
 * Created by yuyang on 1/9/17.
 */
public class StanfordNLP {
    private String[] articleSentences;

    static private StanfordCoreNLP pipeline;

    static {
        //initialize the pipeline
        String[] args = new String[]{"-props", "edu/stanford/nlp/coref/properties/neural-chinese.properties"};
        Properties props = StringUtils.argsToProperties(args);
        pipeline = new StanfordCoreNLP(props);
    }

    public StanfordNLP(String[] sentences) {
        System.out.println("original sentences");
        for (String s: sentences) {
            System.out.println(s);
        }


        this.articleSentences = sentences;
    }

    public String[] decorefSentences() {
        //use ansj to do the cut the sentences
        Annotation document = getAnnotationFromSentences(this.articleSentences);

        //collected sentences
        List<String> retSentences = getDecorefDocumentSentences(document);

        return retSentences.toArray(new String[retSentences.size()]);
    }

    private class DecorefPosition {
        private int startIdx;

        private int endIdx;

        private String refString;

        public DecorefPosition(int start, int end, String ref) {
            startIdx = start;
            endIdx = end;
            refString = ref;
        }

        public int getStartIdx() {return startIdx;}

        public int getEndIdx() {return endIdx;}

        public String getRefString() {return refString;}

    }

    private Annotation getAnnotationFromSentences(String[] sentences) {
        //use ansj to do the cut the sentences
        List<String> terms = new ArrayList<String>();

        for (String sen: articleSentences) {
            Result splitResult = ToAnalysis.parse(sen);
            boolean added = false;
            for (Term term: splitResult.getTerms()) {
                if (StringUtil.isBlank(term.getRealName())) {
                    continue;
                }
                terms.add(term.getRealName());
                added = true;
            }
            if (added) {
                //record this is the end of one sentences
            }
        }

        //use pipeline to do the decoref
        String termText = StringUtil.join(terms, " ");
        Annotation document = new Annotation(termText);
        pipeline.annotate(document);

        return document;
    }

    private int getTermSentenceIndex(List<Integer> boundaryIndexes, int termIndex) {
        int ret = -1;
        for (int i = 0; i < boundaryIndexes.size(); i++) {
            int idx = boundaryIndexes.get(i);
            if (termIndex <= idx) {
                ret = i;
                break;
            }
        }

        return ret;
    }


    private List<String> getAnnotatedTokens(Annotation document) {
        List<String> ret = new ArrayList<String>();
        List<CoreLabel> tokens = document.get(CoreAnnotations.TokensAnnotation.class);
        for (CoreLabel token: tokens) {
            ret.add(token.value());
        }

        return ret;
    }

    private List<Integer> getAnnotatedSentenceBoudnary(Annotation document) {
        List<Integer> ret = new ArrayList<Integer>();
        for (CoreMap m: document.get(CoreAnnotations.SentencesAnnotation.class)) {
            List<CoreLabel> tokens = m.get(CoreAnnotations.TokensAnnotation.class);
            if (ret.size() == 0) {
                ret.add(tokens.size() - 1);
            } else {
                int prev = ret.get(ret.size() - 1);
                ret.add(prev + tokens.size());
            }
        }
        return ret;
    }

    private List<String> getDecorefDocumentSentences(Annotation document) {
        List<String> tokens = getAnnotatedTokens(document);
        //the sentence boundary indexes ends with 。？！
        List<Integer> boundaryIndexes = TextParser.getSentenceBoundaryIndexForTerms(tokens);

        Map<Integer, DecorefPosition> decorefMap = getDecorefMap(document, boundaryIndexes);

        List<String> retSentences = new ArrayList<String>();

        String oneSentence = "";

        for (int i = 0; i < tokens.size();) {

            String term = tokens.get(i);

            int stride = 1;

            if (decorefMap.containsKey(i)) {
                //use decoref string
                DecorefPosition pos = decorefMap.get(i);
                oneSentence += pos.getRefString();
                stride = pos.getEndIdx() - pos.getStartIdx();
            } else {
                //use original term
                oneSentence += term;
            }
            if (boundaryIndexes.indexOf(i) != -1) {
                //this is the end of sentences
                retSentences.add(oneSentence);
                oneSentence = "";
            }

            i += stride;
        }

        return retSentences;
    }

    private Map<Integer, DecorefPosition> getDecorefMap(Annotation document, List<Integer> boundaryIndexes) {
        Map<Integer, DecorefPosition> refMap = new HashMap<Integer, DecorefPosition>();

        List<String> tokens = getAnnotatedTokens(document);

        //end index for annotated sentences
        List<Integer> annoSentIdxes = getAnnotatedSentenceBoudnary(document);

        for (CorefChain cc: document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            List<CorefChain.CorefMention> mentions = cc.getMentionsInTextualOrder();

            System.out.println(mentions);

            if (mentions.size() <= 1) {
                continue;
            }

            //check if all mentions is same
            boolean isSame = true;
            String firstStr = mentions.get(0).mentionSpan;
            for (CorefChain.CorefMention m: mentions) {
                if (!m.mentionSpan.equals(firstStr)) {
                    isSame = false;
                }
            }

            if (isSame) {
                continue;
            }

            String[] parts = mentions.get(0).mentionSpan.split(" ");
            String ref = StringUtil.join(Arrays.asList(parts), "");
            int startOffset = 0;
            if (mentions.get(0).sentNum != 1) {
                startOffset = annoSentIdxes.get(mentions.get(0).sentNum -2) +1;
            }

            List<Integer> startIndexes = new ArrayList<Integer>();
            for (CorefChain.CorefMention m: mentions) {
                //mention.startIndex starts with 1 for the array
                startIndexes.add(m.startIndex-1);
            }

            List<Integer> sentenceIndexes = new ArrayList<Integer>();

            for (CorefChain.CorefMention m: mentions) {
                int sentIdx = getTermSentenceIndex(boundaryIndexes, m.startIndex - 1);
                if (sentIdx == -1) {
                    continue;
                }

                sentenceIndexes.add(sentIdx);
            }

            List<DecorefPosition> decorefPositions = new ArrayList<DecorefPosition>();

            int prevSentenceIdxes = -1;

            for (int i = 0; i < startIndexes.size(); i++) {

                CorefChain.CorefMention m = mentions.get(i);

                int offset = 0;
                if (m.sentNum != 1) {
                    offset = annoSentIdxes.get(m.sentNum - 2) + 1;
                }

                int sentIdx = sentenceIndexes.get(i);
                if (sentIdx != prevSentenceIdxes) {
                    //don't add first mention
                    if (m.startIndex - 1 + offset!= mentions.get(0).startIndex-1 + startOffset) {

                        decorefPositions.add(new DecorefPosition(m.startIndex-1 + offset, m.endIndex-1 + offset, ref));
                    }
                    prevSentenceIdxes = sentIdx;
                }
            }

            for (DecorefPosition p: decorefPositions) {
                refMap.put(p.getStartIdx(), p);
            }

        }

        return refMap;
    }

    public void dependencyParing(String text) {
        String modelPath = "edu/stanford/nlp/models/lexparser/xinhuaFactoredSegmenting.ser.gz";
        LexicalizedParser lp = LexicalizedParser.loadModel(modelPath);
        Tree t = lp.parse(text);

//        t.pennPrint();
        ChineseGrammaticalStructure gs = new ChineseGrammaticalStructure(t);
        Collection<TypedDependency> tdl = gs.typedDependenciesCollapsed();
        for (int i = 0; i < tdl.size(); i++) {
            TypedDependency td = (TypedDependency)tdl.toArray()[i];
            System.out.println(td.dep().toString());
        }
    }



    static private void selfTest() {
        String paragraph = "赵建军透露，去年以来教育部和国务院有关部门，主要是银监会、公安部、网信办、工商总局几个部门联合出台了很多措施、很多文件来治理校园贷的问题。尤其是今年初，上半年教育部和银监会、人力资源部联合印发了规范校园贷管理的文件，这个文件明确取缔校园贷款这个业务，任何网络贷款机构都不允许向在校大学生发放贷款。";
        paragraph = "赵建军首先解答了学生为什么借了钱后会陷入校园贷。因为未来要偿还的利息很高，为什么有这么高的利息学生还要去借这个贷款呢?是因为很多所谓的网络平台机构，在宣传方面做了不诚实的宣传、虚假的宣传，告诉学生这个贷款很方便，很便宜。学生在这种情况下，由于金融知识还不是很丰富，去借了贷款，最终成了高利贷，利滚利，有的学生到了还不起的状态。社会上不良的网贷机构，虚假宣传，引诱学生，造成这种现象。";
        paragraph = "赵建军表示，为了满足学生金融消费的需要，鼓励正规的商业银行开办针对大学生的小额信用贷款。据他了解，不少银行已经开办了这项业务，像中国银行、建设银行等。同时，教育部还要求各高校要认真做好学生的教育，加强对学生金融知识的教育，加强不良校园贷的警示教育，引导学生不要上当受骗。";
        paragraph = "中青在线成都9月6日电 四川省成都市中级人民法院今天对2016年3月发生在四川师范大学的一起杀人案进行宣判，被告人滕某被判处死刑，缓期两年执行。\n" +
                "被害人芦某某的家属芦海强向中国青年报·中青在线记者告知了上述判决结果。\n" +
                "2016年3月27日晚，四川师范大学舞蹈学院大一新生芦某某在宿舍学习室内被室友滕某连砍50多刀后身亡。\n" +
                "次日凌晨，犯罪嫌疑人滕某在现场被警方抓获。\n" +
                "案件发生后，成都市公安局龙泉驿区分局邀请四川华西法医学鉴定中心对嫌疑人滕某进行了法医精神病学鉴定，鉴定意见是“滕某患有抑郁症，对3月27日的违法行为评定为部分刑事责任能力”。";
        paragraph = "我说:Joe先来，他把事情和不满与委屈讲了一遍。\n" +
                "我：妈妈解释给你听，我看到的你 ，你的表情跟你的表达方式，其实你已经对这件事情下的结论，我感受到的只是你的抱怨，跟你觉得自己很委屈，但我必须跟你说：你要反转你的观念，如果你今天跑来跟妈妈表达说: 妈妈..为什么妹妹老是爱告状？妈妈会一起跟你讨论这个问题，而且我会回答你有很多的小朋友可能都是如此，包括你的妹妹！我笑笑的跟他说～想想你们班上的同学没有发生过这样的问题？";
        paragraph = "9月5日，曹格妻子吴速玲在微博上发布长文，称儿子和女儿因为小事吵了起来，并双双跑来告状。她用自己的EQ帮助两个孩子化解了矛盾，并告诉他们，必须学会沟通，而不是什么事情都先为自己下结论，既然下了不好的结论，结果就一定会是不好的，一样要反转你的脑袋！";

        TextParser parser  = new TextParser();

        StanfordNLP nlp = new StanfordNLP(parser.splitSentences(paragraph));
        String[] parsedSentences =  nlp.decorefSentences();
        for (String sentence: parsedSentences) {
            System.out.println(sentence);
        }

//        nlp.dependencyParing(paragraph);
    }

    static private void test() {

        long startTime=System.currentTimeMillis();
        String text = "小明 吃 了 个 冰棒 ，它 很 甜 。";

        String[] args = new String[] {"-props", "edu/stanford/nlp/coref/properties/deterministic-chinese.properties" };

        Annotation document = new Annotation(text);
        Properties props = StringUtils.argsToProperties(args);
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        pipeline.annotate(document);
        System.out.println("---");
        System.out.println("coref chains");

        for (CorefChain cc : document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            System.out.println("\t" + cc);
        }

        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
            System.out.println("---");
            System.out.println("mentions");
            for (Mention m : sentence.get(CorefCoreAnnotations.CorefMentionsAnnotation.class)) {
                System.out.println("\t" + m);
            }
        }

        long endTime=System.currentTimeMillis();
        long time = (endTime-startTime)/1000;
        System.out.println("Running time "+time/60+"min "+time%60+"s");
    }

    public static void main(String[] args) throws Exception {
        selfTest();
//        test();
    }
}
