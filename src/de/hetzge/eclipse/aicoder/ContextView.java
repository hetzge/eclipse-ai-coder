package de.hetzge.eclipse.aicoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import de.hetzge.eclipse.aicoder.Context.ContextEntry;
import de.hetzge.eclipse.aicoder.Context.ContextEntryKey;
import de.hetzge.eclipse.aicoder.Context.CustomContextEntry;
import de.hetzge.eclipse.aicoder.Context.EmptyContextEntry;
import de.hetzge.eclipse.aicoder.Context.TokenCounter;
import jakarta.inject.Inject;

public class ContextView extends ViewPart {
	public static final String ID = "de.hetzge.eclipse.aicoder.ContextView";

	@Inject
	IWorkbench workbench;

	private TreeViewer viewer;
	private DrillDownAdapter drillDownAdapter;
	private Action doubleClickAction;

	private ContextEntry rootContextEntry;

	public void setRootContextEntry(ContextEntry rootContextEntry) {
		this.rootContextEntry = rootContextEntry;
		this.viewer.refresh();
		this.viewer.expandToLevel(2);
	}

	@Override
	public void createPartControl(Composite parent) {
		this.rootContextEntry = new EmptyContextEntry();

		this.viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		this.drillDownAdapter = new DrillDownAdapter(this.viewer);

		this.viewer.setLabelProvider(new DecoratingLabelProvider(new ViewLabelProvider(), new ViewLabelDecorator()));
		this.viewer.setContentProvider(new ViewContentProvider());
		this.viewer.setInput(getViewSite());

		// Create the help context id for the viewer's control
		this.workbench.getHelpSystem().setHelp(this.viewer.getControl(), "de.hetzge.eclipse.aicoder.viewer");
		getSite().setSelectionProvider(this.viewer);
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void hookContextMenu() {
		final MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				ContextView.this.fillContextMenu(manager);
			}
		});
		final Menu menu = menuMgr.createContextMenu(this.viewer.getControl());
		this.viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, this.viewer);
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

			if (firstEntry instanceof Context.UserContextEntry) {
				final Action newAction = new Action("New Custom Context") {
					@Override
					public void run() {
						final CustomContextEntryDialog dialog = new CustomContextEntryDialog(ContextView.this.viewer.getControl().getShell(), null);
						if (dialog.open() == Dialog.OK) {
							final CustomContextEntry newEntry = dialog.createEntry();
							final List<CustomContextEntry> currentEntries = ContextPreferences.getCustomContextEntries();
							final List<CustomContextEntry> newEntries = new ArrayList<>(currentEntries);
							newEntries.add(newEntry);
							ContextPreferences.setCustomContextEntries(newEntries);
							ContextView.this.viewer.refresh(firstEntry);
						}
					}
				};
				manager.add(newAction);
			} else if (firstEntry instanceof Context.CustomContextEntry) {
				final Action editAction = new Action("Edit Custom Context") {
					@Override
					public void run() {
						final CustomContextEntry customEntry = (CustomContextEntry) firstEntry;
						final CustomContextEntryDialog dialog = new CustomContextEntryDialog(ContextView.this.viewer.getControl().getShell(), customEntry);
						if (dialog.open() == Dialog.OK) {
							final CustomContextEntry editedEntry = dialog.createEntry();
							final List<CustomContextEntry> currentEntries = ContextPreferences.getCustomContextEntries();
							final List<CustomContextEntry> newEntries = new ArrayList<>(currentEntries);
							// Replace the existing entry with the edited one
							for (int i = 0; i < newEntries.size(); i++) {
								if (newEntries.get(i).getId().equals(editedEntry.getId())) {
									newEntries.set(i, editedEntry);
									break;
								}
							}
							ContextPreferences.setCustomContextEntries(newEntries);
							ContextView.this.viewer.refresh(firstEntry);
						}
					}
				};
				manager.add(editAction);
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
	}

	private void makeActions() {
		// Double click action shows preview
		this.doubleClickAction = new Action() {
			@Override
			public void run() {
				final IStructuredSelection selection = ContextView.this.viewer.getStructuredSelection();
				if (!selection.isEmpty() && selection.getFirstElement() instanceof ContextEntry) {
					final ContextEntry entry = (ContextEntry) selection.getFirstElement();
					showContentPreview(entry);
				}
			}
		};
	}

	private void showContentPreview(ContextEntry entry) {
		final Shell shell = this.viewer.getControl().getShell();
		final Dialog dialog = new Dialog(shell) {
			private Text textArea;

			@Override
			protected Control createDialogArea(Composite parent) {
				final Composite container = (Composite) super.createDialogArea(parent);

				this.textArea = new Text(container, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
				this.textArea.setLayoutData(new GridData(GridData.FILL_BOTH));

				// Get the content
				final StringBuilder content = new StringBuilder();
				entry.apply(content, new TokenCounter(Integer.MAX_VALUE));
				this.textArea.setText(content.toString());

				// Make the text read-only
				this.textArea.setEditable(false);

				// Set a reasonable size
				final GridData gd = (GridData) this.textArea.getLayoutData();
				gd.widthHint = 600;
				gd.heightHint = 400;

				return container;
			}

			@Override
			protected void configureShell(Shell newShell) {
				super.configureShell(newShell);
				newShell.setText("Content Preview - " + entry.getLabel());
			}

			@Override
			protected boolean isResizable() {
				return true;
			}
		};

		dialog.open();
	}

	private void hookDoubleClickAction() {
		this.viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				ContextView.this.doubleClickAction.run();
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

	private class ViewContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			if (parent.equals(getViewSite())) {
				return new Object[] { ContextView.this.rootContextEntry };
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
				return String.format("%s%s (%s)", text, tag, contextEntry.getTokenCount());
			}
			return null;
		}

		@Override
		public Image decorateImage(Image image, Object element, IDecorationContext context) {
			return null;
		}
	}
}