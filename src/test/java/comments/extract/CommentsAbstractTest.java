package comments.extract;

import article.summary.DependencyParse;
import lombok.extern.log4j.Log4j;
import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.jsoup.helper.StringUtil;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by yuyang on 18/9/17.
 */
@Log4j
public class CommentsAbstractTest {

    /*
    select tag, collect_list(subsen) from dpmid_xt_review_summary_tag_cluster_all
    where hp_cal_dt='2017-09-12' and source='DIANPING' and tag_id > 0 and shop_id=8062978  and sentiment >= 0 group by tag_id, tag;
     */


    private List<TagComments> getTagsFromFile(String path) {
        List<TagComments> ret = new ArrayList<TagComments>();

        try {
            FileReader fr = new FileReader(new File(path));
            int rowNum = 0;
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                rowNum++;
                if (rowNum == 1) {
                    //skip the first row
                    continue;
                }
                String[] parts = line.split("\t");
                if (parts.length != 2) {
                    continue;
                }

                //get tag name
                String tagName = parts[0];
                tagName = tagName.replace("[\"", "");
                tagName = tagName.replace("\"]", "");

                //get comments
                String comments = parts[1];
                if (comments.length() > 4) {
                    comments = comments.substring(2, comments.length() - 4);
                }

                String[] commentsList = comments.split("\",\"");

                if (!StringUtil.isBlank(tagName) && commentsList.length > 0) {
                    TagComments tag = new TagComments(tagName, Arrays.asList(commentsList));
                    //don't add tags where comments could not extract
                    if (StringUtil.isBlank(tag.getExtractComment())) {
                        continue;
                    }
                    ret.add(tag);
                }
            }
        } catch (IOException e) {
            log.error("could not open file ", e);
        }

        return ret;
    }

    private String getAbstract(String path) {
        List<TagComments> tags = getTagsFromFile(path);
        CommentsAbstract ca = new CommentsAbstract(tags);
        return ca.extractAbstracts();
    }

    private void extraTest() {
        System.out.println(getAbstract("/Users/yuyang/Downloads/73c00c95c682402daccbbd2c6f82e091.csv"));
    }

    @Test
    public void extractAbstracts() throws Exception {
        extraTest();

        /*
        //厨房乐章 22886099
        System.out.println(getAbstract("/Users/yuyang/Desktop/work/comment_data_mining/input/cfyz_positive_tags.csv"));

        //中山公园 酸菜鱼 23022705
        System.out.println(getAbstract("/Users/yuyang/Downloads/12fe98d1e8614b6e9703171e7c966a41.csv"));

        //溪雨观酸菜鱼(七宝宝龙城店) 67387637
        System.out.println(getAbstract("/Users/yuyang/Downloads/92cd3aba2422464c9476b83125a12324.csv"));

        //东北农家小院 18769933
        System.out.println(getAbstract("/Users/yuyang/Downloads/2272140e995e401ca7ebbc2b2e4bc148.csv"));

        //一茶一坐▪台菜海鲜精作坊(龙之梦长宁店) 22132248
        System.out.println(getAbstract("/Users/yuyang/Downloads/eb3311d4193241b9a605182d1553aaf6.csv"));

        //西贝莜面村(龙之梦长宁店) 5575882
        System.out.println(getAbstract("/Users/yuyang/Downloads/e27f3a69d89a476d80cd0998669f7720.csv"));

        //南京大牌档(中山公园龙之梦店) 24338223
        System.out.println(getAbstract("/Users/yuyang/Downloads/49c80b92742c4c6b926d778e77293f38.csv"));

        //70后饭吧(长宁龙之梦店) 22708171
        System.out.println(getAbstract("/Users/yuyang/Downloads/6d2c0c18fbb54381afac70c87461eb52.csv"));

        //绿茶(七宝万科广场店) 69937234
        System.out.println(getAbstract("/Users/yuyang/Downloads/8f7443fd883541dd93c2b87b01c1be88.csv"));

        //新堂洞韩国年糕火锅(金钟路店) 8062978
        System.out.println(getAbstract("/Users/yuyang/Downloads/87d0b226515041eeb66c070a5574671f.csv"));
        */
    }

    @Test
    public void testAnsj() {
        String text = "是酸辣甜口味好吃";
        text = "使得口感更鲜美";
        text = "因为太好吃太划算了";
        text = "又是一家比较合我口味且性价比很高的店";
        text = "一种惊艳的味道";
        text = "还推出了好像叫鲍鱼香辣虾锅的";
        text = "三文鱼";
        text = "三个菜都很好吃";
        text = "十多前上学时候这里菜品好";
        text = "价格实惠的份上";
        text = "这种环境的地方即便味道再好";
        text = "再将新鲜的鲈鱼片放在汤里";
        text = "午市套餐很划算";
        text = "最适合朋友聚餐了";
        text = "下午茶随便点点";
        text = "一个难得过来的朋友一起去吃饭";
        Result cutResult = ToAnalysis.parse(text);
        System.out.println(cutResult);

//        for (Term t: cutResult.getTerms()) {
//            System.out.println(t.getNatureStr());
//        }
//        DependencyParse.dependencyParing(text);

    }

}