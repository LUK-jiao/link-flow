package com.linkflow.api.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String role;

    private Byte status;
}
