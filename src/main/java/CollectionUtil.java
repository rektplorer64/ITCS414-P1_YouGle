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

    public static <T> AutoSortList<T> intersect(AutoSortList<T> autoSortListA, AutoSortList<T> autoSortListB) {
        Comparator<T> comparator = autoSortListA.getComparator();
        ArrayList<T> intersection = new ArrayList<>(intersect(autoSortListA.getList(), autoSortListB.getList(), comparator));
        return new AutoSortList<T>(intersection, comparator);
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

    public static <T> List<T> symmetricDifferentiate(List<T> sortedListA, List<T> sortedListB, Comparator<T> c) {

        List<T> intersection = intersect(sortedListA, sortedListB, c);

        List<T> union = new ArrayList<>(sortedListA);
        union.addAll(sortedListB);

        union.removeAll(intersection);
        return union;
    }

    public static <T> AutoSortList<T> symmetricDifferentiate(AutoSortList<T> autoSortListA, AutoSortList<T> autoSortListB) {
        Comparator<T> c = autoSortListA.getComparator();
        ArrayList<T> arrayList = new ArrayList<T>(symmetricDifferentiate(autoSortListA.getList(), autoSortListB.getList(), c));
        return new AutoSortList<T>(arrayList, autoSortListA.getComparator());
    }

    public static <T> Pair<AutoSortList<T>, AutoSortList<T>> allInOne(AutoSortList<T> autoSortListA, AutoSortList<T> autoSortListB) {
        ArrayList<T> intersected = new ArrayList<>();
        ArrayList<T> symmetricDiff = new ArrayList<>();

        int i = 0, j = 0;
        while (i < autoSortListA.size() && j < autoSortListB.size()){
            int comparison = autoSortListA.getComparator().compare(autoSortListA.get(i), autoSortListB.get(j));
            if (comparison > 0){
                symmetricDiff.add(autoSortListA.get(i));
                i++;
            }else if (comparison < 0){
                symmetricDiff.add(autoSortListB.get(j));
                j++;
            }else{
                intersected.add(autoSortListB.get(j++));
                i++;
            }
        }

        return new Pair<>(new AutoSortList<T>(intersected, autoSortListA.getComparator()), new AutoSortList<T>(symmetricDiff, autoSortListA.getComparator()));
    }
}
