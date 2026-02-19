package fr.paris.lutece.e2e.tests.bo.config;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Extension JUnit 5 qui capture automatiquement un screenshot Playwright
 * lorsqu'un test échoue.
 * <p>
 * Utilise {@link TestExecutionExceptionHandler} pour intercepter l'exception
 * AVANT {@code @AfterEach} (qui ferme le contexte navigateur dans BaseTest),
 * garantissant que la page Playwright est encore vivante au moment du screenshot.
 * </p>
 * <p>
 * Fonctionne avec toute classe de test possédant un champ {@code page}
 * de type {@link com.microsoft.playwright.Page}.
 * </p>
 */
public class ScreenshotOnFailureExtension implements TestExecutionExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger( ScreenshotOnFailureExtension.class );
    private static final Path FAILURES_DIR = Paths.get( "target/screenshots/failures" );

    @Override
    public void handleTestExecutionException( ExtensionContext context, Throwable throwable ) throws Throwable
    {
        Object testInstance = context.getTestInstance().orElse( null );
        if ( testInstance != null )
        {
            Page page = getPageField( testInstance );
            if ( page != null )
            {
                try
                {
                    Files.createDirectories( FAILURES_DIR );

                    String className = context.getRequiredTestClass().getSimpleName();
                    String methodName = context.getRequiredTestMethod().getName();
                    Path screenshotPath = FAILURES_DIR.resolve( className + "_" + methodName + ".png" );

                    page.screenshot( new Page.ScreenshotOptions()
                        .setPath( screenshotPath )
                        .setFullPage( true ) );

                    LOGGER.info( "Screenshot d'échec sauvegardé : {}", screenshotPath );
                }
                catch ( Exception e )
                {
                    LOGGER.error( "Erreur lors de la capture du screenshot d'échec", e );
                }
            }
            else
            {
                LOGGER.warn( "Impossible de capturer un screenshot : le champ 'page' est null ou introuvable dans {}", testInstance.getClass().getSimpleName() );
            }
        }

        // Re-lancer l'exception pour que le test échoue normalement
        throw throwable;
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
