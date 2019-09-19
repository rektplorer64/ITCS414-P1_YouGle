import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * The class that responsible for Processing Document Data set by create a Binary index file
 */
public class Index{

    /**
     * Map of Term Id -> Pair of Byte Position and Document Frequency
     */
    private static Map<Integer, Pair<Long, Integer>> postingDict = new TreeMap<Integer, Pair<Long, Integer>>();

    /**
     * Map of Document String (directory + file name) -> Document Id
     */
    private static Map<String, Integer> docDict = new TreeMap<String, Integer>();

    /**
     * Map of Term String -> Term Id
     */
    private static Map<String, Integer> termDict = new TreeMap<String, Integer>();

    /**
     * Block Queue for storing File to be processed
     */
    private static LinkedList<File> blockQueue = new LinkedList<File>();

    /**
     * Count of the total file
     */
    private static int totalFileCount = 0;

    /**
     * Count of the Document Id
     */
    private static int docIdCounter = 0;

    /**
     * Count of the Word Id
     */
    private static int wordIdCounter = 0;

    /**
     * Indexer Instance
     */
    private static BaseIndex index = null;


    /**
     * Write a posting list to the given file You should record the file position of this posting list so that you can
     * read it back during retrieval
     *
     * @param fc      FileChannel Instance of the file to be written
     * @param posting PostingList to be written into the file
     *
     * @throws IOException
     */
    private static void writePosting(FileChannel fc, PostingList posting) throws IOException{
        /*
         * TODO: Your code here
         */
        postingDict.get(posting.getTermId()).setFirst(fc.position());
        index.writePosting(fc, posting);
    }


    /**
     * Pop next element if there is one, otherwise return null
     *
     * @param iter an iterator that contains integers
     *
     * @return next element or null
     */
    private static Integer popNextOrNull(Iterator<Integer> iter){
        if (iter.hasNext()){
            return iter.next();
        }else{
            return null;
        }
    }


    /**
     * Main method to start the indexing process.
     *
     * @param method        :Indexing method. "Basic" by default, but extra credit will be given for those who can
     *                      implement variable byte (VB) or Gamma index compression algorithm
     * @param dataDirname   :relative path to the dataset root directory. E.g. "./datasets/small"
     * @param outputDirname :relative path to the output directory to store index. You must not assume that this
     *                      directory exist. If it does, you must clear out the content before indexing.
     */
    public static int runIndexer(String method, String dataDirname, String outputDirname) throws IOException{
        /* Get index */
        String className = method + "Index";
        try{
            Class<?> indexClass = Class.forName(className);
            index = (BaseIndex) indexClass.newInstance();
        }catch (Exception e){
            System.err.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
            throw new RuntimeException(e);
        }

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

        // TODO: delete all the files/sub folder under outdir
        FileUtil.purgeDirectory(outdir);

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
                        /*
                         * TODO: Your code here
                         *       For each term, build up a list of
                         *       documents in which the term occurs
                         */

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

            /*
             * TODO: Your code here
             *       Write all posting lists for all terms to file (bfc)
             */

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
        // System.out.println(postingDict + "\n");

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

            /*
             * TODO: Your code here
             *       Combine blocks bf1 and bf2 into our combined file, mf
             *       You will want to consider in what order to merge
             *       the two blocks (based on term ID, perhaps?).
             *
             */

            // Calls a static helper method inside IndexHelpers Class (a nested inner class)
            List<PostingList> mergedList = IndexUtil.mergeBinaryIndexFile(bf1.getChannel(),
                                                                          bf2.getChannel(), index);

            // finalList.sort(Query.CollectionUtil.COMPARATOR_POSTING_LIST_TERM_ID);

            // System.out.println(Query.CollectionUtil.postingListArrayToString(mergedList));
            // Iterates the Merged List of PostingList
            for (PostingList posting : mergedList) {
                // Write it to the merged file one-by-one as well as storing BytePosition to postingDict
                writePosting(mf.getChannel(), posting);
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
                    "\n");
        }
        postWriter.close();

