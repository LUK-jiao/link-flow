package com.linkflow.api.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建用户 DTO
 */
@Data
public class UserCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;

    private String password;

    private String email;

    private String phone;

    private String role;
}