package de.mpicbg.tomancaklab;

import org.mastodon.plugin.MastodonPluginAppModel;
import org.mastodon.project.MamutProject;
import org.mastodon.project.MamutProjectIO;
import org.mastodon.revised.mamut.MamutAppModel;
import org.mastodon.revised.mamut.Mastodon;
import org.mastodon.revised.mamut.WindowManager;
import org.mastodon.revised.model.mamut.Link;
import org.mastodon.revised.model.mamut.Model;
import org.mastodon.revised.model.mamut.Spot;
import org.mastodon.revised.model.tag.TagSetModel;
import org.mastodon.revised.model.tag.TagSetStructure;
import org.scijava.Context;

import javax.swing.*;
import java.util.Iterator;
import java.util.Locale;

public class AddColor {

    public static void main(String... args) throws Exception {
        final String projectPath = "/home/manan/Desktop/03_Datasets/13-03-12/Mastodon_w-lineage-ID";
        Locale.setDefault(Locale.US);
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        final Mastodon mastodon = new Mastodon();
        new Context().inject(mastodon);
        mastodon.run();
        final MamutProject project = new MamutProjectIO().load(projectPath);
        mastodon.openProject(project);

        Model model= new MastodonPluginAppModel(mastodon.windowManager.getAppModel(), mastodon.windowManager).getAppModel().getModel();
        final TagSetModel<Spot, Link> tags = model.getTagSetModel();
        final TagSetStructure tss = new TagSetStructure();
        tss.set( tags.getTagSetStructure() );

        final TagSetStructure.TagSet ts = tss.createTagSet( "manan 1" );
        final TagSetStructure.Tag tag1 = ts.createTag( "tag1", 0xffff0000 ); // red
        final TagSetStructure.Tag tag2 = ts.createTag( "tag2", 0xffffff00 ); // yellow
        final TagSetStructure.Tag tag3 = ts.createTag( "tag3", 0xff0000ff ); // blue

        tags.setTagSetStructure( tss );

        final Iterator< Spot > iterator = model.getGraph().vertices().iterator();
        int counter=0;
        while(counter<100){
            System.out.println(counter);
            iterator.next();
            tags.getVertexTags().set(iterator.next(), tag1);
            tags.getVertexTags().set( iterator.next(), tag2);
            tags.getVertexTags().set( iterator.next(), tag3 );
            counter++;
        }


    }
}
