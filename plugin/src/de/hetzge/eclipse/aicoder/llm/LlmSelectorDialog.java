package de.hetzge.eclipse.aicoder.llm;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.BoldStylerProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;
import org.osgi.framework.FrameworkUtil;

import de.hetzge.eclipse.aicoder.AiCoderActivator;
import de.hetzge.eclipse.aicoder.AiCoderImageKey;

public final class LlmSelectorDialog extends FilteredItemsSelectionDialog {

	private static final String DIALOG_SETTINGS = "de.hetzge.eclipse.aicoder.llm.LlmSelectorDialog";
	private ItemsFilter filter;

	public LlmSelectorDialog(Shell shell) {
		super(shell, false);
		setTitle("Select LLM");
		final LlmSelectorLabelProvider labelProvider = new LlmSelectorLabelProvider();
		setListLabelProvider(labelProvider);
		setDetailsLabelProvider(labelProvider);
	}

	@Override
	public void applyFilter() {
		super.applyFilter();
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		final IDialogSettings dialogSettings = PlatformUI
				.getDialogSettingsProvider(FrameworkUtil.getBundle(FilteredResourcesSelectionDialog.class))
				.getDialogSettings();
		IDialogSettings settings = dialogSettings.getSection(DIALOG_SETTINGS);
		if (settings == null) {
			settings = dialogSettings.addNewSection(DIALOG_SETTINGS);
		}
		return settings;
	}

	@Override
	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}

	@Override
	protected ItemsFilter createFilter() {
		this.filter = new ItemsFilter() {

			{
				if (this.patternMatcher.getPattern().isEmpty()) {
					this.patternMatcher.setPattern("**");
				}
			}

			@Override
			public boolean matchItem(Object item) {
				if (item instanceof final LlmModelOption option) {
					final String oldPattern = this.patternMatcher.getPattern();
					final String newPattern = (!oldPattern.startsWith("*") ? "*" : "") + oldPattern + (!oldPattern.endsWith("*") ? "*" : "");
					if (!Objects.equals(this.patternMatcher.getPattern(), newPattern)) {
						this.patternMatcher.setPattern(newPattern);
					}
					return this.patternMatcher.matches(option.getLabel());
				}
				return false;
			}

			@Override
			public boolean isConsistentItem(Object item) {
				return true;
			}
		};
		return this.filter;
	}

	@Override
	protected Comparator<?> getItemsComparator() {
		return new Comparator<LlmModelOption>() {
			@Override
			public int compare(LlmModelOption o1, LlmModelOption o2) {
				return o1.getLabel().compareTo(o2.getLabel());
			}
		};
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {
		progressMonitor.beginTask("Load LLMs", IProgressMonitor.UNKNOWN);
		final List<LlmModelOption> options = LlmModels.INSTANCE.getOrLoadOptions();
		for (final LlmModelOption option : options) {
			contentProvider.add(option, itemsFilter);
		}
		progressMonitor.done();
	}

	@Override
	public String getElementName(Object item) {
		if (!(item instanceof LlmModelOption)) {
			return "???";
		}
		final LlmModelOption option = (LlmModelOption) item;
		return option.getLabel();
	}

	private class LlmSelectorLabelProvider implements ILabelProvider, IStyledLabelProvider {

		private BoldStylerProvider boldStylerProvider;

		@Override
		public void dispose() {
			if (this.boldStylerProvider != null) {
				this.boldStylerProvider.dispose();
				this.boldStylerProvider = null;
			}
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

		@Override
		public Image getImage(Object element) {
			return AiCoderActivator.getImage(AiCoderImageKey.RUN_ICON);
		}

		@Override
		public String getText(Object element) {
			if (element instanceof final LlmModelOption option) {
				return option.getLabel();
			}
			return "???";
		}

		@Override
		public StyledString getStyledText(Object element) {
			final String text = getText(element);
			final String namePattern = LlmSelectorDialog.this.filter != null ? LlmSelectorDialog.this.filter.getPattern() : null;

			return getStyledStringHighlighter().highlight(text, namePattern, getBoldStylerProvider().getBoldStyler());
		}

		private BoldStylerProvider getBoldStylerProvider() {
			if (this.boldStylerProvider == null) {
				this.boldStylerProvider = new BoldStylerProvider(getDialogArea().getFont());
			}
			return this.boldStylerProvider;
		}
	}
}
