import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;


public class BasicIndex implements BaseIndex {

	private static ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES) ;
	private static final int POSTING_LIST_OFFSET = 2;

	@Override
	public PostingList readPosting(FileChannel fc) {
		/*
		 * TODO: Your code here
		 *       Read and return the postings list from the given file.
		 */
		intBuffer.clear();
		ArrayList<Integer> rawReadValues = new ArrayList<>();

		int limit = -1;
		int totalByteRead = 0;

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

		ArrayList<Integer> dataToBeWritten = new ArrayList<>();
		dataToBeWritten.add(p.getTermId());
		dataToBeWritten.add(p.getList().size());
		dataToBeWritten.addAll(p.getList());

		System.out.println("Writing " + p.getTermId() + " w/ " + p.getList());
		try {
			for (int i = 0; i < dataToBeWritten.size(); i++){
				intBuffer.clear();
				intBuffer.putInt(dataToBeWritten.get(i));
				intBuffer.flip();

				fc.write(intBuffer);

				System.out.println(i + "th W BytePos = " + fc.position());
			}
			System.out.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

