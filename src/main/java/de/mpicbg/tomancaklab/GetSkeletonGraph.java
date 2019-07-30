package de.mpicbg.tomancaklab;

import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.plugin.MastodonPlugin;
import org.mastodon.plugin.MastodonPluginAppModel;
import org.mastodon.project.MamutProject;
import org.mastodon.project.MamutProjectIO;
import org.mastodon.revised.mamut.MamutAppModel;
import org.mastodon.revised.mamut.Mastodon;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.ModelGraph;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.spatial.SpatioTemporalIndex;
import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;

import javax.swing.*;
import java.util.*;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.menu;

@Plugin(type = de.mpicbg.tomancaklab.GetSkeletonGraph.class)
public class GetSkeletonGraph extends AbstractContextual implements MastodonPlugin {

    private static final String PluginID = "Convert Model Graph to a Skeleton Graph";
    //------------------------------------------------------------------------

    @Override
    public List<ViewMenuBuilder.MenuItem> getMenuItems() {
        return Arrays.asList(
                menu("Plugins",
                        item(PluginID)));
    }

    private static Map<String, String> menuTexts = new HashMap<>();

    static {
        menuTexts.put(PluginID, "Convert Model Graph to a Skeleton Graph");
    }

    @Override
    public Map<String, String> getMenuTexts() {
        return menuTexts;
    }

    private final AbstractNamedAction GetSkeletonGraph;

    public GetSkeletonGraph() {
        GetSkeletonGraph = new RunnableAction(PluginID, this::getSkeletonGraph);
        updateEnabledActions();
    }


    @Override
    public void installGlobalActions(final Actions actions) {
        final String[] noShortCut = new String[]{};
        actions.namedAction(GetSkeletonGraph, noShortCut);
    }

    private MastodonPluginAppModel pluginAppModel;

    @Override
    public void setAppModel(final MastodonPluginAppModel model) {
        this.pluginAppModel = model;
        updateEnabledActions();
    }

    private void updateEnabledActions() {
        final MamutAppModel appModel = (pluginAppModel == null) ? null : pluginAppModel.getAppModel();
        GetSkeletonGraph.setEnabled(appModel != null);
    }

    private LogService logServiceRef;

    private synchronized void getSkeletonGraph() {
        logServiceRef = this.getContext().getService(LogService.class).log();
        logServiceRef.trace("Converted the Model Graph to a Skeleton graph ...");
        final Model model = pluginAppModel.getAppModel().getModel();
        ModelGraph modelGraph = model.getGraph();
        final SpatioTemporalIndex<Spot> spots = model.getSpatioTemporalIndex();
        convertGraph(spots, pluginAppModel.getAppModel().getMinTimepoint(), pluginAppModel.getAppModel().getMaxTimepoint(), modelGraph);
    }

    private void convertGraph(SpatioTemporalIndex<Spot> spots, int timeStart, int timeEnd, ModelGraph modelGraph) {
        int numberSiblings;
        int numberChildren;
        Map<Spot, Spot> ancestry = new HashMap<>();
        Map<Spot, Spot> denseToSparse = new HashMap<>();
        Map<Spot, Spot> sparseToDense = new HashMap<>(); // backward reference
        ModelGraph skeletonGraph = new ModelGraph();
        double[] pos;
        double[][] covariance = new double[3][3];
        for (int time = timeStart; time < timeEnd + 1; time++) {

            for (final Spot loopspot : spots.getSpatialIndex(time)) {
                Spot spot = modelGraph.vertexRef().refTo(loopspot);
                if (time == timeStart) {
                    pos = new double[]{spot.getDoublePosition(0), spot.getDoublePosition(1), spot.getDoublePosition(2)};
                    spot.getCovariance(covariance);
                    final Spot currentVertex = skeletonGraph.addVertex().init(timeStart, pos, covariance);
                    currentVertex.setLabel(spot.getLabel());
                    ancestry.put(spot, spot);
                    denseToSparse.put(spot, currentVertex);
                    sparseToDense.put(currentVertex, spot);
                } else if (time == timeEnd) {
                    Spot parent = spot.incomingEdges().get(0).getSource();
                    numberSiblings = spot.incomingEdges().get(0).getSource().outgoingEdges().size() - 1;
                    pos = new double[]{spot.getDoublePosition(0), spot.getDoublePosition(1), spot.getDoublePosition(2)};
                    spot.getCovariance(covariance);
                    final Spot currentVertex = skeletonGraph.addVertex().init(time, pos, covariance);
                    currentVertex.setLabel(spot.getLabel());
                    denseToSparse.put(spot, currentVertex);
                    sparseToDense.put(currentVertex, spot);
                    skeletonGraph.addEdge(denseToSparse.get(ancestry.get(parent)), currentVertex);
                    if (numberSiblings == 0) {
                        ancestry.put(spot, ancestry.get(parent));
                    } else {
                        ancestry.put(spot, parent);
                    }


                } else {

                    Spot parent = spot.incomingEdges().get(0).getSource();
                    if (parent != null) {

                        numberChildren = spot.outgoingEdges().size();
                        numberSiblings = spot.incomingEdges().get(0).getSource().outgoingEdges().size() - 1;
                        if (numberChildren > 1 && numberSiblings == 0) {

                            pos = new double[]{spot.getDoublePosition(0), spot.getDoublePosition(1), spot.getDoublePosition(2)};
                            spot.getCovariance(covariance);
                            final Spot currentVertex = skeletonGraph.addVertex().init(time, pos, covariance);
                            currentVertex.setLabel(spot.getLabel());
                            denseToSparse.put(spot, currentVertex);
                            sparseToDense.put(currentVertex, spot);
                            skeletonGraph.addEdge(denseToSparse.get(ancestry.get(parent)), currentVertex);
                            ancestry.put(spot, ancestry.get(parent));


                        } else if (numberSiblings > 0 && numberChildren == 1) {
                            ancestry.put(spot, parent);

                        } else if (numberSiblings > 0 && numberChildren > 1) {
                            ancestry.put(spot, parent);
                            pos = new double[]{spot.getDoublePosition(0), spot.getDoublePosition(1), spot.getDoublePosition(2)};
                            spot.getCovariance(covariance);
                            final Spot currentVertex = skeletonGraph.addVertex().init(time, pos, covariance);
                            currentVertex.setLabel(spot.getLabel());
                            denseToSparse.put(spot, currentVertex);
                            sparseToDense.put(currentVertex, spot);
                            skeletonGraph.addEdge(denseToSparse.get(parent), currentVertex);
                        } else {
                            ancestry.put(spot, ancestry.get(parent));
                        }
                    }

                }

            }
        }
        ConvertTimeToGenerations.showModelGraph(skeletonGraph);


    }


    public static void main(String... args) throws Exception {
        final String projectPath = "/home/manan/Desktop/03_Datasets/13-03-12/Mastodon_w-lineage-ID";
        Locale.setDefault(Locale.US);
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        final Mastodon mastodon = new Mastodon();
        new Context().inject(mastodon);
        mastodon.run();
        final MamutProject project = new MamutProjectIO().load(projectPath);
        mastodon.openProject(project);
    }

}
