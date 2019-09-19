import java.io.*;
import java.util.*;

public class RankedQuery{

    private RandomAccessFile indexFile;

    // termId --> {docId --> score}
    private HashMap<Integer, HashMap<Integer, Double>> scoreMatrix = new HashMap<>();

    // termId --> {docId --> frequency}
    private HashMap<Integer, HashMap<Integer, Integer>> termFreqDict = new HashMap<>();

    // term --> documentFrequency
    private TreeMap<Integer, Integer> frequencyDict;

    @org.jetbrains.annotations.NotNull
    private final Map<Integer, String> docDict;
    private final Map<String, Integer> termDict;

    private int totalDocument;
    private String dataDirname;

    public RankedQuery(RandomAccessFile indexFile, TreeMap<Integer, Integer> frequencyDict, Map<Integer, String> docDict, Map<String, Integer> termDict, String dataDirname){

        this.indexFile = indexFile;
        this.frequencyDict = frequencyDict;

        totalDocument = docDict.size();
        this.docDict = docDict;
        this.termDict = termDict;
        this.dataDirname = dataDirname;
    }

    private void constructTermFreqDict() throws IOException{
        File rootDir = new File(dataDirname);
        File[] dirList = rootDir.listFiles();       // Lists all folders inside the Root

        int wordIdCounter = 0;

        /* For each block */
        for (File block : dirList) {        // For each Folder eg. ./datasets/small/ 0, 1, 2

            // Get the folder eg. ./datasets/small/0, ./datasets/small/1, ./datasets/small/2
            File blockDir = new File(dataDirname, block.getName());

            // Lists all text file inside
            File[] fileList = blockDir.listFiles();

            /* For each file */
            for (File file : fileList) {

                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;

                ++wordIdCounter;

                while ((line = reader.readLine()) !=
                       null){                            // Read each line of the Text File
                    String[] tokens = line.trim().split(
                            "\\s+");                  // Splits the String By white spaces (So we can get the Array Of Word Strings aka Tokens) **
                    for (String token : tokens) {
                        String manipulatedToken = token.toLowerCase().trim();
                        if (termDict.containsKey(
                                manipulatedToken)){              // If the term dict does not contain the token
                            int termId = termDict.get(manipulatedToken);
                            if (!termFreqDict.containsKey(termId)){
                                termFreqDict.put(termId, new HashMap<>());
                            }
                            HashMap<Integer, Integer> hash = termFreqDict.get(termId);
                            if (hash.containsKey(wordIdCounter)){
                                hash.put(termId, hash.get(termId) + 1);
                            }else{
                                hash.put(termId, 1);
                            }
                        }
                    }
                }
                reader.close();
            }
        }
    }

    private double calculateTermFrequency(int frequency){
        if (frequency == 0){
            return 0d;
        }
        return 1d + Math.log10(frequency);
    }

    private double calculateInvertedDocFrequency(int termId){
        return Math.log10((double) totalDocument / frequencyDict.get(termId));
    }

    public void initiate(String query){
        try{
            constructTermFreqDict();
        }catch (IOException e){
            e.printStackTrace();
        }

        // termId -> Frequency
        HashMap<Integer, Integer> queryTermFreq = new HashMap<>();
        TreeSet<String> uniqueWord = new TreeSet<>(Query.QueryUtil.processQuery(query));
        for (String word : uniqueWord) {
            Integer termDictId = termDict.get(word);
            if (termDictId != null){
                if (!queryTermFreq.containsKey(termDictId)){
                    queryTermFreq.put(termDictId, 0);
                }
                queryTermFreq.put(termDictId, queryTermFreq.get(termDictId) + 1);
            }
        }

        termFreqDict.put(-1, queryTermFreq);

        for (Map.Entry<Integer, HashMap<Integer, Integer>> tfEntry : termFreqDict.entrySet()) {
            for (Map.Entry<Integer, Integer> docIdTermFreq : tfEntry.getValue().entrySet()) {
                if (!scoreMatrix.containsKey(tfEntry.getKey())){
                    scoreMatrix.put(tfEntry.getKey(), new HashMap<>());
                }

                double score = calculateTermFrequency(docIdTermFreq.getValue()) * calculateInvertedDocFrequency(tfEntry.getKey());
                scoreMatrix.get(tfEntry.getKey()).put(docIdTermFreq.getKey(), score);
            }
        }

        for (Map.Entry<Integer, Integer> queryTermIdFreqPair : queryTermFreq.entrySet()) {
            if (!scoreMatrix.containsKey(queryTermIdFreqPair.getKey())){
                continue;
            }

            double score = calculateTermFrequency(queryTermIdFreqPair.getValue()) * calculateInvertedDocFrequency(queryTermIdFreqPair.getKey());
            scoreMatrix.get(queryTermIdFreqPair.getKey()).put(queryTermIdFreqPair.getKey(), score);
        }

        // TODO: Extract Vector of each documents

    }
}
