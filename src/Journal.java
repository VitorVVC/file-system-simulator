import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Journal {
    private final String journalPath;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Journal(String journalPath) {
        this.journalPath = journalPath;
    }

    public void begin(String operation, String details) {
        write("BEGIN", operation, details);
    }

    public void commit(String operation, String details) {
        write("COMMIT", operation, details);
    }

    public void error(String operation, String details) {
        write("ERROR", operation, details);
    }

    private void write(String status, String operation, String details) {
        try (FileWriter writer = new FileWriter(journalPath, true)) {
            writer.write("[" + LocalDateTime.now().format(formatter) + "] " + status + " " + operation + " - " + details + System.lineSeparator());
        } catch (IOException e) {
            System.out.println("Erro ao escrever no journal: " + e.getMessage());
        }
    }
}
