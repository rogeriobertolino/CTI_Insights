package com.cti.provedor.controller;

import com.cti.provedor.service.UploadService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** GET /api/dashboard?loteId=1: o Spring lê no banco o JSON que o Python salvou. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
  private final UploadService service;

  public DashboardController(UploadService service) {
    this.service = service;
  }

  @GetMapping
  public Map<String, Object> obter(@RequestParam(required = false) Long loteId) {
    return service.dashboard(loteId);
  }
}
