package fr.paris.lutece.e2e.tests.macro.data;

/**
 * Jeu de donnees pour la configuration des options d'un formulaire (page modifyForm).
 *
 * <p>La brique {@code ConfigureFormOptionsMacroTest} ne modifie que les options effectivement
 * presentes dans la page : chaque locator est garde par {@code isVisible()}. Les valeurs nulles
 * (message, categorie, groupe de travail) signifient "ne pas toucher". Les dates acceptent le
 * format flatpickr ("today", "2033-12-31").</p>
 */
public record FormOptionsDataSet(
        String availabilityStartDate,
        String availabilityEndDate,
        String unavailableMessage,
        Integer maxResponses,
        boolean oneResponsePerUser,
        boolean displaySummary,
        boolean enableBackup,
        boolean displayBreadcrumb,
        boolean requireAuthentication,
        String category,
        String workgroup) {

    public static FormOptionsDataSet defaults() {
        return new FormOptionsDataSet(
                "today", "2033-12-31",
                "Ce formulaire est momentanement indisponible.",
                100, true, true, true, true, false, null, null);
    }

    public static FormOptionsDataSet minimal() {
        return new FormOptionsDataSet(
                "today", "2033-12-31",
                null, null, false, false, false, false, false, null, null);
    }
}
