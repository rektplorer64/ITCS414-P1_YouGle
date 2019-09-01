import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

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
}
