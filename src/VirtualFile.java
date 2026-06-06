import java.io.Serializable;

public class VirtualFile implements Serializable {
    private String name;
    private String content;

    public VirtualFile(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public VirtualFile copy(String newName) {
        return new VirtualFile(newName, content);
    }

    @Override
    public String toString() {
        return name + " (arquivo, " + content.length() + " caracteres)";
    }
}
