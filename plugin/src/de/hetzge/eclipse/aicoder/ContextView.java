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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
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
import de.hetzge.eclipse.aicoder.context.ContextContext;
import de.hetzge.eclipse.aicoder.context.ContextEntry;
import de.hetzge.eclipse.aicoder.context.ContextEntryKey;
import de.hetzge.eclipse.aicoder.context.CustomContextEntry;
import de.hetzge.eclipse.aicoder.context.CustomContextEntryData;
import de.hetzge.eclipse.aicoder.context.EmptyContextEntry;
import de.hetzge.eclipse.aicoder.context.UserContextEntry;
import de.hetzge.eclipse.aicoder.handler.ToggleMultilineHandler;
import de.hetzge.eclipse.aicoder.preferences.AiCoderPreferences;
import de.hetzge.eclipse.aicoder.preferences.ContextPreferencePage;
import de.hetzge.eclipse.aicoder.preferences.ContextPreferences;
import jakarta.inject.Inject;

public class ContextView extends ViewPart {
	public static final String ID = "de.hetzge.eclipse.aicoder.ContextView";

	@Inject
	IWorkbench workbench;

	private CheckboxTreeViewer viewer;
	private DrillDownAdapter drillDownAdapter;
	private ContextEntry rootContextEntry;

	public void setRootContextEntry(ContextEntry rootContextEntry) {
		this.rootContextEntry = rootContextEntry;
		this.viewer.refresh();
	}

	@Override
	public void createPartControl(Composite parent) {
		this.rootContextEntry = new EmptyContextEntry();

		this.viewer = new CheckboxTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CHECK);
		this.viewer.setLabelProvider(new DecoratingLabelProvider(new ViewLabelProvider(), new ViewLabelDecorator()));
		this.viewer.setContentProvider(new ViewContentProvider());
		this.viewer.setInput(getViewSite());
		this.viewer.addCheckStateListener(this::checkStateChanged);
		this.viewer.setCheckStateProvider(new ViewCheckStateProvider());
		this.drillDownAdapter = new DrillDownAdapter(this.viewer);

		// Create the help context id for the viewer's control
		this.workbench.getHelpSystem().setHelp(this.viewer.getControl(), "de.hetzge.eclipse.aicoder.viewer");
		getSite().setSelectionProvider(this.viewer);
		hookContextMenu();
		hookDoubleClickAction();
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

