package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour l'affectation d'un utilisateur admin a une unite.
 * {@code userId} = identifiant de l'utilisateur admin (1 = admin par defaut).
 */
public record UserAssignmentDataSet(int userId) {

    public static UserAssignmentDataSet of(int userId) {
        return new UserAssignmentDataSet(userId);
    }

    public static UserAssignmentDataSet defaults() {
        return new UserAssignmentDataSet(1);
    }
}
