import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
        postingDict.get(posting.getTermId()).setFirst(fc.position());
        index.writePosting(fc, posting);
    }


    /**
     * Pop next element if there is one, otherwise return null
     *
     * @param iter an iterator that contains integers
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


                while ((line = reader.readLine()) != null) {                            // Read each line of the Text File
                    String[] tokens = line.trim().split("\\s+");                  // Splits the String By white spaces (So we can get the Array Of Word Strings aka Tokens) **
                    for (String token : tokens) {
                        /*
                         * TODO: Your code here
                         *       For each term, build up a list of
                         *       documents in which the term occurs
                         */

                        // TODO: Related Data Containers: termDict, postingDict, postingList
                        int currentWordId;
                        String manipulatedToken = token.toLowerCase().trim();
                        if (!termDict.containsKey(manipulatedToken)) {              // If the term dict does not contain the token
                            termDict.put(manipulatedToken, ++wordIdCounter);        // Put it into the termDict TreeMap
                            currentWordId = wordIdCounter;
                        } else {
                            currentWordId = termDict.get(manipulatedToken);
                        }

                        if (!postingDict.containsKey(currentWordId)) {                                        // If PostingDict does not contain that termId
                            postingDict.put(currentWordId, null);                       // Put it in, Mark byte position as -1L
                        }

                        if (!blockPostingLists.containsKey(currentWordId)) {
                            blockPostingLists.put(currentWordId, new TreeSet<>());
                        }

                        blockPostingLists.get(currentWordId).add(docId); // System.out.println("Added termId: " + termDict.get(token) + " == " + token + " (doc=" + docId + ")");
                    }
                }
                reader.close();
            }
            // System.out.println("block " + block.getName() + ", postingList=" + blockPostingLists);


            /* Sort and output */
            if (!blockFile.createNewFile()) {
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
                if (postingDict.containsKey(newPosting.getTermId())) {
                    Pair<Long, Integer> pair = postingDict.get(newPosting.getTermId());
                    if (pair == null) {
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

            // CountDownLatch countDownLatch = new CountDownLatch(2);
            // ExecutorService es = Executors.newFixedThreadPool(2);
            // List<Callable<List<PostingList>>> postingThread = new ArrayList<>(2);
            // postingThread.add(new Callable<List<PostingList>>() {
            //     @Override
            //     public List<PostingList> call() throws Exception {
            //         List<PostingList> result = FileUtil.readPostingList(bf1.getChannel(), index);
            //         countDownLatch.countDown();
            //         return result;
            //     }
            // });
            // postingThread.add(new Callable<List<PostingList>>() {
            //     @Override
            //     public List<PostingList> call() throws Exception {
            //         List<PostingList> result = FileUtil.readPostingList(bf2.getChannel(), index);
            //         countDownLatch.countDown();
            //         return result;
            //     }
            // });
            //
            // List<Future<List<PostingList>>> futures = null;
            // try {
            //     futures = es.invokeAll(postingThread);
            //     countDownLatch.await();
            // } catch (InterruptedException e) {
            //     e.printStackTrace();
            // }

            // List<PostingList> finalList = null;
            // try {
            //     finalList = IndexHelpers.mergePostingList(
            //             futures.get(0).get()
            //                     , futures.get(1).get());
            // } catch (InterruptedException e) {
            //     e.printStackTrace();
            // } catch (ExecutionException e) {
            //     e.printStackTrace();
            // }

            // FIXME: OPTIMIZATION REQUIRED
            // List<PostingList> finalList = new AutoSortList<>(IndexHelpers.mergePostingList(
            //         FileUtil.readPostingList(bf1.getChannel(), index),
            //         FileUtil.readPostingList(bf2.getChannel(), index)), CollectionUtil.POSTING_LIST_COMPARATOR);

            long startTime1 = System.currentTimeMillis();

            List<PostingList> finalList = IndexHelpers.mergePostingList(
                    FileUtil.readPostingList(bf1.getChannel(), index),
                    FileUtil.readPostingList(bf2.getChannel(), index));

            finalList.sort(CollectionUtil.POSTING_LIST_COMPARATOR);

            // System.out.println("final: " + finalList);
            long endTime1 = System.currentTimeMillis();
            System.out.println(combfile.getName() + " Time Used: " + ((endTime1 - startTime1) / 1000.0) + " secs");

            // TODO: Loops thru SortedList
            long startTime2 = System.currentTimeMillis();
            for (PostingList posting : finalList) {
                writePosting(mf.getChannel(), posting);
            }
            long endTime2 = System.currentTimeMillis();
            System.out.println(combfile.getName() + " \tWrite Time Used: " + ((endTime2 - startTime2) / 1000.0) + " secs");

            finalList = null;
            System.gc();
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
