package onetoone.Counsellors;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status of the counsellor")
public enum CounsellorStatus {
    AVAILABLE,
    BUSY,
    OFFLINE
}