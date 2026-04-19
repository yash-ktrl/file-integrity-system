# Advanced File Integrity System

A comprehensive Java system for detecting file modifications, managing file hashes, and verifying data integrity. Perfect for backup verification, security monitoring, and file change detection.

## Features

### Core Functionality
- **Multiple Hash Algorithms**: SHA-256, SHA-512, SHA-1, MD5
- **Parallel Processing**: Multi-threaded directory scanning and verification
- **Persistent Storage**: JSON-based database for storing integrity records
- **Recursive Scanning**: Walk entire directory trees efficiently
- **Change Detection**: Identify modified, deleted, or new files

### Advanced Capabilities
- **Dual-hash Verification**: Cross-verify files with multiple algorithms
- **Detailed Reporting**: Comprehensive reports on integrity status
- **File Comparison**: Before/after analysis with size and time changes
- **CSV Export**: Export records for external analysis
- **Statistics**: Database metrics and analysis

## Architecture

```
FileIntegrityManager (Main Controller)
├── FileHashDatabase (Persistent Storage)
├── FileIntegrityRecord (Data Model)
├── IntegrityVerificationResult (Verification Result)
├── IntegrityVerificationReport (Report Generator)
├── FileComparisonReport (Comparison Analysis)
└── DatabaseStatistics (Metrics)
```

## Installation

### Prerequisites
- Java 11 or higher
- Maven 3.6+
- GSON library (included in pom.xml)

### Build Instructions

```bash
# Clone or download the project
cd file-integrity-system

# Build with Maven
mvn clean package

# Run the demo
java -cp target/file-integrity-system-1.0.0.jar FileIntegrityDemo
```

## Quick Start

### Basic Usage

```java
import java.nio.file.*;

// Initialize manager (4 threads, SHA-256 primary)
FileIntegrityManager manager = new FileIntegrityManager(
    4,
    "integrity_database.json",
    FileIntegrityManager.HashAlgorithm.SHA256,
    8192
);

// Add additional verification algorithms
manager.addVerificationAlgorithm(FileIntegrityManager.HashAlgorithm.SHA512);

try {
    // Scan and store directory hashes
    Path directory = Paths.get("/path/to/directory");
    Map<Path, FileIntegrityRecord> records = manager.scanDirectory(directory);
    manager.storeRecords(records.values());
    
    // Later: Verify the directory
    IntegrityVerificationReport report = manager.verifyDirectory(directory);
    report.printSummary();
    
    // Check specific file
    IntegrityVerificationResult result = manager.verifyFile(
        Paths.get("/path/to/file.txt")
    );
    
    if (result.isModified()) {
        System.out.println("WARNING: File has been modified!");
        System.out.println(result.getMessage());
    }
    
} finally {
    manager.shutdown();
}
```

### Single File Hash Calculation

```java
FileIntegrityManager manager = new FileIntegrityManager(1, "db.json");

// Get hash for single file
String sha256 = manager.calculateHash(
    Paths.get("myfile.txt"),
    FileIntegrityManager.HashAlgorithm.SHA256
);

System.out.println("SHA-256: " + sha256);
```

### Compare File Versions

```java
// Store initial version
manager.storeRecords(records);

// ... Time passes, file is modified ...

// Compare versions
FileComparisonReport report = manager.compareWithStoredVersion(
    Paths.get("myfile.txt")
);
report.printDetailed();

// Check what changed
System.out.println("Size changed: " + report.sizeChanged());
System.out.println("Content changed: " + report.hashChanged(
    FileIntegrityManager.HashAlgorithm.SHA256
));
```

## API Reference

### FileIntegrityManager

#### Constructor
```java
FileIntegrityManager(int threadCount, String databasePath)
FileIntegrityManager(int threadCount, String databasePath, 
                     HashAlgorithm primary, int bufferSize)
```

#### Core Methods
- `String calculateHash(Path filePath, HashAlgorithm algorithm)` - Calculate single hash
- `Map<HashAlgorithm, String> calculateAllHashes(Path filePath)` - All configured hashes
- `Map<Path, FileIntegrityRecord> scanDirectory(Path directory)` - Scan directory recursively
- `void storeRecords(Collection<FileIntegrityRecord> records)` - Save to database
- `IntegrityVerificationResult verifyFile(Path filePath)` - Verify single file
- `IntegrityVerificationReport verifyDirectory(Path directory)` - Verify entire directory
- `FileComparisonReport compareWithStoredVersion(Path filePath)` - Compare versions
- `void addVerificationAlgorithm(HashAlgorithm algorithm)` - Add verification hash
- `DatabaseStatistics getStatistics()` - Get database stats
- `void shutdown()` - Cleanup resources

