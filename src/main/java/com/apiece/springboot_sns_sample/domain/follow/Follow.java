package com.apiece.springboot_sns_sample.domain.follow;

import com.apiece.springboot_sns_sample.domain.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "follows")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "followee_id", nullable = false)
    private Long followeeId;

    public static Follow create(Long followerId, Long followeeId) {
        Follow follow = new Follow();
        follow.followerId = followerId;
        follow.followeeId = followeeId;
        return follow;
    }
}
