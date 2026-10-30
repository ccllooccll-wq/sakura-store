package com.sakurastore.backend.application.dto;

import jakarta.validation.constraints.*;

public class LoginCommand {
  @NotBlank
  @Size(max = 100)
  private String username;

  @NotBlank
  @Size(max = 72)
  private String password;

  public LoginCommand() {}

  public LoginCommand(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
