

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.*;

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
}

