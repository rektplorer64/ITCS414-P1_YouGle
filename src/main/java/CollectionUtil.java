import java.util.*;

public class CollectionUtil {

    public static final Comparator<PostingList> POSTING_LIST_COMPARATOR = new Comparator<PostingList>() {
        @Override
        public int compare(PostingList o1, PostingList o2) {
            return o1.getTermId() - o2.getTermId();
        }
    };

    public static final Comparator<Integer> INT_COMPARATOR = new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            return o1 - o2;
        }
    };

    public static <T> SortedList<T> intersect(SortedList<T> sortedListA, SortedList<T> sortedListB) {
        ArrayList<T> intersected = new ArrayList<>();

        int i = 0, j = 0;
        while (i < sortedListA.size() && j < sortedListB.size()){
            int comparison = sortedListA.getComparator().compare(sortedListA.get(i), sortedListB.get(j));
            if (comparison > 0){
                i++;
            }else if (comparison < 0){
                j++;

            }else{
                intersected.add(sortedListB.get(j++));
                i++;
            }
        }

        return new SortedList<T>(intersected, sortedListA.getComparator());

        // sortedListA.retainAll(sortedListB);
        // return sortedListA;
    }

    public static <T> List<T> intersect(List<T> sortedListA, List<T> sortedListB, Comparator<T> c) {
        ArrayList<T> intersected = new ArrayList<>();

        int i = 0, j = 0;
        while (i < sortedListA.size() && j < sortedListB.size()){
            int comparison = c.compare(sortedListA.get(i), sortedListB.get(j));
            if (comparison > 0){
                i++;
            }else if (comparison < 0){
                j++;

            }else{
                intersected.add(sortedListB.get(j++));
                i++;
            }
        }

        intersected.sort(c);
        return intersected;

        // sortedListA.retainAll(sortedListB);
        // return sortedListA;
    }

    public static <T> Pair<SortedList<T>, SortedList<T>> allInOne(SortedList<T> sortedListA, SortedList<T> sortedListB) {
        ArrayList<T> intersected = new ArrayList<>();
        ArrayList<T> symmetricDiff = new ArrayList<>();

        int i = 0, j = 0;
        while (i < sortedListA.size() && j < sortedListB.size()){
            int comparison = sortedListA.getComparator().compare(sortedListA.get(i), sortedListB.get(j));
            if (comparison > 0){
                symmetricDiff.add(sortedListA.get(i));
                i++;
            }else if (comparison < 0){
                symmetricDiff.add(sortedListB.get(j));
                j++;
            }else{
                intersected.add(sortedListB.get(j++));
                i++;
            }
        }

        return new Pair<>(new SortedList<T>(intersected, sortedListA.getComparator()), new SortedList<T>(symmetricDiff, sortedListA.getComparator()));
    }

    public static <T> SortedList<T> inverseIntersect(SortedList<T> sortedListA, SortedList<T> sortedListB) {
        ArrayList<T> arrayList = new ArrayList<>();
        // Traverse both arrays simultaneously.
        int i = 0, j = 0;
        while (i < sortedListA.size() && j < sortedListB.size()) {
            // Print smaller element and move
            // ahead in array with smaller element

            int comparison = sortedListA.getComparator().compare(sortedListA.get(i), sortedListB.get(j));
            if (comparison > 0) {
                arrayList.add(sortedListA.get(i));
                i++;
            } else if (comparison < 0) {
                arrayList.add(sortedListB.get(j));
                j++;
            } else {
                i++;
                j++;
            }
        }
        return new SortedList<T>(arrayList, sortedListA.getComparator());
    }

}
