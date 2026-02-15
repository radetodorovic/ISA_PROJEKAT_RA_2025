package com.isa.backend.repository;

import com.isa.backend.model.PopularVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PopularVideoRepository extends JpaRepository<PopularVideo, Long> {

    @Query("SELECT p FROM PopularVideo p WHERE p.runAt = (" +
            "SELECT MAX(p2.runAt) FROM PopularVideo p2" +
            ") ORDER BY p.rank ASC")
    List<PopularVideo> findLatest();
}
