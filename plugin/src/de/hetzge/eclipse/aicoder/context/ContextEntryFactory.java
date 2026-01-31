package de.hetzge.eclipse.aicoder.context;

import java.util.function.Supplier;

import org.eclipse.core.runtime.CoreException;

import de.hetzge.eclipse.aicoder.util.LambdaExceptionUtils.Supplier_WithExceptions;

public record ContextEntryFactory(String prefix, Supplier_WithExceptions<ContextEntry, CoreException> supplier, Supplier<ContextEntry> emptySupplier) {
}
