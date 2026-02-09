package fr.paris.lutece.e2e.web;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Configuration JAX-RS pour l'API REST.
 */
@ApplicationPath("/api")
public class JaxRsApplication extends Application {
}
