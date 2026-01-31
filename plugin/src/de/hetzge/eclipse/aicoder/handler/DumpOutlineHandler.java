package de.hetzge.eclipse.aicoder.handler;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServers;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import de.hetzge.eclipse.aicoder.util.EclipseUtils;

public class DumpOutlineHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		System.out.println("DumpOutlineHandler.execute()");

		final AbstractTextEditor textEditor = EclipseUtils.getActiveTextEditor().get();

		// Get document from editor
		final IDocument document = textEditor.getDocumentProvider()
				.getDocument(textEditor.getEditorInput());

		// Build parameters
		final DocumentSymbolParams params = new DocumentSymbolParams();
		params.setTextDocument(LSPEclipseUtils.toTextDocumentIdentifier(document));

		// Query language servers using public LanguageServers API
		LanguageServers.forDocument(document)
				.withCapability(ServerCapabilities::getDocumentSymbolProvider)
				.collectAll(server -> server.getTextDocumentService().documentSymbol(params))
				.thenAccept(results -> {
					results.stream()
							.flatMap(List::stream)
							.map(Either::getRight)
							.map(symbol -> {
								return symbol.getKind().toString() + " " + symbol.getDetail() + " " + symbol.getName() + " " + symbol.getRange().toString();
							})
							.forEach(System.out::println);
				});

		return null;
	}
}