package br.com.cti.insights.imports;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins="${cti.cors.origin:http://localhost:5173}")
public class ImportController {
    private final ImportService service;
    public ImportController(ImportService service){this.service=service;}
    @PostMapping("/imports") public ResponseEntity<Map<String,Object>> upload(@RequestPart("file") MultipartFile file) throws Exception{return ResponseEntity.ok(service.process(file));}
    @GetMapping("/dashboard/latest") public ResponseEntity<Map<String,Object>> latest() throws Exception{return ResponseEntity.ok(service.latest());}
}
