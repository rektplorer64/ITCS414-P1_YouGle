import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;


public class BasicIndex implements BaseIndex {

    /**
     * This number indicates how many header for writing a PostingList into file.
     * Posting Header includes termId and docFreq.
     */
    private static final int POSTING_LIST_OFFSET = 2;

    /**
     * A common ByteBuffer for reading integer. Specifically used for reading the PostingList header
     */
    private static ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);

    /**
     * Read a PostingList from the given FileChannel
     * @param fc FileChannel of a binary index file
     * @return a new PostingList
     */
    @Override
    public PostingList readPosting(FileChannel fc) {
        /*
         * TODO: Your code here
         *       Read and return the postings list from the given file.
         */

        // Clean up the content inside intBuffer
        intBuffer.clear();

        // Storage for read integers to be process
        ArrayList<Integer> rawReadValues = new ArrayList<>();

        // Read only the header (termId and docFreq)
        for (int i = 0; i < POSTING_LIST_OFFSET; i++) {
            try {
                // Read from file by using ByteBuffer
                fc.read(intBuffer);

                // Flip it; Set the Position pointer to ZERO & the Limit pointer to old Position Pointer.
                intBuffer.flip();

                // Add the value inside to the Array
                rawReadValues.add(intBuffer.getInt());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (BufferUnderflowException e) {
                e.printStackTrace();
                break;
            }
            // Clear every time, because we are in the loop.
            intBuffer.clear();
        }
        // IntBuffer is field static variable, so it will be reused again. Flip it.
        intBuffer.flip();

        /*
         * Reading Document Id Array
         */

        // Find appropriate buffer size
        // GOAL: Adjust the algorithm so that the program issues IO READ less frequent --> More Speed!
        int docFrequency = rawReadValues.get(1);

        // Actual size of the buffer in Byte unit (Calculated by using max Prime Factor)
        int bufferSizeByte = calculateBestBufferSize(docFrequency);
        // Allocate the buffer
        ByteBuffer docIdBuffer = ByteBuffer.allocate(bufferSizeByte * Integer.BYTES);

        /* Start File WRITING iteration */

        // Get the actual limit getting Document Frequency
        // Byte Capacity divide by BufferSize
        // to get how many time do we have to iterate.
        final int actualLimit = docFrequency / bufferSizeByte;
        for (int i = 0; i < actualLimit; i++) {
            try {

                // Read the file by buffer
                fc.read(docIdBuffer);

                // Flip it to reset the Position to 0
                docIdBuffer.flip();

                // Gradually takes N Integers with a loop; N = bufferSizeByte ÷ 4

                // ** Calculating Math inside a loop is most likely to create unnecessary overheads
                // int loopLimit = (docIdBuffer.capacity() - docIdBuffer.position()) / Integer.BYTES;

                // Buffer sometimes can takes multiple int (size > 4 Bytes), so we loop when getting int out of it.
                for (int j = 0; j < bufferSizeByte; j++) {
                    rawReadValues.add(docIdBuffer.getInt());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (BufferUnderflowException e) {
                e.printStackTrace();
                break;
            }
            // Clear the contents
            docIdBuffer.clear();
        }

        ArrayList<Integer> docIds = new ArrayList<>();

        // Add only document Ids which start from index = 2 of rawReadValues: ArrayList<Int>()
        int postingSize = rawReadValues.get(1);
        final int actualLimit2 = postingSize + POSTING_LIST_OFFSET;
        for (int i = POSTING_LIST_OFFSET; i < actualLimit2; i++) {
            docIds.add(rawReadValues.get(i));
        }

        docIdBuffer = null;
        return new PostingList(rawReadValues.get(0), docIds);
    }

    /**
     * Find the best Buffer Size for given Data count by using Max Prime Factor of the parameter
     * @param dataCount size in term of number of elements; Not Byte
     * @return the Best Size for the Data size
     */
    private int calculateBestBufferSize(int dataCount){
        int bufferSizeByte = 1;
        if (dataCount != 1) {   // If its not 1
            for (int i = 2; i < dataCount; i++) {
                while (dataCount % i == 0) {
                    bufferSizeByte = Math.max(bufferSizeByte, i);       // Find the max prime factor
                    dataCount = dataCount / i;
                }
            }

            if (bufferSizeByte == 1){
                return dataCount;
            }
        }
        return bufferSizeByte;
    }

    /**
     * Write the given PostingList into the given FileChannel
     * @param fc FileChannel of the file to be written
     * @param p PostingList to be written
     */
    @Override
    public void writePosting(FileChannel fc, PostingList p) {
        /*
         * TODO: Your code here
         *       Write the given postings list to the given file.
         */

        // FORMAT FOR a POSTING LIST: termId, docFreq, [docId...]

        // Add all fields of PostingList into the Array in the correct order
        int[] dataToBeWritten = new int[p.getList().size() + POSTING_LIST_OFFSET];
        dataToBeWritten[0] = p.getTermId();
        dataToBeWritten[1] = p.getList().size();

        int dataCounter = POSTING_LIST_OFFSET;
        // Collect the Document Ids into the array
        for (int docId : p.getList()) {
            dataToBeWritten[dataCounter++] = docId;
        }
        p = null;

        // Finds the best buffer size for writing to reduce the amount of Disk IO -> More Speed
        int suitableBufferSize = calculateBestBufferSize(dataCounter);

        // Initialize the Buffer
        ByteBuffer intBuffer = ByteBuffer.allocate(suitableBufferSize * Integer.BYTES);

        int elemCounter = 0;
        final int actualLimit = dataToBeWritten.length / suitableBufferSize;
        for (int i = 0; i < actualLimit; i++){
            for (int j = 0; j < suitableBufferSize; j++) {
                // System.out.println("Putted ["+ j+ "/" +(suitableBufferSize -1 )+ "] = " + dataToBeWritten[elemCounter]+ "\telem@" + elemCounter) ;
                intBuffer.putInt(dataToBeWritten[elemCounter++]);
            }

            // Sets the position to 0 before writing
            intBuffer.flip();

            try {
                // Write to the buffer
                fc.write(intBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            } finally{
                intBuffer.clear();
            }
        }
        intBuffer = null;
    }
}

