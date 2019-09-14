import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;


public class BasicIndex implements BaseIndex {

    private static final int POSTING_LIST_OFFSET = 2;

    @Override
    public PostingList readPosting(FileChannel fc) {
        /*
         * TODO: Your code here
         *       Read and return the postings list from the given file.
         */
        ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);
        intBuffer.clear();
        ArrayList<Integer> rawReadValues = new ArrayList<>();

        // TODO: READ HEADER ONLY
        for (int i = 0; i < POSTING_LIST_OFFSET; i++) {
            try {
                fc.read(intBuffer);
                intBuffer.flip();
                rawReadValues.add(intBuffer.getInt());
            } catch (IOException e) {
                // e.printStackTrace();
            } catch (BufferUnderflowException e) {
                // e.printStackTrace();
                break;
            }
            intBuffer.clear();
        }


        // System.out.println("\nHeader="+ rawReadValues);
        // TODO: FIND APPROPRIATE BUFFER SIZE
        int bufferSizeByte = 1, tempDocFreq = rawReadValues.get(1);
        // System.out.println("BufferSize=" + bufferSizeByte + ", tempDocFreq=" + tempDocFreq);
        if (tempDocFreq != 1) {
            // System.out.print("Prime=");
            for (int i = 2; i < tempDocFreq; i++) {
                while (tempDocFreq % i == 0) {
                    // System.out.print(i + ", ");
                    bufferSizeByte = Math.max(bufferSizeByte, i);
                    tempDocFreq = tempDocFreq / i;
                }
            }

            // System.out.println();
        } else {
            bufferSizeByte = 1;
        }
        // System.out.print("maxPrime=" + bufferSizeByte);
        bufferSizeByte *= Integer.BYTES;

        // System.out.println("\tbufferSizeByte=" + bufferSizeByte);
        ByteBuffer docIdBuffer = ByteBuffer.allocate(bufferSizeByte);

        // FIXME: Adjust the algorithm so that the program issues IO READ less frequent --> More Speed!
        int actualLimit = (rawReadValues.get(1) * Integer.BYTES) / bufferSizeByte;
        for (int i = 0; i < actualLimit; i++) {
            try {
                fc.read(docIdBuffer);
                docIdBuffer.flip();

                // byte[] rawByteArray = docIdBuffer.array();
                // System.out.println("Read Val = " + Arrays.toString(readValues));

                // Gradually takes (@bufferSizeByte ÷ 4) Integers with a loop
                int loopLimit = (docIdBuffer.capacity() - docIdBuffer.position()) / 4;
                // System.out.println("loopLimit=" + loopLimit);
                for (int j = 0; j < loopLimit; j++) {
                    int readVal = docIdBuffer.getInt();
                    rawReadValues.add(readVal);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (BufferUnderflowException e) {
                e.printStackTrace();
                break;
            }
            docIdBuffer.clear();
        }

        // System.out.println("rawReadValues = " + rawReadValues);

        ArrayList<Integer> docIds = new ArrayList<>();

        int postingSize = rawReadValues.get(1);
        // System.out.println("postingSize=" + postingSize);
        for (int i = POSTING_LIST_OFFSET; i < postingSize + POSTING_LIST_OFFSET; i++) {
            docIds.add(rawReadValues.get(i));
        }

        // System.out.println("postingList = " + docIds);

        return new PostingList(rawReadValues.get(0), docIds);
    }

    @Override
    public void writePosting(FileChannel fc, PostingList p) {
        /*
         * TODO: Your code here
         *       Write the given postings list to the given file.
         *
         *
         */

        int[] dataToBeWritten = new int[p.getList().size() + POSTING_LIST_OFFSET];
        dataToBeWritten[0] = p.getTermId();
        dataToBeWritten[1] = p.getList().size();

        int dataCounter = POSTING_LIST_OFFSET;
        for (int docId : p.getList()) {
            dataToBeWritten[dataCounter++] = docId;
        }
        p = null;

        // Converts INT[] to BYTE[]
        byte[] bytePrimitiveArr = new byte[dataToBeWritten.length * 4];
        int byteCounter = 0;

        for (int i = 0; i < dataToBeWritten.length; i++) {
            byte[] convertedInt = Index.FileUtil.intToByteArray(dataToBeWritten[i]);
            for (int j = 0; j < convertedInt.length; j++) {
                bytePrimitiveArr[byteCounter++] = convertedInt[j];
            }
        }

        // Initialize the Buffer
        ByteBuffer intBuffer = ByteBuffer.allocate((dataCounter + 1) * 4);

        // Put the BYTE[] to the Buffer
        intBuffer.put(bytePrimitiveArr);

        // Sets the position to 0 before writing
        intBuffer.flip();
        try {
            // Write to the buffer
            fc.write(intBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // System.out.println("Writing " + p.getTermId() + " w/ " + p.getList());
        // try {
        // 	for (int i = 0; i < dataToBeWritten.size(); i++){
        // 		intBuffer.clear();
        // 		intBuffer.putInt(dataToBeWritten.get(i));
        // 		intBuffer.flip();
        //
        // 		fc.write(intBuffer);
        //
        // 		// System.out.println(i + "th W BytePos = " + fc.position());
        // 	}
        // 	// System.out.println();
        // } catch (IOException e) {
        // 	e.printStackTrace();
        // }
    }
}

