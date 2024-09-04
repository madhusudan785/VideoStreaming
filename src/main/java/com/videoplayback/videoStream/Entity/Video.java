package com.videoplayback.videoStream.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "video")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id
    private  String videoId;

    private  String title;

    private  String description;

    private  String  contentType;

    private  String filePath;

    private VideoStatus status;

    private String thumbnail;

    
}
