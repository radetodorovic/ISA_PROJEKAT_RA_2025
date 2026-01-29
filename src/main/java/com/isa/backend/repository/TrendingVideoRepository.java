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

    @Query(
            value = "SELECT t.* FROM trending_videos t " +
                    "JOIN video_posts v ON v.id = t.video_id " +
                    "WHERE t.location = :location AND t.run_at = (" +
                    "  SELECT MAX(t2.run_at) FROM trending_videos t2 WHERE t2.location = :location" +
                    ") AND v.latitude IS NOT NULL AND v.longitude IS NOT NULL " +
                    "AND ST_DWithin(" +
                    "  geography(ST_SetSRID(ST_MakePoint(v.longitude, v.latitude), 4326))," +
                    "  geography(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326))," +
                    "  :radiusMeters" +
                    ") " +
                    "ORDER BY t.rank ASC",
            nativeQuery = true
    )
    List<TrendingVideo> findLatestByLocationWithinRadius(
            @Param("location") String location,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") int radiusMeters
    );
}
