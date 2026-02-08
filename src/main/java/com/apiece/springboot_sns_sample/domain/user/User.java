package com.apiece.springboot_sns_sample.domain.user;

import com.apiece.springboot_sns_sample.domain.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    private Long profileMediaId;

    public static User create(String username, String password) {
        User user = new User();
        user.username = username;
        user.password = password;
        return user;
    }

    public void updateProfileMediaId(Long profileMediaId) {
        this.profileMediaId = profileMediaId;
    }
}
