package com.linkflow.user.service;

public interface PasswordService {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String storedPassword);

    boolean isEncoded(String storedPassword);
}
