package br.com.cti.insights.imports;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "import_batch")
public class ImportBatch {
    @Id private UUID id;
    @Column(nullable = false) private String fileName;
    @Column(nullable = false) private String status;
    private long rowCount;
    private double qualityScore;
    @Column(columnDefinition = "TEXT", nullable = false) private String analysisJson;
    @Column(nullable = false) private OffsetDateTime createdAt;

    protected ImportBatch() {}
    public ImportBatch(String fileName) { this.id=UUID.randomUUID(); this.fileName=fileName; this.status="PROCESSING"; this.analysisJson="{}"; this.createdAt=OffsetDateTime.now(); }
    public void complete(long rows,double quality,String json){this.rowCount=rows;this.qualityScore=quality;this.analysisJson=json;this.status="COMPLETED";}
    public void fail(){this.status="FAILED";}
    public UUID getId(){return id;} public String getFileName(){return fileName;} public String getStatus(){return status;} public long getRowCount(){return rowCount;} public double getQualityScore(){return qualityScore;} public String getAnalysisJson(){return analysisJson;} public OffsetDateTime getCreatedAt(){return createdAt;}
}
