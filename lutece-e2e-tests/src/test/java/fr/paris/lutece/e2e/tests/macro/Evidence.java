package fr.paris.lutece.e2e.tests.macro;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ScreenshotType;
import io.qameta.allure.Allure;

import java.io.ByteArrayInputStream;

/**
 * Preuves d'execution jointes au rapport.
 *
 * <p>Un rapport qui n'expose que des noms d'etapes dit ce que le test a tente, jamais ce que le
 * site a repondu. Les captures ne sont donc pas prises a chaque action — leur volume masquerait
 * le comportement eprouve — mais aux moments ou l'etat observe est ce qui fonde le verdict :
 * la reponse soumise, le detail d'un dossier instruit.</p>
 *
 * <p>Les captures d'echec restent prises par ailleurs, a l'endroit ou l'echec se produit. Celles-ci
 * s'ajoutent aux executions reussies, ou rien ne subsistait du site une fois le test vert.</p>
 */
public final class Evidence {

    private Evidence() {
    }

    /**
     * Joint au rapport une capture de la page, nommee par l'etat qu'elle atteste.
     *
     * @param page  page dont l'etat est capture
     * @param titre ce que la capture doit prouver, en quelques mots
     */
    public static void capture(Page page, String titre) {
        try {
            byte[] image = page.screenshot(new Page.ScreenshotOptions().setType(ScreenshotType.PNG));
            Allure.addAttachment(titre, "image/png", new ByteArrayInputStream(image), ".png");
        } catch (RuntimeException captureImpossible) {
            // Une page fermee ou en cours de navigation ne doit pas faire echouer un test vert :
            // la capture est une preuve supplementaire, pas la verification elle-meme.
        }
    }

    /**
     * Joint au rapport un extrait de texte, lorsque c'est l'etat lu qui fonde le verdict.
     *
     * @param titre   ce que l'extrait atteste
     * @param contenu texte observe
     */
    public static void texte(String titre, String contenu) {
        Allure.addAttachment(titre, "text/plain", contenu == null ? "" : contenu, ".txt");
    }

    /**
     * Declare les parametres d'execution du run, ceux qui expliquent qu'un meme test ne se comporte
     * pas pareil d'un environnement a l'autre.
     *
     * @param siteUrl adresse du site eprouve
     */
    public static void contexteDExecution(String siteUrl) {
        Allure.parameter("site", siteUrl);
        Allure.parameter("navigateur", "chromium");
        Allure.parameter("headless", System.getProperty("test.headless", "true"));
    }
}
