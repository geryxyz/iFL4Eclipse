package org.eclipse.sed.ifl.ide.gui;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.sed.ifl.control.score.filter.BooleanRule;
import org.eclipse.sed.ifl.control.score.filter.DoubleRule;
import org.eclipse.sed.ifl.control.score.filter.LastActionRule;
import org.eclipse.sed.ifl.control.score.filter.Rule;
import org.eclipse.sed.ifl.control.score.filter.StringRule;
import org.eclipse.sed.ifl.general.IEmbeddable;
import org.eclipse.sed.ifl.general.IEmbedee;
import org.eclipse.sed.ifl.ide.gui.dialogs.RuleCreatorDialog;
import org.eclipse.sed.ifl.ide.gui.element.RuleElementUI;
import org.eclipse.sed.ifl.util.event.IListener;
import org.eclipse.sed.ifl.util.event.INonGenericListenerCollection;
import org.eclipse.sed.ifl.util.event.core.NonGenericListenerCollection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Label;

public class FilterPart extends ViewPart implements IEmbeddable, IEmbedee {

	public static final String ID = "org.eclipse.sed.ifl.views.IFLFilterView";
	
	@Inject IWorkbench workbench;
	
	private Composite composite;
	private Button addRuleButton;
	private ScrolledComposite scrolledComposite;
	private Button resetAllButton;
	private Composite rulesComposite;
	
	private List<Rule> rules = new ArrayList<>();
	
	public FilterPart() {
		System.out.println("filter part ctr");
	}

	
	@Override
	public void embed(IEmbeddable embedded) {
		embedded.setParent(composite);
	}

	@Override
	public void setParent(Composite parent) {
		composite.setParent(parent);
	}

	@Override
	public void createPartControl(Composite parent) {
		composite = parent;
		composite.setLayout(new GridLayout(2, false));
		
		enableInfoLabel = new Label(parent, SWT.NONE);
		enableInfoLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		enableInfoLabel.setText("Load some defined scores to enable filtering.");
		
		addRuleButton = new Button(parent, SWT.NONE);
		addRuleButton.setEnabled(false);
		addRuleButton.setText("Add rule");
		addRuleButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				RuleCreatorDialog ruleDialog = new RuleCreatorDialog(Display.getCurrent().getActiveShell(), SWT.NONE);
				ruleDialog.eventRuleCreated().add(ruleCreatedListener);
				ruleDialog.open();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			
		});
		
		resetAllButton = new Button(parent, SWT.NONE);
		resetAllButton.setEnabled(false);
		resetAllButton.setText("Reset all");
		resetAllButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteRules.invoke(rules);
				for(Control control : rulesComposite.getChildren()) {
					control.dispose();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			
		});
		
		scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridData gd_scrolledComposite = new GridData(SWT.CENTER, SWT.CENTER, false, false, 2, 1);
		 gd_scrolledComposite.widthHint = 360;
		 gd_scrolledComposite.heightHint = 280;
		scrolledComposite.setLayoutData( gd_scrolledComposite);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		
		rulesComposite = new Composite(scrolledComposite, SWT.NONE);
		rulesComposite.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		rulesComposite.setSize(new Point(360, 280));
		rulesComposite.setLayout(new GridLayout(1, false));
		scrolledComposite.setContent(rulesComposite);
		scrolledComposite.setMinSize(rulesComposite.getSize());
	}

	private NonGenericListenerCollection<List<Rule>> deleteRules = new NonGenericListenerCollection<>();
	
	public INonGenericListenerCollection<List<Rule>> eventDeleteRules() {
		return deleteRules;
	}
	
	private IListener<Rule> ruleDeleted = rule -> {
		rules.remove(rule);
		List<Rule> list = new ArrayList<>();
		list.add(rule);
		deleteRules.invoke(list);
	};
	
	private IListener<Rule> ruleCreatedListener = event -> {
		rules.add(event);
		ruleAdded(event);
		RuleElementUI ruleElement = null;
		ruleElement = new RuleElementUI(rulesComposite, SWT.None, event);
		ruleElement.eventruleDeleted().add(ruleDeleted);
		scrolledComposite.setMinSize(rulesComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		ruleElement.requestLayout();
	};
	
	private NonGenericListenerCollection<StringRule> stringRuleAdded = new NonGenericListenerCollection<>();
	
	public INonGenericListenerCollection<StringRule> eventStringRuleAdded() {
		return stringRuleAdded;
	}
	
	private void ruleAdded(Rule rule) {
		if(rule instanceof StringRule) {
			stringRuleAdded.invoke((StringRule) rule);
		}
		if(rule instanceof DoubleRule) {
			doubleRuleAdded.invoke((DoubleRule) rule);
		}
		if(rule instanceof BooleanRule) {
			booleanRuleAdded.invoke((BooleanRule) rule);
		}
		if(rule instanceof LastActionRule) {
			lastActionRuleAdded.invoke((LastActionRule) rule);
		}
	}
	
	private NonGenericListenerCollection<DoubleRule> doubleRuleAdded = new NonGenericListenerCollection<>();
	
	public INonGenericListenerCollection<DoubleRule> eventDoubleRuleAdded() {
		return doubleRuleAdded;
	}
	
	private NonGenericListenerCollection<BooleanRule> booleanRuleAdded = new NonGenericListenerCollection<>();
	
	public INonGenericListenerCollection<BooleanRule> eventBooleanRuleAdded() {
		return booleanRuleAdded;
	}
	
	private NonGenericListenerCollection<LastActionRule> lastActionRuleAdded = new NonGenericListenerCollection<>();
	private Label enableInfoLabel;
	
	public INonGenericListenerCollection<LastActionRule> eventLastActionRuleAdded() {
		return lastActionRuleAdded;
	}
	
	/*
	Listener sortListener = new Listener() {
		public void handleEvent(Event e) {
			int dir;
			if(sortDescendingButton.getSelection()) {
				dir = SWT.DOWN;
			} else {
				dir = SWT.UP;
			}
			
			SortingArg arg;
			String text = sortCombo.getText();
			
			switch(text) {
			case "Score": arg = SortingArg.Score;
				break;
			case "Name": arg = SortingArg.Name;
				break;
			case "Signature": arg = SortingArg.Signature;
				break;
			case "Parent type": arg = SortingArg.ParentType;
				break;
			case "Path": arg = SortingArg.Path;
				break;
			case "Context size": arg = SortingArg.ContextSize;
				break;
			case "Position": arg = SortingArg.Position;
				break;
			case "Interactivity": arg = SortingArg.Interactivity;
				break;
			case "Last action":  arg = SortingArg.LastAction;
				break;
			default: arg = SortingArg.Score;
				break;
			}
			
			arg.setDescending(dir == SWT.DOWN);
			
			sortRequired.invoke(arg);
			saveState();
		}
		
	};
	
	*/
	
	@Override
	public void setFocus() {
	}
	
	@Override
	public void dispose() {
		this.getSite().getPage().hideView(this);
	}


	public void enableFiltering() {
		enableInfoLabel.setVisible(false);
		enableInfoLabel.requestLayout();
		addRuleButton.setEnabled(true);
		resetAllButton.setEnabled(true);
	}
	
}
