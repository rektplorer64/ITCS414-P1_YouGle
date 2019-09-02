import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.nio.channels.FileChannel;
import java.util.*;

public class QueryHelpers {
    public static ArrayList<String> processQuery(String inputQuery) {
        String[] tokens = inputQuery.trim().split("\\s+");
        return new ArrayList<>(Arrays.asList(tokens));
    }

    public static ArrayList<Integer> getTermIdByQuery(ArrayList<String> queries, Map<String, Integer> termDict) {
        ArrayList<Integer> termIds = new ArrayList<>();
        for (String query : queries) {
            if (termDict.get(query) != null) {
                termIds.add(termDict.get(query));
            }
        }
        return termIds;
    }

    public static PostingList getPostingListFromFile(BaseIndex index, RandomAccessFile raf, Long bytePosition) throws IOException {
        if (bytePosition == null) {
            return null;
        }
        raf.getChannel().position(bytePosition);
        return index.readPosting(raf.getChannel());
    }

    /**
     * Finds the Intersection of an Array of Postings regardless of termId
     *
     * @param postingLists an Array contains Postings with various termId
     *
     * @return a list of document that intersects or presents in all given postingLists
     */
    public static List<Integer> booleanRetrieval(List<PostingList> postingLists) {

        if (postingLists.isEmpty()){
            return new ArrayList<>();
        }else if (postingLists.size() == 1){
            return new ArrayList<>(postingLists.get(0).getList());
        }

        ArrayList<Integer> firstElem = new ArrayList<>(postingLists.get(0).getList());
        ArrayList<Integer> secondElem = new ArrayList<>(postingLists.get(1).getList());
        List<Integer> accumulateIntersectedDocId = CollectionUtil.intersect(firstElem, secondElem,CollectionUtil.INT_COMPARATOR);

        for (int i = 1; i < postingLists.size(); i++) {
            PostingList nextPosting = null;
            try {
                nextPosting = postingLists.get(i + 1);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            accumulateIntersectedDocId = CollectionUtil.intersect(accumulateIntersectedDocId,
                    nextPosting.getList(), CollectionUtil.INT_COMPARATOR);
        }
        return accumulateIntersectedDocId;
    }

}
