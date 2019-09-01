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
        }

        PostingList accumulatePosting = postingLists.get(0);
        for (int i = 0; i < postingLists.size(); i++) {
            PostingList nextPosting = null;
            try {
                nextPosting = postingLists.get(i + 1);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            List<Integer> intersectedPostings = CollectionUtil.intersect(accumulatePosting.getList(),
                    nextPosting.getList(), CollectionUtil.INT_COMPARATOR);

            for (Integer posting : intersectedPostings) {
                if (!accumulatePosting.getList().contains(posting)) {
                    accumulatePosting.getList().add(posting);
                }
            }
        }
        return accumulatePosting.getList();
    }

}
