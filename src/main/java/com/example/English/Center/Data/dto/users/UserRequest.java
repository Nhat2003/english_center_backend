// UserRequest.java
package com.example.English.Center.Data.dto.users;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {
    private String username;
    private String password;
    private String role;
    private Boolean root;
    private Boolean isActive;
    private String fullName;
}
