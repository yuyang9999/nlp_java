package comments.extract;


import lombok.extern.log4j.Log4j;
import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.jsoup.helper.StringUtil;
import org.omg.IOP.TAG_MULTIPLE_COMPONENTS;

import java.io.*;
import java.util.*;

/**
 * Created by yuyang on 18/9/17.
 */
@Log4j
public class CommentsAbstract {
    static private Map<String, Integer> tagIndex;
    //根据配置文件可以让某些tag提取1条以上的评论
    static private Map<String, Integer> tagExtractCnt;

    static {
        tagIndex = new HashMap<String, Integer>();
        tagExtractCnt = new HashMap<String, Integer>();

        //load tag index
        InputStream is = CommentsAbstract.class.getResourceAsStream("/similar_tags.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        int idx = 0;
        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    //skip the comment
                    continue;
                }

                int extractNum = 1;
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    extractNum = Integer.parseInt(parts[1]);
                }
                String[] tags = parts[0].split(",");
                for (String tag: tags) {
                    tagIndex.put(tag, idx);
                    tagExtractCnt.put(tag, extractNum);
                }
                idx ++;
            }
        } catch (IOException e) {
            log.error("can't open similar tag file", e);
        }
    }

    private List<TagComments> tags;

    public CommentsAbstract(List<TagComments> tags) {
        this.tags = tags;
    }

    public String extractAbstracts() {
        List<TagComments> filterTags = new ArrayList<TagComments>();

        for (TagComments t: this.tags) {
            if (tagIndex.containsKey(t.getTagName()) && !StringUtil.isBlank(t.getExtractComment())) {
                filterTags.add(t);
            }
        }

        //only use one tag among tags with same index (similar meanings)
        List<TagComments> filterTags2 = new ArrayList<TagComments>();
        Set<Integer> collectTagIdxes = new HashSet<Integer>();
        Map<Integer, Set> existingTags = new HashMap<Integer, Set>();

        for (TagComments tag: filterTags) {
            Integer idx = tagIndex.get(tag.getTagName());
//            if (collectTagIdxes.contains(idx)) {
//                continue;
//            }

//            collectTagIdxes.add(idx);
//            filterTags2.add(tag);

            if (!existingTags.containsKey(idx)) {
                existingTags.put(idx, new HashSet<TagComments>());
            }
            existingTags.get(idx).add(tag);
        }

        //把每个index 中选择一个tagComment，选中的tagComment 必须是在同一类中抽取评论最多的
        for (Map.Entry<Integer, Set>entry: existingTags.entrySet()) {
            Set<TagComments> tagSet = entry.getValue();
            if (tagSet.size() == 1) {
                TagComments tag = tagSet.iterator().next();
                filterTags2.add(tag);
                continue;
            } else {
                List<TagComments> tagArr = Arrays.asList(tagSet.toArray(new TagComments[tagSet.size()]));
                //sort the tags according to the extract comment number
                Collections.sort(tagArr, new Comparator<TagComments>() {
                    @Override
                    public int compare(TagComments o1, TagComments o2) {
                        Integer cnt1 = o1.getFilteredComments().size();
                        Integer cnt2 = o2.getFilteredComments().size();
                        return -cnt1.compareTo(cnt2);
                    }
                });
                filterTags2.add(tagArr.get(0));
            }
        }

        //sort the tags according to the tag index
        Collections.sort(filterTags2, new Comparator<TagComments>() {
            public int compare(TagComments o1, TagComments o2) {
                Integer idx1 = tagIndex.get(o1.getTagName());
                Integer idx2 = tagIndex.get(o2.getTagName());
                return idx1.compareTo(idx2);
            }
        });

        //extract tag represent comment and concatenate them
        List<String> comments = new ArrayList<String>();
        List<String> collectedTags = new ArrayList<String>();
        Map<String, Integer> importWords = new HashMap<String, Integer>();

        for (TagComments tag: filterTags2) {
            List<String> extractComments = getTagExtractComments(tag, importWords);
            if (extractComments.size() != 0) {
                comments.addAll(extractComments);
                collectedTags.add(tag.getTagName());
            }

        }

        System.out.println("\n");
        System.out.println(collectedTags);

        String ret = "";
        if (comments.size() != 0) {
            ret = StringUtil.join(comments, "，");
            ret += "。";
        }

        return ret;
    }

    private List<String> getTagExtractComments(TagComments tag,  Map<String, Integer> importWords) {
        //名词跟形容词必须只出现一次
        List<String> ret = new ArrayList<String>();

        int extractNum = tagExtractCnt.get(tag.getTagName());

        List<String> comments = tag.getFilteredComments();

        for (String s: comments) {
            Result cutResult = ToAnalysis.parse(s);

            List<String> collectWords = new ArrayList<String>();
            List<String> collectNatures = new ArrayList<String>();
            /*
            boolean shouldSkip = false;
            //名词跟形容词不能出现多次
            for (Term t: cutResult.getTerms()) {
                String nature = t.getNatureStr();
                String realName = t.getRealName();

                collectNatures.add(nature);

                boolean isNoun = nature.startsWith("n");
                boolean isAdj = nature.startsWith("a");
                //名词或者形容词
                if (isNoun || isAdj) {
                    collectWords.add(realName);
                    if (importWords.containsKey(realName)) {
                        shouldSkip = true;
                        break;
                    }
                }
            }

            if (shouldSkip) {
                continue;
            }



            for (String word: collectWords) {
                if (!importWords.containsKey(word)) {
                    importWords.put(word, 0);
                }

                importWords.put(word, importWords.get(word) + 1);
            }
            */

            ret.add(s);
            if (ret.size() == extractNum) {
                break;
            }
        }

        return ret;
    }

}
