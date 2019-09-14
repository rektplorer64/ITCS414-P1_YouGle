

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class Query {

    // Term id -> position in index file
    private Map<Integer, Long> posDict = new TreeMap<Integer, Long>();
    // Term id -> document frequency
    private Map<Integer, Integer> freqDict = new TreeMap<Integer, Integer>();
    // Doc id -> doc name dictionary
    private Map<Integer, String> docDict = new TreeMap<Integer, String>();
    // Term -> term id dictionary
    private Map<String, Integer> termDict = new TreeMap<String, Integer>();
    // Index
    private BaseIndex index = null;


    //indicate whether the query service is running or not
    private boolean running = false;
    private RandomAccessFile indexFile = null;

    /*
     * Read a posting list with a given termID from the file
     * You should seek to the file position of this specific
     * posting list and read it back.
     * */
    private PostingList readPosting(FileChannel fc, int termId)
            throws IOException {
        /*
         * TODO: Your code here
         */
        Long bytePosition = posDict.get(termId);
        if (bytePosition == null) {
            return null;
        }
        fc.position(bytePosition);
        return index.readPosting(fc);
    }


    public void runQueryService(String indexMode, String indexDirname) throws IOException {
        //Get the index reader
        try {
            Class<?> indexClass = Class.forName(indexMode + "Index");
            index = (BaseIndex) indexClass.newInstance();
        } catch (Exception e) {
            System.err
                    .println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
            throw new RuntimeException(e);
        }

        //Get Index file
        File inputdir = new File(indexDirname);
        if (!inputdir.exists() || !inputdir.isDirectory()) {
            System.err.println("Invalid index directory: " + indexDirname);
            return;
        }

        /* Index file */
        indexFile = new RandomAccessFile(new File(indexDirname,
                "corpus.index"), "r");

        String line = null;
        /* Term dictionary */
        BufferedReader termReader = new BufferedReader(new FileReader(new File(
                indexDirname, "term.dict")));
        while ((line = termReader.readLine()) != null) {
            String[] tokens = line.split("\t");
            termDict.put(tokens[0], Integer.parseInt(tokens[1]));
        }
        termReader.close();

        /* Doc dictionary */
        BufferedReader docReader = new BufferedReader(new FileReader(new File(
                indexDirname, "doc.dict")));
        while ((line = docReader.readLine()) != null) {
            String[] tokens = line.split("\t");
            docDict.put(Integer.parseInt(tokens[1]), tokens[0]);
        }
        docReader.close();

        /* Posting dictionary */
        BufferedReader postReader = new BufferedReader(new FileReader(new File(
                indexDirname, "posting.dict")));
        while ((line = postReader.readLine()) != null) {
            String[] tokens = line.split("\t");
            posDict.put(Integer.parseInt(tokens[0]), Long.parseLong(tokens[1]));
            freqDict.put(Integer.parseInt(tokens[0]),
                    Integer.parseInt(tokens[2]));
        }
        postReader.close();

        this.running = true;
    }

    public List<Integer> retrieve(String query) throws IOException {
        if (!running) {
            System.err.println("Error: Query service must be initiated");
        }

        /*
         * TODO: Your code here
         *       Perform query processing with the inverted index.
         *       return the list of IDs of the documents that match the query
         *
         */

        // System.out.println();
        // System.out.println("Term Dict = " + termDict);

        // Manipulate the Query
        ArrayList<String> queries = QueryHelpers.processQuery(query);
        // System.out.println("Queries = " + queries);


        // Get TermId by query string
        ArrayList<Integer> termIds = QueryHelpers.getTermIdByQuery(queries, termDict);
        // System.out.println("Term Id = " + termIds);

        // Fetch Byte Position inside the index file of each Term (termId: Int -> PostingList)
        HashMap<Integer, PostingList> termIdPostingListMap = new HashMap<>();
        for (Integer termId : termIds) {		// For each term Id
            try {
            	// Put PostingList to the Map; Null if there is none
                termIdPostingListMap.put(termId, readPosting(indexFile.getChannel(), termId));
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        }

        // System.out.println("Map Size = " + termIdPostingListMap.size());

        // System.out.println(intersectedDocId);
        // System.out.println();

        return QueryHelpers.booleanRetrieval(new ArrayList<>(termIdPostingListMap.values()));
    }

    String outputQueryResult(List<Integer> res) {
        /*
         * TODO:
         *
         * Take the list of documents ID and prepare the search results, sorted by lexicon order.
         *
         * E.g.
         * 	0/fine.txt
         *	0/hello.txt
         *	1/bye.txt
         *	2/fine.txt
         *	2/hello.txt
         *
         * If there no matched document, output:
         *
         * no results found
         *
         * */

        StringBuilder resultStringBuilder = new StringBuilder();
        Collections.sort(res);
        if (!res.isEmpty()) {
            for (int docId : res) {
                String docName = docDict.get(docId);
                resultStringBuilder.append(docName).append("\n");
            }
        }else{
            resultStringBuilder.append("no results found");
        }
        return resultStringBuilder.toString();
    }

    public static void main(String[] args) throws IOException {
        /* Parse command line */
        if (args.length != 2) {
            System.err.println("Usage: java Query [Basic|VB|Gamma] index_dir");
            return;
        }

        /* Get index */
        String className = null;
        try {
            className = args[0];
        } catch (Exception e) {
            System.err
                    .println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
            throw new RuntimeException(e);
        }

        /* Get index directory */
        String input = args[1];

        Query queryService = new Query();
        queryService.runQueryService(className, input);

        /* Processing queries */
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        /* For each query */
        String line = null;
        while ((line = br.readLine()) != null) {
            List<Integer> hitDocs = queryService.retrieve(line);
            queryService.outputQueryResult(hitDocs);
        }

        br.close();
    }

    protected void finalize() {
        try {
            if (indexFile != null) indexFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class QueryHelpers {
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

    public static class CollectionUtil {

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

        /**
         * A simple Wrapper Class that mimics the idea of SortedList
         * @param <T>
         */
        public static class AutoSortList<T> extends ArrayList<T> {

            private Comparator<T> comparator;

            public AutoSortList(Comparator<T> comparator){
                this(new ArrayList<>(), comparator);
            }

            public AutoSortList(@NotNull Collection<? extends T> c, Comparator<T> comparator) {
                super(c);
                this.comparator = comparator;
                this.sort(comparator);
            }

            @Override
            public boolean add(T t) {
                final boolean outcome = super.add(t);
                this.sort(comparator);
                return outcome;
            }

            @Override
            public void add(int index, T element) {
                super.add(index, element);
                this.sort(comparator);
            }

            @Override
            public boolean addAll(Collection<? extends T> c) {
                final boolean outcome = super.addAll(c);
                this.sort(comparator);
                return outcome;
            }

            @Override
            public boolean addAll(int index, Collection<? extends T> c) {
                final boolean outcome = super.addAll(index, c);
                this.sort(comparator);
                return outcome;
            }

            @Override
            public void replaceAll(UnaryOperator<T> operator) {
                super.replaceAll(operator);
                this.sort(comparator);
            }

            @Override
            public boolean removeIf(Predicate<? super T> filter) {
                final boolean outcome = super.removeIf(filter);
                this.sort(comparator);
                return outcome;
            }

            @Override
            public AutoSortList<T> clone() {
                return new AutoSortList<T>((Collection<? extends T>) super.clone(), comparator);
            }

            public Comparator<T> getComparator() {
                return comparator;
            }

            public void setComparator(Comparator<T> comparator) {
                this.comparator = comparator;
            }

        }
    }
}

