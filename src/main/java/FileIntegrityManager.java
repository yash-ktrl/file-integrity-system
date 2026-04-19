import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class FileIntegrityManager {
    
    private final ExecutorService executorService;
    private final FileHashDatabase hashDatabase;
    private final HashAlgorithm primaryAlgorithm;
    private final Set<HashAlgorithm> verificationAlgorithms;
    private final int bufferSize;
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    
    public enum HashAlgorithm {
        MD5("MD5"),
        SHA1("SHA-1"),
        SHA256("SHA-256"),
        SHA512("SHA-512");
        
        private final String algorithm;
        
        HashAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }
        
        public String getAlgorithm() {
            return algorithm;
        }
    }
    
    public FileIntegrityManager(int threadCount, String databasePath) {
        this(threadCount, databasePath, HashAlgorithm.SHA256, DEFAULT_BUFFER_SIZE);
    }
    
    public FileIntegrityManager(int threadCount, String databasePath, 
                               HashAlgorithm primary, int bufferSize) {
        this.executorService = Executors.newFixedThreadPool(threadCount);
        this.hashDatabase = new FileHashDatabase(databasePath);
        this.primaryAlgorithm = primary;
        this.verificationAlgorithms = new HashSet<>();
        this.verificationAlgorithms.add(primary);
        this.bufferSize = bufferSize;
    }
    
    public void addVerificationAlgorithm(HashAlgorithm algorithm) {
        verificationAlgorithms.add(algorithm);
    }
    
    public String calculateHash(Path filePath, HashAlgorithm algorithm) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm.getAlgorithm());
        
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        return bytesToHex(digest.digest());
    }
    
    public Map<HashAlgorithm, String> calculateAllHashes(Path filePath) throws Exception {
        Map<HashAlgorithm, String> hashes = new HashMap<>();
        for (HashAlgorithm algorithm : verificationAlgorithms) {
            hashes.put(algorithm, calculateHash(filePath, algorithm));
        }
        return hashes;
    }
    
    public Map<Path, FileIntegrityRecord> scanDirectory(Path directory) throws Exception {
        Map<Path, FileIntegrityRecord> records = new ConcurrentHashMap<>();
        List<Future<FileIntegrityRecord>> futures = new ArrayList<>();
        
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                futures.add(executorService.submit(() -> {
                    try {
                        return createIntegrityRecord(file, attrs);
                    } catch (Exception e) {
                        System.err.println("Error processing file: " + file + " - " + e.getMessage());
                        return null;
                    }
                }));
                return FileVisitResult.CONTINUE;
            }
        });
        
        for (Future<FileIntegrityRecord> future : futures) {
            try {
                FileIntegrityRecord record = future.get();
                if (record != null) {
                    records.put(record.getFilePath(), record);
                }
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error retrieving result: " + e.getMessage());
            }
        }
        
        return records;
    }
    
    private FileIntegrityRecord createIntegrityRecord(Path filePath, BasicFileAttributes attrs) throws Exception {
        Map<HashAlgorithm, String> hashes = calculateAllHashes(filePath);
        long size = attrs.size();
        long modifiedTime = attrs.lastModifiedTime().toInstant().getEpochSecond();
        
        return new FileIntegrityRecord(
            filePath,
            hashes.get(primaryAlgorithm),
            hashes,
            size,
            modifiedTime,
            Instant.now().getEpochSecond()
        );
    }
    
    public void storeRecords(Collection<FileIntegrityRecord> records) throws Exception {
        hashDatabase.saveRecords(records);
    }
    
    public IntegrityVerificationResult verifyFile(Path filePath) throws Exception {
        FileIntegrityRecord storedRecord = hashDatabase.getRecord(filePath);
        
        if (storedRecord == null) {
            return IntegrityVerificationResult.notFound(filePath);
        }
        
        if (!Files.exists(filePath)) {
            return IntegrityVerificationResult.deleted(filePath);
        }
        
        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
        long currentSize = attrs.size();
        long currentModified = attrs.lastModifiedTime().toInstant().getEpochSecond();
        
        if (currentSize != storedRecord.getFileSize()) {
            return IntegrityVerificationResult.modified(filePath, "File size mismatch");
        }
        
        Map<HashAlgorithm, String> currentHashes = calculateAllHashes(filePath);
        
        String currentPrimaryHash = currentHashes.get(primaryAlgorithm);
        if (!currentPrimaryHash.equals(storedRecord.getPrimaryHash())) {
            return IntegrityVerificationResult.modified(filePath, "Content hash mismatch");
        }
        
        for (HashAlgorithm algorithm : storedRecord.getHashes().keySet()) {
            String storedHash = storedRecord.getHashes().get(algorithm);
            String currentHash = currentHashes.get(algorithm);
            if (!storedHash.equals(currentHash)) {
                return IntegrityVerificationResult.modified(
                    filePath, 
                    "Hash mismatch for algorithm: " + algorithm
                );
            }
        }
        
        boolean timeChanged = currentModified != storedRecord.getLastModifiedTime();
        
        return IntegrityVerificationResult.verified(filePath, timeChanged);
    }
    
    public IntegrityVerificationReport verifyDirectory(Path directory) throws Exception {
        Map<Path, IntegrityVerificationResult> results = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();
        
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                futures.add(executorService.submit(() -> {
                    try {
                        IntegrityVerificationResult result = verifyFile(file);
                        results.put(file, result);
                    } catch (Exception e) {
                        results.put(file, IntegrityVerificationResult.error(file, e.getMessage()));
                    }
                }));
                return FileVisitResult.CONTINUE;
            }
        });
        
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error during verification: " + e.getMessage());
            }
        }
        
        return new IntegrityVerificationReport(results);
    }
    
    public FileComparisonReport compareWithStoredVersion(Path filePath) throws Exception {
        FileIntegrityRecord storedRecord = hashDatabase.getRecord(filePath);
        
        if (storedRecord == null) {
            return FileComparisonReport.notFound(filePath);
        }
        
        Map<HashAlgorithm, String> currentHashes = calculateAllHashes(filePath);
        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
        
        return new FileComparisonReport(
            filePath,
            storedRecord,
            currentHashes,
            attrs.size(),
            attrs.lastModifiedTime().toInstant().getEpochSecond()
        );
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        hashDatabase.close();
    }
    
    public DatabaseStatistics getStatistics() {
        return hashDatabase.getStatistics();
    }
}