	private void checkStateChanged(CheckStateChangedEvent event) {
		final Object element = event.getElement();
		final boolean checked = event.getChecked();
		updateParentCheckState(element);
		if (element instanceof final ContextEntry contextEntry) {
			if (isRootItem(contextEntry)) {
				ContextPreferences.setContextTypeEnabled(contextEntry.getKey().prefix(), checked);
			} else {
				ContextPreferences.setTemporaryDisabled(contextEntry.getKey(), this.viewer.getChecked(element));
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
		menuManager.addMenuListener(ContextView.this::fillContextMenu);
		final Menu menu = menuManager.createContextMenu(this.viewer.getControl());
		this.viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuManager, this.viewer);
	}

	private void contributeToActionBars() {
		final IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		// Empty as we don't need any pull-down menu items
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
						final CustomContextEntryDialog dialog = new CustomContextEntryDialog(ContextView.this.viewer.getControl().getShell(), null);
						if (dialog.open() == Dialog.OK) {
							final CustomContextEntryData newEntry = dialog.createEntry();
							final List<CustomContextEntryData> currentEntries = ContextPreferences.getCustomContextEntryDatas();
							final List<CustomContextEntryData> newEntries = new ArrayList<>(currentEntries);
							newEntries.add(newEntry);
							ContextPreferences.setCustomContextEntries(newEntries);
							ContextView.this.viewer.refresh(firstEntry);
						}
					}
				};
				manager.add(newAction);
			} else if (firstEntry instanceof CustomContextEntry) {
				final Action editAction = new Action("Edit custom context") {

					@Override
					public void run() {
						final CustomContextEntry customEntry = (CustomContextEntry) firstEntry;
						final CustomContextEntryDialog dialog = new CustomContextEntryDialog(ContextView.this.viewer.getControl().getShell(), customEntry);
						if (dialog.open() == Dialog.OK) {
							final CustomContextEntryData editedEntry = dialog.createEntry();
							final List<CustomContextEntryData> currentEntries = ContextPreferences.getCustomContextEntryDatas();
							final List<CustomContextEntryData> newEntries = new ArrayList<>(currentEntries);
							// Replace the existing entry with the edited one
							for (int i = 0; i < newEntries.size(); i++) {
								if (newEntries.get(i).getId().equals(editedEntry.getId())) {
									newEntries.set(i, editedEntry);
									break;
								}
							}
							ContextPreferences.setCustomContextEntries(newEntries);
							ContextView.this.viewer.refresh(firstEntry);
							ContextView.this.viewer.refresh(editedEntry);
						}
					}
				};
				manager.add(editAction);

				final Action removeAction = new Action("Remove custom context") {
					@Override
					public void run() {
						final CustomContextEntry customEntry = (CustomContextEntry) firstEntry;
						if (MessageDialog.openConfirm(ContextView.this.viewer.getControl().getShell(), "Confirm", "Are you sure?")) {
							final List<CustomContextEntryData> currentDatas = ContextPreferences.getCustomContextEntryDatas();
							final List<CustomContextEntryData> newEntries = currentDatas.stream().filter(it -> !Objects.equals(customEntry.getData(), it)).toList();
							ContextPreferences.setCustomContextEntries(newEntries);
							ContextView.this.viewer.refresh(firstEntry);
						}
					}
				};
				manager.add(removeAction);
			}

			final Action blacklistAction = new Action(ContextPreferences.isBlacklisted(key) ? "Remove from Blacklist" : "Add to Blacklist") {
				@Override
				public void run() {
					final boolean isBlacklisted = ContextPreferences.isBlacklisted(key);
					for (final ContextEntry contextEntry : entries) {
						if (isBlacklisted) {
							ContextPreferences.removeFromBlacklist(contextEntry.getKey());
						} else {
							ContextPreferences.addToBlacklist(contextEntry.getKey());
						}
					}
					if (isBlacklisted) {
						showMessage("Removed from blacklist");
					} else {
						showMessage("Added to blacklist");
					}
					ContextView.this.viewer.resetFilters();
					ContextView.this.viewer.refresh(true);
				}
			};

			final Action stickyAction = new Action(ContextPreferences.isSticky(key) ? "Remove Sticky" : "Make Sticky") {
				@Override
				public void run() {
					final boolean isSticky = ContextPreferences.isSticky(key);
					for (final ContextEntry contextEntry : entries) {
						if (isSticky) {
							ContextPreferences.removeFromStickylist(contextEntry.getKey());
						} else {
							ContextPreferences.addToStickylist(contextEntry.getKey());
						}
					}
					if (isSticky) {
						showMessage("Removed sticky");
					} else {
						showMessage("Made sticky");
					}
					ContextView.this.viewer.resetFilters();
					ContextView.this.viewer.refresh(true);
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

	private void fillLocalToolBar(IToolBarManager manager) {
		this.drillDownAdapter.addNavigationActions(manager);
		final Action expandAllAction = new Action() {
			@Override
			public void run() {
				ContextView.this.viewer.expandAll();
			}
		};
		expandAllAction.setImageDescriptor(AiCoderActivator.getImageDescriptor(AiCoderImageKey.EXPAND_ICON));
		manager.add(expandAllAction);
		final Action collapseAllAction = new Action() {
			@Override
			public void run() {
				ContextView.this.viewer.collapseAll();
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

	private void showContentPreview(ContextEntry entry) {
		final Shell shell = this.viewer.getControl().getShell();
		final String title = "Content Preview - " + entry.getLabel();
		final String content = ContextEntry.apply(entry, new ContextContext());
		new ContentPreviewDialog(shell, title, content).open();
	}

	private void hookDoubleClickAction() {
		this.viewer.addDoubleClickListener(event -> {
			final IStructuredSelection selection = ContextView.this.viewer.getStructuredSelection();
			if (!selection.isEmpty() && selection.getFirstElement() instanceof ContextEntry) {
				final ContextEntry entry = (ContextEntry) selection.getFirstElement();
				showContentPreview(entry);
			}
		});
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(
				this.viewer.getControl().getShell(),
				"AI Coder Context",
				message);
	}

	@Override
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

	public static Optional<ContextView> get() throws CoreException {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		return workbench.getDisplay().syncCall(() -> {
			return Optional.ofNullable(workbench.getActiveWorkbenchWindow().getActivePage().findView(ID)).map(view -> (ContextView) view);
		});
	}

	private final class ViewCheckStateProvider implements ICheckStateProvider {
		@Override
		public boolean isChecked(Object element) {
			if (element instanceof final ContextEntry contextEntry) {
				return !ContextPreferences.isBlacklisted(contextEntry.getKey())
						&& (isRootItem(contextEntry) || !ContextPreferences.isTemporaryDisabled(contextEntry.getKey()))
						&& (!isRootItem(contextEntry) || ContextPreferences.isContextTypeEnabled(contextEntry.getKey().getKeyString()));
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

	private class ViewContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			if (parent.equals(getViewSite())) {
				BlacklistedContextEntry blacklistedContextEntry = null;
				try {
					blacklistedContextEntry = BlacklistedContextEntry.create();
				} catch (final CoreException exception) {
					throw new RuntimeException("Failed to create blacklisted context entry", exception);
				}
				return Stream.concat(ContextView.this.rootContextEntry.getChildContextEntries().stream(), Stream.of(blacklistedContextEntry).filter(Objects::nonNull)).toArray();
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
		private static final Color SKIPPED_BACKGROUND_COLOR = new Color(0, 0, 0, 0);
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
				if (ContextPreferences.isBlacklisted(key)) {
					return BLACKLISTED_FOREGROUND_COLOR;
				} else if (contextEntry.getTokenCount() == 0) {
					return SKIPPED_FOREGROUND_COLOR;
				} else if (ContextPreferences.isSticky(key)) {
					return STICKY_FOREGROUND_COLOR;
				}
			}
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			if (element instanceof final ContextEntry contextEntry) {
				final ContextEntryKey key = contextEntry.getKey();
				if (ContextPreferences.isBlacklisted(key)) {
					return BLACKLISTED_BACKGROUND_COLOR;
				} else if (contextEntry.getTokenCount() == 0) {
					return SKIPPED_BACKGROUND_COLOR;
				} else if (ContextPreferences.isSticky(key)) {
					return STICKY_BACKGROUND_COLOR;
				}
			}
			return null;
		}

		@Override
		public Font getFont(Object element) {
			if (element instanceof final ContextEntry contextEntry) {
				final ContextEntryKey key = contextEntry.getKey();
				if (ContextPreferences.isBlacklisted(key)) {
					return JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);
				} else if (contextEntry.getTokenCount() == 0) {
					return JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);
				} else if (ContextPreferences.isSticky(key)) {
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
				if (ContextPreferences.isBlacklisted(key)) {
					tag += " [Blacklisted]";
				}
				if (ContextPreferences.isSticky(key)) {
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