        return totalFileCount;
    }

    public static void main(String[] args) throws IOException{
        /* Parse command line */
        if (args.length != 3){
            System.err.println("Usage: java Index [Basic|VB|Gamma] data_dir output_dir");
            return;
        }

        /* Get index */
        String className = "";
        try{
            className = args[0];
        }catch (Exception e){
            System.err.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
            throw new RuntimeException(e);
        }

        /* Get root directory */
        String root = args[1];

        /* Get output directory */
        String output = args[2];
        runIndexer(className, root, output);
    }

    /**
     * IndexHelpers consists of static helper methods for Indexing
     */
    public static class IndexUtil{

        /**
         * Read a pair of PostingList from both file (fc1 & fc2). Then compare them.
         * <p>
         * The PostingList that has lower termId gets stored in the result array, and has its file pointer.
         * <p>
         * If both has the same termId, merge their docId.
         * <p>
         * Future Improvement: Reduce redundant READINGS by introducing variables to store PostingList when the file
         * pointer position is supposed to stay at the position before reading.
         *
         * @param fc1   FileChannel of the First File
         * @param fc2   FileChannel of the Second File
         * @param index Indexing Utility Instance for Reading
         *
         * @return A merged array of PostingList
         *
         * @throws IOException
         */
        public static ArrayList<PostingList> mergeBinaryIndexFile(FileChannel fc1, FileChannel fc2, BaseIndex index)
                throws IOException{

            // Setup initial Pointer positions
            long i = 0, j = 0;

            // Setup the limit of the file
            long sizeA = fc1.size(), sizeB = fc2.size();

            // Get the comparator for PostingList
            Comparator<PostingList> c = Query.CollectionUtil.COMPARATOR_POSTING_LIST_TERM_ID;

            // Storage for file pointer before reading new PostingLists
            long prev_i, prev_j;

            // Results to be returned
            ArrayList<PostingList> mergedResult = new ArrayList<>();

            // While either first file limit is not reached or second file limit is not reached
            // TL;DR -> If one of the file pointer goes equal to its size, the loop will stop immediately.
            while (i < sizeA && j < sizeB){

                prev_i = fc1.position();                    // Get the pointer pre-read position for file A
                PostingList p1 = index.readPosting(fc1);    // Read it out and store it to a variable
                i = prev_i;

                prev_j = fc2.position();                    // Get the pointer pre-read position for file B
                PostingList p2 = index.readPosting(fc2);    // Read it out and store it to a variable
                j = prev_j;

                int comparison = c.compare(p1, p2);         // Compare it by PostingLists' termId

                if (comparison <
                    0){                        // If the comparison is negative (termId of p1 is lower than p2's)
                    mergedResult.add(p1);                   // Add p1 to the result immediately

                    i = fc1.position();                     // Set the pointer position of i to the post-read position
                    fc1.position(i);

                    fc2.position(
                            j);                        // Move back p2's pointer to the pre-read position, because it is moved when we read it.

                    // j = prev_j;

                }else if (comparison >
                          0){                  // If the comparison is negative (termId of p2 is lower than p1's)
                    mergedResult.add(p2);                   // Add p2 to the result immediately

                    // i = prev_i;
                    fc1.position(
                            i);                        // Move back p1's pointer to the pre-read position, because it is moved when we read it.

                    j = fc2.position();                     // Set the pointer position of i to the post-read position
                    fc2.position(j);
                }else{

                    // Due to the way combiningDuplicatePostingList() works, it supports more than 2 PostingList to be merged.
                    // Supplying it with a List is the only option
                    mergedResult.add(combineDuplicatePostingList(p1, p2));

                    // Update both i and j to the latest pointer positions
                    i = fc1.position();
                    j = fc2.position();
                }
                // System.out.println("i=" + i + "/" + sizeA + " j=" + j + "/" + sizeB + " --> " + (i < sizeA) + " and " + (j < sizeB) + " = " + (i < sizeA && j < sizeB));
            }

            /*
             * POST-MERGE
             */

            if (i != sizeA){        // If file A is not totally read
                // Add the rest to the result array
                mergedResult.addAll(FileUtil.readAllPostingLists(fc1, index));
            }

            if (j != sizeB){        // If file B is not totally read
                // Add the rest to the result array
                mergedResult.addAll(FileUtil.readAllPostingLists(fc2, index));
            }

            // System.out.println("Merged Postings: " + Query.CollectionUtil.postingListArrayToString(mergedResult));
            return mergedResult;
        }

        /**
         * Deprecated method for Merging the whole PostingList.
         *
         * @param block1 PostingList list of block 1
         * @param block2 PostingList list of block 2
         *
         * @return Merged array of PostingList
         *
         * @deprecated This method is inefficient in term of memory usage as it loads up all PostingLists from files.
         */
        @Deprecated
        public static List<PostingList> expensivelyMergeListOfPostingList(List<PostingList> block1, List<PostingList> block2){
            // System.out.println("Started Merging → Block 1 = " + block1.size() + " | Block 2 = " + block2.size());
            block1.addAll(block2);
            return combineDuplicatePostingList(block1);
        }

        /**
         * Combine a list of PostingList by its termId. Duplicate will get its docId appended.
         *
         * @param lists List of PostingList
         *
         * @return merged list of PostingList
         *
         * @deprecated The method is an overkill for PostingList pair because of the HashMap. If you wants to merge only
         *         2 PostingList, see {@link #combineDuplicatePostingList(PostingList, PostingList)}
         */
        @Deprecated
        public static List<PostingList> combineDuplicatePostingList(List<PostingList> lists){
            HashMap<Integer, TreeSet<Integer>> termIdListPair = new HashMap<>();
            // Construct a HashMap that contains every entry
            // Combines duplications
            for (PostingList p : lists) {
                if (!termIdListPair.containsKey(p.getTermId())){
                    termIdListPair.put(p.getTermId(), new TreeSet<>());
                }
                termIdListPair.get(p.getTermId()).addAll(p.getList());
            }

            List<PostingList> result = new ArrayList<>();
            for (Map.Entry<Integer, TreeSet<Integer>> entry : termIdListPair.entrySet()) {
                result.add(new PostingList(entry.getKey(), new ArrayList<>(entry.getValue())));
            }
            return result;
        }

        /**
         * Combine a pair of PostingList with the same termId. Duplication of Document Id will be removed. Document Id
         * will be automatically sorted.
         *
         * @param p1 PostingList 1
         * @param p2 PostingList 2
         *
         * @return Merged PostingList
         */
        public static PostingList combineDuplicatePostingList(PostingList p1, PostingList p2){
            if (p1.getTermId() != p2.getTermId()){
                throw new IllegalArgumentException("Both PostingList's termId should be the same.");
            }

            // Put everything in p1 into the set
            TreeSet<Integer> documentIdSet = new TreeSet<>(p1.getList());

            // Add another from p2
            documentIdSet.addAll(p2.getList());

            return new PostingList(p1.getTermId(), new ArrayList<>(documentIdSet));
        }
    }

    /**
     * FileUtil consists of static helper methods for File Manipulations
     */
    public static class FileUtil{
        /**
         * Recursively Delete files and folders inside given Directory instance
         *
         * @param dir a directory to had its contents deleted
         */
        public static void purgeDirectory(File dir){
            for (File file : dir.listFiles()) {
                if (file.isDirectory()){
                    purgeDirectory(file);
                }
                file.delete();
            }
        }

        /**
         * Read all PostingList from given FileChannel from its current Position to the end of the file
         *
         * @param fileChannel File to be read
         * @param index       Posting index file reader
         *
         * @return ArrayList of PostingList inside the file
         *
         * @throws IOException
         */
        public static List<PostingList> readAllPostingLists(FileChannel fileChannel, BaseIndex index)
                throws IOException{
            ArrayList<PostingList> postingLists = new ArrayList<>();
            try{
                while (fileChannel.position() <= fileChannel.size() - 1){
                    postingLists.add(index.readPosting(fileChannel));
                }
                return postingLists;
            }catch (IOException e){
                e.printStackTrace();
                throw e;
            }
        }

        /**
         * Convert an Array of Int to an Array of Bytes
         *
         * The method was introduced because initially we want to bulk put multiple integers into ByteBuffer when writing.
         * It is no longer used.
         *
         * @param value integer to be converted
         *
         * @return byte array of the integer
         */
        public static byte[] convertIntToByteArray(int value){
            return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
        }
    }
}
