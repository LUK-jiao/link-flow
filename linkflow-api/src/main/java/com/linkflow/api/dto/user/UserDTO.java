package com.linkflow.api.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户 DTO
 */
@Data
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String email;

    private String phone;

    private String role;

    private Byte status;

    private Date createTime;
}