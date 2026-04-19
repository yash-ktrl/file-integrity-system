import java.nio.file.*;
import java.util.Map;

public class FileIntegrityDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║     ADVANCED FILE INTEGRITY SYSTEM DEMONSTRATION              ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");
        
        FileIntegrityManager manager = new FileIntegrityManager(
            4,
            "integrity_database.json",
            FileIntegrityManager.HashAlgorithm.SHA256,
            8192
        );
        
        manager.addVerificationAlgorithm(FileIntegrityManager.HashAlgorithm.SHA512);
        manager.addVerificationAlgorithm(FileIntegrityManager.HashAlgorithm.MD5);
        
        try {
            demonstrateSingleFileHash(manager);
            demonstrateDirectoryScan(manager);
            demonstrateVerification(manager);
            demonstrateComparison(manager);
            demonstrateStatistics(manager);
        } finally {
            manager.shutdown();
        }
    }
    
    private static void demonstrateSingleFileHash(FileIntegrityManager manager) throws Exception {
        System.out.println("\n┌─ EXAMPLE 1: Single File Hash Calculation ─────────────────┐\n");
        
        Path testFile = Files.createTempFile("test_", ".txt");
        Files.writeString(testFile, "This is a test file for integrity checking.");
        
        System.out.println("Test file created: " + testFile);
        System.out.println("File size: " + Files.size(testFile) + " bytes\n");
        
        System.out.println("Calculating hashes...\n");
        Map<FileIntegrityManager.HashAlgorithm, String> hashes = 
            manager.calculateAllHashes(testFile);
        
        hashes.forEach((algo, hash) ->
            System.out.println(String.format("  %-8s: %s", algo.name(), hash))
        );
        
        Files.delete(testFile);
        System.out.println("\n└─────────────────────────────────────────────────────────────┘");
    }
    
    private static void demonstrateDirectoryScan(FileIntegrityManager manager) throws Exception {
        System.out.println("\n┌─ EXAMPLE 2: Directory Scanning & Recording ───────────────┐\n");
        
        Path testDir = Files.createTempDirectory("integrity_test_");
        Path file1 = testDir.resolve("document.txt");
        Path file2 = testDir.resolve("data.json");
        Path subdir = testDir.resolve("subdir");
        Files.createDirectories(subdir);
        Path file3 = subdir.resolve("config.xml");
        
        Files.writeString(file1, "Content of document file");
        Files.writeString(file2, "{\"key\": \"value\"}");
        Files.writeString(file3, "<config></config>");
        
        System.out.println("Created test directory structure:");
        System.out.println("  " + testDir);
        System.out.println("  ├── document.txt");
        System.out.println("  ├── data.json");
        System.out.println("  └── subdir/");
        System.out.println("      └── config.xml\n");
        
        System.out.println("Scanning directory...\n");
        Map<Path, FileIntegrityRecord> records = manager.scanDirectory(testDir);
        
        System.out.println(String.format("Found %d files\n", records.size()));
        
        System.out.println("Storing integrity records...");
        manager.storeRecords(records.values());
        System.out.println("Records stored successfully!\n");
        
        records.values().forEach(record ->
            System.out.println(String.format("  ✓ %s (%d bytes)", 
                record.getFilePath().getFileName(), 
                record.getFileSize()))
        );
        
        deleteDirectory(testDir);
        System.out.println("\n└─────────────────────────────────────────────────────────────┘");
    }
    
    private static void demonstrateVerification(FileIntegrityManager manager) throws Exception {
        System.out.println("\n┌─ EXAMPLE 3: File Verification ──────────────────────────────┐\n");
        
        Path testFile = Files.createTempFile("verify_test_", ".txt");
        Files.writeString(testFile, "Content to verify");
        
        System.out.println("Created test file: " + testFile.getFileName());
        System.out.println("Original content: 'Content to verify'\n");
        
        Map<FileIntegrityManager.HashAlgorithm, String> hashes = 
            manager.calculateAllHashes(testFile);
        BasicFileAttributes attrs = Files.readAttributes(testFile, BasicFileAttributes.class);
        
        FileIntegrityRecord record = new FileIntegrityRecord(
            testFile,
            hashes.get(FileIntegrityManager.HashAlgorithm.SHA256),
            hashes,
            attrs.size(),
            attrs.lastModifiedTime().toInstant().getEpochSecond(),
            System.currentTimeMillis() / 1000
        );
        
        manager.storeRecords(java.util.Collections.singleton(record));
        System.out.println("Hash stored in database\n");
        
        System.out.println("Verifying unmodified file...");
        IntegrityVerificationResult result1 = manager.verifyFile(testFile);
        System.out.println(String.format("  Status: %s", result1.getStatus()));
        System.out.println(String.format("  Message: %s\n", result1.getMessage()));
        
        System.out.println("Modifying file content...");
        Files.writeString(testFile, "Modified content");
        System.out.println("New content: 'Modified content'\n");
        
        System.out.println("Verifying modified file...");
        IntegrityVerificationResult result2 = manager.verifyFile(testFile);
        System.out.println(String.format("  Status: %s ⚠", result2.getStatus()));
        System.out.println(String.format("  Message: %s\n", result2.getMessage()));
        
        Files.delete(testFile);
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }
    
    private static void demonstrateComparison(FileIntegrityManager manager) throws Exception {
        System.out.println("\n┌─ EXAMPLE 4: File Comparison & Change Detection ────────────┐\n");
        
        Path testFile = Files.createTempFile("compare_", ".txt");
        Files.writeString(testFile, "Initial content");
        
        Map<FileIntegrityManager.HashAlgorithm, String> hashes = 
            manager.calculateAllHashes(testFile);
        BasicFileAttributes attrs = Files.readAttributes(testFile, BasicFileAttributes.class);
        
        FileIntegrityRecord record = new FileIntegrityRecord(
            testFile,
            hashes.get(FileIntegrityManager.HashAlgorithm.SHA256),
            hashes,
            attrs.size(),
            attrs.lastModifiedTime().toInstant().getEpochSecond(),
            System.currentTimeMillis() / 1000
        );
        
        manager.storeRecords(java.util.Collections.singleton(record));
        
        Thread.sleep(1000);
        
        Files.writeString(testFile, "Modified content that is longer than before");
        
        System.out.println("Comparing file versions...\n");
        FileComparisonReport report = manager.compareWithStoredVersion(testFile);
        report.printDetailed();
        
        Files.delete(testFile);
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }
    
    private static void demonstrateStatistics(FileIntegrityManager manager) {
        System.out.println("\n┌─ EXAMPLE 5: Database Statistics ─────────────────────────────┐\n");
        
        DatabaseStatistics stats = manager.getStatistics();
        stats.printStatistics();
        
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }
    
    private static void deleteDirectory(Path directory) throws Exception {
        Files.walk(directory)
            .sorted(java.util.Comparator.reverseOrder())
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (Exception e) {
                    System.err.println("Failed to delete: " + path);
                }
            });
    }
}
