package com.example.studiondonationstokvel;

public class Student {
    private String uid;
    private String imageUrl;
    private String name;
    private String surname;
    private String institution;
    private String studentNumber;
    private String story;
    private double amountRequired;
    private double amountRaised;
    private int likes;
    private double progress;
    private String videoLink; // New: Video link field

    public Student() {}

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
    public String getStory() { return story; }
    public void setStory(String story) { this.story = story; }
    public double getAmountRequired() { return amountRequired; }
    public void setAmountRequired(double amountRequired) { this.amountRequired = amountRequired; }
    public double getAmountRaised() { return amountRaised; }
    public void setAmountRaised(double amountRaised) { this.amountRaised = amountRaised; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }
    public String getVideoLink() { return videoLink; } // New: Getter for video link
    public void setVideoLink(String videoLink) { this.videoLink = videoLink; } // New: Setter for video link
}