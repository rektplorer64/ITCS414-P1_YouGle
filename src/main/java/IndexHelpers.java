import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.*;

public class IndexHelpers {

    public static SortedList<PostingList> mergePostingList(SortedList<PostingList> block1, SortedList<PostingList> block2) {
        // System.out.println("Started Merging → Block 1 = " + block1.size() + " | Block 2 = " + block2.size());
        ArrayList<PostingList> mergedPostingList = new ArrayList<>(block1);
        mergedPostingList.addAll(block2);
        return combiningDuplicatePostingList(mergedPostingList);
    }

    private static SortedList<PostingList> combiningDuplicatePostingList(ArrayList<PostingList> lists) {
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
        return new SortedList<>(result, CollectionUtil.POSTING_LIST_COMPARATOR);
    }

}
