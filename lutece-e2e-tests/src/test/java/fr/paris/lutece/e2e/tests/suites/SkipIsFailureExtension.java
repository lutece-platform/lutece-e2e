package fr.paris.lutece.e2e.tests.suites;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

/**
 * Dans une suite de non-regression, un skip est un echec : une brique qui abandonne sur une {@code Assumptions}
 * signale un scenario non joue, que Surefire ne compterait pas comme rouge.
 *
 * <p>Les briques historiques composees par les suites (non modifiees) abandonnent quand l'ecran attendu manque :
 * sans cette extension, une regression sur cet ecran rendrait la suite entiere « skipped ». Revue adversariale du
 * 2026-10-30, constat 1.</p>
 */
public class SkipIsFailureExtension implements TestExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        if (throwable instanceof TestAbortedException) {
            AssertionFailedError failure = new AssertionFailedError(
                "Brique abandonnee dans une suite de non-regression (skip interdit) : " + throwable.getMessage());
            failure.initCause(throwable);
            throw failure;
        }
        throw throwable;
    }
}
