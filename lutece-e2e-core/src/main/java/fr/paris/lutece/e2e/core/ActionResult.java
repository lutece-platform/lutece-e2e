package fr.paris.lutece.e2e.core;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Resultat structure d'une action E2E.
 * Utilise par les Actions et les Tools.
 *
 * @param <T> Type des donnees retournees
 */
public class ActionResult<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final String error;
    private final Path screenshot;

    private ActionResult(boolean success, T data, String message, String error, Path screenshot) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.error = error;
        this.screenshot = screenshot;
    }

    public static <T> ActionResult<T> success(T data, String message) {
        return new ActionResult<>(true, data, message, null, null);
    }

    public static <T> ActionResult<T> success(T data, String message, Path screenshot) {
        return new ActionResult<>(true, data, message, null, screenshot);
    }

    public static <T> ActionResult<T> failure(String error) {
        return new ActionResult<>(false, null, null, error, null);
    }

    public static <T> ActionResult<T> failure(String error, Path screenshot) {
        return new ActionResult<>(false, null, null, error, screenshot);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public Optional<Path> getScreenshot() {
        return Optional.ofNullable(screenshot);
    }

    /**
     * Formate le resultat pour un tool LangChain4j.
     */
    public String toToolMessage() {
        StringBuilder sb = new StringBuilder();
        if (success) {
            sb.append("OK: ").append(message);
            if (data != null) {
                sb.append("\nDonnees: ").append(data);
            }
        } else {
            sb.append("ERREUR: ").append(error);
        }
        if (screenshot != null) {
            sb.append("\nScreenshot: ").append(screenshot);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toToolMessage();
    }
}
