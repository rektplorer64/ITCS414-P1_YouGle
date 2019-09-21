import java.io.*;
import java.util.*;

class BonusRunner{

    public static String[] week4HomeworkQueries = {"car insurance"};

    public static String[] customQueries = {"shortest path algorithm"};

    public static String[] queriesSmall = {"hello", "bye", "you", "how are you", "how are you ?"};

    public static String[] queriesLarge = {"we are", "stanford class", "stanford students", "very cool", "the", "a",
                                           "the the", "stanford computer science"};

    public static String[] queriesCiteseer = {"shortest path algorithm", "support vector machine", "random forest",
                                              "convolutional neural networks", "jesus", "mahidol", "chulalongkorn",
                                              "thailand", "polar bears penguins tigers", "algorithm search engine",
                                              "innovative product design social media", "suppawong", "tuarob",
                                              "suppawong tuarob", "suppawong tuarob conrad tucker"};

    public static void testQuery(String indexMode, String indexDirname, String[] queries, String outputDir){
        StringBuilder str = new StringBuilder();
        str.append("Query Test Result: " + Arrays.toString(queries) + ":\n");

        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startTime = System.currentTimeMillis();

        Query queryService = new RankedQuery();
        try{
            queryService.runQueryService(indexMode, indexDirname);
        }catch (IOException e){
            e.printStackTrace();
        }

        File f = new File(outputDir);
        if (!f.exists()){
            f.mkdirs();
        }

        for (int i = 0; i < queries.length; i++) {
            System.out.println("Query[" + (i + 1) + "]:" + queries[i]);
            List<Integer> hitDocs = null;
            try{
                hitDocs = queryService.retrieve(queries[i]);
            }catch (IOException e){
                e.printStackTrace();
            }
            String output = queryService.outputQueryResult(hitDocs);
            System.out.println();
            try{
                File file = new File(outputDir, (i + 1) + ".out");

                // if file doesnt exists, then create it
                if (!file.exists()){
                    file.createNewFile();
                }

                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);

                bw.write(output);
                bw.close();
            }catch (IOException e){

                e.printStackTrace();
            }

        }
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long endTime = System.currentTimeMillis();
        str.append("\tMemory Used: " + ((memoryAfter - memoryBefore) / 1000000.0) + " MBs\n");
        str.append("\tTime Used: " + ((endTime - startTime) / 1000.0) + " secs\n");
        str.append("\tNo problem. Have a good day.\n");
        System.out.println(str.toString());

        //Writing out the stats to a log file
        try{
            File file = new File(outputDir, "stats.txt");

            // if file doesnt exists, then create it
            if (!file.exists()){
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(str.toString());
            bw.close();
        }catch (IOException e){

            e.printStackTrace();
        }

    }

