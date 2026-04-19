public class DatabaseStatistics {
    
    private final long totalRecords;
    private final long totalDataSize;
    private final long timestamp;
    
    public DatabaseStatistics(long totalRecords, long totalDataSize, long timestamp) {
        this.totalRecords = totalRecords;
        this.totalDataSize = totalDataSize;
        this.timestamp = timestamp;
    }
    
    public long getTotalRecords() {
        return totalRecords;
    }
    
    public long getTotalDataSize() {
        return totalDataSize;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getFormattedSize() {
        return formatBytes(totalDataSize);
    }
    
    public double getAverageFileSize() {
        if (totalRecords == 0) return 0;
        return (double) totalDataSize / totalRecords;
    }
    
    public String getFormattedAverageSize() {
        return formatBytes((long) getAverageFileSize());
    }
    
    private static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    
    public void printStatistics() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("DATABASE STATISTICS");
        System.out.println("=".repeat(50));
        System.out.println(String.format("Total Records: %d", totalRecords));
        System.out.println(String.format("Total Data Size: %s", getFormattedSize()));
        System.out.println(String.format("Average File Size: %s", getFormattedAverageSize()));
        System.out.println("=".repeat(50) + "\n");
    }
    
    @Override
    public String toString() {
        return String.format(
            "DatabaseStatistics{records=%d, totalSize=%s, avgSize=%s}",
            totalRecords, getFormattedSize(), getFormattedAverageSize()
        );
    }
}
