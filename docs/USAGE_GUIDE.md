# Advanced File Integrity System - Usage Guide

## Table of Contents
1. [Installation & Setup](#installation--setup)
2. [Basic Examples](#basic-examples)
3. [Advanced Scenarios](#advanced-scenarios)
4. [Best Practices](#best-practices)
5. [Troubleshooting](#troubleshooting)

---

## Installation & Setup

### Step 1: Build the Project

```bash
# Navigate to project directory
cd file-integrity-system

# Clean and build
mvn clean package

# The JAR file will be in target/
ls target/file-integrity-system-1.0.0.jar
```

### Step 2: Add to Your Project (Maven)

```xml
<dependency>
    <groupId>com.fileintegrity</groupId>
    <artifactId>file-integrity-system</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or add the JAR to your classpath manually.

---

## Basic Examples

### Example 1: Calculate a Single File Hash

```java
import java.nio.file.*;

public class HashExample {
    public static void main(String[] args) throws Exception {
        FileIntegrityManager manager = new FileIntegrityManager(
            1,
            "hash_database.json"
        );
        
        try {
            Path file = Paths.get("important_file.zip");
            
            // Calculate SHA-256
            String hash = manager.calculateHash(
                file,
                FileIntegrityManager.HashAlgorithm.SHA256
            );
            
            System.out.println("File: " + file.getFileName());
            System.out.println("SHA-256: " + hash);
            
        } finally {
            manager.shutdown();
        }
    }
}
```

### Example 2: Store File Hashes

```java
import java.nio.file.*;
import java.util.*;

public class StorageExample {
    public static void main(String[] args) throws Exception {
        FileIntegrityManager manager = new FileIntegrityManager(
            4,
            "storage_database.json"
        );
        
        try {
            // Add additional hashes for verification
            manager.addVerificationAlgorithm(
                FileIntegrityManager.HashAlgorithm.SHA512
            );
            
            Path directory = Paths.get("/home/user/important_documents");
            
            System.out.println("Scanning directory: " + directory);
            Map<Path, FileIntegrityRecord> records = 
                manager.scanDirectory(directory);
            
            System.out.println("Found " + records.size() + " files");
            
            // Store in database
            manager.storeRecords(records.values());
            System.out.println("Records stored successfully!");
            
            // Print summary
            for (FileIntegrityRecord record : records.values()) {
                System.out.println(
                    String.format(
                        "  ✓ %s (%d bytes)",
                        record.getFilePath().getFileName(),
                        record.getFileSize()
                    )
                );
            }
            
        } finally {
            manager.shutdown();
        }
    }
}
```

### Example 3: Verify File Integrity

```java
import java.nio.file.*;

public class VerificationExample {
    public static void main(String[] args) throws Exception {
        FileIntegrityManager manager = new FileIntegrityManager(
            4,
            "verification_database.json"
        );
        
        try {
            Path file = Paths.get("/home/user/documents/report.pdf");
            
            // Verify single file
            IntegrityVerificationResult result = manager.verifyFile(file);
            
            System.out.println("File: " + file.getFileName());
            System.out.println("Status: " + result.getStatus());
            System.out.println("Message: " + result.getMessage());
            
            // Check result
            if (result.isVerified()) {
                System.out.println("✓ File integrity confirmed");
            } else if (result.isModified()) {
                System.out.println("✗ WARNING: File has been modified!");
            } else if (result.isDeleted()) {
                System.out.println("✗ ERROR: File has been deleted!");
            } else if (result.getStatus() == 
                IntegrityVerificationResult.Status.NOT_FOUND) {
                System.out.println("? No record found for this file");
            }
            
        } finally {
            manager.shutdown();
        }
    }
}
```

### Example 4: Verify Entire Directory

```java
import java.nio.file.*;

public class DirectoryVerificationExample {
    public static void main(String[] args) throws Exception {
        FileIntegrityManager manager = new FileIntegrityManager(
            8,  // Use 8 threads for faster verification
            "directory_verification.json"
        );
        
        try {
            Path directory = Paths.get("/backup/restore");
            
            System.out.println("Starting directory verification...\n");
            
            IntegrityVerificationReport report = 
                manager.verifyDirectory(directory);
            
            // Print formatted summary
            report.printSummary();
            
            // Access detailed information
            System.out.println("Integrity Score: " + 
                String.format("%.2f%%", report.getIntegrityPercentage()));
            
            // List modified files
            if (report.getModifiedCount() > 0) {
                System.out.println("\nModified Files:");
                report.getModifiedFiles().forEach(r -> {
                    System.out.println("  - " + r.getFilePath() + 
                        " (" + r.getMessage() + ")");
                });
            }
            
            // List deleted files
            if (report.getDeletedCount() > 0) {
                System.out.println("\nDeleted Files:");
                report.getDeletedFiles().forEach(r -> {
                    System.out.println("  - " + r.getFilePath());
                });
            }
            
        } finally {
            manager.shutdown();
        }
    }
}
```

---

## Advanced Scenarios

### Scenario 1: Backup Integrity Verification

```java
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupIntegrityChecker {
    
    public static void main(String[] args) throws Exception {
        String dbPath = "backup_integrity.json";
        FileIntegrityManager manager = new FileIntegrityManager(
            4,
            dbPath,
            FileIntegrityManager.HashAlgorithm.SHA256
        );
        
        // Add SHA-512 for additional verification
        manager.addVerificationAlgorithm(
            FileIntegrityManager.HashAlgorithm.SHA512
        );
        
        try {
            // First run: Store baseline
            Path backupDir = Paths.get("/backups/production");
            storeBackupHashes(manager, backupDir);
            
            // Later: Verify backup integrity
            verifyBackupIntegrity(manager, backupDir);
            
        } finally {
            manager.shutdown();
        }
    }
    
    private static void storeBackupHashes(
            FileIntegrityManager manager,
            Path backupDir) throws Exception {
        System.out.println("=== INITIAL BACKUP SCAN ===\n");
        
        Map<Path, FileIntegrityRecord> records = 
            manager.scanDirectory(backupDir);
        
        manager.storeRecords(records.values());
        
        long totalSize = records.values().stream()
            .mapToLong(FileIntegrityRecord::getFileSize)
            .sum();
        
        System.out.println(String.format(
            "Stored %d files (%.2f GB) with integrity hashes\n",
            records.size(),
            totalSize / (1024.0 * 1024.0 * 1024.0)
        ));
    }
    
    private static void verifyBackupIntegrity(
            FileIntegrityManager manager,
            Path backupDir) throws Exception {
        System.out.println("=== BACKUP VERIFICATION ===\n");
        System.out.println("Time: " + LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        ));
        System.out.println();
        
        IntegrityVerificationReport report = 
            manager.verifyDirectory(backupDir);
        
        report.printSummary();
        
        // Alert on problems
        if (report.getModifiedCount() > 0 || 
            report.getDeletedCount() > 0) {
            System.out.println("⚠  ALERT: Backup integrity issues detected!");
            System.out.println("   Take immediate action to investigate.");
        } else if (report.getIntegrityPercentage() == 100) {
            System.out.println("✓ All files verified successfully!");
        }
    }
}
```

### Scenario 2: Software Update Verification

```java
import java.nio.file.*;
import java.util.*;

public class SoftwareUpdateVerifier {
    
    private static final Map<String, String> OFFICIAL_HASHES = 
        new HashMap<>();
    
    static {
        // These would come from the official software provider
        OFFICIAL_HASHES.put("app.jar", 
            "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6");
        OFFICIAL_HASHES.put("lib/core.jar",
            "f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1");
    }
    
    public static void main(String[] args) throws Exception {
        FileIntegrityManager manager = new FileIntegrityManager(
            1,
            "software_verification.json",
            FileIntegrityManager.HashAlgorithm.SHA256
        );
        
        try {
            Path softwareDir = Paths.get("/opt/myapp");
            
            verifyAllFiles(manager, softwareDir);
            
        } finally {
            manager.shutdown();
        }
    }
    
    private static void verifyAllFiles(
            FileIntegrityManager manager,
            Path softwareDir) throws Exception {
        System.out.println("Software Integrity Verification\n");
        
        boolean allValid = true;
        
        for (Map.Entry<String, String> entry : 
             OFFICIAL_HASHES.entrySet()) {
            Path filePath = softwareDir.resolve(entry.getKey());
            String expectedHash = entry.getValue();
            
            if (!Files.exists(filePath)) {
                System.out.println("✗ MISSING: " + entry.getKey());
                allValid = false;
                continue;
            }
            
            String actualHash = manager.calculateHash(
                filePath,
                FileIntegrityManager.HashAlgorithm.SHA256
            );
            
            if (actualHash.equals(expectedHash)) {
                System.out.println("✓ VALID: " + entry.getKey());
            } else {
                System.out.println("✗ CORRUPTED: " + entry.getKey());
                System.out.println("  Expected: " + expectedHash);
                System.out.println("  Actual:   " + actualHash);
                allValid = false;
            }
        }
        
        System.out.println();
        if (allValid) {
            System.out.println("✓ All files are authentic and unmodified");
        } else {
            System.out.println("✗ ALERT: File integrity issues detected!");
        }
    }
}
```

### Scenario 3: Change Detection and Tracking

```java
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ChangeTracker {
    
    public static void main(String[] args) throws Exception {
        FileIntegrityManager manager = new FileIntegrityManager(
            4,
            "change_tracking.json"
        );
        
        manager.addVerificationAlgorithm(
            FileIntegrityManager.HashAlgorithm.SHA512
        );
        
        try {
            Path projectDir = Paths.get("/home/user/MyProject");
            
            // Initial scan
            if (!shouldRecoverFromDatabase()) {
                System.out.println("Initial project scan...");
                takeSnapshot(manager, projectDir);
            }
            
            // Simulate some file changes
            System.out.println("\nMaking changes to project files...\n");
            
            // Later: detect changes
            detectChanges(manager, projectDir);
            
        } finally {
            manager.shutdown();
        }
    }
    
    private static void takeSnapshot(
            FileIntegrityManager manager,
            Path projectDir) throws Exception {
        Map<Path, FileIntegrityRecord> records = 
            manager.scanDirectory(projectDir);
        
        manager.storeRecords(records.values());
        
        System.out.println("Snapshot stored for " + records.size() + 
            " files\n");
    }
    
    private static void detectChanges(
            FileIntegrityManager manager,
            Path projectDir) throws Exception {
        System.out.println("Detecting changes...\n");
        
        IntegrityVerificationReport report = 
            manager.verifyDirectory(projectDir);
        
        System.out.println("Files Modified: " + report.getModifiedCount());
        System.out.println("Files Deleted: " + report.getDeletedCount());
        System.out.println();
        
        if (report.getModifiedCount() > 0) {
            System.out.println("Modified Files:");
            for (IntegrityVerificationResult result : 
                 report.getModifiedFiles()) {
                
                // Show detailed comparison
                FileComparisonReport comparison = 
                    manager.compareWithStoredVersion(
                        result.getFilePath()
                    );
                
                System.out.println("\n  " + 
                    result.getFilePath().getFileName());
                System.out.println(String.format(
                    "    Size change: %+d bytes",
                    comparison.getSizeChange()
                ));
                System.out.println(String.format(
                    "    Modified: %s",
                    formatTime(comparison.getCurrentModifiedTime())
                ));
            }
        }
    }
    
    private static String formatTime(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    private static boolean shouldRecoverFromDatabase() {
        return Files.exists(Paths.get("change_tracking.json"));
    }
}
```

### Scenario 4: Parallel Database Export

```java
import java.nio.file.*;
import java.util.*;

public class DatabaseExporter {
    
    public static void main(String[] args) throws Exception {
        FileIntegrityManager manager = new FileIntegrityManager(
            4,
            "export_source.json"
        );
        
        try {
            // Export to CSV for analysis in spreadsheet
            Path csvPath = Paths.get("integrity_report.csv");
            
            // Note: FileHashDatabase has exportToCSV method
            // manager.exportToCSV(csvPath.toString());
            
            System.out.println("Exported to: " + csvPath);
            
            // Get statistics
            DatabaseStatistics stats = manager.getStatistics();
            stats.printStatistics();
            
        } finally {
            manager.shutdown();
        }
    }
}
```

---

## Best Practices

### 1. Resource Management

Always use try-finally to ensure proper cleanup:

```java
FileIntegrityManager manager = new FileIntegrityManager(4, "db.json");
try {
    // Your operations here
} finally {
    manager.shutdown();  // Important!
}
```

### 2. Thread Configuration

```java
// For I/O bound operations: Use CPU_COUNT + extra
int threads = Runtime.getRuntime().availableProcessors() + 2;
FileIntegrityManager manager = new FileIntegrityManager(threads, "db.json");

// For local filesystem: 4-8 threads usually optimal
// For network filesystem: More threads may help
```

### 3. Algorithm Selection

```java
// Recommended for most cases
FileIntegrityManager manager = new FileIntegrityManager(
    4,
    "db.json",
    FileIntegrityManager.HashAlgorithm.SHA256  // Best balance
);

// Add SHA-512 for additional verification
manager.addVerificationAlgorithm(
    FileIntegrityManager.HashAlgorithm.SHA512
);
```

### 4. Error Handling

```java
try {
    IntegrityVerificationReport report = 
        manager.verifyDirectory(directory);
    
    if (report.getErrorCount() > 0) {
        System.err.println("Verification had errors:");
        report.getErrors().forEach(e -> 
            System.err.println("  - " + e.getFilePath() + 
                ": " + e.getMessage())
        );
    }
} catch (IOException e) {
    System.err.println("I/O error: " + e.getMessage());
    e.printStackTrace();
}
```

### 5. Performance Optimization

```java
// For large directories, process in chunks
List<Path> subdirs = Files.list(rootDir)
    .filter(Files::isDirectory)
    .collect(Collectors.toList());

for (Path subdir : subdirs) {
    Map<Path, FileIntegrityRecord> records = 
        manager.scanDirectory(subdir);
    manager.storeRecords(records.values());
}
```

---

## Troubleshooting

### Issue: OutOfMemoryError

**Solution:**
```bash
# Increase heap size
java -Xmx4G -cp target/file-integrity-system-1.0.0.jar MyClass

# Or process in smaller batches in code
```

### Issue: Slow Verification

**Solution:**
```java
// Increase thread count
FileIntegrityManager manager = new FileIntegrityManager(16, "db.json");

// Or increase buffer size for large files
FileIntegrityManager manager = new FileIntegrityManager(
    8,
    "db.json",
    FileIntegrityManager.HashAlgorithm.SHA256,
    65536  // 64KB buffer
);
```

### Issue: Database Corruption

**Solution:**
```java
// Delete and rebuild
Files.delete(Paths.get("integrity_database.json"));

// Re-scan directory
Map<Path, FileIntegrityRecord> records = 
    manager.scanDirectory(directory);
manager.storeRecords(records.values());
```

---

For more information, see README.md
