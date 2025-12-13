package net.mrbernie.customCosmetics.cosmetics;

public enum CosmeticType {
    HEAD("Head"),
    SHOULDER("Shoulder"),
    BACK("Back");

    private final String displayName;

    CosmeticType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }


    public static CosmeticType fromString(String text) {
        String lowerText = text.toLowerCase();
        switch (lowerText) {
            case "head":
            case "Head":
                return HEAD;
            case "shoulder":
            case "Shoulder":
                return SHOULDER;
            case "back":
            case "Back":
                return BACK;
            default:
                return null;
        }
    }
}