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
            Map<Integer, Set<Integer>> blockPostingLists = new TreeMap<>();          // Temporary Data Structure for storing PostingList before conversion

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

                        // TODO: Related Data Containers: termDict, postingDict, postingList
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
            // System.out.println("block " + block.getName() + ", postingList=" + blockPostingLists);

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
                if (postingDict.containsKey(newPosting.getTermId())){
                    Pair<Long, Integer> pair = postingDict.get(newPosting.getTermId());
                    if (pair == null){
                        postingDict.put(newPosting.getTermId(), new Pair<>(-1L, 0));
                        pair = postingDict.get(newPosting.getTermId());
                    }
                    pair.setSecond(pair.getSecond() + newPosting.getList().size());
                }
                // System.out.println(newPosting.getTermId() + " -> " + newPosting.getList());
                index.writePosting(bfc.getChannel(), newPosting);
            }
            bfc.close();
            blockPostingLists = null;
        }
        // System.out.println(postingDict + "\n");

        /* Required: output total number of files. */
        System.out.println("Total Files Indexed: " + totalFileCount);

        /* Merge blocks */
        while (true){
            if (blockQueue.size() <= 1){
                break;
            }

            File b1 = blockQueue.removeFirst();
            File b2 = blockQueue.removeFirst();

            File combfile = new File(outputDirname, b1.getName() + "+" + b2.getName());
            if (!combfile.createNewFile()){
                System.err.println("Create new block failure.");
                return -1;
            }

            RandomAccessFile bf1 = new RandomAccessFile(b1, "r");
            RandomAccessFile bf2 = new RandomAccessFile(b2, "r");
            RandomAccessFile mf = new RandomAccessFile(combfile, "rw");

            /*
             * TODO: Your code here
             *       Combine blocks bf1 and bf2 into our combined file, mf
             *       You will want to consider in what order to merge
             *       the two blocks (based on term ID, perhaps?).
             *
             */

            // long startTimeRead = System.currentTimeMillis();

            // List<PostingList> postingListA = FileUtil.readAllPostingLists(bf1.getChannel(), index);
            // List<PostingList> postingListB = FileUtil.readAllPostingLists(bf2.getChannel(), index);

            // long endTimeRead = System.currentTimeMillis();

            // System.out.println(combfile.getName() + "\n\tRead Time Used: " + ((endTimeRead - startTimeRead) / 1000.0) + " secs");

            // long startTimeProcess = System.currentTimeMillis();

            List<PostingList> finalList = IndexHelpers.mergePostingList(bf1.getChannel(), bf2.getChannel(), index);
            finalList.sort(Query.CollectionUtil.COMPARATOR_POSTING_LIST_TERM_ID);

            // long endTimeProcess = System.currentTimeMillis();

            // System.out.println("\tMerge Time Used: " + ((endTimeProcess - startTimeProcess) / 1000.0) + " secs");

            // System.out.println("final: " + finalList);

            // TODO: Loops thru SortedList
            // long startTime2 = System.currentTimeMillis();
            for (PostingList posting : finalList) {
                // System.out.println("Final " + posting.getTermId() + " " + posting.getList());
                writePosting(mf.getChannel(), posting);
            }
            // long endTime2 = System.currentTimeMillis();
            // System.out.println("\tWrite Time Used: " + ((endTime2 - startTime2) / 1000.0) + " secs");

            finalList = null;
            // System.gc();
            bf1.close();
            bf2.close();
            mf.close();
            b1.delete();
            b2.delete();
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

    public static class IndexHelpers{

        public static ArrayList<PostingList> mergePostingList(FileChannel fc1, FileChannel fc2, BaseIndex index)
                throws IOException{

            long i = 0, j = 0;
            long sizeA = fc1.size(), sizeB = fc2.size();

            Comparator<PostingList> c = Query.CollectionUtil.COMPARATOR_POSTING_LIST_TERM_ID;

            long prev_i, prev_j;

            // System.out.println("\nMerging PostingLists");

            ArrayList<PostingList> mergedResult = new ArrayList<>();
            while (i < sizeA && j < sizeB){
                prev_i = fc1.position();
                PostingList p1 = index.readPosting(fc1);
                i = prev_i;

                prev_j = fc2.position();
                PostingList p2 = index.readPosting(fc2);
                j = prev_j;

                int comparison = c.compare(p1, p2);
                if (comparison < 0){
                    mergedResult.add(p1);

                    i = fc1.position();
                    fc1.position(i);

                    fc2.position(j);

                    // j = prev_j;

                }else if (comparison > 0){
                    mergedResult.add(p2);

                    // i = prev_i;
                    fc1.position(i);

                    j = fc2.position();
                    fc2.position(j);
                }else{
                    ArrayList<PostingList> merged = new ArrayList<PostingList>();
                    merged.add(p1);
                    merged.add(p2);
                    mergedResult.addAll(combiningDuplicatePostingList(merged));
                    i = fc1.position();
                    j = fc2.position();
                }
                // System.out.println("i=" + i + "/" + sizeA + " j=" + j + "/" + sizeB + " --> " + (i < sizeA) + " and " + (j < sizeB) + " = " + (i < sizeA && j < sizeB));
            }

            if (i != sizeA){
                mergedResult.addAll(FileUtil.readAllPostingLists(fc1, index));
            }

            if (j != sizeB){
                mergedResult.addAll(FileUtil.readAllPostingLists(fc2, index));
            }

            // System.out.println("Merged Postings: " + Query.CollectionUtil.postingListArrayToString(mergedResult));
            return mergedResult;
        }

        public static List<PostingList> mergePostingList(List<PostingList> block1, List<PostingList> block2){
            // System.out.println("Started Merging → Block 1 = " + block1.size() + " | Block 2 = " + block2.size());
            block1.addAll(block2);
            return combiningDuplicatePostingList(block1);
        }

        public static List<PostingList> combiningDuplicatePostingList(List<PostingList> lists){
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

    }

    /**
     *
     */
    public static class FileUtil{
        public static void purgeDirectory(File dir){
            for (File file : dir.listFiles()) {
                if (file.isDirectory()){
                    purgeDirectory(file);
                }
                file.delete();
            }
        }

        public static ArrayList<PostingList> readAllPostingLists(FileChannel fileChannel, BaseIndex index)
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

        public static byte[] intToByteArray(int value){
            return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
        }
    }
}
