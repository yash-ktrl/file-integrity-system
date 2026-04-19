import java.nio.file.Path;

public class IntegrityVerificationResult {
    
    public enum Status {
        VERIFIED,
        MODIFIED,
        DELETED,
        NOT_FOUND,
        ERROR
    }
    
    private final Path filePath;
    private final Status status;
    private final String message;
    private final boolean timeModified;
    
    private IntegrityVerificationResult(Path filePath, Status status, String message, boolean timeModified) {
        this.filePath = filePath;
        this.status = status;
        this.message = message;
        this.timeModified = timeModified;
    }
    
    public static IntegrityVerificationResult verified(Path filePath, boolean timeModified) {
        return new IntegrityVerificationResult(
            filePath,
            Status.VERIFIED,
            "File integrity verified",
            timeModified
        );
    }
    
    public static IntegrityVerificationResult modified(Path filePath, String reason) {
        return new IntegrityVerificationResult(filePath, Status.MODIFIED, reason, false);
    }
    
    public static IntegrityVerificationResult deleted(Path filePath) {
        return new IntegrityVerificationResult(filePath, Status.DELETED, "File has been deleted", false);
    }
    
    public static IntegrityVerificationResult notFound(Path filePath) {
        return new IntegrityVerificationResult(
            filePath,
            Status.NOT_FOUND,
            "No integrity record found for this file",
            false
        );
    }
    
    public static IntegrityVerificationResult error(Path filePath, String error) {
        return new IntegrityVerificationResult(filePath, Status.ERROR, "Error: " + error, false);
    }
    
    public Path getFilePath() {
        return filePath;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public boolean isVerified() {
        return status == Status.VERIFIED;
    }
    
    public boolean isModified() {
        return status == Status.MODIFIED;
    }
    
    public boolean isDeleted() {
        return status == Status.DELETED;
    }
    
    public boolean isTimeModified() {
        return timeModified;
    }
    
    @Override
    public String toString() {
        return String.format(
            "IntegrityVerificationResult{path=%s, status=%s, message=%s}",
            filePath, status, message
        );
    }
}
