package Util;

public class UserSession {
    private static int currentClientId;

    public static int getCurrentClientId() {
        return currentClientId;
    }

    public static void setCurrentClientId(int id) {
        currentClientId = id;
    }

    public static void cleanUserSession() {
        currentClientId = 0; // Call this when logging out!
    }
}