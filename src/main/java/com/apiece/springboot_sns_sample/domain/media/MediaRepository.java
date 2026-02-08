package com.apiece.springboot_sns_sample.domain.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<Media, Long> {

    Optional<Media> findByIdAndDeletedAtIsNull(Long id);

    List<Media> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    @Query(value = "SELECT * FROM media WHERE attributes -> 'parts' @> :partNumberJson\\:\\:jsonb AND deleted_at IS NULL ORDER BY created_at DESC", nativeQuery = true)
    List<Media> findByPartNumber(@Param("partNumberJson") String partNumberJson);
}
