import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class BasicIndex implements BaseIndex {

	// private static ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);

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

		int limit = -1;
		int totalByteRead = 0;


		// FIXME: Adjust the algorithm so that the program issues IO READ less frequent --> More Speed!
		while (totalByteRead != limit){
			try {
				fc.read(intBuffer);

				intBuffer.flip();

				rawReadValues.add(intBuffer.getInt());
				totalByteRead += Integer.BYTES;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (BufferUnderflowException e){
				e.printStackTrace();
				break;
			}

			if (rawReadValues.size() == POSTING_LIST_OFFSET){
				limit = (POSTING_LIST_OFFSET + rawReadValues.get(POSTING_LIST_OFFSET - 1)) * Integer.BYTES;
			}
			intBuffer.clear();
		}

		// System.out.println("rawReadValues = " + rawReadValues);

		ArrayList<Integer> docIds = new ArrayList<>();

		int postingSize = rawReadValues.get(1);
		// System.out.println("postingSize = " + postingSize);
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

		// FIXME: CONVERTING INT TO BYTE!!!
		byte[] bytePrimitiveArr = new byte[dataToBeWritten.length * 4];
		int byteCounter = 0;

		for (int i = 0; i < dataToBeWritten.length; i++) {
			byte[] convertedInt = FileUtil.intToByteArray(dataToBeWritten[i]);
			for (int j = 0; j < convertedInt.length; j++){
				bytePrimitiveArr[byteCounter++] = convertedInt[j];
			}
		}

		ByteBuffer intBuffer = ByteBuffer.allocate((dataCounter + 1) * 4);
		intBuffer.put(bytePrimitiveArr);
		intBuffer.flip();
		try {
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

