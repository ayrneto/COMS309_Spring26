package onetoone.Counsellors.dto;

import onetoone.Counsellors.CounsellorStatus;

public class CounsellorProfileResponse {

    public Long id;
    public Integer userId;

    public String displayName;
    public String specialization;
    public String bio;
    public String profilePictureUrl;

    public Double ratingAverage;
    public Integer ratingCount;

    public CounsellorStatus status;
}