package org.eclipse.sed.ifl.control.session;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.sed.ifl.control.Control;
import org.eclipse.sed.ifl.control.monitor.ActivityMonitorControl;
import org.eclipse.sed.ifl.control.monitor.PartMonitorControl;
import org.eclipse.sed.ifl.control.score.Score;
import org.eclipse.sed.ifl.control.score.ScoreListControl;
import org.eclipse.sed.ifl.control.score.ScoreLoaderControl;
import org.eclipse.sed.ifl.control.score.ScoreRecalculatorControl;
import org.eclipse.sed.ifl.ide.Activator;
import org.eclipse.sed.ifl.ide.accessor.source.CodeEntityAccessor;
import org.eclipse.sed.ifl.model.monitor.ActivityMonitorModel;
import org.eclipse.sed.ifl.model.monitor.event.SessionEvent;
import org.eclipse.sed.ifl.model.score.ScoreListModel;
import org.eclipse.sed.ifl.model.session.SessionModel;
import org.eclipse.sed.ifl.model.user.interaction.SideEffect;
import org.eclipse.sed.ifl.util.event.IListener;
import org.eclipse.sed.ifl.util.event.INonGenericListenerCollection;
import org.eclipse.sed.ifl.util.event.core.EmptyEvent;
import org.eclipse.sed.ifl.util.event.core.NonGenericListenerCollection;
import org.eclipse.sed.ifl.util.exception.EU;
import org.eclipse.sed.ifl.util.profile.NanoWatch;
import org.eclipse.sed.ifl.view.ScoreListView;
import org.eclipse.sed.ifl.view.ScoreLoaderView;
import org.eclipse.sed.ifl.view.SessionView;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.sed.ifl.commons.model.source.CodeChunkLocation;
import org.eclipse.sed.ifl.commons.model.source.IMethodDescription;
import org.eclipse.sed.ifl.commons.model.source.Method;
import org.eclipse.sed.ifl.commons.model.source.MethodIdentity;
import org.eclipse.sed.ifl.commons.model.source.Position;

public class SessionControl extends Control<SessionModel, SessionView> {
	private IJavaProject selectedProject;

	private ActivityMonitorControl activityMonitor;
	private PartMonitorControl partMonitor;

	public SessionControl(IJavaProject selectedProject, PartMonitorControl partMonitor) {
		this.selectedProject = selectedProject;
		this.partMonitor = partMonitor;
	}

	private CodeEntityAccessor accessor = new CodeEntityAccessor();

	private ScoreListControl scoreListControl;
	
	private void startNewSession() {
		NanoWatch watch = new NanoWatch("starting session");
		Map<IMethodBinding, IMethod> resolvedMethods = accessor.getFilteredResolvedMethods(selectedProject);

		Random r = new Random();
		
		List<IMethodDescription> methods = resolvedMethods.entrySet().stream()
		.map(method -> new Method(accessor.identityFrom(method), accessor.locationFrom(method), accessor.contextFrom(method, resolvedMethods), accessor.setInteractivity(r)))
		.collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
		System.out.printf("%d method found\n", methods.size());

		Map<IMethodDescription, Score> sampleScores = methods.stream().map(method -> method)
				.collect(Collectors.collectingAndThen(Collectors.toMap(id -> id, id -> new Score(r.nextDouble())),
						Collections::unmodifiableMap));
		ScoreLoaderControl.saveSample(sampleScores, new File("sampleFor_" + selectedProject.getElementName() + ".csv"));

		ScoreListModel model = new ScoreListModel(methods);
		scoreListControl = new ScoreListControl();
		scoreListControl.setModel(model);
		ScoreListView scoreListView = new ScoreListView();
		getView().embed(scoreListView);
		scoreListControl.setView(scoreListView);
		scoreLoaderControl = new ScoreLoaderControl(accessor.setInteractivity(r));
		scoreLoaderControl.setModel(model);
		scoreLoaderControl.setView(new ScoreLoaderView());
		scoreRecalculatorControl = new ScoreRecalculatorControl(selectedProject);
		scoreRecalculatorControl.setModel(model);
		addSubControl(scoreLoaderControl);
		addSubControl(scoreListControl);
		addSubControl(scoreRecalculatorControl);
		System.out.println(watch);

		MessageDialog.open(MessageDialog.INFORMATION, null, "iFL interactivity",
				"Interactivity of all code elements is set to " + accessor.getInteractivity(), SWT.NONE);

	}

	@Override
	public void init() {
		activityMonitor = new ActivityMonitorControl(new ActivityMonitorModel());
		addSubControl(activityMonitor);
		addSubControl(partMonitor);
		getView().eventClosed().add(closeListener);
		getView().eventScoreLoadRequested().add(scoreLoadRequestedListener);
		getView().eventHideUndefinedRequested().add(hideUndefinedListener);
		getView().eventScoreRecalculateRequested().add(scoreRecalculateRequestedListener);
		startNewSession();
		scoreListControl.eventTerminationRequested().add(terminationReqestedListener);
		super.init();

		activityMonitor.log(SessionEvent.start(selectedProject));
	}

	@Override
	public void teardown() {
		scoreListControl.eventTerminationRequested().remove(terminationReqestedListener);
		getView().eventClosed().remove(closeListener);
		getView().eventScoreLoadRequested().remove(scoreLoadRequestedListener);
		getView().eventHideUndefinedRequested().remove(hideUndefinedListener);
		getView().eventScoreRecalculateRequested().remove(scoreRecalculateRequestedListener);
		super.teardown();
		scoreListControl = null;
		scoreLoaderControl = null;
		scoreRecalculatorControl = null;
		activityMonitor = null;
	}

	private IListener<SideEffect> terminationReqestedListener = event -> {
		getView().close();
	};

	private NonGenericListenerCollection<EmptyEvent> finished = new NonGenericListenerCollection<>();

	public INonGenericListenerCollection<EmptyEvent> eventFinished() {
		return finished;
	}

	private IListener<IWorkbenchPart> closeListener = part -> {
		terminate();
	};

	private void terminate() {
		System.out.println("Session closing...");
		activityMonitor.log(SessionEvent.stop(selectedProject));
		scoreListControl.closeFilterPart();
		scoreListControl.closeDualListPart();
		this.finished.invoke(new EmptyEvent());
	}

	private ScoreLoaderControl scoreLoaderControl;
	private ScoreRecalculatorControl scoreRecalculatorControl;

	private IListener<EmptyEvent> scoreLoadRequestedListener = __ -> {
		System.out.println("Loading scores from files are requested...");
		this.scoreLoaderControl.load();
	};
	private IListener<Boolean> hideUndefinedListener = status -> scoreListControl.setHideUndefinedScores(status);

	private IListener<EmptyEvent> scoreRecalculateRequestedListener = __ -> {
		try {
			this.scoreRecalculatorControl.recalculate();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	};

}
