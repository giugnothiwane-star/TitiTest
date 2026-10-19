package com.ultimateautobuilder.core.importer;

/** Fichier refusé : malformé, trop volumineux ou hors des limites de sécurité. */
public final class ImportException extends Exception {
    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
