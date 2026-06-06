import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        FileSystemSimulator fs = new FileSystemSimulator();
        Scanner scanner = new Scanner(System.in);

        System.out.println("Simulador de Sistema de Arquivos com Journaling");
        System.out.println("Digite 'help' para ver os comandos. Digite 'exit' para sair.\n");

        while (true) {
            System.out.print("fs> ");
            String line = scanner.nextLine().trim();
            if (line.isBlank()) continue;
            if (line.equalsIgnoreCase("exit")) break;

            try {
                handleCommand(fs, line);
            } catch (Exception e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }

        System.out.println("Encerrando simulador.");
    }

    private static void handleCommand(FileSystemSimulator fs, String line) throws IOException {
        String[] parts = line.split("\\s+", 3);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "help" -> printHelp();
            case "mkdir" -> fs.createDirectory(require(parts, 1, "Uso: mkdir /diretorio"));
            case "rmdir" -> fs.deleteDirectory(require(parts, 1, "Uso: rmdir /diretorio"));
            case "mvdir" -> {
                String[] args = requireTwoArgs(line, "Uso: mvdir /diretorio novoNome");
                fs.renameDirectory(args[0], args[1]);
            }
            case "touch" -> fs.createFile(require(parts, 1, "Uso: touch /arquivo.txt"), "");
            case "write" -> {
                String[] args = requirePathAndText(line, "Uso: write /arquivo.txt conteudo do arquivo");
                fs.createFile(args[0], args[1]);
            }
            case "cp" -> {
                String[] args = requireTwoArgs(line, "Uso: cp /origem.txt /destino.txt");
                fs.copyFile(args[0], args[1]);
            }
            case "rm" -> fs.deleteFile(require(parts, 1, "Uso: rm /arquivo.txt"));
            case "mv" -> {
                String[] args = requireTwoArgs(line, "Uso: mv /arquivo.txt novoNome.txt");
                fs.renameFile(args[0], args[1]);
            }
            case "ls" -> System.out.print(fs.listDirectory(parts.length > 1 ? parts[1] : "/"));
            case "tree" -> System.out.print(fs.tree());
            case "journal" -> printJournal();
            default -> System.out.println("Comando desconhecido. Digite 'help'.");
        }
    }

    private static String require(String[] parts, int index, String message) {
        if (parts.length <= index) throw new IllegalArgumentException(message);
        return parts[index];
    }

    private static String[] requireTwoArgs(String line, String message) {
        String[] parts = line.split("\\s+");
        if (parts.length != 3) throw new IllegalArgumentException(message);
        return new String[]{parts[1], parts[2]};
    }

    private static String[] requirePathAndText(String line, String message) {
        String[] parts = line.split("\\s+", 3);
        if (parts.length < 3) throw new IllegalArgumentException(message);
        return new String[]{parts[1], parts[2]};
    }

    private static void printJournal() throws IOException {
        Path path = Path.of("journal.log");
        if (!Files.exists(path)) {
            System.out.println("journal.log ainda nao existe.");
            return;
        }
        System.out.println(Files.readString(path));
    }

    private static void printHelp() {
        System.out.println("Comandos disponiveis:");
        System.out.println("  mkdir /docs                    -> cria diretorio");
        System.out.println("  rmdir /docs                    -> apaga diretorio");
        System.out.println("  mvdir /docs documentos         -> renomeia diretorio");
        System.out.println("  touch /docs/a.txt              -> cria arquivo vazio");
        System.out.println("  write /docs/a.txt Ola mundo    -> cria arquivo com conteudo");
        System.out.println("  cp /docs/a.txt /docs/b.txt     -> copia arquivo");
        System.out.println("  rm /docs/a.txt                 -> apaga arquivo");
        System.out.println("  mv /docs/a.txt novo.txt        -> renomeia arquivo");
        System.out.println("  ls /docs                       -> lista diretorio");
        System.out.println("  tree                           -> mostra arvore completa");
        System.out.println("  journal                        -> mostra log de journaling");
        System.out.println("  exit                           -> sair");
    }
}
