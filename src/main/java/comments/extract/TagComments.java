package comments.extract;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import org.jsoup.helper.StringUtil;

import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by yuyang on 18/9/17.
 */
@Log4j
public class TagComments {
    static private final int goodCommentMinSize = 7;
    static private final int goodCommentMaxSize = 14;

    static private Set<String> badWords;

    private static final String debugFile = "/Users/yuyang/Desktop/temp/comment_extract.txt";

    static {
        badWords = new HashSet<String>();
        InputStream is = TagComments.class.getResourceAsStream("/bad_words.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (StringUtil.isBlank(line) || line.startsWith("#")) {
                    continue;
                }
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
    @Getter
    private String extractComment;
    @Getter
    private List<String> filteredComments;



    public TagComments(String tag, List<String> comments) {
        this.tag = tag;
        this.comments = comments;
        this.extractComment = extractComents();
    }

    private String extractComents() {
        List<String> commentsList = new ArrayList<String>();

        for (String s: this.comments) {
            if (shouldFilterSentence(s)) {
                continue;
            }

            commentsList.add(s);
        }

        //去除重复的
        Set<String> commentSet = new HashSet<String>();
        List<String> uniqueList = new ArrayList<String>();
        for (String s: commentsList) {
            if (commentSet.contains(s)) {
                continue;
            }

            commentSet.add(s);
            uniqueList.add(s);
        }


        //按字符串长度并且按字符排序
        Collections.sort(uniqueList, new Comparator<String>() {
            public int compare(String o1, String o2) {
                if (o1.length() != o2.length()) {
                    Integer i1 = o1.length();
                    Integer i2 = o2.length();
                    return i1.compareTo(i2);
                }
                return o1.compareTo(o2);
            }
        });

        this.filteredComments = uniqueList;

        //write to the debug file
//        writeFilterCommentsToDebugFile(uniqueList);

        if (uniqueList.size() == 0) {
            return "";
        }

        //随机取一个 （debug only)
        int idx = ThreadLocalRandom.current().nextInt(0, uniqueList.size());
        return uniqueList.get(idx);
    }

    @Override
    public String toString() {
        return tag;
    }

    private void writeFilterCommentsToDebugFile(List<String> comments) {
        try {
            FileWriter fw = new FileWriter(debugFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);

            for (String comment: comments) {
                out.println(comment);
            }

            out.close();

        } catch (IOException e) {
            log.error(e);
        }
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
        if (len < goodCommentMinSize || len > goodCommentMaxSize) {
            return true;
        }

        //必须不含某些词
        for (String word: badWords) {
            if (s.contains(word)) {
                return true;
            }
        }

        return false;
    }

}
