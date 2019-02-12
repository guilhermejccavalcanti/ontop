package org.semanticweb.ontop.protege.views;

import it.unibz.krdb.obda.io.TargetQueryVocabularyValidator;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDAModelImpl;
import it.unibz.krdb.obda.owlapi3.TargetQueryValidator;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.selection.OWLSelectionModelListener;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
import org.protege.editor.owl.ui.view.Findable;
import org.semanticweb.ontop.protege.core.OBDAModelManager;
import org.semanticweb.ontop.protege.core.OBDAModelManagerListener;
import org.semanticweb.ontop.protege.panels.MappingManagerPanel;
import org.semanticweb.owlapi.model.OWLEntity;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

public class MappingsManagerView extends AbstractOWLViewComponent implements OBDAModelManagerListener, Findable<OWLEntity> {

    private static final long serialVersionUID = 1790921396564256165L;

    OBDAModelManager controller = null;

    OBDAModel obdaModel;

    MappingManagerPanel mappingPanel = null;

    @Override
    protected void disposeOWLView() {
        controller.removeListener(this);
    }

    @Override
    protected void initialiseOWLView() throws Exception {
        final OWLEditorKit editor = getOWLEditorKit();
        controller = (OBDAModelManager) editor.get(OBDAModelImpl.class.getName());
        controller.addListener(this);
        obdaModel = controller.getActiveOBDAModel();
        TargetQueryVocabularyValidator validator = new TargetQueryValidator(obdaModel.getOntologyVocabulary());
        mappingPanel = new MappingManagerPanel(obdaModel, validator);
        editor.getOWLWorkspace().getOWLSelectionModel().addListener(new OWLSelectionModelListener() {

            @Override
            public void selectionChanged() throws Exception {
                OWLEntity entity = editor.getOWLWorkspace().getOWLSelectionModel().getSelectedEntity();
                if (entity == null) {
                    return;
                }
                if (!entity.isTopEntity()) {
                    String shortf = entity.getIRI().getFragment();
                    if (shortf == null) {
                        String iri = entity.getIRI().toString();
                        shortf = iri.substring(iri.lastIndexOf("/"));
                    }
                    mappingPanel.setFilter("pred:" + shortf);
                } else {
                    mappingPanel.setFilter("");
                }
            }
        });
        if (obdaModel.getSources().size() > 0) {
            mappingPanel.datasourceChanged(mappingPanel.getSelectedSource(), obdaModel.getSources().get(0));
        }
        mappingPanel.setBorder(new TitledBorder("Mapping manager"));
        setLayout(new BorderLayout());
        add(mappingPanel, BorderLayout.CENTER);
    }

    @Override
    public void activeOntologyChanged() {
        obdaModel = controller.getActiveOBDAModel();
        TargetQueryVocabularyValidator validator = new TargetQueryValidator(obdaModel.getOntologyVocabulary());
        mappingPanel.setOBDAModel(obdaModel);
        mappingPanel.setTargetQueryValidator(validator);
    }

    @Override
    public List<OWLEntity> find(String match) {
        return null;
    }

    @Override
    public void show(OWLEntity owlEntity) {
    }
}
