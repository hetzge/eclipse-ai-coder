package de.hetzge.eclipse.aicoder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import de.hetzge.eclipse.aicoder.context.BlacklistedContextEntry;
import de.hetzge.eclipse.aicoder.context.CodeViewportMemoryContextEntry;
import de.hetzge.eclipse.aicoder.context.ContextContext;
import de.hetzge.eclipse.aicoder.context.ContextEntry;
import de.hetzge.eclipse.aicoder.context.ContextEntryKey;
import de.hetzge.eclipse.aicoder.context.CustomContextEntry;
import de.hetzge.eclipse.aicoder.context.CustomContextEntryData;
import de.hetzge.eclipse.aicoder.context.EmptyContextEntry;
import de.hetzge.eclipse.aicoder.context.UserContextEntry;
import de.hetzge.eclipse.aicoder.handler.ResetCodeViewportMemoryHandler;
import de.hetzge.eclipse.aicoder.handler.ToggleMultilineHandler;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.preferences.ContextPreferencePage;
import de.hetzge.eclipse.aicoder.preferences.ContextPreferences;
import de.hetzge.eclipse.aicoder.util.EclipseUtils;
import jakarta.inject.Inject;

public class ContextView extends ViewPart {

	public static final String ID = "de.hetzge.eclipse.aicoder.ContextView";

	@Inject
	IWorkbench workbench;

	private CCombo modeCombo;
	private StackLayout stackLayout;
	private Composite panelContainer;
	private List<ContextViewPanel> panels;

