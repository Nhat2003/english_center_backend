package com.example.English.Center.Data.repository.users;


import com.example.English.Center.Data.entity.users.User;
import com.example.English.Center.Data.entity.users.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long > {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByRole(UserRole role);

}
