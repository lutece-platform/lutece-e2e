package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Usager front-office MyLutece (module database) cree depuis le back-office.
 *
 * @param loginPrefix  prefixe du login (la brique y ajoute le suffixe d'execution)
 * @param password     mot de passe pose par l'administrateur (expire des la premiere connexion)
 * @param newPassword  mot de passe choisi par l'usager au changement impose
 */
public record FrontUserDataSet(String loginPrefix, String password, String newPassword) {

    public static FrontUserDataSet defaults() {
        return new FrontUserDataSet("e2eusager", "E2e-Usager-2026!a", "E2e-Usager-2026!b");
    }
}
