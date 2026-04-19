import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;

public class FileHashDatabase {
    
    private final Path databasePath;
    private final Gson gson;
    private final Map<String, FileIntegrityRecord> cache;
    private final Object lock = new Object();
    
    public FileHashDatabase(String databasePath) {
        this.databasePath = Paths.get(databasePath);
        this.cache = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        
        try {
            Files.createDirectories(this.databasePath.getParent());
            if (!Files.exists(this.databasePath)) {
                Files.createFile(this.databasePath);
                saveEmptyDatabase();
            } else {
                loadDatabase();
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }
    
    public synchronized void saveRecords(Collection<FileIntegrityRecord> records) throws IOException {
        synchronized (lock) {
            for (FileIntegrityRecord record : records) {
                cache.put(record.getFilePathString(), record);
            }
            
            DatabaseSnapshot snapshot = new DatabaseSnapshot(
                System.currentTimeMillis(),
                new ArrayList<>(cache.values())
            );
            
            String json = gson.toJson(snapshot);
            Files.write(databasePath, json.getBytes());
        }
    }
    
    public FileIntegrityRecord getRecord(Path filePath) {
        return cache.get(filePath.toString());
    }
    
    public Collection<FileIntegrityRecord> getAllRecords() {
        return new ArrayList<>(cache.values());
    }
    
    public synchronized void deleteRecord(Path filePath) throws IOException {
        synchronized (lock) {
            cache.remove(filePath.toString());
            saveDatabase();
        }
    }
    
    public synchronized void clearAll() throws IOException {
        synchronized (lock) {
            cache.clear();
            saveEmptyDatabase();
        }
    }
    
    public DatabaseStatistics getStatistics() {
        long totalSize = cache.values().stream()
            .mapToLong(FileIntegrityRecord::getFileSize)
            .sum();
        
        return new DatabaseStatistics(
            cache.size(),
            totalSize,
            System.currentTimeMillis()
        );
    }
    
    private void loadDatabase() throws IOException {
        try {
            String json = Files.readString(databasePath);
            if (json.trim().isEmpty()) {
                return;
            }
            
            DatabaseSnapshot snapshot = gson.fromJson(json, DatabaseSnapshot.class);
            if (snapshot != null && snapshot.records != null) {
                for (FileIntegrityRecord record : snapshot.records) {
                    cache.put(record.getFilePathString(), record);
                }
            }
        } catch (JsonSyntaxException e) {
            System.err.println("Failed to parse database JSON: " + e.getMessage());
        }
    }
    
    private void saveDatabase() throws IOException {
        DatabaseSnapshot snapshot = new DatabaseSnapshot(
            System.currentTimeMillis(),
            new ArrayList<>(cache.values())
        );
        String json = gson.toJson(snapshot);
        Files.write(databasePath, json.getBytes());
    }
    
    private void saveEmptyDatabase() throws IOException {
        DatabaseSnapshot snapshot = new DatabaseSnapshot(
            System.currentTimeMillis(),
            new ArrayList<>()
        );
        String json = gson.toJson(snapshot);
        Files.write(databasePath, json.getBytes());
    }
    
    public void exportToCSV(String csvPath) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("FilePath,Size,SHA256,SHA512,LastModified,RecordedTime\n");
        
        for (FileIntegrityRecord record : cache.values()) {
            csv.append(escapeCsvField(record.getFilePathString())).append(",");
            csv.append(record.getFileSize()).append(",");
            csv.append(escapeCsvField(record.getHash(FileIntegrityManager.HashAlgorithm.SHA256))).append(",");
            csv.append(escapeCsvField(record.getHash(FileIntegrityManager.HashAlgorithm.SHA512))).append(",");
            csv.append(record.getLastModifiedTime()).append(",");
            csv.append(record.getRecordedTime()).append("\n");
        }
        
        Files.write(Paths.get(csvPath), csv.toString().getBytes());
    }
    
    public void close() {
    }
    
    private String escapeCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
    
    private static class DatabaseSnapshot {
        public long timestamp;
        public List<FileIntegrityRecord> records;
        
        public DatabaseSnapshot(long timestamp, List<FileIntegrityRecord> records) {
            this.timestamp = timestamp;
            this.records = records;
        }
    }
}
