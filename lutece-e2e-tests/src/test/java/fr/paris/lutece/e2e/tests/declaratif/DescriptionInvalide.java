package fr.paris.lutece.e2e.tests.declaratif;

import java.util.Collection;

/**
 * Le fichier de suite ne decrit pas quelque chose d'executable.
 *
 * <p>Ces erreurs sont lues par quelqu'un qui ecrit un fichier, pas par quelqu'un qui debogue du
 * code : le message situe l'endroit fautif dans le fichier, dit ce qui ne va pas, et quand le mot
 * employe n'existe pas, enumere ceux qui conviennent. Une pile d'appels ne lui apprendrait rien.</p>
 */
public class DescriptionInvalide extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param emplacement endroit du fichier concerne, par exemple « formulaire > etape 3 > question 2 »
     * @param probleme    ce qui ne va pas
     */
    public DescriptionInvalide(String emplacement, String probleme) {
        super(emplacement + " : " + probleme);
    }

    /**
     * @param emplacement endroit du fichier concerne
     * @param probleme    ce qui ne va pas
     * @param attendus    les valeurs acceptees a cet endroit
     */
    public DescriptionInvalide(String emplacement, String probleme, Collection<String> attendus) {
        super(emplacement + " : " + probleme + System.lineSeparator()
            + "  valeurs acceptees : " + String.join(", ", attendus));
    }
}
