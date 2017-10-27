package baidu;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by yuyang on 9/10/17.
 */
public class NlpServiceTest {
    @Test
    public void sentimentAnalysis() throws Exception {
        String text = "结果第二天和家人一起拉肚子几次";
        text = "这些鱼不太新鲜";
        NlpService.sentimentAnalysis1(text);
    }

    @Test
    public void dependencyParseNegative() throws Exception {
        String text = "嗓子很痛闺蜜想吃这口"; //存在IC, (independent clause)
        text = "今年的年会吃好"; //主干SBV, 且v为v
        text = "阳光明媚的午后"; //主干为DE，且午后(n or t)为最后一个词
        text = "特别喜欢在晴朗凉爽的午后"; //分辨不出来，除了午后是个时间词
        text = "直接就去那里坐坐"; //VOB(动宾) + VV(并列动词)
        text = "这些鱼不太新鲜";  //分不出来，只能看情感分析?
        text = "下午茶随便点点"; //SBV, 且v为v
        NlpService.dependencyParser(text);
    }

    @Test
    public void dependencyParsePositive() throws Exception {
        String text = "一楼大厅环境不错"; //主干为SBV, v其实为adj
        text = "脆皮乳鸽很好吃"; //SBV, v为adj
        text = "门口看着挺干净清爽的"; //SBV, V为DE
        text = "朋友一起聚会的好地方"; //DE, 且root 为名词且在最后一个位置
//        text = "这里的菜很好吃";
        NlpService.dependencyParser(text);
    }

    @Test
    public void testDependencyParser() throws Exception {

    }
}