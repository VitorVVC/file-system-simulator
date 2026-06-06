import java.io.*;
import java.util.Arrays;
import java.util.stream.Collectors;

public class FileSystemSimulator {
    private static final String DATA_FILE = "dados.dat";
    private static final String JOURNAL_FILE = "journal.log";

    private VirtualDirectory root;
    private final Journal journal = new Journal(JOURNAL_FILE);

    public FileSystemSimulator() {
        load();
    }

    public void createFile(String path, String content) {
        executeWithJournal("CREATE_FILE", path, () -> {
            PathParts parts = splitPath(path);
            VirtualDirectory parent = findDirectoryByPath(parts.parentPath);
            ensureDirectoryExists(parent, parts.parentPath);
            if (parent.findFile(parts.name) != null || parent.findDirectory(parts.name) != null) {
                throw new IllegalArgumentException("Ja existe arquivo ou diretorio com esse nome: " + parts.name);
            }
            parent.getFiles().add(new VirtualFile(parts.name, content));
        });
    }

    public void copyFile(String sourcePath, String destinationPath) {
        executeWithJournal("COPY_FILE", sourcePath + " -> " + destinationPath, () -> {
            PathParts source = splitPath(sourcePath);
            VirtualDirectory sourceParent = findDirectoryByPath(source.parentPath);
            ensureDirectoryExists(sourceParent, source.parentPath);
            VirtualFile original = sourceParent.findFile(source.name);
            if (original == null) throw new IllegalArgumentException("Arquivo origem nao encontrado: " + sourcePath);

            PathParts destination = splitPath(destinationPath);
            VirtualDirectory destinationParent = findDirectoryByPath(destination.parentPath);
            ensureDirectoryExists(destinationParent, destination.parentPath);
            if (destinationParent.findFile(destination.name) != null || destinationParent.findDirectory(destination.name) != null) {
                throw new IllegalArgumentException("Destino ja existe: " + destinationPath);
            }
            destinationParent.getFiles().add(original.copy(destination.name));
        });
    }

    public void deleteFile(String path) {
        executeWithJournal("DELETE_FILE", path, () -> {
            PathParts parts = splitPath(path);
            VirtualDirectory parent = findDirectoryByPath(parts.parentPath);
            ensureDirectoryExists(parent, parts.parentPath);
            VirtualFile file = parent.findFile(parts.name);
            if (file == null) throw new IllegalArgumentException("Arquivo nao encontrado: " + path);
            parent.getFiles().remove(file);
        });
    }

    public void renameFile(String oldPath, String newName) {
        executeWithJournal("RENAME_FILE", oldPath + " -> " + newName, () -> {
            PathParts parts = splitPath(oldPath);
            VirtualDirectory parent = findDirectoryByPath(parts.parentPath);
            ensureDirectoryExists(parent, parts.parentPath);
            VirtualFile file = parent.findFile(parts.name);
            if (file == null) throw new IllegalArgumentException("Arquivo nao encontrado: " + oldPath);
            if (parent.findFile(newName) != null || parent.findDirectory(newName) != null) {
                throw new IllegalArgumentException("Ja existe item com esse nome: " + newName);
            }
            file.setName(newName);
        });
    }

    public void createDirectory(String path) {
        executeWithJournal("CREATE_DIR", path, () -> {
            PathParts parts = splitPath(path);
            VirtualDirectory parent = findDirectoryByPath(parts.parentPath);
            ensureDirectoryExists(parent, parts.parentPath);
            if (parent.findDirectory(parts.name) != null || parent.findFile(parts.name) != null) {
                throw new IllegalArgumentException("Ja existe arquivo ou diretorio com esse nome: " + parts.name);
            }
            parent.getDirectories().add(new VirtualDirectory(parts.name));
        });
    }

    public void deleteDirectory(String path) {
        executeWithJournal("DELETE_DIR", path, () -> {
            if (path.equals("/") || path.isBlank()) throw new IllegalArgumentException("Nao e permitido apagar o diretorio raiz.");
            PathParts parts = splitPath(path);
            VirtualDirectory parent = findDirectoryByPath(parts.parentPath);
            ensureDirectoryExists(parent, parts.parentPath);
            VirtualDirectory directory = parent.findDirectory(parts.name);
            if (directory == null) throw new IllegalArgumentException("Diretorio nao encontrado: " + path);
            parent.getDirectories().remove(directory);
        });
    }

