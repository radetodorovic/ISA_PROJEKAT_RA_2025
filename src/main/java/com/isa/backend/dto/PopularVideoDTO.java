package com.isa.backend.dto;

import java.time.LocalDateTime;

public class PopularVideoDTO {
    private VideoPostDTO video;
    private Long score;
    private Integer rank;
    private LocalDateTime runAt;

    public VideoPostDTO getVideo() {
        return video;
    }

    public void setVideo(VideoPostDTO video) {
        this.video = video;
    }

    public Long getScore() {
        return score;
    }

    public void setScore(Long score) {
        this.score = score;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public LocalDateTime getRunAt() {
        return runAt;
    }

    public void setRunAt(LocalDateTime runAt) {
        this.runAt = runAt;
    }
}
