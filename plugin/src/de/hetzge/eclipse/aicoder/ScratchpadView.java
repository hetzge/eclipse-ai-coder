package de.hetzge.eclipse.aicoder;

import java.util.Optional;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import de.hetzge.eclipse.aicoder.util.EclipseUtils;

/**
 * View containing the scratchpad text editor.
 */
public class ScratchpadView extends ViewPart {

	public static final String ID = "de.hetzge.eclipse.aicoder.ScratchpadView";

	private StyledText styledText;

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		this.styledText = new StyledText(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		this.styledText.setText(ScratchpadStorage.getContent());
		this.styledText.addModifyListener(event -> ScratchpadStorage.setContent(this.styledText.getText()));
		contributeToActionBars();
	}

	@Override
	public void setFocus() {
		if (this.styledText != null) {
			this.styledText.setFocus();
		}
	}

	private void contributeToActionBars() {
		final IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();

		final Action toggleAction = new Action("Toggle Scratchpad", IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				final boolean enabled = isChecked();
				ScratchpadStorage.setEnabled(enabled);
				setChecked(enabled);
			}
		};
		toggleAction.setToolTipText("Enable/Disable scratchpad context");
		toggleAction.setImageDescriptor(AiCoderActivator.getImageDescriptor(AiCoderImageKey.SCRATCHPAD_ICON));
		toggleAction.setChecked(ScratchpadStorage.isEnabled());
		toolBarManager.add(toggleAction);

		final Action clearAction = new Action("Clear") {
			@Override
			public void run() {
				final boolean confirmed = MessageDialog.openConfirm(getViewSite().getShell(), "Clear Scratchpad", "Clear the scratchpad content?");
				if (confirmed) {
					ScratchpadView.this.styledText.setText("");
					ScratchpadStorage.setContent("");
				}
			}
		};
		clearAction.setToolTipText("Clear the scratchpad");
		clearAction.setImageDescriptor(AiCoderActivator.getImageDescriptor(AiCoderImageKey.REJECT_ICON));
		toolBarManager.add(clearAction);
	}

	public static ScratchpadView openView() throws PartInitException {
		final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		final ScratchpadView view = (ScratchpadView) activePage.showView(ID);
		view.setFocus();
		return view;
	}

	public static Optional<ScratchpadView> findView() {
		return EclipseUtils.syncCall(() -> Optional.ofNullable(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ID)).map(ScratchpadView.class::cast));
	}

	public static void refreshContentIfOpen() {
		EclipseUtils.asyncExec(() -> {
			findView().ifPresent(view -> {
				if (view.styledText != null) {
					view.styledText.setText(ScratchpadStorage.getContent());
				}
			});
		});
	}
}
