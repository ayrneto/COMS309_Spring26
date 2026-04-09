package onetoone.Tasks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskStatus {

    NOT_STARTED("Not Started"),
    ONGOING("Ongoing"),
    COMPLETED("Completed");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static TaskStatus fromString(String value) {
        for (TaskStatus s : values()) {
            if (s.displayName.equalsIgnoreCase(value) || s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException(
                "Invalid status '" + value + "'. Must be: Not Started, Ongoing, or Completed");
    }
}