	@Override
	public void createPartControl(Composite parent) {
		this.panels = new ArrayList<>();

		final GridLayout parentLayout = new GridLayout(1, false);
		parentLayout.marginWidth = 0;
		parentLayout.marginHeight = 0;
		parentLayout.verticalSpacing = 0;
		parent.setLayout(parentLayout);

		this.modeCombo = new CCombo(parent, SWT.DROP_DOWN | SWT.READ_ONLY | SWT.FLAT | SWT.BORDER);
		this.modeCombo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		this.panelContainer = new Composite(parent, SWT.NONE);
		this.panelContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		this.stackLayout = new StackLayout();
		this.panelContainer.setLayout(this.stackLayout);

		for (final CompletionMode mode : CompletionMode.values()) {
			this.modeCombo.add(mode.name());
			final ContextViewPanel panel = new ContextViewPanel(mode);
			final Composite panelComposite = new Composite(this.panelContainer, SWT.NONE);

			final GridLayout panelLayout = new GridLayout(1, false);
			panelLayout.marginWidth = 0;
			panelLayout.marginHeight = 0;
			panelLayout.verticalSpacing = 0;
			panelComposite.setLayout(panelLayout);

			panel.createPartControl(panelComposite);
			panel.viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			this.panels.add(panel);
		}

		this.modeCombo.select(0);
		toggleContextViewPanel(this.panels.get(0));

		this.modeCombo.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			final int index = this.modeCombo.getSelectionIndex();
			if (index >= 0 && index < this.panels.size()) {
				toggleContextViewPanel(this.panels.get(index));
			}
		}));

		contributeToActionBars();
		final IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
		menuManager.add(new CommandContributionItem(
				new CommandContributionItemParameter(
						PlatformUI.getWorkbench(),
						null,
						ToggleMultilineHandler.COMMAND_ID,
						CommandContributionItem.STYLE_CHECK)));
		menuManager.add(new OpenContextPreferencesAction());
	}

	private void toggleContextViewPanel(final ContextViewPanel panel) {
		this.stackLayout.topControl = panel.viewer.getControl().getParent();
		getSite().setSelectionProvider(panel.viewer);
		this.panelContainer.layout();
	}

	@Override
	public void setFocus() {
		getCurrentPanel().setFocus();
	}

	public ContextViewPanel getAndEnablePanel(CompletionMode mode) {
		final int index = mode.ordinal();
		this.modeCombo.select(index);
		final ContextViewPanel contextViewPanel = this.panels.get(index);
		toggleContextViewPanel(contextViewPanel);
		return contextViewPanel;
	}

	public ContextViewPanel getCurrentPanel() {
		return this.panels.get(this.modeCombo.getSelectionIndex());
	}

	private void contributeToActionBars() {
		final IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		final Action expandAllAction = new Action() {
			@Override
			public void run() {
				getCurrentPanel().viewer.expandAll();
			}
		};
		expandAllAction.setImageDescriptor(AiCoderActivator.getImageDescriptor(AiCoderImageKey.EXPAND_ICON));
		manager.add(expandAllAction);
		final Action collapseAllAction = new Action() {
			@Override
			public void run() {
				getCurrentPanel().viewer.collapseAll();
			}
		};
		collapseAllAction.setImageDescriptor(AiCoderActivator.getImageDescriptor(AiCoderImageKey.COLLAPSE_ICON));
		manager.add(collapseAllAction);
		final Action toggleAction = new Action("Toggle Multiline", IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				final boolean enabled = isChecked();
				AiCoderPreferences.setMultilineEnabled(enabled);
				setChecked(enabled);
			}
		};
		toggleAction.setToolTipText("Toggle Multiline");
		toggleAction.setImageDescriptor(AiCoderActivator.getImageDescriptor(AiCoderImageKey.MULTILINE_ICON));
		toggleAction.setChecked(AiCoderPreferences.isMultilineEnabled());
		manager.add(toggleAction);
	}

	public static Optional<ContextView> get() throws CoreException {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		return workbench.getDisplay().syncCall(() -> {
			return Optional.ofNullable(workbench.getActiveWorkbenchWindow().getActivePage().findView(ID)).map(view -> (ContextView) view);
		});
	}

	public class ContextViewPanel {

		private CheckboxTreeViewer viewer;
		private DrillDownAdapter drillDownAdapter;
		private ContextEntry rootContextEntry;
		private final CompletionMode mode;

		public ContextViewPanel(CompletionMode mode) {
			this.mode = mode;
		}

		public void setRootContextEntry(ContextEntry rootContextEntry) {
			this.rootContextEntry = rootContextEntry;
			this.viewer.refresh();
		}

		public void createPartControl(Composite parent) {
			this.rootContextEntry = new EmptyContextEntry();

			this.viewer = new CheckboxTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CHECK);
			this.viewer.setLabelProvider(new DecoratingLabelProvider(new ViewLabelProvider(), new ViewLabelDecorator()));
			this.viewer.setContentProvider(new ViewContentProvider());
			this.viewer.setInput(getViewSite());
			this.viewer.addCheckStateListener(this::checkStateChanged);
			this.viewer.setCheckStateProvider(new ViewCheckStateProvider());
			this.drillDownAdapter = new DrillDownAdapter(this.viewer);

			ContextView.this.workbench.getHelpSystem().setHelp(this.viewer.getControl(), "de.hetzge.eclipse.aicoder.viewer");
			hookContextMenu();
			hookDoubleClickAction();
		}

		private void checkStateChanged(CheckStateChangedEvent event) {
			final Object element = event.getElement();
			final boolean checked = event.getChecked();
			updateParentCheckState(element);
			if (element instanceof final ContextEntry contextEntry) {
				if (isRootItem(contextEntry)) {
					ContextPreferences.get(this.mode).setContextTypeEnabled(contextEntry.getKey().prefix(), checked);
				} else {
					ContextPreferences.get(this.mode).setTemporaryDisabled(contextEntry.getKey(), this.viewer.getChecked(element));
				}
			}
		}

		private boolean isRootItem(ContextEntry entry) {
			return this.rootContextEntry.getChildContextEntries().contains(entry);
		}

		private void updateParentCheckState(Object element) {
			if (element instanceof final ContextEntry contextEntry) {
				final ContextEntry parent = findParent(contextEntry);
				if (parent != null) {
					final boolean allChecked = parent.getChildContextEntries().stream().allMatch(it -> this.viewer.getChecked(it));
					this.viewer.setGrayed(parent, !allChecked);
					updateParentCheckState(parent);
				}
			}
		}

		private ContextEntry findParent(ContextEntry contextEntry) {
			for (final ContextEntry child : this.rootContextEntry.getChildContextEntries()) {
				if (child.getChildContextEntries().contains(contextEntry)) {
					return child;
				}
				final ContextEntry parent = findParent(child, contextEntry);
				if (parent != null) {
					return parent;
				}
			}
			return null;
		}

		private ContextEntry findParent(ContextEntry parent, ContextEntry contextEntry) {
			for (final ContextEntry child : parent.getChildContextEntries()) {
				if (child.getChildContextEntries().contains(contextEntry)) {
					return child;
				}
				final ContextEntry foundParent = findParent(child, contextEntry);
				if (foundParent != null) {
					return foundParent;
				}
			}
			return null;
		}

		private void hookContextMenu() {
			final MenuManager menuManager = new MenuManager("#PopupMenu");
			menuManager.setRemoveAllWhenShown(true);
			menuManager.addMenuListener(this::fillContextMenu);
			final Menu menu = menuManager.createContextMenu(this.viewer.getControl());
			this.viewer.getControl().setMenu(menu);
			getSite().registerContextMenu(menuManager, this.viewer);
		}

		private void showContentPreview(ContextEntry entry) {
			final Shell shell = this.viewer.getControl().getShell();
			final String title = "Content Preview - " + entry.getLabel();
			final String content = ContextEntry.apply(entry, new ContextContext(ContextPreferences.get(this.mode)));
			new ContentPreviewDialog(shell, title, content).open();
		}

		private void hookDoubleClickAction() {
			this.viewer.addDoubleClickListener(event -> {
				final IStructuredSelection selection = this.viewer.getStructuredSelection();
				if (!selection.isEmpty() && selection.getFirstElement() instanceof ContextEntry) {
					final ContextEntry entry = (ContextEntry) selection.getFirstElement();
					showContentPreview(entry);
				}
			});
		}

		private void fillContextMenu(IMenuManager manager) {
			final IStructuredSelection selection = this.viewer.getStructuredSelection();
			if (!selection.isEmpty() && selection.getFirstElement() instanceof ContextEntry) {
				final List<ContextEntry> entries = selection.stream().filter(ContextEntry.class::isInstance).map(ContextEntry.class::cast).toList();
				final ContextEntry firstEntry = (ContextEntry) selection.getFirstElement();
				final ContextEntryKey key = firstEntry.getKey();

				if (firstEntry instanceof UserContextEntry) {
					final Action newAction = new Action("New Custom Context") {
						@Override
						public void run() {
							final CustomContextEntryDialog dialog = new CustomContextEntryDialog(ContextViewPanel.this.viewer.getControl().getShell(), null);
							if (dialog.open() == Dialog.OK) {
								final CustomContextEntryData newEntry = dialog.createEntry();
								final List<CustomContextEntryData> currentEntries = ContextPreferences.get(ContextViewPanel.this.mode).getCustomContextEntryDatas();
								final List<CustomContextEntryData> newEntries = new ArrayList<>(currentEntries);
								newEntries.add(newEntry);
								ContextPreferences.get(ContextViewPanel.this.mode).setCustomContextEntries(newEntries);
								ContextViewPanel.this.viewer.refresh(firstEntry);
							}
						}
					};
					manager.add(newAction);
				} else if (firstEntry instanceof CustomContextEntry) {
					final Action editAction = new Action("Edit custom context") {

						@Override
						public void run() {
							final CustomContextEntry customEntry = (CustomContextEntry) firstEntry;
							final CustomContextEntryDialog dialog = new CustomContextEntryDialog(ContextViewPanel.this.viewer.getControl().getShell(), customEntry);
							if (dialog.open() == Dialog.OK) {
								final CustomContextEntryData editedEntry = dialog.createEntry();
								final List<CustomContextEntryData> currentEntries = ContextPreferences.get(ContextViewPanel.this.mode).getCustomContextEntryDatas();
								final List<CustomContextEntryData> newEntries = new ArrayList<>(currentEntries);
								for (int i = 0; i < newEntries.size(); i++) {
									if (newEntries.get(i).getKey().equals(editedEntry.getKey())) {
										newEntries.set(i, editedEntry);
										break;
									}
								}
								ContextPreferences.get(ContextViewPanel.this.mode).setCustomContextEntries(newEntries);
								ContextViewPanel.this.viewer.refresh(firstEntry);
								ContextViewPanel.this.viewer.refresh(editedEntry);
							}
						}
					};
					manager.add(editAction);

					final Action removeAction = new Action("Remove custom context") {
						@Override
						public void run() {
							final CustomContextEntry customEntry = (CustomContextEntry) firstEntry;
							if (MessageDialog.openConfirm(ContextViewPanel.this.viewer.getControl().getShell(), "Confirm", "Are you sure?")) {
								final List<CustomContextEntryData> currentDatas = ContextPreferences.get(ContextViewPanel.this.mode).getCustomContextEntryDatas();
								final List<CustomContextEntryData> newEntries = currentDatas.stream().filter(it -> !Objects.equals(customEntry.getData(), it)).toList();
								ContextPreferences.get(ContextViewPanel.this.mode).setCustomContextEntries(newEntries);
								ContextViewPanel.this.viewer.refresh(firstEntry);
							}
						}
					};
					manager.add(removeAction);
				} else if (firstEntry instanceof CodeViewportMemoryContextEntry) {
					final Action resetAction = new Action("Reset") {
						@Override
						public void run() {
							EclipseUtils.executeCommand(ResetCodeViewportMemoryHandler.COMMAND_ID);
							ContextViewPanel.this.viewer.refresh(firstEntry);
						}
					};
					manager.add(resetAction);
				}

				final Action blacklistAction = new Action(ContextPreferences.get(this.mode).isBlacklisted(key) ? "Remove from Blacklist" : "Add to Blacklist") {
					@Override
					public void run() {
						final boolean isBlacklisted = ContextPreferences.get(ContextViewPanel.this.mode).isBlacklisted(key);
						for (final ContextEntry contextEntry : entries) {
							if (isBlacklisted) {
								ContextPreferences.get(ContextViewPanel.this.mode).removeFromBlacklist(contextEntry.getKey());
							} else {
								ContextPreferences.get(ContextViewPanel.this.mode).addToBlacklist(contextEntry.getKey());
							}
						}
						if (isBlacklisted) {
							showMessage("Removed from blacklist");
						} else {
							showMessage("Added to blacklist");
						}
						ContextViewPanel.this.viewer.resetFilters();
						ContextViewPanel.this.viewer.refresh(true);
					}
				};

				final Action stickyAction = new Action(ContextPreferences.get(this.mode).isSticky(key) ? "Remove Sticky" : "Make Sticky") {
					@Override
					public void run() {
						final boolean isSticky = ContextPreferences.get(ContextViewPanel.this.mode).isSticky(key);
						for (final ContextEntry contextEntry : entries) {
							if (isSticky) {
								ContextPreferences.get(ContextViewPanel.this.mode).removeFromStickylist(contextEntry.getKey());
							} else {
								ContextPreferences.get(ContextViewPanel.this.mode).addToStickylist(contextEntry.getKey());
							}
						}
						if (isSticky) {
							showMessage("Removed sticky");
						} else {
							showMessage("Made sticky");
						}
						ContextViewPanel.this.viewer.resetFilters();
						ContextViewPanel.this.viewer.refresh(true);
					}
				};

				final Action previewAction = new Action("Preview Content") {
					@Override
					public void run() {
						showContentPreview(firstEntry);
					}
				};

				manager.add(blacklistAction);
				manager.add(stickyAction);
				manager.add(previewAction);
				manager.add(new Separator());
			}

			this.drillDownAdapter.addNavigationActions(manager);
			manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		}

		private void showMessage(String message) {
			MessageDialog.openInformation(
					this.viewer.getControl().getShell(),
					"AI Coder Context",
					message);
		}

		public void setFocus() {
			this.viewer.getControl().setFocus();
		}

		public static ContextView open() throws CoreException {
			try {
				final IWorkbench workbench = PlatformUI.getWorkbench();
				return workbench.getDisplay().syncCall(() -> {
					return (ContextView) workbench.getActiveWorkbenchWindow().getActivePage().showView(ID);
				});
			} catch (final PartInitException exception) {
				throw new CoreException(Status.error("Failed to open view", exception));
			}
		}

		private final class ViewCheckStateProvider implements ICheckStateProvider {
			@Override
			public boolean isChecked(Object element) {
				if (element instanceof final ContextEntry contextEntry) {
					return !ContextPreferences.get(ContextViewPanel.this.mode).isBlacklisted(contextEntry.getKey())
							&& (isRootItem(contextEntry) || !ContextPreferences.get(ContextViewPanel.this.mode).isTemporaryDisabled(contextEntry.getKey()))
							&& (!isRootItem(contextEntry) || ContextPreferences.get(ContextViewPanel.this.mode).isContextTypeEnabled(contextEntry.getKey().getKeyString()));
				}
				return true;
			}

			@Override
			public boolean isGrayed(Object element) {
				if (element instanceof final ContextEntry contextEntry) {
					if (!contextEntry.getChildContextEntries().isEmpty()) {
						return !contextEntry.getChildContextEntries().stream().allMatch(this::isChecked);
					}
				}
				return false;
			}
		}

		private class ViewContentProvider implements ITreeContentProvider {
			@Override
			public Object[] getElements(Object parent) {
				if (parent.equals(getViewSite())) {
					BlacklistedContextEntry blacklistedContextEntry = null;
					try {
						blacklistedContextEntry = BlacklistedContextEntry.create(new ContextContext(ContextPreferences.get(ContextViewPanel.this.mode)));
					} catch (final CoreException exception) {
						throw new RuntimeException("Failed to create blacklisted context entry", exception);
					}
					return Stream.concat(ContextViewPanel.this.rootContextEntry.getChildContextEntries().stream(), Stream.of(blacklistedContextEntry).filter(Objects::nonNull)).toArray();
				}
				return getChildren(parent);
			}

			@Override
			public Object[] getChildren(Object parent) {
				if (parent instanceof final ContextEntry contextEntry) {
					return contextEntry.getChildContextEntries().toArray();
				}
				return new Object[0];
			}

			@Override
			public boolean hasChildren(Object parent) {
				if (parent instanceof final ContextEntry contextEntry) {
					return !contextEntry.getChildContextEntries().isEmpty();
				}
				return false;
			}

			@Override
			public Object getParent(Object child) {
				return null;
			}
		}

		private class ViewLabelProvider extends LabelProvider implements IColorProvider, IFontProvider {

			private static final Color BLACKLISTED_BACKGROUND_COLOR = new Color(255, 240, 240);
			private static final Color BLACKLISTED_FOREGROUND_COLOR = new Color(100, 100, 100);
			private static final Color STICKY_BACKGROUND_COLOR = new Color(240, 255, 240);
			private static final Color STICKY_FOREGROUND_COLOR = new Color(0, 0, 0);
			private static final Color SKIPPED_BACKGROUND_COLOR = null;
			private static final Color SKIPPED_FOREGROUND_COLOR = new Color(200, 200, 200);

			@Override
			public String getText(Object obj) {
				if (obj instanceof final ContextEntry contextEntry) {
					return contextEntry.getLabel();
				}
				return obj.toString();
			}

			@Override
			public Image getImage(Object obj) {
				if (obj instanceof final ContextEntry contextEntry) {
					return contextEntry.getImage();
				}
				return ContextView.this.workbench.getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
			}

			@Override
			public Color getForeground(Object element) {
				if (element instanceof final ContextEntry contextEntry) {
					final ContextEntryKey key = contextEntry.getKey();
					if (ContextPreferences.get(ContextViewPanel.this.mode).isBlacklisted(key)) {
						return BLACKLISTED_FOREGROUND_COLOR;
					} else if (contextEntry.getTokenCount() == 0) {
						return SKIPPED_FOREGROUND_COLOR;
					} else if (ContextPreferences.get(ContextViewPanel.this.mode).isSticky(key)) {
						return STICKY_FOREGROUND_COLOR;
					}
				}
				return null;
			}

			@Override
			public Color getBackground(Object element) {
				if (element instanceof final ContextEntry contextEntry) {
					final ContextEntryKey key = contextEntry.getKey();
					if (ContextPreferences.get(ContextViewPanel.this.mode).isBlacklisted(key)) {
						return BLACKLISTED_BACKGROUND_COLOR;
					} else if (contextEntry.getTokenCount() == 0) {
						return SKIPPED_BACKGROUND_COLOR;
					} else if (ContextPreferences.get(ContextViewPanel.this.mode).isSticky(key)) {
						return STICKY_BACKGROUND_COLOR;
					}
				}
				return null;
			}

			@Override
			public Font getFont(Object element) {
				if (element instanceof final ContextEntry contextEntry) {
					final ContextEntryKey key = contextEntry.getKey();
					if (ContextPreferences.get(ContextViewPanel.this.mode).isBlacklisted(key)) {
						return JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);
					} else if (contextEntry.getTokenCount() == 0) {
						return JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);
					} else if (ContextPreferences.get(ContextViewPanel.this.mode).isSticky(key)) {
						return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
					}
				}
				return null;
			}
		}

		private final class ViewLabelDecorator extends LabelDecorator {
			@Override
			public void removeListener(ILabelProviderListener listener) {
			}

			@Override
			public boolean isLabelProperty(Object element, String property) {
				return false;
			}

			@Override
			public void dispose() {
			}

			@Override
			public void addListener(ILabelProviderListener listener) {
			}

			@Override
			public String decorateText(String text, Object element) {
				return null;
			}

			@Override
			public Image decorateImage(Image image, Object element) {
				return null;
			}

			@Override
			public boolean prepareDecoration(Object element, String originalText, IDecorationContext context) {
				return true;
			}

			@Override
			public String decorateText(String text, Object element, IDecorationContext context) {
				if (element instanceof final ContextEntry contextEntry) {
					final ContextEntryKey key = contextEntry.getKey();
					String tag = "";
					if (ContextPreferences.get(ContextViewPanel.this.mode).isBlacklisted(key)) {
						tag += " [Blacklisted]";
					}
					if (ContextPreferences.get(ContextViewPanel.this.mode).isSticky(key)) {
						tag += " [Sticky]";
					}
					final Duration duration = contextEntry.getCreationDuration();
					final long seconds = duration.getSeconds();
					final long absSeconds = Math.abs(seconds);
					final String formattedDuration = formatDuration(absSeconds);
					return String.format("%s%s (chars: %s, duration: %s, children: %s)", text, tag, contextEntry.getTokenCount(), formattedDuration, contextEntry.getChildContextEntries().size());
				}
				return null;
			}

			private String formatDuration(final long absSeconds) {
				if (absSeconds < 60) {
					return String.format("%ds", absSeconds);
				} else if (absSeconds < 3600) {
					final long minutes = absSeconds / 60;
					final long remainingSeconds = absSeconds % 60;
					return String.format("%dmin %ds", minutes, remainingSeconds);
				} else {
					final long hours = absSeconds / 3600;
					final long remainingMinutes = (absSeconds % 3600) / 60;
					final long remainingSeconds = absSeconds % 60;
					return String.format("%dh %dmin %ds", hours, remainingMinutes, remainingSeconds);
				}
			}

			@Override
			public Image decorateImage(Image image, Object element, IDecorationContext context) {
				return null;
			}
		}
	}

	private static class OpenContextPreferencesAction extends Action {
		private OpenContextPreferencesAction() {
			super("Context preferences");
		}

		@Override
		public void run() {
			final PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null, ContextPreferencePage.ID, null, null);
			dialog.open();
		}
	}
}