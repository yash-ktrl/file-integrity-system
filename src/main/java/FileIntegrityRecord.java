import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FileIntegrityRecord {
    
    private final Path filePath;
    private final String primaryHash;
    private final Map<FileIntegrityManager.HashAlgorithm, String> hashes;
    private final long fileSize;
    private final long lastModifiedTime;
    private final long recordedTime;
    
    public FileIntegrityRecord(
        Path filePath,
        String primaryHash,
        Map<FileIntegrityManager.HashAlgorithm, String> hashes,
        long fileSize,
        long lastModifiedTime,
        long recordedTime
    ) {
        this.filePath = Objects.requireNonNull(filePath);
        this.primaryHash = Objects.requireNonNull(primaryHash);
        this.hashes = new HashMap<>(Objects.requireNonNull(hashes));
        this.fileSize = fileSize;
        this.lastModifiedTime = lastModifiedTime;
        this.recordedTime = recordedTime;
    }
    
    public Path getFilePath() {
        return filePath;
    }
    
    public String getPrimaryHash() {
        return primaryHash;
    }
    
    public Map<FileIntegrityManager.HashAlgorithm, String> getHashes() {
        return new HashMap<>(hashes);
    }
    
    public String getHash(FileIntegrityManager.HashAlgorithm algorithm) {
        return hashes.get(algorithm);
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public long getLastModifiedTime() {
        return lastModifiedTime;
    }
    
    public long getRecordedTime() {
        return recordedTime;
    }
    
    public String getFilePathString() {
        return filePath.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FileIntegrityRecord{\n");
        sb.append("  Path: ").append(filePath).append("\n");
        sb.append("  Size: ").append(fileSize).append(" bytes\n");
        sb.append("  Hashes:\n");
        for (var entry : hashes.entrySet()) {
            sb.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("  Last Modified: ").append(lastModifiedTime).append("\n");
        sb.append("  Recorded: ").append(recordedTime).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
