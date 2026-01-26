package com.isa.backend.repository;

import com.isa.backend.model.TrendingVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrendingVideoRepository extends JpaRepository<TrendingVideo, Long> {

    @Query("SELECT t FROM TrendingVideo t WHERE t.location = :location AND t.runAt = (" +
            "SELECT MAX(t2.runAt) FROM TrendingVideo t2 WHERE t2.location = :location" +
            ") ORDER BY t.rank ASC")
    List<TrendingVideo> findLatestByLocation(@Param("location") String location);
}
