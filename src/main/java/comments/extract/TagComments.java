package comments.extract;

import lombok.Getter;
import lombok.extern.log4j.Log4j;

import java.io.*;
import java.util.*;

/**
 * Created by yuyang on 18/9/17.
 */
@Log4j
public class TagComments {
    static private final int goodCommentMinSize = 7;
    static private final int goodCommentMaxSize = 14;

    static private Set<String> badWords;

    static {
        badWords = new HashSet<String>();
        InputStream is = TagComments.class.getResourceAsStream("/bad_words.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                badWords.add(line);
            }
        } catch (IOException e) {
            log.error(e);
        }

    }

    @Getter
    private String tag;
    @Getter
    private List<String> comments;

    public TagComments(String tag, List<String> comments) {
        this.tag = tag;
        this.comments = comments;
    }

    public String extractComents() {
        List<String> commentsList = new ArrayList<String>();

        //按照句子的长度过滤
        for (String s: this.comments) {
            if (shouldFilterSentence(s)) {
                continue;
            }

            commentsList.add(s);
        }

        //todo: 过滤掉含有连接词的句子

        //按字符串长度并且按字符排序
        Collections.sort(commentsList, new Comparator<String>() {
            public int compare(String o1, String o2) {
                if (o1.length() != o2.length()) {
                    Integer i1 = o1.length();
                    Integer i2 = o2.length();
                    return i1.compareTo(i2);
                }
                return o1.compareTo(o2);
            }
        });

        //取中位数的句子
        if (commentsList.size() == 0) {
            return "";
        }

        return commentsList.get(commentsList.size() / 2);
    }

    @Override
    public String toString() {
        return tag;
    }


    private int getChineseCharacterCount(String s) {
        int ret = 0;
        for (int i = 0; i < s.length();) {
            int codePoint = s.codePointAt(i);
            i += Character.charCount(codePoint);
            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                ret += 1;
            }
        }
        return ret;
    }

    /***
     * whether or not filter the sentence
     * @param s the sentence
     * @return true if required, false otherwise
     */
    private boolean shouldFilterSentence(String s) {
        int len = getChineseCharacterCount(s);

        //必须全都是汉字
        if (len != s.length()) {
            return true;
        }

        //字数必须在规定的范围内
        if (len < goodCommentMinSize && len > goodCommentMaxSize) {
            return true;
        }

        //必须不含某些词
        for (String word: badWords) {
            if (s.startsWith(word)) {
                return true;
            }
        }

        return false;
    }

}