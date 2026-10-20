package br.com.cti.insights.shared;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
    OffsetDateTime timestamp, int status, String message, List<FieldError> fields) {
  public record FieldError(String field, String message) {}
}
