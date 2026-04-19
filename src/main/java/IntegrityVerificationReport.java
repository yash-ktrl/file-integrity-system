import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class IntegrityVerificationReport {
    
    private final Map<Path, IntegrityVerificationResult> results;
    private final long generatedTime;
    
    public IntegrityVerificationReport(Map<Path, IntegrityVerificationResult> results) {
        this.results = new HashMap<>(results);
        this.generatedTime = System.currentTimeMillis();
    }
    
    public long getTotalFilesVerified() {
        return results.size();
    }
    
    public long getVerifiedCount() {
        return results.values().stream()
            .filter(IntegrityVerificationResult::isVerified)
            .count();
    }
    
    public long getModifiedCount() {
        return results.values().stream()
            .filter(IntegrityVerificationResult::isModified)
            .count();
    }
    
    public long getDeletedCount() {
        return results.values().stream()
            .filter(IntegrityVerificationResult::isDeleted)
            .count();
    }
    
    public long getNotFoundCount() {
        return results.values().stream()
            .filter(r -> r.getStatus() == IntegrityVerificationResult.Status.NOT_FOUND)
            .count();
    }
    
    public long getErrorCount() {
        return results.values().stream()
            .filter(r -> r.getStatus() == IntegrityVerificationResult.Status.ERROR)
            .count();
    }
    
    public double getIntegrityPercentage() {
        if (results.isEmpty()) return 0.0;
        return (double) getVerifiedCount() / getTotalFilesVerified() * 100.0;
    }
    
    public List<IntegrityVerificationResult> getModifiedFiles() {
        return results.values().stream()
            .filter(IntegrityVerificationResult::isModified)
            .collect(Collectors.toList());
    }
    
    public List<IntegrityVerificationResult> getDeletedFiles() {
        return results.values().stream()
            .filter(IntegrityVerificationResult::isDeleted)
            .collect(Collectors.toList());
    }
    
    public List<IntegrityVerificationResult> getErrors() {
        return results.values().stream()
            .filter(r -> r.getStatus() == IntegrityVerificationResult.Status.ERROR)
            .collect(Collectors.toList());
    }
    
    public IntegrityVerificationResult getResult(Path filePath) {
        return results.get(filePath);
    }
    
    public Collection<IntegrityVerificationResult> getAllResults() {
        return new ArrayList<>(results.values());
    }
    
    public long getGeneratedTime() {
        return generatedTime;
    }
    
    public void printSummary() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("FILE INTEGRITY VERIFICATION REPORT");
        System.out.println("=".repeat(70));
        System.out.println(String.format("Total Files Verified: %d", getTotalFilesVerified()));
        System.out.println(String.format("✓ Verified: %d (%.2f%%)", getVerifiedCount(), getIntegrityPercentage()));
        System.out.println(String.format("✗ Modified: %d", getModifiedCount()));
        System.out.println(String.format("✗ Deleted: %d", getDeletedCount()));
        System.out.println(String.format("? Not Found: %d", getNotFoundCount()));
        System.out.println(String.format("⚠ Errors: %d", getErrorCount()));
        System.out.println("=".repeat(70));
        
        if (getModifiedCount() > 0) {
            System.out.println("\nMODIFIED FILES:");
            getModifiedFiles().forEach(r -> 
                System.out.println("  - " + r.getFilePath() + " (" + r.getMessage() + ")")
            );
        }
        
        if (getDeletedCount() > 0) {
            System.out.println("\nDELETED FILES:");
            getDeletedFiles().forEach(r -> 
                System.out.println("  - " + r.getFilePath())
            );
        }
        
        if (getErrorCount() > 0) {
            System.out.println("\nERRORS:");
            getErrors().forEach(r -> 
                System.out.println("  - " + r.getFilePath() + ": " + r.getMessage())
            );
        }
        
        System.out.println("\n");
    }
    
    @Override
    public String toString() {
        return String.format(
            "IntegrityVerificationReport{verified=%d, modified=%d, deleted=%d, not_found=%d, errors=%d, integrity=%.2f%%}",
            getVerifiedCount(), getModifiedCount(), getDeletedCount(), getNotFoundCount(), 
            getErrorCount(), getIntegrityPercentage()
        );
    }
}
