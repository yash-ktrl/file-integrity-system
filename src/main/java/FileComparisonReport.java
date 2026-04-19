import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FileComparisonReport {
    
    private final Path filePath;
    private final FileIntegrityRecord storedRecord;
    private final Map<FileIntegrityManager.HashAlgorithm, String> currentHashes;
    private final long currentSize;
    private final long currentModifiedTime;
    private final boolean found;
    
    public FileComparisonReport(
        Path filePath,
        FileIntegrityRecord storedRecord,
        Map<FileIntegrityManager.HashAlgorithm, String> currentHashes,
        long currentSize,
        long currentModifiedTime
    ) {
        this.filePath = filePath;
        this.storedRecord = storedRecord;
        this.currentHashes = new HashMap<>(currentHashes);
        this.currentSize = currentSize;
        this.currentModifiedTime = currentModifiedTime;
        this.found = true;
    }
    
    private FileComparisonReport(Path filePath) {
        this.filePath = filePath;
        this.storedRecord = null;
        this.currentHashes = new HashMap<>();
        this.currentSize = 0;
        this.currentModifiedTime = 0;
        this.found = false;
    }
    
    public static FileComparisonReport notFound(Path filePath) {
        return new FileComparisonReport(filePath);
    }
    
    public boolean wasFound() {
        return found;
    }
    
    public Path getFilePath() {
        return filePath;
    }
    
    public boolean sizeChanged() {
        return storedRecord != null && currentSize != storedRecord.getFileSize();
    }
    
    public long getSizeChange() {
        if (storedRecord == null) return 0;
        return currentSize - storedRecord.getFileSize();
    }
    
    public long getStoredSize() {
        return storedRecord != null ? storedRecord.getFileSize() : 0;
    }
    
    public long getCurrentSize() {
        return currentSize;
    }
    
    public boolean modificationTimeChanged() {
        return storedRecord != null && currentModifiedTime != storedRecord.getLastModifiedTime();
    }
    
    public long getStoredModifiedTime() {
        return storedRecord != null ? storedRecord.getLastModifiedTime() : 0;
    }
    
    public long getCurrentModifiedTime() {
        return currentModifiedTime;
    }
    
    public boolean hashChanged(FileIntegrityManager.HashAlgorithm algorithm) {
        if (storedRecord == null) return false;
        String storedHash = storedRecord.getHash(algorithm);
        String currentHash = currentHashes.get(algorithm);
        return storedHash != null && currentHash != null && !storedHash.equals(currentHash);
    }
    
    public String getStoredHash(FileIntegrityManager.HashAlgorithm algorithm) {
        return storedRecord != null ? storedRecord.getHash(algorithm) : null;
    }
    
    public String getCurrentHash(FileIntegrityManager.HashAlgorithm algorithm) {
        return currentHashes.get(algorithm);
    }
    
    public long getTimeSinceStored() {
        if (storedRecord == null) return 0;
        return System.currentTimeMillis() / 1000 - storedRecord.getRecordedTime();
    }
    
    public void printDetailed() {
        if (!found) {
            System.out.println("No stored record found for: " + filePath);
            return;
        }
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("FILE COMPARISON REPORT");
        System.out.println("=".repeat(70));
        System.out.println("File: " + filePath);
        System.out.println();
        
        System.out.println("SIZE:");
        System.out.println(String.format("  Stored:  %d bytes", getStoredSize()));
        System.out.println(String.format("  Current: %d bytes", getCurrentSize()));
        if (sizeChanged()) {
            System.out.println(String.format("  Change:  %+d bytes (%.1f%%)", 
                getSizeChange(), 
                (double) getSizeChange() / getStoredSize() * 100));
        }
        System.out.println();
        
        System.out.println("MODIFICATION TIME:");
        System.out.println(String.format("  Stored:  %d", getStoredModifiedTime()));
        System.out.println(String.format("  Current: %d", getCurrentModifiedTime()));
        System.out.println(String.format("  Changed: %s", modificationTimeChanged() ? "YES" : "NO"));
        System.out.println();
        
        System.out.println("HASHES:");
        for (var entry : storedRecord.getHashes().entrySet()) {
            FileIntegrityManager.HashAlgorithm algo = entry.getKey();
            String stored = entry.getValue();
            String current = currentHashes.get(algo);
            boolean changed = hashChanged(algo);
            
            System.out.println(String.format("  %s:", algo));
            System.out.println(String.format("    Stored:  %s", stored));
            System.out.println(String.format("    Current: %s", current));
            System.out.println(String.format("    Changed: %s", changed ? "YES ⚠" : "NO ✓"));
        }
        
        System.out.println("\n" + "=".repeat(70) + "\n");
    }
}
