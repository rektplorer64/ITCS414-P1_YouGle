import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.TreeSet;

public class FileUtil {
    public static void purgeDirectory(File dir) {
        for (File file: dir.listFiles()) {
            if (file.isDirectory())
                purgeDirectory(file);
            file.delete();
        }
    }

    public static ArrayList<PostingList> readPostingList(FileChannel fileChannel, BaseIndex index) throws IOException {
        ArrayList<PostingList> postingLists = new ArrayList<>();
        try {
            while (fileChannel.position() <= fileChannel.size() - 1) {
                postingLists.add(index.readPosting(fileChannel));
            }
            return postingLists;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }
}
