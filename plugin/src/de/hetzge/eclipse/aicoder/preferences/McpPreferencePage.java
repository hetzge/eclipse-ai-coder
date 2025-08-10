package de.hetzge.eclipse.aicoder.preferences;

import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.hetzge.eclipse.aicoder.mcp.AiCoderMcpContent;
import de.hetzge.eclipse.aicoder.mcp.McpClients;
import mjson.Json;
import mjson.Json.MalformedJsonException;

public class McpPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private StyledText text;
	private Table table;
	private ProgressBar progressBar;

	public McpPreferencePage() {
		super();
		setDescription("MCP server configurations");
	}

	@Override
	protected Control createContents(Composite parent) {
		final Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).create());
		control.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).create());
		final TextViewer textViewer = new TextViewer(control, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP);
		this.text = textViewer.getTextWidget();
		this.text.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).hint(300, SWT.DEFAULT).create());
		this.text.setText(formatJson(AiCoderPreferences.getMcpServerConfigurations()));
		this.text.addModifyListener(event -> {
			if (isValidJson()) {
				setErrorMessage(null);
				setValid(true);
			} else {
				setErrorMessage("Invalid json");
				setValid(false);
			}
		});
		this.progressBar = new ProgressBar(control, SWT.INDETERMINATE);
		this.progressBar.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(true, false).create());
		this.progressBar.setVisible(false);
		this.table = new Table(control, SWT.BORDER | SWT.FULL_SELECTION);
		this.table.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).create());
		this.table.setHeaderVisible(true);
		this.table.setLinesVisible(true);
		final TableColumn keyColumn = new TableColumn(this.table, SWT.NONE);
		keyColumn.setText("Key");
		keyColumn.setWidth(100);
		final TableColumn statusColumn = new TableColumn(this.table, SWT.NONE);
		statusColumn.setText("Status");
		statusColumn.setWidth(100);
		final TableColumn titleColumn = new TableColumn(this.table, SWT.NONE);
		titleColumn.setText("Title");
		titleColumn.setWidth(100);
		final TableColumn promptsColumn = new TableColumn(this.table, SWT.NONE);
		promptsColumn.setText("Prompts");
		promptsColumn.setWidth(100);
		initTable();
		return control;
	}

	private void initTable() {
		final List<AiCoderMcpContent> contents = McpClients.INSTANCE.getContents();
		this.table.removeAll();
		for (final AiCoderMcpContent content : contents) {
			final TableItem item = new TableItem(this.table, SWT.NONE);
			item.setText(0, content.key());
			item.setText(1, content.success() ? "OK" : "Error");
			item.setText(2, content.title() != null ? content.title() : "");
			item.setText(3, String.valueOf(content.editInstructions().size()));
		}
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	private boolean isValidJson() {
		try {
			return Json.read(this.text.getText()) != null;
		} catch (final MalformedJsonException exception) {
			return false;
		}
	}

	@Override
	protected void performApply() {
		AiCoderPreferences.setMcpServerConfigurations(Json.read(this.text.getText()));
		this.table.setEnabled(false);
		this.progressBar.setVisible(true);
		McpClients.INSTANCE.reload(() -> Display.getDefault().syncExec(() -> {
			try {
				initTable();
			} finally {
				this.table.setEnabled(true);
				this.progressBar.setVisible(false);
			}
		}));
		super.performApply();
	}

	@Override
	protected void performDefaults() {
		this.text.setText(formatJson(AiCoderPreferences.getDefaultMcpServerConfigurations()));
		super.performDefaults();
	}

	private String formatJson(Json json) {
		try {
			final ObjectMapper objectMapper = new ObjectMapper();
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(json.toString()));
		} catch (final JsonProcessingException exception) {
			throw new RuntimeException("Failed to format JSON", exception);
		}
	}
}