    public void renameDirectory(String oldPath, String newName) {
        executeWithJournal("RENAME_DIR", oldPath + " -> " + newName, () -> {
            if (oldPath.equals("/") || oldPath.isBlank()) throw new IllegalArgumentException("Nao e permitido renomear o diretorio raiz.");
            PathParts parts = splitPath(oldPath);
            VirtualDirectory parent = findDirectoryByPath(parts.parentPath);
            ensureDirectoryExists(parent, parts.parentPath);
            VirtualDirectory directory = parent.findDirectory(parts.name);
            if (directory == null) throw new IllegalArgumentException("Diretorio nao encontrado: " + oldPath);
            if (parent.findDirectory(newName) != null || parent.findFile(newName) != null) {
                throw new IllegalArgumentException("Ja existe item com esse nome: " + newName);
            }
            directory.setName(newName);
        });
    }

    public String listDirectory(String path) {
        VirtualDirectory directory = findDirectoryByPath(path);
        ensureDirectoryExists(directory, path);
        StringBuilder sb = new StringBuilder();
        sb.append("Conteudo de ").append(normalizePath(path)).append(":\n");
        for (VirtualDirectory child : directory.getDirectories()) sb.append("[DIR]  ").append(child.getName()).append("\n");
        for (VirtualFile file : directory.getFiles()) sb.append("[FILE] ").append(file).append("\n");
        if (directory.getDirectories().isEmpty() && directory.getFiles().isEmpty()) sb.append("(diretorio vazio)\n");
        return sb.toString();
    }

    public String tree() {
        StringBuilder sb = new StringBuilder();
        buildTree(root, sb, "", true);
        return sb.toString();
    }

    private void buildTree(VirtualDirectory directory, StringBuilder sb, String prefix, boolean isRoot) {
        sb.append(prefix).append(isRoot ? "/" : directory.getName() + "/").append("\n");
        String childPrefix = isRoot ? "" : prefix + "  ";
        for (VirtualDirectory child : directory.getDirectories()) buildTree(child, sb, childPrefix, false);
        for (VirtualFile file : directory.getFiles()) sb.append(childPrefix).append("  ").append(file.getName()).append("\n");
    }

    private void executeWithJournal(String operation, String details, Runnable action) {
        journal.begin(operation, details);
        try {
            action.run();
            save();
            journal.commit(operation, details);
            System.out.println("OK: " + operation + " - " + details);
        } catch (RuntimeException e) {
            journal.error(operation, details + " | " + e.getMessage());
            throw e;
        }
    }

    private void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(root);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar dados.dat: " + e.getMessage());
        }
    }

    private void load() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            root = new VirtualDirectory("/");
            save();
            return;
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            root = (VirtualDirectory) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Erro ao carregar dados.dat: " + e.getMessage());
        }
    }

    private VirtualDirectory findDirectoryByPath(String path) {
        String normalized = normalizePath(path);
        if (normalized.equals("/")) return root;
        String[] parts = normalized.substring(1).split("/");
        VirtualDirectory current = root;
        for (String part : parts) {
            current = current.findDirectory(part);
            if (current == null) return null;
        }
        return current;
    }

    private void ensureDirectoryExists(VirtualDirectory directory, String path) {
        if (directory == null) throw new IllegalArgumentException("Diretorio nao encontrado: " + path);
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) return "/";
        path = path.trim();
        if (!path.startsWith("/")) path = "/" + path;
        while (path.contains("//")) path = path.replace("//", "/");
        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path;
    }

    private PathParts splitPath(String path) {
        String normalized = normalizePath(path);
        if (normalized.equals("/")) throw new IllegalArgumentException("Caminho invalido para essa operacao.");
        int lastSlash = normalized.lastIndexOf('/');
        String parentPath = lastSlash == 0 ? "/" : normalized.substring(0, lastSlash);
        String name = normalized.substring(lastSlash + 1);
        if (name.isBlank()) throw new IllegalArgumentException("Nome invalido.");
        return new PathParts(parentPath, name);
    }

    private static class PathParts {
        String parentPath;
        String name;
        PathParts(String parentPath, String name) {
            this.parentPath = parentPath;
            this.name = name;
        }
    }
}
