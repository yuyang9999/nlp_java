package comments.extract;

import lombok.extern.log4j.Log4j;
import org.jsoup.helper.StringUtil;

import java.io.*;
import java.util.*;

/**
 * Created by yuyang on 18/9/17.
 */
@Log4j
public class CommentsAbstract {
    static private Map<String, Integer> tagIndex;

    static {
        tagIndex = new HashMap<String, Integer>();

        //load tag index
        InputStream is = CommentsAbstract.class.getResourceAsStream("/similar_tags.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        int idx = 0;
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String[] tags = line.split(",");
                for (String tag: tags) {
                    tagIndex.put(tag, idx);
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
            if (tagIndex.containsKey(t.getTag()) && !StringUtil.isBlank(t.getExtractComment())) {
                filterTags.add(t);
            }
        }

        //only use one tag among tags with same index (similar meanings)
        List<TagComments> filterTags2 = new ArrayList<TagComments>();
        Set<Integer> collectTagIdxes = new HashSet<Integer>();
        for (TagComments tag: filterTags) {
            Integer idx = tagIndex.get(tag.getTag());
            if (collectTagIdxes.contains(idx)) {
                continue;
            }

            collectTagIdxes.add(idx);
            filterTags2.add(tag);
        }

        //sort the tags according to the tag index
        Collections.sort(filterTags2, new Comparator<TagComments>() {
            public int compare(TagComments o1, TagComments o2) {
                Integer idx1 = tagIndex.get(o1.getTag());
                Integer idx2 = tagIndex.get(o2.getTag());
                return idx1.compareTo(idx2);
            }
        });

        //print the filterTags2 as debug info
        System.out.println("output tags");
        System.out.println(filterTags2);

        //extract tag represent comment and concatenate them
        List<String> comments = new ArrayList<String>();
        for (TagComments tag: filterTags2) {
            String comm = tag.getExtractComment();
            if (!StringUtil.isBlank(comm)) {
                comments.add(comm);
            }
        }

        String ret = "";
        if (comments.size() != 0) {
            ret = StringUtil.join(comments, "，");
            ret += "。";
        }

        return ret;
    }

}