### IntegrityVerificationResult

Status values:
- `VERIFIED` - File integrity confirmed
- `MODIFIED` - File content or size changed
- `DELETED` - File no longer exists
- `NOT_FOUND` - No stored record
- `ERROR` - Processing error

Methods:
- `isVerified()` - Check if verified
- `isModified()` - Check if modified
- `isDeleted()` - Check if deleted
- `getMessage()` - Get status message

### IntegrityVerificationReport

Methods:
- `long getTotalFilesVerified()` - Total files checked
- `long getVerifiedCount()` - Files with valid integrity
- `long getModifiedCount()` - Files with changes
- `long getDeletedCount()` - Missing files
- `double getIntegrityPercentage()` - % of verified files
- `List<IntegrityVerificationResult> getModifiedFiles()` - Changed files
- `List<IntegrityVerificationResult> getDeletedFiles()` - Deleted files
- `void printSummary()` - Print formatted report

## Use Cases

### 1. Backup Verification
```java
// After backup restoration, verify all files
IntegrityVerificationReport report = manager.verifyDirectory(
    Paths.get("/restored/backup")
);

if (report.getIntegrityPercentage() < 100) {
    System.out.println("ALERT: Backup integrity compromised!");
    report.printSummary();
}
```

### 2. Security Monitoring
```java
// Monitor critical system files
manager.scanDirectory(Paths.get("/etc/critical"));
manager.storeRecords(records.values());

// Periodic verification
IntegrityVerificationReport report = manager.verifyDirectory(
    Paths.get("/etc/critical")
);

if (report.getModifiedCount() > 0) {
    // Alert: critical files modified
    report.printSummary();
}
```

### 3. Software Integrity Checking
```java
// Verify downloaded files match publisher hashes
Path downloadedFile = Paths.get("software.zip");
String calculatedHash = manager.calculateHash(
    downloadedFile,
    FileIntegrityManager.HashAlgorithm.SHA256
);

if (calculatedHash.equals(publisherHash)) {
    System.out.println("Download is authentic");
} else {
    System.out.println("WARNING: Hash mismatch! File may be corrupted or compromised.");
}
```

### 4. Change Tracking
```java
// Track changes to project files
manager.scanDirectory(Paths.get("/project"));
manager.storeRecords(records.values());

// ... Make changes ...

FileComparisonReport report = manager.compareWithStoredVersion(
    Paths.get("/project/important_file.java")
);

if (report.sizeChanged()) {
    System.out.println("Size changed by: " + report.getSizeChange() + " bytes");
}
```

## Performance Considerations

### Buffer Size
- Default: 8192 bytes
- Larger for big files: 65536 bytes
- Smaller for many small files: 4096 bytes

### Thread Count
- Depends on CPU cores and I/O capabilities
- Recommended: (CPU cores) to (CPU cores * 2)
- Example: 4-core system → 4-8 threads

### Hash Algorithm Selection
- **SHA-256**: Best balance of speed and security (recommended)
- **SHA-512**: Slower but more collision-resistant
- **SHA-1**: Deprecated, avoid for security
- **MD5**: Fast but cryptographically broken

## Database Format

The system stores integrity records in JSON format:

```json
{
  "timestamp": 1234567890,
  "records": [
    {
      "filePath": "/path/to/file.txt",
      "primaryHash": "abc123...",
      "hashes": {
        "SHA256": "abc123...",
        "SHA512": "def456..."
      },
      "fileSize": 1024,
      "lastModifiedTime": 1234567890,
      "recordedTime": 1234567890
    }
  ]
}
```

## Error Handling

```java
try {
    Map<Path, FileIntegrityRecord> records = manager.scanDirectory(dir);
} catch (IOException e) {
    System.err.println("I/O error during scan: " + e.getMessage());
} catch (Exception e) {
    System.err.println("Unexpected error: " + e.getMessage());
} finally {
    manager.shutdown();
}
```

## Troubleshooting

### OutOfMemoryError during large directory scans
- Reduce thread count
- Increase JVM heap: `java -Xmx4G ...`
- Process directory in chunks

### Slow verification
- Increase buffer size for large files
- Increase thread count (check CPU/IO)
- Use faster hash algorithm (SHA-256 vs SHA-512)

### Database corruption
- Delete `integrity_database.json` to reset
- Re-scan directory to rebuild

## License

MIT License - Feel free to use in your projects

## Contributing

Contributions welcome! Areas for enhancement:
- Database indexing for faster lookups
- Remote database support
- Incremental scanning (only new/modified files)
- Real-time file monitoring
- REST API wrapper
- GUI application

## Support

For issues or questions, please refer to the included demo and documentation.
