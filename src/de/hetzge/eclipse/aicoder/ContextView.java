package de.hetzge.eclipse.aicoder;

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
            final ContextEntry entry = (ContextEntry) selection.getFirstElement();
            final ContextEntryKey key = entry.getKey();

            final Action blacklistAction = new Action(ContextPreferences.isBlacklisted(key) ? "Remove from Blacklist" : "Add to Blacklist") {
                @Override
                public void run() {
                    if (ContextPreferences.isBlacklisted(key)) {
                        ContextPreferences.removeFromBlacklist(key);
                        showMessage("Removed from blacklist: " + entry.getLabel());
                    } else {
                        ContextPreferences.addToBlacklist(key);
                        showMessage("Added to blacklist: " + entry.getLabel());
                    }
                    ContextView.this.viewer.refresh(entry);
                }
            };

            final Action stickyAction = new Action(ContextPreferences.isSticky(key) ? "Remove Sticky" : "Make Sticky") {
                @Override
                public void run() {
                    if (ContextPreferences.isSticky(key)) {
                        ContextPreferences.removeFromStickylist(key);
                        showMessage("Removed sticky: " + entry.getLabel());
                    } else {
                        ContextPreferences.addToStickylist(key);
                        showMessage("Made sticky: " + entry.getLabel());
                    }
                    ContextView.this.viewer.refresh(entry);
                }
            };

            final Action previewAction = new Action("Preview Content") {
                @Override
                public void run() {
                    showContentPreview(entry);
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

                this.textArea = new Text(container, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
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
		@Override
		public String getText(Object obj) {
			if (obj instanceof final ContextEntry contextEntry) {
				String label = contextEntry.getLabel();
				final String key = contextEntry.getKey();

				// Add indicators for blacklisted and sticky items
				if (ContextPreferences.isBlacklisted(key)) {
					label += " [Blacklisted]";
				}
				if (ContextPreferences.isSticky(key)) {
					label += " [Sticky]";
				}
				return label;
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
				final String key = contextEntry.getKey();
				if (ContextPreferences.isBlacklisted(key)) {
					// Use a gray color for blacklisted items
					return ContextView.this.viewer.getControl().getDisplay().getSystemColor(SWT.COLOR_GRAY);
				}
			}
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			if (element instanceof final ContextEntry contextEntry) {
				final String key = contextEntry.getKey();
				if (ContextPreferences.isSticky(key)) {
					// Use a light yellow background for sticky items
					return ContextView.this.viewer.getControl().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
				}
			}
			return null;
		}

		@Override
		public Font getFont(Object element) {
			if (element instanceof final ContextEntry contextEntry) {
				final String key = contextEntry.getKey();
				if (ContextPreferences.isSticky(key)) {
					// Make sticky items bold
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
			return false;
		}

		@Override
		public String decorateText(String text, Object element, IDecorationContext context) {
			if (element instanceof final ContextEntry contextEntry) {
				return String.format("%s (%s)", text, contextEntry.getTokenCount());
			}
			return null;
		}

		@Override
		public Image decorateImage(Image image, Object element, IDecorationContext context) {
			return null;
		}
	}
}