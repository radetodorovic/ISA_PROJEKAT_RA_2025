package com.isa.backend.transcoding;

public class TranscodingProfile {
    private final String name;
    private final String scale;
    private final String videoBitrate;
    private final String audioBitrate;

    public TranscodingProfile(String name, String scale, String videoBitrate, String audioBitrate) {
        this.name = name;
        this.scale = scale;
        this.videoBitrate = videoBitrate;
        this.audioBitrate = audioBitrate;
    }

    public String getName() {
        return name;
    }

    public String getScale() {
        return scale;
    }

    public String getVideoBitrate() {
        return videoBitrate;
    }

    public String getAudioBitrate() {
        return audioBitrate;
    }
}
