

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

/*
This Code is modified by Section 1 Students of Mahidol University, the Faculty of ICT, 2019
as part of a project in ITCS414 - Information Retrieval and Storage.

The group consists of
    1. Krittin      Chatrinan       ID 6088022
    2. Anon         Kangpanich      ID 6088053
    3. Tanawin      Wichit          ID 6088221
 */

/**
 * The class that responsible for Query Processing and Document Searching
 */
public class Query{

    /**
     * Map of Term Id -> Byte Position in the index file
     */
    private Map<Integer, Long> posDict = new TreeMap<Integer, Long>();

    /**
     * Map of Term Id -> Document Frequency
     */
    private Map<Integer, Integer> freqDict = new TreeMap<Integer, Integer>();

    /**
     * Map of Doc Id -> Term Id
     */
    private Map<Integer, String> docDict = new TreeMap<Integer, String>();

    /**
     * Map of Term String -> Term Id
     */
    private Map<String, Integer> termDict = new TreeMap<String, Integer>();

    /**
     * Indexer Instance
     */
    private BaseIndex index = null;

    /**
     * Indicate whether the query service is running or not
     */
    private boolean running = false;

    /**
     * File instance of the Binary Index File
     */
    private RandomAccessFile indexFile = null;

    /**
     * Read a posting list with a given termID from the file You should seek to the file position of this specific
     * posting list and read it back.
     *
     * @param fc     FileChannel Instance of the file to be read
     * @param termId Integer of Term Id
     *
     * @return PostingList read from the file at the position referred from FileChannel Param
     *
     * @throws IOException
     */
    private PostingList readPosting(FileChannel fc, int termId) throws IOException{
        /*
         * TODO: Your code here
         */
        Long bytePosition = posDict.get(termId);
        if (bytePosition == null){
            return null;
        }
        fc.position(bytePosition);
        return index.readPosting(fc);
    }

    /**
     * Initiates the indexing process by Reading 1. index file 2. term dictionary file 3. document dictionary file
     *
     * @param indexMode    Indexing mode String
     * @param indexDirname Index output directory
     *
     * @throws IOException
     */
    public void runQueryService(String indexMode, String indexDirname) throws IOException{
        //Get the index reader
        try{
            Class<?> indexClass = Class.forName(indexMode + "Index");
            index = (BaseIndex) indexClass.newInstance();
        }catch (Exception e){
            System.err.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
            throw new RuntimeException(e);
        }

        //Get Index file
        File inputdir = new File(indexDirname);
        if (!inputdir.exists() || !inputdir.isDirectory()){
            System.err.println("Invalid index directory: " + indexDirname);
            return;
        }

        /* Index file */
        indexFile = new RandomAccessFile(new File(indexDirname, "corpus.index"), "r");

        String line = null;
        /* Term dictionary */
        BufferedReader termReader = new BufferedReader(new FileReader(new File(indexDirname, "term.dict")));
        while ((line = termReader.readLine()) != null){
            String[] tokens = line.split("\t");
            termDict.put(tokens[0], Integer.parseInt(tokens[1]));
        }
        termReader.close();

        /* Doc dictionary */
        BufferedReader docReader = new BufferedReader(new FileReader(new File(indexDirname, "doc.dict")));
        while ((line = docReader.readLine()) != null){
            String[] tokens = line.split("\t");
            docDict.put(Integer.parseInt(tokens[1]), tokens[0]);
        }
        docReader.close();

        /* Posting dictionary */
        BufferedReader postReader = new BufferedReader(new FileReader(new File(indexDirname, "posting.dict")));
        while ((line = postReader.readLine()) != null){
            String[] tokens = line.split("\t");
            posDict.put(Integer.parseInt(tokens[0]), Long.parseLong(tokens[1]));
            freqDict.put(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[2]));
        }
        postReader.close();

