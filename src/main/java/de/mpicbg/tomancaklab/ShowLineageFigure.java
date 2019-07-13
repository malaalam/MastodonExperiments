package de.mpicbg.tomancaklab;

import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.LineStrip;
import org.jzy3d.plot3d.rendering.canvas.Quality;
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

@Plugin(type = ShowLineageFigure.class)
public class ShowLineageFigure extends AbstractContextual implements MastodonPlugin {

    private static final String PluginID = "Show Lineage Figure";
    //------------------------------------------------------------------------

    @Override
    public List<ViewMenuBuilder.MenuItem> getMenuItems() {
        return Arrays.asList(
                menu("Plugins",
                        item(PluginID)));
    }

    private static Map<String, String> menuTexts = new HashMap<>();

    static {
        menuTexts.put(PluginID, "Show Lineage Figure");
    }

    @Override
    public Map<String, String> getMenuTexts() {
        return menuTexts;
    }

    private final AbstractNamedAction ShowLineageFigure;

    public ShowLineageFigure() {
        ShowLineageFigure = new RunnableAction(PluginID, this::showLineageFigure);
        updateEnabledActions();
    }

    @Override
    public void installGlobalActions(final Actions actions) {
        final String[] noShortCut = new String[]{};
        actions.namedAction(ShowLineageFigure, noShortCut);
    }

    private MastodonPluginAppModel pluginAppModel;

    @Override
    public void setAppModel(final MastodonPluginAppModel model) {
        this.pluginAppModel = model;
        updateEnabledActions();
    }

    private void updateEnabledActions() {
        final MamutAppModel appModel = (pluginAppModel == null) ? null : pluginAppModel.getAppModel();
        ShowLineageFigure.setEnabled(appModel != null);
    }

    private LogService logServiceRef;

    private synchronized void showLineageFigure()  {
        logServiceRef = this.getContext().getService(LogService.class).log();
        logServiceRef.trace("Saved the tracked data in a new format ...");
        final Model model = pluginAppModel.getAppModel().getModel();
        ModelGraph modelGraph = model.getGraph();
        final SpatioTemporalIndex<Spot> spots = model.getSpatioTemporalIndex();
        try {
            createLineageFigure(spots, 0, 50);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void createLineageFigure(SpatioTemporalIndex<Spot> spots, int timeStart, int timeEnd) throws Exception {
        AnalysisLauncher analysisLauncher = new AnalysisLauncher();
        class LineageFigure extends AbstractAnalysis {
            @Override
            public void init() {
                Map<String, Color> colorRegister = new HashMap<>();
                LineStrip ls;
                Coord3d[] points = new Coord3d[2];
                float x;
                float y;
                float z;
                chart = AWTChartComponentFactory.chart(Quality.Advanced, "newt");
                for (int time = timeStart; time <= 50; time++) {
                    for (final Spot spot : spots.getSpatialIndex(time)) {
                        if (time == timeStart) {
                            colorRegister.put(spot.getLabel(), Color.random());
                        } else {
                            ls = new LineStrip();
                            x = spot.incomingEdges().get(0).getSource().getFloatPosition(0);
                            y = spot.incomingEdges().get(0).getSource().getFloatPosition(1);
                            z = spot.incomingEdges().get(0).getSource().getFloatPosition(2);
                            points[0] = new Coord3d(x, y, z);
                            x = spot.getFloatPosition(0);
                            y = spot.getFloatPosition(1);
                            z = spot.getFloatPosition(2);
                            points[1] = new Coord3d(x, y, z);
                            ls.add(points[0]);
                            ls.add(points[1]);
                            ls.setWireframeColor(colorRegister.get(spot.incomingEdges().get(0).getSource().getLabel()));

                            ls.setDisplayed(true);
                            chart.getScene().add(ls);

                            colorRegister.put(spot.getLabel(), colorRegister.get(spot.incomingEdges().get(0).getSource().getLabel()));

                        }

                    }
                }
            }
        }
        ;
        analysisLauncher.open(new LineageFigure());
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
