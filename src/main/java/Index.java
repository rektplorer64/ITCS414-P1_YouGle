import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;

public class Index {

    // Term id -> (position in index file, doc frequency) dictionary
    private static Map<Integer, Pair<Long, Integer>> postingDict
            = new TreeMap<Integer, Pair<Long, Integer>>();
    // Doc name -> doc id dictionary
    private static Map<String, Integer> docDict
            = new TreeMap<String, Integer>();
    // Term -> term id dictionary
    private static Map<String, Integer> termDict
            = new TreeMap<String, Integer>();
    // Block queue
    private static LinkedList<File> blockQueue
            = new LinkedList<File>();

    // Total file counter
    private static int totalFileCount = 0;
    // Document counter
    private static int docIdCounter = 0;
    // Term counter
    private static int wordIdCounter = 0;
    // Index
    private static BaseIndex index = null;



    /*
     * Write a posting list to the given file
     * You should record the file position of this posting list
     * so that you can read it back during retrieval
     *
     * */
    private static void writePosting(FileChannel fc, PostingList posting)
            throws IOException {
        /*
         * TODO: Your code here
         */
    }


    /**
     * Pop next element if there is one, otherwise return null
     *
     * @param iter an iterator that contains integers
     *
     * @return next element or null
     */
    private static Integer popNextOrNull(Iterator<Integer> iter) {
        if (iter.hasNext()) {
            return iter.next();
        } else {
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
    public static int runIndexer(String method, String dataDirname, String outputDirname) throws IOException {
        /* Get index */
        String className = method + "Index";
        try {
            Class<?> indexClass = Class.forName(className);
            index = (BaseIndex) indexClass.newInstance();
        } catch (Exception e) {
            System.err
                    .println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
            throw new RuntimeException(e);
        }

        /* Get root directory of where the Data set is stored */
        File rootdir = new File(dataDirname);
        if (!rootdir.exists() || !rootdir.isDirectory()) {
            System.err.println("Invalid data directory: " + dataDirname);
            return -1;
        }


        /* Get output directory*/
        File outdir = new File(outputDirname);
        if (outdir.exists() && !outdir.isDirectory()) {     // If the given url exists and is not a folder -> Error
            System.err.println("Invalid output directory: " + outputDirname);
            return -1;
        }

        // TODO: delete all the files/sub folder under outdir
        FileUtil.purgeDirectory(outdir);

        // Recreate the Directory if it is not finish
        if (!outdir.exists()) {
            if (!outdir.mkdirs()) {
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

            Map<Integer, Set<Integer>> tempPostingLists = new TreeMap<>();          // Temporary Data Structure for storing PostingList before conversion

            /* For each file */
            for (File file : filelist) {
                ++totalFileCount;
                String fileName = block.getName() + "/" + file.getName();   // etc. "00/fine.txt" is the Document Name

                // use pre-increment to ensure docID > 0
                int docId = ++docIdCounter;
                docDict.put(fileName, docId);       // Map Document Name with auto-incremented ID Integer


                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;

                while ((line = reader.readLine()) != null) {                            // Read each line of the Text File
                    String[] tokens = line.trim().split("\\s+");                  // Splits the String By white spaces (So we can get the Array Of Word Strings aka Tokens) **
                    for (String token : tokens) {
                        /*
                         * TODO: Your code here
                         *       For each term, build up a list of
                         *       documents in which the term occurs
                         */

                        // TODO: Related Data Containers: termDict, postingDict, postingList
                        String manipulatedToken = token.toLowerCase().trim();
                        if (!termDict.containsKey(manipulatedToken)) {              // If the term dict does not contain the token
                            termDict.put(manipulatedToken, ++wordIdCounter);        // Put it into the termDict TreeMap
                        }

                        if (!postingDict.containsKey(wordIdCounter)) {                                        // If PostingDict does not contain that termId
                            postingDict.put(wordIdCounter, Pair.make(-1L, 1));                       // Put it in, Mark byte position as -1L
                        } else {                                                                               // If it is already contain that termId
                            Pair<Long, Integer> indexPosWithWordFreq = postingDict.get(wordIdCounter);
                            indexPosWithWordFreq.setSecond(indexPosWithWordFreq.getSecond() + 1);            // Increment the FREQUENCY up by 1
                        }

                        if (!tempPostingLists.containsKey(wordIdCounter)) {
                            tempPostingLists.put(wordIdCounter, new TreeSet<>());
                        }
                        // FIXME: Validate Performance vs. Ordinary List w/ if-else
                        tempPostingLists.get(wordIdCounter).add(docIdCounter);
                    }
                }
                reader.close();
            }


            /* Sort and output */
            if (!blockFile.createNewFile()) {
                System.err.println("Create new block failure.");
                return -1;
            }

            RandomAccessFile bfc = new RandomAccessFile(blockFile, "rw");

            /*
             * TODO: Your code here
             *       Write all posting lists for all terms to file (bfc)
             *
             */

            /* Form an Array of PostingList */
            ArrayList<PostingList> postingListArray = new ArrayList<>();
            for (int key : tempPostingLists.keySet()) {
                // System.out.println("Added " + key);
                postingListArray.add(new PostingList(key, new ArrayList<>(tempPostingLists.get(key))));
            }

            for (PostingList postingList : postingListArray) {
                // TODO: Write Posting List to file
                index.writePosting(bfc.getChannel(), postingList);
            }

            bfc.close();
        }
        // System.out.println(tempPostingLists);

        /* Required: output total number of files. */
        System.out.println("Total Files Indexed: " + totalFileCount);

        /* Merge blocks */
        while (true) {
            if (blockQueue.size() <= 1)
                break;

            File b1 = blockQueue.removeFirst();
            File b2 = blockQueue.removeFirst();

            File combfile = new File(outputDirname, b1.getName() + "+" + b2.getName());
            if (!combfile.createNewFile()) {
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

            // FIXME: OPTIMIZATION REQUIRED
            AutoSortList<PostingList> blockA = new AutoSortList<>(FileUtil.readPostingList(bf1.getChannel(), index), CollectionUtil.POSTING_LIST_COMPARATOR);
            AutoSortList<PostingList> blockB = new AutoSortList<>(FileUtil.readPostingList(bf2.getChannel(), index), CollectionUtil.POSTING_LIST_COMPARATOR);

            AutoSortList<PostingList> finalList = IndexHelpers.mergePostingList(blockA, blockB);

            // System.out.println("final: " + finalList);

            // TODO: Loops thru SortedList
            for (PostingList posting : finalList) {
                postingDict.get(posting.getTermId()).setFirst(mf.getChannel().position());
                index.writePosting(mf.getChannel(), posting);
            }

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

        BufferedWriter termWriter = new BufferedWriter(new FileWriter(new File(
                outputDirname, "term.dict")));
        for (String term : termDict.keySet()) {
            termWriter.write(term + "\t" + termDict.get(term) + "\n");
        }
        termWriter.close();

        BufferedWriter docWriter = new BufferedWriter(new FileWriter(new File(
                outputDirname, "doc.dict")));
        for (String doc : docDict.keySet()) {
            docWriter.write(doc + "\t" + docDict.get(doc) + "\n");
        }
        docWriter.close();

        BufferedWriter postWriter = new BufferedWriter(new FileWriter(new File(
                outputDirname, "posting.dict")));
        for (Integer termId : postingDict.keySet()) {
            postWriter.write(termId + "\t" + postingDict.get(termId).getFirst()
                    + "\t" + postingDict.get(termId).getSecond() + "\n");
        }
        postWriter.close();

        return totalFileCount;
    }

    public static void main(String[] args) throws IOException {
        /* Parse command line */
        if (args.length != 3) {
            System.err
                    .println("Usage: java Index [Basic|VB|Gamma] data_dir output_dir");
            return;
        }

        /* Get index */
        String className = "";
        try {
            className = args[0];
        } catch (Exception e) {
            System.err
                    .println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
            throw new RuntimeException(e);
        }

        /* Get root directory */
        String root = args[1];


        /* Get output directory */
        String output = args[2];
        runIndexer(className, root, output);
    }

}
