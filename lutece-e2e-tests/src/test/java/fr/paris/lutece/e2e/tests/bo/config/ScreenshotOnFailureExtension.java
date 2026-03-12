package fr.paris.lutece.e2e.tests.bo.config;

import com.microsoft.playwright.Page;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Extension JUnit 5 qui capture automatiquement un screenshot Playwright
 * et sauvegarde la trace Playwright lorsqu'un test échoue.
 * <p>
 * Les artefacts (screenshot + trace) sont attachés au rapport Allure
 * pour un diagnostic complet dans le rapport HTML.
 * </p>
 */
public class ScreenshotOnFailureExtension implements TestExecutionExceptionHandler {

    private static final Logger LOGGER = LogManager.getLogger( ScreenshotOnFailureExtension.class );
    private static final Path FAILURES_DIR = Paths.get( "target/screenshots/failures" );

    @Override
    public void handleTestExecutionException( ExtensionContext context, Throwable throwable ) throws Throwable
    {
        Object testInstance = context.getTestInstance().orElse( null );
        if ( testInstance != null )
        {
            String className = context.getRequiredTestClass().getSimpleName();
            String methodName = context.getRequiredTestMethod().getName();
            String testName = className + "_" + methodName;

            // 1. Capturer le screenshot
            Page page = getPageField( testInstance );
            if ( page != null )
            {
                captureScreenshot( page, testName );
            }
            else
            {
                LOGGER.warn( "Impossible de capturer un screenshot : le champ 'page' est null ou introuvable dans {}", className );
            }

            // 2. Sauvegarder la trace Playwright
            saveAndAttachTrace( testInstance, testName );
        }

        // Re-lancer l'exception pour que le test échoue normalement
        throw throwable;
    }

    private void captureScreenshot( Page page, String testName )
    {
        try
        {
            Files.createDirectories( FAILURES_DIR );
            Path screenshotPath = FAILURES_DIR.resolve( testName + ".png" );

            page.screenshot( new Page.ScreenshotOptions()
                .setPath( screenshotPath )
                .setFullPage( true ) );

            LOGGER.info( "Screenshot d'échec sauvegardé : {}", screenshotPath );

            // Attacher au rapport Allure
            try ( InputStream is = Files.newInputStream( screenshotPath ) )
            {
                Allure.addAttachment( testName + " - screenshot", "image/png", is, ".png" );
            }
        }
        catch ( Exception e )
        {
            LOGGER.error( "Erreur lors de la capture du screenshot d'échec", e );
        }
    }

    private void saveAndAttachTrace( Object testInstance, String testName )
    {
        if ( testInstance instanceof BaseTest baseTest )
        {
            Path tracePath = baseTest.saveTrace( testName );
            if ( tracePath != null && Files.exists( tracePath ) )
            {
                LOGGER.info( "Trace Playwright sauvegardée : {}", tracePath );
                try ( InputStream is = Files.newInputStream( tracePath ) )
                {
                    Allure.addAttachment( testName + " - trace", "application/zip", is, ".zip" );
                }
                catch ( Exception e )
                {
                    LOGGER.error( "Erreur lors de l'attachement de la trace au rapport Allure", e );
                }
            }
        }
    }

    /**
     * Recherche le champ {@code page} de type {@link Page} dans la hiérarchie de classes
     * de l'instance de test.
     */
    private Page getPageField( Object testInstance )
    {
        Class<?> clazz = testInstance.getClass();
        while ( clazz != null )
        {
            try
            {
                Field field = clazz.getDeclaredField( "page" );
                field.setAccessible( true );
                Object value = field.get( testInstance );
                if ( value instanceof Page )
                {
                    return (Page) value;
                }
            }
            catch ( NoSuchFieldException e )
            {
                // Remonter dans la hiérarchie
            }
            catch ( IllegalAccessException e )
            {
                LOGGER.error( "Impossible d'accéder au champ 'page' dans {}", clazz.getSimpleName(), e );
                return null;
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
