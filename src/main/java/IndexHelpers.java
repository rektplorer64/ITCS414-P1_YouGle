import sun.reflect.generics.tree.Tree;

import java.util.*;

public class IndexHelpers {

    public static List<PostingList> mergePostingList(List<PostingList> block1, List<PostingList> block2) {
        // System.out.println("Started Merging → Block 1 = " + block1.size() + " | Block 2 = " + block2.size());
        block1.addAll(block2);
        return combiningDuplicatePostingList(block1);
    }

    public static List<PostingList> combiningDuplicatePostingList(List<PostingList> lists) {
        HashMap<Integer, TreeSet<Integer>> termIdListPair = new HashMap<>();

        // Construct a HashMap that contains every entry
        // Combines duplications
        for (PostingList p : lists) {
            if (!termIdListPair.containsKey(p.getTermId())) {
                termIdListPair.put(p.getTermId(), new TreeSet<>());
            }
            termIdListPair.get(p.getTermId()).addAll(p.getList());
        }

        List<PostingList> result = new ArrayList<>();


        for (Map.Entry<Integer, TreeSet<Integer>> entry : termIdListPair.entrySet()) {
            result.add(new PostingList(entry.getKey(), new ArrayList<>(entry.getValue())));
        }
        return result;
    }

}
