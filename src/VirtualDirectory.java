import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VirtualDirectory implements Serializable {
    private String name;
    private final List<VirtualFile> files = new ArrayList<>();
    private final List<VirtualDirectory> directories = new ArrayList<>();

    public VirtualDirectory(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<VirtualFile> getFiles() { return files; }
    public List<VirtualDirectory> getDirectories() { return directories; }

    public VirtualFile findFile(String fileName) {
        for (VirtualFile file : files) {
            if (file.getName().equals(fileName)) return file;
        }
        return null;
    }

    public VirtualDirectory findDirectory(String directoryName) {
        for (VirtualDirectory directory : directories) {
            if (directory.getName().equals(directoryName)) return directory;
        }
        return null;
    }
}
