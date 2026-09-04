package br.com.cti.insights.imports;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="imported_record",indexes=@Index(name="idx_record_batch",columnList="batch_id"))
public class ImportedRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="batch_id",nullable=false) private UUID batchId;
    @Column(nullable=false) private int rowNumber;
    @Column(columnDefinition="TEXT",nullable=false) private String dataJson;
    protected ImportedRecord(){}
    public ImportedRecord(UUID batchId,int rowNumber,String dataJson){this.batchId=batchId;this.rowNumber=rowNumber;this.dataJson=dataJson;}
}
