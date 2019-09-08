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

    public static <T> AutoSortList<T> intersect(AutoSortList<T> listA, AutoSortList<T> listB) {
        Comparator<T> comparator = listA.getComparator();
        ArrayList<T> intersection = new ArrayList<>(intersect(listA, listB, comparator));
        return new AutoSortList<T>(intersection, comparator);
    }

    public static <T> List<T> intersect(List<T> listA, List<T> listB, Comparator<T> c) {
        ArrayList<T> intersected = new ArrayList<>();

        int i = 0, j = 0;
        int sizeA = listA.size(), sizeB = listB.size();

        while (i < sizeA && j < sizeB){
            int comparison = c.compare(listA.get(i), listB.get(j));
            if (comparison < 0){
                i++;
            }else if (comparison > 0){
                j++;
            }else{
                // System.out.println("Intersected: " + listB.get(j));
                intersected.add(listB.get(j++));
                i++;
            }
        }

        // System.out.println(intersected);
        intersected.sort(c);
        return intersected;

        // sortedListA.retainAll(sortedListB);
        // return sortedListA;
    }

    public static <T> List<T> symmetricDifferentiate(List<T> listA, List<T> listB, Comparator<T> c) {

        List<T> intersection = intersect(listA, listB, c);

        List<T> union = new ArrayList<>(listA);
        union.addAll(listB);

        union.removeAll(intersection);
        return union;
    }

    public static <T> AutoSortList<T> symmetricDifferentiate(AutoSortList<T> listA, AutoSortList<T> listB) {
        Comparator<T> c = listA.getComparator();
        ArrayList<T> arrayList = new ArrayList<T>(symmetricDifferentiate(listA, listB, c));
        return new AutoSortList<T>(arrayList, listA.getComparator());
    }

    public static <T> Pair<AutoSortList<T>, AutoSortList<T>> allInOne(AutoSortList<T> listA, AutoSortList<T> listB) {
        ArrayList<T> intersected = new ArrayList<>();
        ArrayList<T> symmetricDiff = new ArrayList<>();

        int i = 0, j = 0;
        while (i < listA.size() && j < listB.size()){
            int comparison = listA.getComparator().compare(listA.get(i), listB.get(j));
            if (comparison > 0){
                symmetricDiff.add(listA.get(i));
                i++;
            }else if (comparison < 0){
                symmetricDiff.add(listB.get(j));
                j++;
            }else{
                intersected.add(listB.get(j++));
                i++;
            }
        }

        return new Pair<>(new AutoSortList<T>(intersected, listA.getComparator()), new AutoSortList<T>(symmetricDiff, listA.getComparator()));
    }

    public static String postingListArrayToString(List<PostingList> postingLists){
        StringBuilder stringBuilder = new StringBuilder("[");
        for(PostingList p : postingLists){
            stringBuilder.append(p.getTermId()).append("->{");
            for (int i = 0; i < p.getList().size(); i++){
                stringBuilder.append(p.getList().get(i));
                if (i < p.getList().size() - 1){
                    stringBuilder.append(", ");
                }
            }
            stringBuilder.append("}, ");
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
