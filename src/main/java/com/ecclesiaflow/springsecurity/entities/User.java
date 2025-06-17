package com.ecclesiaflow.springsecurity.entities;

import jakarta.persistence.*;
import lombok.Data;
import javax.management.relation.Role;

@Data
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Role role;


}
