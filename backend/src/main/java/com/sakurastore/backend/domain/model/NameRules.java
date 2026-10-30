package com.sakurastore.backend.domain.model;
import com.sakurastore.backend.domain.exception.DomainException;
import java.text.Normalizer;

public final class NameRules {
  private NameRules() {}
  public static final String PATTERN = " *\\p{L}[\\p{L}\\p{M}]*(?: +\\p{L}[\\p{L}\\p{M}]*)* *";
  public static final String MESSAGE = "El nombre solo puede contener letras y espacios, sin números ni símbolos.";
  public static String require(String value, int maxLength) {
    if (value == null || value.length() > maxLength || !value.matches(PATTERN)) throw new DomainException(MESSAGE);
    return Normalizer.normalize(value.trim(), Normalizer.Form.NFC);
  }
}
