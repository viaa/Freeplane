package org.freeplane.core.ui.menubuilders.menu;

import static org.freeplane.core.ui.menubuilders.generic.PhaseProcessor.Phase.ACCELERATORS;
import static org.freeplane.core.ui.menubuilders.generic.PhaseProcessor.Phase.ACTIONS;
import static org.freeplane.core.ui.menubuilders.generic.PhaseProcessor.Phase.UI;

import org.freeplane.core.ui.IUserInputListenerFactory;
import org.freeplane.core.ui.menubuilders.action.AcceleratebleActionProvider;
import org.freeplane.core.ui.menubuilders.action.AcceleratorBuilder;
import org.freeplane.core.ui.menubuilders.action.AcceleratorDestroyer;
import org.freeplane.core.ui.menubuilders.action.ActionFinder;
import org.freeplane.core.ui.menubuilders.action.ActionSelectListener;
import org.freeplane.core.ui.menubuilders.action.EntriesForAction;
import org.freeplane.core.ui.menubuilders.action.IAcceleratorMap;
import org.freeplane.core.ui.menubuilders.generic.BuildProcessFactory;
import org.freeplane.core.ui.menubuilders.generic.EntryPopupListenerCollection;
import org.freeplane.core.ui.menubuilders.generic.EntryVisitor;
import org.freeplane.core.ui.menubuilders.generic.PhaseProcessor;
import org.freeplane.core.ui.menubuilders.generic.RecursiveMenuStructureProcessor;
import org.freeplane.core.ui.menubuilders.generic.ResourceAccessor;
import org.freeplane.core.ui.menubuilders.generic.SubtreeProcessor;
import org.freeplane.features.mode.FreeplaneActions;

public class MenuBuildProcessFactory implements BuildProcessFactory {

	private PhaseProcessor buildProcessor;
	
	private SubtreeProcessor childProcessor;

	public PhaseProcessor getBuildProcessor() {
		return buildProcessor;
	}

	public SubtreeProcessor getChildProcessor() {
		return childProcessor;
	}

	public MenuBuildProcessFactory(IUserInputListenerFactory userInputListenerFactory,
	                                                    FreeplaneActions freeplaneActions,
	                                           ResourceAccessor resourceAccessor, IAcceleratorMap acceleratorMap, EntriesForAction entries) {
		final RecursiveMenuStructureProcessor actionBuilder = new RecursiveMenuStructureProcessor();
		actionBuilder.setDefaultBuilder(new ActionFinder(freeplaneActions));

		final RecursiveMenuStructureProcessor acceleratorBuilder = new RecursiveMenuStructureProcessor();
		acceleratorBuilder.setDefaultBuilderPair(new AcceleratorBuilder(acceleratorMap, entries),
		    new AcceleratorDestroyer(acceleratorMap, entries));

		RecursiveMenuStructureProcessor uiBuilder = new RecursiveMenuStructureProcessor();
		uiBuilder.setDefaultBuilder(EntryVisitor.EMTPY);
		uiBuilder.addBuilder("ignore", EntryVisitor.CHILD_ENTRY_REMOVER);
		uiBuilder.addBuilder("skip", EntryVisitor.SKIP);
		
		childProcessor = new SubtreeProcessor();
		final ActionSelectListener actionSelectListener = new ActionSelectListener();
		EntryPopupListenerCollection entryPopupListenerCollection = new EntryPopupListenerCollection();
		entryPopupListenerCollection.addEntryPopupListener(childProcessor);
		entryPopupListenerCollection.addEntryPopupListener(actionSelectListener);

		
		acceleratorMap.addAcceleratorChangeListener(new MenuAcceleratorChangeListener(entries));
		
		uiBuilder.addBuilder("toolbar", new JToolbarBuilder(userInputListenerFactory));
		uiBuilder.setSubtreeDefaultBuilderPair("toolbar", "toolbar.action");
		uiBuilder.addBuilder("toolbar.action", new JToolbarComponentBuilder());

		uiBuilder.addBuilder("main_menu", new JMenubarBuilder(userInputListenerFactory));
		uiBuilder.setSubtreeDefaultBuilderPair("main_menu", "menu.action");
		
		uiBuilder.addBuilderPair("radio_button_group", //
		    new JMenuRadioGroupBuilder(entryPopupListenerCollection, acceleratorMap, new AcceleratebleActionProvider(),
		        resourceAccessor), new JComponentRemover());
		
		uiBuilder.addBuilder("map_popup", new MapPopupBuilder(userInputListenerFactory));
		uiBuilder.setSubtreeDefaultBuilderPair("map_popup", "menu.action");
		uiBuilder.addBuilder("node_popup", new NodePopupBuilder(userInputListenerFactory));
		uiBuilder.setSubtreeDefaultBuilderPair("node_popup", "menu.action");

		uiBuilder.addBuilderPair("menu.action", //
		    new JMenuItemBuilder(entryPopupListenerCollection, acceleratorMap, new AcceleratebleActionProvider(),
		        resourceAccessor), new JComponentRemover());

		buildProcessor = new PhaseProcessor()
								.withPhase(ACTIONS, actionBuilder) //
							    .withPhase(ACCELERATORS, acceleratorBuilder)
							    .withPhase(UI, uiBuilder);
		childProcessor.setProcessor(buildProcessor);
	}
}

