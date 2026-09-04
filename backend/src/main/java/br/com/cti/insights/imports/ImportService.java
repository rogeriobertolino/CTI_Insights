package br.com.cti.insights.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class ImportService {
    private final ImportBatchRepository repository; private final ImportedRecordRepository records; private final PythonAnalysisService python; private final ObjectMapper mapper;
    public ImportService(ImportBatchRepository repository,ImportedRecordRepository records,PythonAnalysisService python,ObjectMapper mapper){this.repository=repository;this.records=records;this.python=python;this.mapper=mapper;}
    public Map<String,Object> process(MultipartFile file) throws Exception {
        if(file.isEmpty()||file.getOriginalFilename()==null||!file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) throw new IllegalArgumentException("Envie um arquivo XLSX válido.");
        ImportBatch batch=repository.save(new ImportBatch(file.getOriginalFilename())); Path temp=Files.createTempFile("cti-", ".xlsx");
        try { file.transferTo(temp); Map<String,Object> result=python.analyze(temp); List<Map<String,Object>> imported=(List<Map<String,Object>>)result.getOrDefault("records",List.of()); List<ImportedRecord> entities=new ArrayList<>(); for(int i=0;i<imported.size();i++)entities.add(new ImportedRecord(batch.getId(),i+2,mapper.writeValueAsString(imported.get(i)))); records.saveAll(entities); result.put("records",imported.subList(0,Math.min(imported.size(),1000))); long rows=((Number)result.getOrDefault("totalRows",0)).longValue(); double quality=((Number)result.getOrDefault("quality",0)).doubleValue(); batch.complete(rows,quality,mapper.writeValueAsString(result));repository.save(batch);return response(batch,result); }
        catch(Exception e){batch.fail();repository.save(batch);throw e;} finally{Files.deleteIfExists(temp);}
    }
    public Map<String,Object> latest() throws Exception { ImportBatch batch=repository.findFirstByStatusOrderByCreatedAtDesc("COMPLETED").orElseThrow(()->new IllegalStateException("Nenhuma análise disponível.")); Map<String,Object> result=mapper.readValue(batch.getAnalysisJson(),new TypeReference<>(){});return response(batch,result); }
    private Map<String,Object> response(ImportBatch batch,Map<String,Object> result){Map<String,Object> response=new LinkedHashMap<>(result);response.put("batchId",batch.getId());response.put("fileName",batch.getFileName());response.put("status",batch.getStatus());response.put("createdAt",batch.getCreatedAt());return response;}
}
