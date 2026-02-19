package fr.paris.lutece.e2e.tests.bo.config;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
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
 * Fonctionne avec toute classe de test possédant un champ {@code page}
 * de type {@link com.microsoft.playwright.Page}.
 * </p>
 */
public class ScreenshotOnFailureExtension implements TestWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger( ScreenshotOnFailureExtension.class );
    private static final Path FAILURES_DIR = Paths.get( "target/screenshots/failures" );

    @Override
    public void testFailed( ExtensionContext context, Throwable cause )
    {
        Object testInstance = context.getTestInstance().orElse( null );
        if ( testInstance == null )
        {
            LOGGER.warn( "Impossible de capturer un screenshot : pas d'instance de test disponible" );
            return;
        }

        Page page = getPageField( testInstance );
        if ( page == null )
        {
            LOGGER.warn( "Impossible de capturer un screenshot : le champ 'page' est null ou introuvable dans {}", testInstance.getClass().getSimpleName() );
            return;
        }

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
