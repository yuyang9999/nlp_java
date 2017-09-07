package article.summary;


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
import org.jsoup.helper.StringUtil;

import java.util.*;

/**
 * Created by yuyang on 1/9/17.
 */
public class StanfordNLP {
    private String article;

    static private StanfordCoreNLP pipeline;

    static {
        //initialize the pipeline
        String[] args = new String[]{"-props", "/Users/yuyang/Desktop/work/nlppractice/src/main/java/article/summary/chinese.properties"};
        Properties props = StringUtils.argsToProperties(args);
        pipeline = new StanfordCoreNLP(props);
    }

    public StanfordNLP(String article) {
        this.article = article;
    }

    public String[] decorefSentences() {
        //use ansj to do the cut the sentences
        Annotation document = getAnnotationFromSentences(this.article);

        //collected sentences
        List<String> retSentences = getDecorefDocumentSentences(document);

        return retSentences.toArray(new String[retSentences.size()]);
    }

    private class DecorefPosition {
        private int startIdx;

        private int endIdx;

        private int sentenceIdx;

        private String refString;

        public DecorefPosition(int start, int end, int sentIdx,  String ref) {
            startIdx = start;
            endIdx = end;
            refString = ref;
            sentenceIdx = sentIdx;
        }

        public int getStartIdx() {return startIdx;}

        public int getEndIdx() {return endIdx;}

        public int getSentenceIdx() {return sentenceIdx;}

        public String getRefString() {return refString;}

    }

    private Annotation getAnnotationFromSentences(String article) {
        Annotation document = new Annotation(article);
        pipeline.annotate(document);

        return document;
    }


    private List<String> getDecorefDocumentSentences(Annotation document) {
        List<String> retSentences = new ArrayList<String>();
        List<DecorefPosition> decorefPositions = getDecorefMap(document);

        //get sentences
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        for (int i = 0; i < sentences.size(); i++) {
            CoreMap sentence = sentences.get(i);
            String sentenceStr = "";
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (int j = 0; j < tokens.size();) {
                String token = tokens.get(j).value();

                DecorefPosition decorefPosition = null;
                for (DecorefPosition p: decorefPositions) {
                    if (p.getSentenceIdx() == i && p.getStartIdx() == j) {
                        decorefPosition = p;
                        break;
                    }
                }

                if (decorefPosition != null) {
                    //replace
                    sentenceStr += decorefPosition.getRefString();
                    j += decorefPosition.getEndIdx() - decorefPosition.getStartIdx();
                } else {
                    sentenceStr += token;
                    j += 1;
                }
            }
            retSentences.add(sentenceStr);
        }

        return retSentences;
    }

    private List<DecorefPosition> getDecorefMap(Annotation document) {

        List<DecorefPosition> decorefPositions = new ArrayList<DecorefPosition>();

        for (CorefChain cc: document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            List<CorefChain.CorefMention> mentions = cc.getMentionsInTextualOrder();

            System.out.println(mentions);

            if (mentions.size() <= 1) {
                continue;
            }

            //check if all mentions is same
            boolean isSame = true;
            String firstStr = mentions.get(0).mentionSpan;
            for (CorefChain.CorefMention m : mentions) {
                if (!m.mentionSpan.equals(firstStr)) {
                    isSame = false;
                }
            }

            if (isSame) {
                continue;
            }

            String[] parts = mentions.get(0).mentionSpan.split(" ");
            String ref = StringUtil.join(Arrays.asList(parts), "");
            int prevSentNum = mentions.get(0).sentNum;

            for (int i = 1; i < mentions.size(); i++) {
                CorefChain.CorefMention m = mentions.get(i);
                if (m.sentNum == prevSentNum)
                    continue;
                prevSentNum = m.sentNum;
                DecorefPosition p = new DecorefPosition(m.startIndex - 1, m.endIndex - 1, m.sentNum - 1, ref);
                decorefPositions.add(p);
            }
        }
        return decorefPositions;
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
//        paragraph = "9月5日，曹格妻子吴速玲在微博上发布长文，称儿子和女儿因为小事吵了起来，并双双跑来告状。她用自己的EQ帮助两个孩子化解了矛盾，并告诉他们，必须学会沟通，而不是什么事情都先为自己下结论，既然下了不好的结论，结果就一定会是不好的，一样要反转你的脑袋！";

        System.out.println(paragraph);
        StanfordNLP nlp = new StanfordNLP(paragraph);
        String[] parsedSentences =  nlp.decorefSentences();
        for (String sentence: parsedSentences) {
            System.out.println(sentence);
        }

//        nlp.dependencyParing(paragraph);
    }

    static private void test() {

        long startTime=System.currentTimeMillis();
        String text = "小明 吃 了 个 冰棒 ，它 很 甜 。";

        String[] args = new String[] {"-props", "edu/stanford/nlp/coref/properties/neural-chinese.properties" };

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

    static private void testChinese() {
        String text = "EA在给特郎普总统的公开信上签名，并支持DACA政策和受这个政策保护的人们。EA在信中写道“追梦者们是我们公司和国家经济未来的活力所在。正是有了他们我们才有成长并创造就业机会。他们是我们成为世界竞争中获得优势的源泉。”EA呼吁特郎普总统和他的议会能为这些追梦者们提供更有效的永久合法化保护策略。参与签署这封公开信的还有很多美国IT业巨头，包括了谷歌和亚马逊。不过，在这封公开信中，EA和微软是仅有的两家游戏公司。";
        text = "Kotaku向美国的几家本土和海外游戏发行商询问了他们的看法，以及这个政策会如何影响他们受DACA保护的那些雇员。索尼的一位发言人称：“我们的国际竞争力来自与给所有人创造机会，这其中包括今天的追梦者们能成为明日的发明者。联邦立法能维持DACA政策的话，能从经济和人性的角度提升美国的价值。”";
        text = "很多DACA支持者和团体领袖指出，这个政策让很多从贫困地区出生的人获得了机会。比如YouTube的主播戴维·多比利克，出生在斯洛伐克的他童年时来到美国芝加哥，并在Vine网站上成名，该网站关闭之后泽转投YouTube继续他的播客事业。他在Twitter上发言“去年我为这个国家交了40万美元的税，得到的就是一张免费回斯洛伐克老家的机票。”许多YouTube上的播客主都加入了声援保护DACA的行列。";
        Annotation document = new Annotation(text);

        String[] args = new String[] {"-props", "/Users/yuyang/Desktop/work/nlppractice/src/main/java/article/summary/chinese.properties" };
        Properties props = StringUtils.argsToProperties(args);

        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        pipeline.annotate(document);

        for (CoreMap m: document.get(CoreAnnotations.SentencesAnnotation.class)) {
            System.out.println(m);
        }

        for (CorefChain cc : document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            System.out.println("\t" + cc);
        }
    }

    public static void main(String[] args) throws Exception {
//        selfTest();
//        test();
        testChinese();
    }
}
