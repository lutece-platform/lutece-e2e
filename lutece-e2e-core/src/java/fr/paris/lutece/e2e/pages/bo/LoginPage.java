package fr.paris.lutece.e2e.pages.bo;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * Page Object pour la page de connexion admin Lutece.
 */
public class LoginPage {

    private final Page page;
    private final String baseUrl;

    /**
     * Mots de passe effectifs par compte, quand Lutece a impose une rotation pendant le run.
     *
     * <p>Partage entre toutes les instances : une brique qui se reconnecte plus tard avec le mot de
     * passe de configuration doit utiliser celui reellement en vigueur.</p>
     */
    private static final java.util.Map<String, String> ROTATED = new java.util.concurrent.ConcurrentHashMap<>();

    // Locators
    private static final String USERNAME_FIELD = "Code d'accès. *";
    private static final String PASSWORD_FIELD = "Mot de passe *";
    private static final String LOGIN_BUTTON = "Se connecter";

    public LoginPage(Page page, String baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    /**
     * Navigue vers la page de login.
     */
    public LoginPage navigate() {
        page.navigate(baseUrl + "/jsp/admin/AdminLogin.jsp");
        return this;
    }

    /**
     * Remplit le champ code d'accès.
     * Utilise l'id stable #access_code (le libellé accessible varie selon la version du thème).
     */
    public LoginPage fillUsername(String username) {
        page.locator("#access_code").fill(username);
        return this;
    }

    /**
     * Remplit le champ mot de passe (id stable #password).
     */
    public LoginPage fillPassword(String password) {
        page.locator("#password").fill(password);
        return this;
    }

    /**
     * Clique sur le bouton de connexion.
     */
    public void clickLogin() {
        page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName(LOGIN_BUTTON)).click();
    }

    /**
     * Effectue une connexion complète, puis neutralise l'éventuel écran d'expiration de mot de passe.
     *
     * <p>Selon la date d'expiration du compte dans l'image testée, Lutece intercale après la connexion
     * un écran « Votre mot de passe n'est plus valide ». Son apparition <b>n'est pas prévisible</b> :
     * elle dépend des données de l'image, pas du test. La neutralisation est donc toujours tentée et
     * ne fait rien quand l'écran est absent — les deux parcours (connexion directe et connexion avec
     * interstitiel) aboutissent au même état.</p>
     */
    public AdminMenuPage loginAs(String username, String password) {
        String effective = ROTATED.getOrDefault(username, password);
        if (!attempt(username, effective) && !effective.equals(derive(password))) {
            // Echec avec le mot de passe de configuration : un run precedent a peut-etre deja fait
            // tourner le mot de passe sur cette instance. La derivation etant deterministe, on retente
            // avec celle-ci -> le harnais se repare seul sur une instance persistante.
            navigate();
            attempt(username, derive(password));
        }
        completeForcedPasswordChange(username, password);
        return new AdminMenuPage(page, baseUrl);
    }

    /** Soumet le formulaire de connexion ; retourne false si un message d'erreur est affiche. */
    private boolean attempt(String username, String password) {
        fillUsername(username);
        fillPassword(password);
        clickLogin();
        page.waitForLoadState();
        return !page.url().contains("AdminLogin.jsp") || !hasErrorMessage();
    }

    /**
     * Derive un mot de passe de remplacement, de facon <b>deterministe</b> : la valeur est reproductible
     * d'un run a l'autre, ce qui permet de se reconnecter apres une rotation imposee. Respecte les
     * exigences observees (longueur >= 8, majuscule, chiffre, caractere special).
     */
    private static String derive(String password) {
        return password + "E2e!9";
    }

    /**
     * Neutralise l'écran d'expiration de mot de passe s'il est affiché ; sans effet sinon.
     *
     * <p>Idempotent et sûr à appeler après n'importe quelle connexion. Le clic n'est déclenché que si
     * le message d'expiration est effectivement présent : on ne clique jamais un « OK » au hasard, ce
     * qui perturberait les autres écrans. Le bouton est cherché en tant que bouton <i>et</i> en tant
     * que lien, le thème variant selon la version.</p>
     *
     * @throws IllegalStateException si Lutece impose un formulaire de changement de mot de passe : le
     *         parcours ne peut pas continuer sans changer le mot de passe du compte de test, et cela
     *         doit être traité au niveau de l'image (date d'expiration) plutôt que masqué ici.
     */
    public LoginPage dismissPasswordExpiryIfPresent() {
        for (int attempt = 0; attempt < 3 && isPasswordExpiryScreen(); attempt++) {
            if (!clickOkControl()) {
                break;
            }
            page.waitForLoadState();
        }
        return this;
    }

