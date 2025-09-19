// UserRequest.java
package com.example.English.Center.Data.dto.users;

import com.example.English.Center.Data.entity.users.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {
    private String username;
    private String password;
    private String role;
    private Boolean root;
    private UserRole status;
}
