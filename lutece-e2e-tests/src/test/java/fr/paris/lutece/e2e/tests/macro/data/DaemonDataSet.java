package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour le declenchement immediat d'un daemon Lutece.
 *
 * <p>La cle est celle exposee par la page de gestion des daemons (champ {@code daemon} du
 * formulaire d'action), par exemple {@code formsIndexerDaemon} ou {@code fullIndexingDaemon}.</p>
 *
 * @param daemonKey cle du daemon a declencher
 */
public record DaemonDataSet(String daemonKey) {

    /**
     * Daemon designe par sa cle.
     *
     * @param daemonKey cle du daemon
     * @return le jeu de donnees correspondant
     */
    public static DaemonDataSet of(String daemonKey) {
        return new DaemonDataSet(daemonKey);
    }

    /**
     * Daemon d'indexation des reponses de formulaire.
     *
     * <p>C'est lui qui alimente l'index Lucene sur lequel s'appuie la multivue : sans passage de ce
     * daemon, une reponse fraichement soumise en front office reste invisible en back office.</p>
     *
     * @return le jeu de donnees du daemon d'indexation forms
     */
    public static DaemonDataSet formsIndexer() {
        return new DaemonDataSet("formsIndexerDaemon");
    }

    /**
     * Jeu de donnees par defaut : l'indexation des reponses de formulaire.
     *
     * @return le jeu de donnees par defaut
     */
    public static DaemonDataSet defaults() {
        return formsIndexer();
    }
}