    /**
     * Mène à son terme le changement de mot de passe quand Lutece l'impose ; sans effet sinon.
     *
     * <p>Sur les images dont le compte de test est expiré, l'écran d'avertissement débouche sur
     * {@code ModifyDefaultUserPassword.jsp}, et Lutece <b>refuse</b> de reprendre le mot de passe
     * actuel (« Vous ne pouvez pas reprendre votre mot de passe actuel »). On pose donc une valeur
     * dérivée déterministe, puis on la mémorise pour les connexions suivantes du même run.</p>
     */
    private void completeForcedPasswordChange(String username, String password) {
        dismissPasswordExpiryIfPresent();
        if (!isForcedPasswordChangeForm()) {
            return;
        }
        String current = ROTATED.getOrDefault(username, password);
        String renewed = derive(password);
        fillIfPresent("password_current", current);
        fillIfPresent("new_password", renewed);
        fillIfPresent("confirm_new_password", renewed);
        Locator submit = page.locator("button[type='submit'], input[type='submit']");
        if (submit.count() == 0) {
            throw new IllegalStateException("Formulaire de changement de mot de passe sans bouton de "
                + "validation (url: " + page.url() + ")");
        }
        submit.first().click();
        page.waitForLoadState();

        // Cas nominal : Lutece confirme le changement ET DECONNECTE la session (« Votre mot de passe a
        // bien ete modifie. Vous allez etre deconnecte. »). Il faut donc se reconnecter avec la nouvelle
        // valeur — sans cette etape, la session restait anonyme et l'ecran d'expiration revenait au test
        // suivant. Match sans accents : ils varient selon l'encodage du theme.
        if (pageMessage().contains("mot de passe a bien")) {
            ROTATED.put(username, renewed);
            clickOkControl();
            page.waitForLoadState();
            navigate();
            attempt(username, renewed);
            dismissPasswordExpiryIfPresent();
            return;
        }

        dismissPasswordExpiryIfPresent();
        // Exiger un etat final REELLEMENT utilisable : rester sur AdminMessage.jsp ou sur le formulaire
        // signifie que le serveur a refuse le nouveau mot de passe (complexite, historique...). Sans ce
        // controle, on concluait au succes et l'ecran d'expiration revenait au test suivant.
        String url = page.url();
        if (isForcedPasswordChangeForm() || url.contains("ModifyDefaultUserPassword")
                || url.contains("AdminMessage.jsp")) {
            throw new IllegalStateException("Le changement de mot de passe impose par Lutece a echoue "
                + "(url: " + url + "). Reponse du serveur : \"" + pageMessage() + "\". Adapter la "
                + "derivation du mot de passe aux exigences, ou repousser "
                + "core_admin_user.password_max_valid_date dans l'image.");
        }
        ROTATED.put(username, renewed);
    }

    /** Texte visible de la page, tronqué, pour les messages de diagnostic. */
    private String pageMessage() {
        try {
            String text = page.locator("body").innerText().replaceAll("\\s+", " ").trim();
            return text.length() > 180 ? text.substring(0, 180) : text;
        } catch (RuntimeException unreadable) {
            return "(page illisible)";
        }
    }

    /** Renseigne un champ par son attribut {@code name} s'il est présent. */
    private void fillIfPresent(String name, String value) {
        Locator field = page.locator("input[name='" + name + "']");
        if (field.count() > 0) {
            field.first().fill(value);
        }
    }

    /** Vrai si l'écran d'expiration de mot de passe est affiché. */
    private boolean isPasswordExpiryScreen() {
        try {
            page.waitForLoadState();
            String text = page.locator("body").innerText();
            // Formulations sans apostrophe : celle-ci varie (droite ou typographique) selon le theme.
            return text.contains("mot de passe") && (text.contains("plus valide") || text.contains("devez le changer"));
        } catch (RuntimeException notReadable) {
            return false;
        }
    }

    /** Clique le contrôle « OK » (bouton ou lien selon le thème) ; retourne false s'il est absent. */
    private boolean clickOkControl() {
        Locator[] candidates = {
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("OK")),
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("OK")),
            page.locator("button:has-text('OK'), a:has-text('OK')")
        };
        for (Locator candidate : candidates) {
            if (candidate.count() > 0 && candidate.first().isVisible()) {
                candidate.first().click();
                return true;
            }
        }
        return false;
    }

    /** Vrai si la page courante est un formulaire de changement de mot de passe imposé. */
    private boolean isForcedPasswordChangeForm() {
        return !page.url().contains("AdminLogin.jsp")
            && page.locator("input[type='password']").count() >= 2;
    }

    /**
     * Vérifie si un message d'erreur est affiché.
     */
    public boolean hasErrorMessage() {
        return page.locator(".card-status-start.bg-danger").isVisible();
    }

    /**
     * Récupère le texte du message d'erreur.
     */
    public String getErrorMessage() {
        return page.locator(".card-status-start.bg-danger").textContent();
    }
}
