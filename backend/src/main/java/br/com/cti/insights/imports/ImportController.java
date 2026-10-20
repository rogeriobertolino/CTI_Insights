package br.com.cti.insights.imports;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class ImportController {
  private final ImportService service;

  public ImportController(ImportService service) {
    this.service = service;
  }

  @PostMapping("/imports")
  public ResponseEntity<Map<String, Object>> upload(@RequestPart("file") MultipartFile file)
      throws Exception {
    return ResponseEntity.ok(service.process(file));
  }

  @GetMapping("/dashboard/latest")
  public ResponseEntity<Map<String, Object>> latest() throws Exception {
    return ResponseEntity.ok(service.latest());
  }
}