    public static void testBonusIndex(String dataDirname, String indexDirname){
        StringBuilder str = new StringBuilder();
        int numFiles = -1;
        str.append("Indexing Test Result: " + indexDirname + ":\n");
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startTime = System.currentTimeMillis();

        RankedIndex rankedIndex = new RankedIndex();
        try{
            numFiles = rankedIndex.runIndexer(dataDirname, indexDirname);
        }catch (IOException e){
            e.printStackTrace();
        }

        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long endTime = System.currentTimeMillis();
        File indexFile = new File(indexDirname, "corpus.index");
        long indexSize = indexFile.length();
        str.append("\tTotal Files Indexed: " + numFiles + "\n");
        str.append("\tMemory Used: " + ((memoryAfter - memoryBefore) / 1000000.0) + " MBs\n");
        str.append("\tTime Used: " + ((endTime - startTime) / 1000.0) + " secs\n");
        str.append("\tIndex Size: " + (indexSize / 1048576.0) + " MBs\n");
        str.append("\tAlright. Good Bye.\n");

        System.out.println(str.toString());

        //Writing out the stats to a log file
        try{
            File file = new File(indexDirname, "stats.txt");

            // if file does not exist, then create it
            if (!file.exists()){
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(str.toString());
            bw.close();
        }catch (IOException e){

            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        // testBonusIndex("./datasets/week4Homework", "./index/bonus/week4Homework");
        // testQuery("Basic", "./index/bonus/week4Homework", week4HomeworkQueries, "./output/bonus/week4Homework");
        // Taken From the Homework of Week 4 Slide

        // testBonusIndex("./datasets/small", "./index/bonus/small");
        // testQuery("Basic", "./index/bonus/small", customQueries, "./output/bonus/small");

        // testBonusIndex("./datasets/large", "./index/bonus/large");
        // testQuery("Basic", "./index/bonus/large", queriesLarge, "./output/bonus/large");

        testBonusIndex("./datasets/citeseer", "./index/bonus/citeseer");
        testQuery("Basic", "./index/bonus/citeseer", queriesLarge, "./output/bonus/citeseer");
    }
}

class RankedQuery extends Query{
    /**
     * Map of Term Id -> Byte Position in the index file
     */
    private Map<Integer, Long> posDict = new TreeMap<>();

    /**
     * Map of Term Id -> Document Frequency
     */
    private Map<Integer, Integer> freqDict = new TreeMap<>();

    /**
     * Map of Term Id -> Term Frequency
     */
    private Map<Integer, Integer> termFreqDict = new TreeMap<>();


    /**
     * Map of Doc Id -> Term Id
     */
    private Map<Integer, String> docDict = new TreeMap<>();

    /**
     * Map of Term String -> Term Id
     */
    private Map<String, Integer> termDict = new TreeMap<>();

    /**
     * Indexer Instance
     */
    private RankedIndexer index = null;

    /**
     * Indicate whether the query service is running or not
     */
    private boolean running = false;

    /**
     * File instance of the Binary Index File
     */
    private RandomAccessFile indexFile = null;

    // termId --> {docId --> score}
    private Map<Integer, Map<Integer, Double>> scoreMatrix = new HashMap<>();
    private Map<Integer, Double> normMap = new HashMap<>();

    private int totalDocument;

    public RankedQuery(){ }

    @Override
    public List<Integer> retrieve(String query) throws IOException{
        // Manipulate the Query
        ArrayList<String> queries = QueryUtil.tokenizeQuery(query);
        // System.out.println("Queries = " + queries);

        // Get TermId by query string
        ArrayList<Integer> termIds = QueryUtil.getTermIdByQuery(termDict, queries);
        // System.out.println("Term Dict = " + termDict);
        // System.out.println("Term Id = " + termIds);

        // Fetch Byte Position inside the index file of each Term (termId: Int -> PostingList)
        HashMap<Integer, PostingList> termIdPostingListMap = new HashMap<>();
        for (Integer termId : termIds) {        // For each term Id
            try{
                // Put PostingList to the Map; Null if there is none
                Long bytePosition = posDict.get(termId);
                indexFile.getChannel().position(bytePosition);

                PostingList p = index.readPosting(indexFile.getChannel());
                // System.out.println("Fetched PostingList = " + p.getTermId());
                termIdPostingListMap.put(termId, p);
            }catch (IOException | NullPointerException e){
                e.printStackTrace();
            }
        }

        ArrayList<PostingList> queryTermPostingLists = new ArrayList<>(termIdPostingListMap.values());
        queryTermPostingLists.sort(CollectionUtil.COMPARATOR_POSTING_LIST_DOC_FREQ);

        TreeSet<Integer> docIdAccumulator = new TreeSet<>();

        // totalDocument = 811400;

        System.out.println("\n----------------------------------");
        System.out.println(" ** CONSTRUCTING QUERY VECTOR **");
        System.out.println("----------------------------------");
        // Construct the Document Vector for Query
        DocumentVector queryVector = new DocumentVector(-1);
        for (PostingList p : queryTermPostingLists) {
            docIdAccumulator.addAll(p.getList());
            String term = null;
            for (String s : termDict.keySet()) {
                if (termDict.get(s) == p.getTermId()){
                    term = s;
                    break;
                }
            }

            int termFreqInQuery = 0;
            for (String s : queries) {
                if (s.equals(term)){
                    termFreqInQuery++;
                }
            }

            double termFrequency = RankingMathHelper.calculateTermFrequency(termFreqInQuery);
            System.out.println("\nTerm #"+ p.getTermId() + "\t\tdf = " + freqDict.get(p.getTermId()));
            double idfScore = RankingMathHelper.calculateInvertedDocFrequency(totalDocument,
                                                                              freqDict.get(p.getTermId()));

            System.out.println("Score: tf = " + termFrequency + "\t\t×\tidf = " + idfScore + "\t=>\t" + (termFrequency * idfScore));
            queryVector.getScoreMatrix().put(p.getTermId(), termFrequency * idfScore);
        }
        queryVector.setNorm(RankingMathHelper.calculateNorm(queryVector.getScoreMatrix()));
        System.out.println();
        // TODO: We got the norm of the query vector, now we have to calculate cosine similarity against all doc related

        // System.out.println(normMap);
        // System.out.println(scoreMatrix);

        System.out.println("----------------------------------");
        System.out.println("** CALCULATING SIMILARITY SCORE **");
        System.out.println("----------------------------------");
        TreeMap<Double, Integer> ranked = new TreeMap<>();
        for (int docId : docIdAccumulator) {
            DocumentVector documentVector = new DocumentVector(docId, normMap.get(docId), scoreMatrix.get(docId));
            double similarity = RankingMathHelper.cosineSimilarity(documentVector, queryVector);
            ranked.put(similarity, docId);
        }

        System.out.println("\n\n** RESULT MAP = " + ranked);
        return printRankedResult(ranked);
    }

    private ArrayList<Integer> printRankedResult(TreeMap<Double, Integer> rankedResult){
        ArrayList<Double> scores = new ArrayList<>(rankedResult.keySet());
        Collections.reverse(scores);

        List<Double> trimmed = scores.subList(0, Math.min(10, scores.size()));

        ArrayList<Integer> rankedDocId = new ArrayList<>();
        int i = 1;
        System.out.println();
        System.out.println("FINAL DOCUMENT RANKING");
        for (double score : trimmed){
            int docId = rankedResult.get(score);
            System.out.println("Rank #" + (i++) + "\twith Score = " + String.format("%.6f", score) + "\tis\t" + docDict.get(docId) + "\t(#" + docId +")");
            rankedDocId.add(docId);
        }
        return rankedDocId;
    }

    @Override
    String outputQueryResult(List<Integer> res){
        StringBuilder resultStringBuilder = new StringBuilder();
        // Collections.sort(res);
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

    @Override
    public void runQueryService(String indexMode, String indexDirname) throws IOException{

        index = new RankedIndexer();

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

        totalDocument = docDict.size();
        // System.out.println("Total DocDict = " + totalDocument);

        /* Posting dictionary */
        BufferedReader postReader = new BufferedReader(new FileReader(new File(indexDirname, "posting.dict")));
        while ((line = postReader.readLine()) != null){
            String[] tokens = line.split("\t");
            int termId = Integer.parseInt(tokens[0]);
            posDict.put(termId, Long.parseLong(tokens[1]));
            freqDict.put(termId, Integer.parseInt(tokens[2]));
            termFreqDict.put(termId, Integer.parseInt(tokens[3]));
        }
        postReader.close();

        RandomAccessFile docTermFreqFile = new RandomAccessFile(new File(indexDirname, "score.matrix"), "rw");
        while (docTermFreqFile.getFilePointer() < docTermFreqFile.length()){
            // System.out.println(docTermFreqFile.getFilePointer() + "/" + docTermFreqFile.length());
            DocumentVector vector = index.readDocFreqIndex(docTermFreqFile);

            scoreMatrix.put(vector.getDocId(), vector.getScoreMatrix());
            normMap.put(vector.getDocId(), vector.getNorm());
        }
        docTermFreqFile.close();

        this.running = true;
    }

    public static class RankingMathHelper{
        static double calculateTermFrequency(int frequency){
            if (frequency == 0){
                return 0d;
            }
            return 1d + Math.log10(frequency);
            // return frequency;
        }

        static double calculateInvertedDocFrequency(int totalDocument, int docFrequency){
            return Math.log10((double) totalDocument / docFrequency);
        }

        static Double calculateNorm(Map<Integer, Double> scoreVector){
            Map<Integer, Map<Integer, Double>> wrapper = new HashMap<>();
            wrapper.put(0, scoreVector);
            return RankingMathHelper.calculateNormMatrix(wrapper).get(0);
        }

        static Map<Integer, Double> calculateNormMatrix(Map<Integer, Map<Integer, Double>> scoreMatrix){
            Map<Integer, Double> normMap = new TreeMap<>();
            for (Map.Entry<Integer, Map<Integer, Double>> entry : scoreMatrix.entrySet()) {
                double sum = 0;
                for (int termId : entry.getValue().keySet()) {
                    sum += Math.pow(entry.getValue().get(termId), 2);
                }
                normMap.put(entry.getKey(), Math.sqrt(sum));
            }
            return normMap;
        }

        static double cosineSimilarity(DocumentVector doc, DocumentVector query){
            double sum = 0;
            System.out.println("\n** Cosine Similarity of Query & Doc #" + doc.getDocId());
            System.out.println("\tDoc Norm = " + doc.getNorm());
            for (int termId : query.getScoreMatrix().keySet()) {
                if (doc.getScoreMatrix().get(termId) == null){
                    continue;
                }
                double queryScore = query.getScoreMatrix().get(termId);
                double docScore = doc.getScoreMatrix().get(termId);
                System.out.println("\n\tTerm #" + termId);
                System.out.println("\tScore -> Query = " + queryScore + "\t\t×\tDoc = " + docScore + "\t=>\t" + (queryScore * docScore));
                sum += queryScore * docScore;
            }

            double result = sum / (doc.getNorm() * query.getNorm());
            System.out.println("\n\tFinal Similarity = " + result);
            return result;
        }
    }
}

public class RankedIndex{

    /**
     * Map of Term Id -> Pair of Byte Position and Document Frequency
     */
    private static Map<Integer, Pair<Long, Integer>> postingDict = new TreeMap<Integer, Pair<Long, Integer>>();

    /**
     * Map of Document String (directory + file name) -> Document Id
     */
    private Map<String, Integer> docDict = new TreeMap<String, Integer>();

    /**
     * Map of Term String -> Term Id
     */
    private Map<String, Integer> termDict = new TreeMap<String, Integer>();

    /**
     * Block Queue for storing File to be processed
     */
    private LinkedList<File> blockQueue = new LinkedList<File>();

    private Map<Integer, Integer> termFreqMap = new TreeMap<>();

    /**
     * Map between docId and {Map termId between termFreq}
     */
    private Map<Integer, Map<Integer, Integer>> docTermFrequency = new TreeMap<>();

    /**
     * Count of the total file
     */
    private int totalFileCount = 0;

    /**
     * Count of the Document Id
     */
    private int docIdCounter = 0;

    /**
     * Count of the Word Id
     */
    private int wordIdCounter = 0;

    /**
     * Indexer Instance
     */
    private RankedIndexer index = null;

    public int runIndexer(String dataDirname, String outputDirname) throws IOException{
        /* Get index */
        index = new RankedIndexer();

        /* Get root directory of where the Data set is stored */
        File rootdir = new File(dataDirname);
        if (!rootdir.exists() || !rootdir.isDirectory()){
            System.err.println("Invalid data directory: " + dataDirname);
            return -1;
        }


        /* Get output directory*/
        File outdir = new File(outputDirname);
        if (outdir.exists() && !outdir.isDirectory()){     // If the given url exists and is not a folder -> Error
            System.err.println("Invalid output directory: " + outputDirname);
            return -1;
        }

        try{
            FileUtil.purgeDirectory(outdir);
        }catch (NullPointerException e){
            outdir.mkdirs();
        }

        // Recreate the Directory if it is not finish
        if (!outdir.exists()){
            if (!outdir.mkdirs()){
                System.err.println("Create output directory failure");
                return -1;
            }
        }

        /* BSBI indexing algorithm */
        File[] dirlist = rootdir.listFiles();       // Lists all folders inside the Root

        /* For each block */
        for (File block : dirlist) {        // For each Folder eg. ./datasets/small/ 0, 1, 2

            // Get output folder
            File blockFile = new File(outputDirname, block.getName());
            System.out.println("Processing block " + block.getName());

            // One Folder == One Block
            blockQueue.add(blockFile);

            // Get the folder eg. ./datasets/small/0, ./datasets/small/1, ./datasets/small/2
            File blockDir = new File(dataDirname, block.getName());

            // Lists all text file inside
            File[] filelist = blockDir.listFiles();

            // Temporary Data Structure for storing PostingList in a Block before writing
            // Integer Key -> termId & Set<Integer> -> a TreeSet for docId (Why TreeSet? -> We want sorting and no duplication capabilities!)
            Map<Integer, Set<Integer>> blockPostingLists = new TreeMap<>();

            // Map<Integer, Pair<Long, Integer>> blockPostingDict = new TreeMap<>();

            /* For each file */
            for (File file : filelist) {
                ++totalFileCount;
                String fileName = block.getName() + "/" + file.getName();   // etc. "00/fine.txt" is the Document Name

                // use pre-increment to ensure docID > 0
                int docId = ++docIdCounter;
                docDict.put(fileName, docId);       // Map Document Name with auto-incremented ID Integer


                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;

                while ((line = reader.readLine()) != null){
                    // Read each line of the Text File
                    String[] tokens = line.trim().split(
                            "\\s+");                  // Splits the String By white spaces (So we can get the Array Of Word Strings aka Tokens) **
                    for (String token : tokens) {

                        // Related Data Containers: termDict, postingDict, blockPostingLists
                        int currentWordId;
                        String manipulatedToken = token.toLowerCase().trim();
                        if (!termDict.containsKey(
                                manipulatedToken)){              // If the term dict does not contain the token
                            termDict.put(manipulatedToken, ++wordIdCounter);        // Put it into the termDict TreeMap
                            currentWordId = wordIdCounter;
                        }else{
                            currentWordId = termDict.get(manipulatedToken);
                        }

                        if (!postingDict.containsKey(currentWordId)){    // If PostingDict does not contain that termId
                            postingDict.put(currentWordId, null);       // Put it in, Mark byte position as -1L
                        }

                        // If we never meet this token before in termFreqMap
                        if (!termFreqMap.containsKey(currentWordId)){
                            termFreqMap.put(currentWordId, 0);    // Prepare to count
                        }
                        termFreqMap.put(currentWordId, termFreqMap.get(currentWordId) + 1); // Count It up

                        if (!docTermFrequency.containsKey(docId)){
                            // System.out.println("Created Vect for doc: " + docId);
                            docTermFrequency.put(docId, new TreeMap<>());
                        }

                        final Map<Integer, Integer> termFreqVector = docTermFrequency.get(docId);
                        if (!termFreqVector.containsKey(currentWordId)){
                            termFreqVector.put(currentWordId, 0);
                        }
                        // System.out.println("Counting term=" + currentWordId);
                        termFreqVector.put(currentWordId, termFreqVector.get(currentWordId) + 1);
                        // System.out.println("\ntermFreq=" + termFrequency);
                        // System.out.println("docTermFreq=" + docTermFrequency);

                        if (!blockPostingLists.containsKey(currentWordId)){
                            blockPostingLists.put(currentWordId, new TreeSet<>());
                        }
                        blockPostingLists.get(currentWordId).add(docId);
                        // System.out.println("Added termId: " + termDict.get(token) + " == " + token + " (doc=" + docId + ")");
                    }
                }
                reader.close();
            }

            /*
             * !! POST-INDIVIDUAL BLOCK READING !!
             */

            /* Sort and output */
            if (!blockFile.createNewFile()){
                System.err.println("Create new block failure.");
                return -1;
            }

            RandomAccessFile bfc = new RandomAccessFile(blockFile, "rw");

            /* Form an Array of PostingList */
            for (int key : blockPostingLists.keySet()) {
                PostingList newPosting = new PostingList(key, new ArrayList<>(blockPostingLists.get(key)));

                // Count up the frequency
                if (postingDict.containsKey(
                        newPosting.getTermId())){                       // If there is an entry of current termId in the postingDict
                    Pair<Long, Integer> pair = postingDict.get(newPosting.getTermId());     // Get the pair inside
                    if (pair == null){      // If the pair is null.
                        postingDict.put(newPosting.getTermId(), new Pair<>(-1L, 0));        // Instantiate it
                        pair = postingDict.get(
                                newPosting.getTermId());                     // Add the current document frequency
                    }
                    pair.setSecond(pair.getSecond() +
                                   newPosting.getList().size());         // Add up with other document frequency
                }
                index.writePosting(bfc.getChannel(),
                                   newPosting);                           // Write it down into individual block index file namely 0, 1, ... (basically the Block name)
            }
            bfc.close();
            blockPostingLists = null;       // Clean up the reference so that it can be clean by System's GarbageCollector
        }

        /* !! POST-ALL BLOCKS READING !! */

        /* Required: output total number of files. */
        System.out.println("Total Files Indexed: " + totalFileCount);

        /* Merge blocks */
        while (true){
            if (blockQueue.size() <= 1){
                break;
            }

            // Get a Pair of index file reference from the Queue
            File b1 = blockQueue.removeFirst();
            File b2 = blockQueue.removeFirst();

            // Instantiate a new File to be merged
            File combfile = new File(outputDirname, b1.getName() + "+" + b2.getName());
            if (!combfile.createNewFile()){
                System.err.println("Create new block failure.");
                return -1;
            }

            // Get the reference of the first index file
            RandomAccessFile bf1 = new RandomAccessFile(b1, "r");

            // Get the reference of the second index file
            RandomAccessFile bf2 = new RandomAccessFile(b2, "r");

            // Get the reference of the result file (the Merged one) with READ + WRITE Access
            RandomAccessFile mf = new RandomAccessFile(combfile, "rw");

            // Calls a static helper method inside IndexHelpers Class (a nested inner class)
            List<PostingList> mergedList = IndexUtil.mergeBinaryIndexFile(bf1.getChannel(), bf2.getChannel(),
                                                                          index);
            // finalList.sort(Query.CollectionUtil.COMPARATOR_POSTING_LIST_TERM_ID);

            // System.out.println(Query.CollectionUtil.postingListArrayToString(mergedList));
            // Iterates the Merged List of PostingList
            for (PostingList posting : mergedList) {
                // Write it to the merged file one-by-one as well as storing BytePosition to postingDict
                postingDict.get(posting.getTermId()).setFirst(mf.getChannel().position());
                index.writePosting(mf.getChannel(), posting);

            }

            // Remove a large object reference, so that it may get cleaned by System's Garbage Collector
            mergedList = null;

            // Tidy up the file; Closing them
            bf1.close();
            bf2.close();
            mf.close();

            // Delete old index files because they are merged.
            b1.delete();
            b2.delete();

            // Add the merged file to be merge again.
            blockQueue.add(combfile);
        }

        // Hardcoded variables to reflect Week 4 homework

        // int[] hardCodedDocFreq = new int[]{18165, 6723, 25235, 19241};
        // int i = 0;
        // for (int termId : postingDict.keySet()) {
        //     postingDict.get(termId).setSecond(hardCodedDocFreq[i++]);
        // }
        // System.out.println("termDict=" + termDict);
        // System.out.println("docTermFrequency=" + docTermFrequency);
        // System.out.println("postingDict=" + postingDict);
        // totalFileCount = 811400;

        /* Dump constructed index back into file system */
        File indexFile = blockQueue.removeFirst();
        indexFile.renameTo(new File(outputDirname, "corpus.index"));

        BufferedWriter termWriter = new BufferedWriter(new FileWriter(new File(outputDirname, "term.dict")));
        for (String term : termDict.keySet()) {
            termWriter.write(term + "\t" + termDict.get(term) + "\n");
        }
        termWriter.close();

        BufferedWriter docWriter = new BufferedWriter(new FileWriter(new File(outputDirname, "doc.dict")));
        for (String doc : docDict.keySet()) {
            docWriter.write(doc + "\t" + docDict.get(doc) + "\n");
        }
        docWriter.close();

        BufferedWriter postWriter = new BufferedWriter(new FileWriter(new File(outputDirname, "posting.dict")));
        for (Integer termId : postingDict.keySet()) {
            postWriter.write(
                    termId + "\t" + postingDict.get(termId).getFirst() + "\t" + postingDict.get(termId).getSecond() +
                    "\t" + termFreqMap.get(termId) + "\n");
        }
        postWriter.close();


        // TODO: Calculate Score
        Map<Integer, Map<Integer, Double>> ifIdf = calculateTfIdf(totalFileCount);

        // DocId -> Norms Value
        Map<Integer, Double> norms = RankedQuery.RankingMathHelper.calculateNormMatrix(ifIdf);
        RandomAccessFile docTermFreqFile = new RandomAccessFile(new File(outputDirname, "score.matrix"), "rw");
        for (Map.Entry<Integer, Map<Integer, Double>> entry : ifIdf.entrySet()) {
            index.writeDocFreqIndex(docTermFreqFile, entry.getKey(), entry.getValue(), norms.get(entry.getKey()));
        }
        docTermFreqFile.close();

        return totalFileCount;
    }

    private Map<Integer, Map<Integer, Double>> calculateTfIdf(int totalFileCount){
        Map<Integer, Map<Integer, Double>> scoreMatrix = new TreeMap<>();

        for (Map.Entry<Integer, Map<Integer, Integer>> docTermFreq : docTermFrequency.entrySet()) {
            // System.out.println("DocId=" + docTermFreq.getKey());
            if (!scoreMatrix.containsKey(docTermFreq.getKey())){
                scoreMatrix.put(docTermFreq.getKey(), new TreeMap<>());
            }
            Map<Integer, Double> termIdScore = scoreMatrix.get(docTermFreq.getKey());
            for (Map.Entry<Integer, Integer> termIdFreqMap : docTermFreq.getValue().entrySet()) {

                int termId = termIdFreqMap.getKey();
                int termFreq = termIdFreqMap.getValue();
                int docFreq = postingDict.get(termId).getSecond();

                double tf = RankedQuery.RankingMathHelper.calculateTermFrequency(termFreq);
                double idf = RankedQuery.RankingMathHelper.calculateInvertedDocFrequency(totalFileCount, docFreq);
                // System.out.println("\tidf=" + idf + "\t*\ttf=" + tf + "\t= " + (tf * idf));
                termIdScore.put(termId, tf * idf);
            }
        }
        return scoreMatrix;
    }
}

class DocumentVector{
    private int docId;
    private double norm;
    private Map<Integer, Double> scoreMatrix;

    public DocumentVector(int docId, double norm, Map<Integer, Double> scoreMatrix){
        this.docId = docId;
        this.norm = norm;
        this.scoreMatrix = scoreMatrix;
    }

    public DocumentVector(int docId){
        this(docId, 0, new HashMap<>());
    }

    public int getDocId(){
        return docId;
    }

    public void setDocId(int docId){
        this.docId = docId;
    }

    public double getNorm(){
        return norm;
    }

    public void setNorm(double norm){
        this.norm = norm;
    }

    public Map<Integer, Double> getScoreMatrix(){
        return scoreMatrix;
    }

    public void setScoreMatrix(Map<Integer, Double> scoreMatrix){
        this.scoreMatrix = scoreMatrix;
    }
}

class RankedIndexer extends BasicIndex{
    public DocumentVector readDocFreqIndex(RandomAccessFile file) throws IOException{
        int docId = file.readInt();
        int termMapSize = file.readInt();

        double norm = file.readDouble();

        Map<Integer, Double> scoreMat = new TreeMap<>();
        for (int i = 0; i < termMapSize; i++) {
            scoreMat.put(file.readInt(), file.readDouble());
        }

        return new DocumentVector(docId, norm, scoreMat);
    }

    public void writeDocFreqIndex(RandomAccessFile file, int docId, Map<Integer, Double> scoreMap, double norm)
            throws IOException{
        file.writeInt(docId);

        int termMapSize = scoreMap.size();
        file.writeInt(termMapSize);
        file.writeDouble(norm);
        for (Map.Entry<Integer, Double> entry : scoreMap.entrySet()) {
            file.writeInt(entry.getKey());
            file.writeDouble(entry.getValue());
        }
    }
}