        this.running = true;
    }

    /**
     * Retrieves Result based on given Query
     *
     * @param query query to be search
     *
     * @return list of Document Id containing the given query
     *
     * @throws IOException when there is an unexpected error related to file reading
     */
    public List<Integer> retrieve(String query) throws IOException{
        if (!running){
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
        ArrayList<String> queries = QueryUtil.tokenizeQuery(query);
        // System.out.println("Queries = " + queries);

        // Get TermId by query string
        ArrayList<Integer> termIds = QueryUtil.getTermIdByQuery(termDict, queries);
        // System.out.println("Term Id = " + termIds);

        // Fetch Byte Position inside the index file of each Term (termId: Int -> PostingList)
        HashMap<Integer, PostingList> termIdPostingListMap = new HashMap<>();
        for (Integer termId : termIds) {        // For each term Id
            try{
                // Put PostingList to the Map; Null if there is none
                termIdPostingListMap.put(termId, readPosting(indexFile.getChannel(), termId));
            }catch (IOException | NullPointerException e){
                e.printStackTrace();
            }
        }

        // System.out.println("Map Size = " + termIdPostingListMap.size());

        // System.out.println(intersectedDocId);
        // System.out.println();
        ArrayList<PostingList> postingLists = new ArrayList<>(termIdPostingListMap.values());
        postingLists.sort(CollectionUtil.COMPARATOR_POSTING_LIST_DOC_FREQ);

        return QueryUtil.booleanRetrieval(postingLists);
    }

    /**
     * Fetches documents based on given Document Id
     *
     * @param res result document id
     *
     * @return string to be written to a file
     */
    String outputQueryResult(List<Integer> res){
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
        if (!res.isEmpty()){
            for (int docId : res) {
                String docName = docDict.get(docId);
                resultStringBuilder.append(docName).append("\n");
            }
        }else{
            resultStringBuilder.append("no results found");
        }
        return resultStringBuilder.toString();
    }

    /**
     * The main method for executing the Query method
     *
     * @param args parameters for the program
     *
     * @throws IOException
     */
    public static void main(String[] args) throws IOException{
        /* Parse command line */
        if (args.length != 2){
            System.err.println("Usage: java Query [Basic|VB|Gamma] index_dir");
            return;
        }

        /* Get index */
        String className = null;
        try{
            className = args[0];
        }catch (Exception e){
            System.err.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
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
        while ((line = br.readLine()) != null){
            List<Integer> hitDocs = queryService.retrieve(line);
            queryService.outputQueryResult(hitDocs);
        }

        br.close();
    }

    /**
     * Finalize file io instances by closing them
     */
    protected void finalize(){
        try{
            if (indexFile != null){
                indexFile.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}

/**
 * A helper static class that contains Various Helper Static Methods
 */
class QueryUtil{

    /**
     * Receives a query, then process it and return it as an ArrayList of Strings
     *
     * @param inputQuery string consists of english terms delimited by white spaces
     *
     * @return ArrayList of processed Strings
     */
    public static ArrayList<String> tokenizeQuery(String inputQuery){
        String[] tokens = inputQuery.trim().split("\\s+");
        return new ArrayList<>(Arrays.asList(tokens));
    }

    /**
     * Fetch terms based on given ArrayList of query strings
     *
     * @param termDict the Map of term string -> term identification number
     * @param queries  ArrayList of query strings
     *
     * @return ArrayList of term identification numbers
     */
    public static ArrayList<Integer> getTermIdByQuery(Map<String, Integer> termDict, ArrayList<String> queries){
        ArrayList<Integer> termIds = new ArrayList<>();
        for (String query : queries) {
            if (termDict.get(query) != null){
                termIds.add(termDict.get(query));
            }
        }
        return termIds;
    }

    /**
     * Reads the the given binary index file at the given byte position and construct a new PostingList Instance
     *
     * @param index        instance of indexing helper
     * @param raf          a reference of the binary index file
     * @param bytePosition a long which represents the byte position to be read
     *
     * @return a PostingList read from the binary file
     *
     * @throws IOException
     */
    public static PostingList getPostingListFromFile(BaseIndex index, RandomAccessFile raf, Long bytePosition)
            throws IOException{
        // If the given position is non existence
        if (bytePosition == null){
            return null;
        }

        // Seeks to the given byte position
        raf.getChannel().position(bytePosition);

        // Reads from the File and gives out new PostingList
        return index.readPosting(raf.getChannel());
    }

    /**
     * Finds the Intersection of an Array of Postings regardless of termId
     *
     * @param postingLists an Array contains Postings with various termId
     *
     * @return a list of document that intersects or presents in all given postingLists
     */
    public static List<Integer> booleanRetrieval(List<PostingList> postingLists){

        if (postingLists.isEmpty()){                                    // If there is no PostingList
            return new ArrayList<>();                                   // There is no result to be returned
        }else if (postingLists.size() == 1){                            // If there is only one
            return new ArrayList<>(postingLists.get(0).getList());      // Return the list as the result
        }

        // Get Posting of both initial first 2 arrays
        ArrayList<Integer> firstElem = new ArrayList<>(postingLists.get(0).getList());
        ArrayList<Integer> secondElem = new ArrayList<>(postingLists.get(1).getList());

        // Calculate the intersection of the first two Postings
        List<Integer> accumulateIntersectedDocId = CollectionUtil.intersect(firstElem, secondElem,
                                                                                  CollectionUtil.COMPARATOR_INTEGER);

        // Loops until it reaches the size of PostingLists
        for (int i = 1; i < postingLists.size(); i++) {
            PostingList nextPosting = null;
            try{
                nextPosting = postingLists.get(i + 1);      // Get the next PostingList to be intersected
            }catch (IndexOutOfBoundsException e){           // If we reached the end (i == postingList.size() - 1), postingList.get(i + 1) will throw OutOfBound Exception.
                break;                                      // We break the loop.
            }
            // Accumulatively apply the intersection; When doing binary operation, we have to do it Accumulatively.
            accumulateIntersectedDocId = CollectionUtil.intersect(accumulateIntersectedDocId, nextPosting.getList(),
                                                                        CollectionUtil.COMPARATOR_INTEGER);
        }
        return accumulateIntersectedDocId;
    }

}

/**
 * CollectionUtil consists of static helper methods for Manipulating certain Data Structures
 */
class CollectionUtil{

    /**
     * PostingList Comparator which compares them by their Term Identification numbers
     */
    public static final Comparator<PostingList> COMPARATOR_POSTING_LIST_TERM_ID = new Comparator<PostingList>(){
        @Override
        public int compare(PostingList o1, PostingList o2){
            return o1.getTermId() - o2.getTermId();
        }
    };

    /**
     * PostingList Comparator which compares them by their Posting Length
     */
    public static final Comparator<PostingList> COMPARATOR_POSTING_LIST_DOC_FREQ = new Comparator<PostingList>(){
        @Override
        public int compare(PostingList o1, PostingList o2){
            return o1.getList().size() - o2.getList().size();
        }
    };

    /**
     * Integer Comparator
     */
    public static final Comparator<Integer> COMPARATOR_INTEGER = Comparator.comparingInt(o -> o);

    /**
     * Calculates an intersection between two given Lists with duplications allowed.
     *
     * @param listA a List containing @T elements
     * @param listB a List containing @T elements
     * @param c     @T Comparator
     * @param <T>   Type of the elements inside the list
     *
     * @return Intersection with duplications
     */
    public static <T> List<T> intersect(List<T> listA, List<T> listB, Comparator<T> c){
        ArrayList<T> intersected = new ArrayList<>();

        // Initializes the pointer indexes
        int i = 0, j = 0;

        // Get the size of both lists
        int sizeA = listA.size(), sizeB = listB.size();

        // While both pointers are still lower than sizes
        while (i < sizeA && j < sizeB){
            // Compares both Elements against each other
            int comparison = c.compare(listA.get(i), listB.get(j));
            if (comparison < 0){                    // If the First Element is lesser
                // Move up the pointer of the First Array by one position
                i++;
            }else if (comparison > 0){              // If the First Element is greater
                // Move up the pointer of the Second Array by one position
                j++;
            }else{
                // Move up the pointer of both arrays by one position
                // Add the element into the Intersection Array
                intersected.add(listB.get(j++));
                i++;
            }
        }

        // Sorts the Result by using type comparator
        intersected.sort(c);
        return intersected;

        // sortedListA.retainAll(sortedListB);
        // return sortedListA;
    }

    /**
     * Finds Symmetrical Difference between two given Lists with duplication allowed
     *
     * @param listA a List containing @T elements
     * @param listB a List containing @T elements
     * @param c     @T Comparator
     * @param <T>   Type of the elements inside the list
     *
     * @return List of Symmetrical Difference with duplications
     */
    public static <T> List<T> symmetricDifferentiate(List<T> listA, List<T> listB, Comparator<T> c){

        // Calculate the intersection
        List<T> intersection = intersect(listA, listB, c);

        // Get the Union between both Lists
        List<T> union = new ArrayList<>(listA);
        union.addAll(listB);

        // Remove the intersection
        union.removeAll(intersection);
        return union;   // Return the rest
    }

    /**
     * Finds both Intersection and Symmetrical Difference between two given Lists with duplication allowed
     *
     * @param listA a List containing @T elements
     * @param listB a List containing @T elements
     * @param <T>   Type of the elements inside the list
     *
     * @return Result List with duplicate elements
     */
    public static <T> Pair<AutoSortList<T>, AutoSortList<T>> intersectAndSymmetricDifferentiate(AutoSortList<T> listA, AutoSortList<T> listB){

        // Initialize array to store the intersection
        ArrayList<T> intersected = new ArrayList<>();

        // Initialize array to store the symmetrical differences
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

        return new Pair<>(new AutoSortList<T>(intersected, listA.getComparator()),
                          new AutoSortList<T>(symmetricDiff, listA.getComparator()));
    }

    /**
     * Builds a string represents the content of a List of PostingList
     *
     * @param postingLists PostingList to be fetched content from
     *
     * @return String of the PostingList List
     */
    public static String postingListArrayToString(List<PostingList> postingLists){
        StringBuilder stringBuilder = new StringBuilder("[");
        for (PostingList p : postingLists) {
            stringBuilder.append(p.getTermId()).append("->{");
            for (int i = 0; i < p.getList().size(); i++) {
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
     * A simple Extended Class that mimics the idea of SortedList
     *
     * @param <T>
     */
    public static class AutoSortList<T> extends ArrayList<T>{

        private Comparator<T> comparator;

        public AutoSortList(Comparator<T> comparator){
            this(new ArrayList<>(), comparator);
        }

        public AutoSortList(@NotNull Collection<? extends T> c, Comparator<T> comparator){
            super(c);
            this.comparator = comparator;

            // Sort the List
            this.sort(comparator);
        }

        @Override
        public boolean add(T t){
            final boolean outcome = super.add(t);
            this.sort(comparator);
            return outcome;
        }

        @Override
        public void add(int index, T element){
            super.add(index, element);
            this.sort(comparator);
        }

        @Override
        public boolean addAll(Collection<? extends T> c){
            final boolean outcome = super.addAll(c);
            this.sort(comparator);
            return outcome;
        }

        @Override
        public boolean addAll(int index, Collection<? extends T> c){
            final boolean outcome = super.addAll(index, c);
            this.sort(comparator);
            return outcome;
        }

        @Override
        public void replaceAll(UnaryOperator<T> operator){
            super.replaceAll(operator);
            this.sort(comparator);
        }

        @Override
        public boolean removeIf(Predicate<? super T> filter){
            final boolean outcome = super.removeIf(filter);
            this.sort(comparator);
            return outcome;
        }

        @Override
        public AutoSortList<T> clone(){
            return new AutoSortList<T>((Collection<? extends T>) super.clone(), comparator);
        }

        public Comparator<T> getComparator(){
            return comparator;
        }

        public void setComparator(Comparator<T> comparator){
            this.comparator = comparator;
        }

    }
}

