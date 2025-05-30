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
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import de.hetzge.eclipse.aicoder.Context.ContextEntry;
import de.hetzge.eclipse.aicoder.Context.EmptyContextEntry;
import de.hetzge.eclipse.aicoder.Context.TokenCounter;
import jakarta.inject.Inject;

public class ContextView extends ViewPart {
	public static final String ID = "de.hetzge.eclipse.aicoder.ContextView";

	@Inject
	IWorkbench workbench;

	private TreeViewer viewer;
	private DrillDownAdapter drillDownAdapter;
	private Action action1;
	private Action action2;
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
		manager.add(this.action1);
		manager.add(new Separator());
		manager.add(this.action2);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(this.action1);
		manager.add(this.action2);
		manager.add(new Separator());
		this.drillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(this.action1);
		manager.add(this.action2);
		manager.add(new Separator());
		this.drillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions() {
		this.action1 = new Action() {
			@Override
			public void run() {
				showMessage("Action 1 executed");
			}
		};
		this.action1.setText("Action 1");
		this.action1.setToolTipText("Action 1 tooltip");
		this.action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

		this.action2 = new Action() {
			@Override
			public void run() {
				showMessage("Action 2 executed");
			}
		};
		this.action2.setText("Action 2");
		this.action2.setToolTipText("Action 2 tooltip");
		this.action2.setImageDescriptor(this.workbench.getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		this.doubleClickAction = new Action() {
			@Override
			public void run() {
				final IStructuredSelection selection = ContextView.this.viewer.getStructuredSelection();
				final ContextEntry obj = (ContextEntry) selection.getFirstElement();

				final StringBuilder builder = new StringBuilder();
				obj.apply(builder, new TokenCounter(Integer.MAX_VALUE));

				showMessage("Double-click detected on " + builder);
			}
		};
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
			return null;
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}

		@Override
		public Font getFont(Object element) {
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
