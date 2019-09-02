import java.util.*;

public class IndexHelpers {

    public static AutoSortList<PostingList> mergePostingList(AutoSortList<PostingList> block1, AutoSortList<PostingList> block2) {
        // System.out.println("Started Merging → Block 1 = " + block1.size() + " | Block 2 = " + block2.size());
        ArrayList<PostingList> mergedPostingList = new ArrayList<>(block1);
        mergedPostingList.addAll(block2);
        return combiningDuplicatePostingList(mergedPostingList);
    }

    public static AutoSortList<PostingList> combiningDuplicatePostingList(ArrayList<PostingList> lists) {
        HashMap<Integer, TreeSet<Integer>> termIdListPair = new HashMap<>();

        // Construct a HashMap that contains every entry
        for (PostingList p : lists) {
            if (!termIdListPair.containsKey(p.getTermId())) {
                termIdListPair.put(p.getTermId(), new TreeSet<>());
            }
            termIdListPair.get(p.getTermId()).addAll(p.getList());
        }

        ArrayList<PostingList> result = new ArrayList<>();

        // Combines duplications
        for (Map.Entry<Integer, TreeSet<Integer>> entry : termIdListPair.entrySet()) {
            result.add(new PostingList(entry.getKey(), new ArrayList<>(entry.getValue())));
        }
        return new AutoSortList<>(result, CollectionUtil.POSTING_LIST_COMPARATOR);
    }

}
