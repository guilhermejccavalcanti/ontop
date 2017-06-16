package it.unibz.inf.ontop.protege.panels;

/*
 * #%L
 * ontop-protege4
 * %%
 * Copyright (C) 2009 - 2013 KRDB Research Centre. Free University of Bozen Bolzano.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import it.unibz.inf.ontop.model.OBDADataSource;
import it.unibz.inf.ontop.model.OBDADataSourceFactory;
import it.unibz.inf.ontop.model.impl.OBDADataSourceFactoryImpl;
import it.unibz.inf.ontop.model.impl.SQLPPMappingImpl;
import it.unibz.inf.ontop.model.impl.RDBMSourceParameterConstants;
import it.unibz.inf.ontop.protege.core.OBDAModelManager;
import it.unibz.inf.ontop.protege.core.OBDAModelWrapper;
import it.unibz.inf.ontop.protege.gui.IconLoader;
import it.unibz.inf.ontop.protege.utils.ConnectionTools;
import it.unibz.inf.ontop.protege.utils.CustomTraversalPolicy;
import it.unibz.inf.ontop.protege.utils.DatasourceSelectorListener;
import it.unibz.inf.ontop.protege.utils.DialogUtils;
import it.unibz.inf.ontop.sql.JDBCConnectionManager;
import org.protege.editor.core.ProtegeManager;
import org.protege.editor.owl.OWLEditorKit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatasourceParameterEditorPanel extends javax.swing.JPanel implements DatasourceSelectorListener {

    private static final OBDADataSourceFactory DATASOURCE_FACTORY = OBDADataSourceFactoryImpl.getInstance();
    private static final long serialVersionUID = 3506358479342412849L;
    private final OWLEditorKit owlEditorKit;

    private OBDADataSource currentDataSource;

	private OBDAModelWrapper obdaModel;

    private ComboBoxItemListener comboListener;

    private Timer timer = null;

    /**
     * Creates new form DatasourceParameterEditorPanel
     */
    public DatasourceParameterEditorPanel(OWLEditorKit owlEditorKit) {

        this.owlEditorKit = owlEditorKit;
        OBDAModelManager obdaModelManager = (OBDAModelManager) owlEditorKit.get(SQLPPMappingImpl.class.getName());
        OBDAModelWrapper model = obdaModelManager.getActiveOBDAModelWrapper();

        timer = new Timer(200, e -> handleTimer());

        initComponents();

        this.comboListener = new ComboBoxItemListener();
        txtJdbcDriver.addItemListener(comboListener);

        setNewDatasource(model);

        List<Component> order = new ArrayList<>(7);
        order.add(pnlDataSourceParameters);
        order.add(txtJdbcUrl);
        order.add(txtDatabaseUsername);
        order.add(txtDatabasePassword);
        order.add(txtJdbcDriver);
        order.add(cmdTestConnection);
        this.setFocusTraversalPolicy(new CustomTraversalPolicy(order));
    }

    private void handleTimer() {
        timer.stop();
        updateSourceValues();
    }


    private class ComboBoxItemListener implements ItemListener {

        private boolean notify = false;

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (notify) {
                fieldChangeHandler(null);
            }
        }

        public void setNotify(boolean notify) {
            this.notify = notify;
        }
    }

    public void setNewDatasource(OBDAModelWrapper model) {
        obdaModel = model;
        resetTextFields();

        /**
         * Selects the first data source if it exists
         */
        obdaModel.getDatasource()
                .ifPresent(this::currentDatasourceChange);
    }


    private void resetTextFields() {

        txtJdbcUrl.setText("");
        txtDatabasePassword.setText("");
        txtDatabaseUsername.setText("");
        comboListener.setNotify(false);
        txtJdbcDriver.setSelectedIndex(0);
        comboListener.setNotify(true);
    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        pnlDataSourceParameters = new javax.swing.JPanel();
        txtJdbcUrl = new javax.swing.JTextField();
        txtDatabaseUsername = new javax.swing.JTextField();
        txtDatabasePassword = new javax.swing.JPasswordField();
        txtJdbcDriver = new javax.swing.JComboBox<>();
        cmdTestConnection = new javax.swing.JButton();
        lblJdbcUrl = new javax.swing.JLabel();
        lblDatabaseUsername = new javax.swing.JLabel();
        lblDatabasePassword = new javax.swing.JLabel();
        lblJdbcDriver = new javax.swing.JLabel();
        lblConnectionStatus = new javax.swing.JLabel();
        pnlCommandButton = new javax.swing.JPanel();
        cmdSave = new javax.swing.JButton();
        cmdHelp = new javax.swing.JButton();
        pnlInformation = new javax.swing.JPanel();

        setFocusable(false);
        setMinimumSize(new java.awt.Dimension(640, 480));
        setPreferredSize(new java.awt.Dimension(640, 480));
        setLayout(new java.awt.GridBagLayout());

        pnlDataSourceParameters.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Connection parameters", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Lucida Grande", 0, 13), new java.awt.Color(53, 113, 163))); // NOI18N
        pnlDataSourceParameters.setForeground(new java.awt.Color(53, 113, 163));
        pnlDataSourceParameters.setAlignmentX(5.0F);
        pnlDataSourceParameters.setAlignmentY(5.0F);
        pnlDataSourceParameters.setAutoscrolls(true);
        pnlDataSourceParameters.setFocusable(false);
        pnlDataSourceParameters.setMaximumSize(new java.awt.Dimension(32767, 23));
        pnlDataSourceParameters.setMinimumSize(new java.awt.Dimension(0, 0));
        pnlDataSourceParameters.setPreferredSize(new java.awt.Dimension(1, 300));
        pnlDataSourceParameters.setLayout(new java.awt.GridBagLayout());

        txtJdbcUrl.setFont(new java.awt.Font("Courier New", 1, 13)); // NOI18N
        txtJdbcUrl.setMaximumSize(new java.awt.Dimension(25, 2147483647));
        txtJdbcUrl.setMinimumSize(new java.awt.Dimension(180, 24));
        txtJdbcUrl.setPreferredSize(new java.awt.Dimension(180, 24));
        txtJdbcUrl.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                fieldChangeHandler(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(30, 0, 2, 10);
        pnlDataSourceParameters.add(txtJdbcUrl, gridBagConstraints);

        txtDatabaseUsername.setFont(new java.awt.Font("Courier New", 1, 13)); // NOI18N
        txtDatabaseUsername.setMaximumSize(new java.awt.Dimension(25, 2147483647));
        txtDatabaseUsername.setMinimumSize(new java.awt.Dimension(180, 24));
        txtDatabaseUsername.setPreferredSize(new java.awt.Dimension(180, 24));
        txtDatabaseUsername.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                fieldChangeHandler(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 2, 10);
        pnlDataSourceParameters.add(txtDatabaseUsername, gridBagConstraints);

        txtDatabasePassword.setFont(new java.awt.Font("Courier New", 1, 13)); // NOI18N
        txtDatabasePassword.setMinimumSize(new java.awt.Dimension(180, 24));
        txtDatabasePassword.setPreferredSize(new java.awt.Dimension(180, 24));
        txtDatabasePassword.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                fieldChangeHandler(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 2, 10);
        pnlDataSourceParameters.add(txtDatabasePassword, gridBagConstraints);

        txtJdbcDriver.setEditable(true);
        txtJdbcDriver.setFont(new java.awt.Font("Courier New", 1, 13)); // NOI18N
        txtJdbcDriver.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "select or type the JDBC Driver's class...", "org.postgresql.Driver", "com.mysql.jdbc.Driver", "org.h2.Driver", "com.ibm.db2.jcc.DB2Driver", "oracle.jdbc.driver.OracleDriver", "com.microsoft.sqlserver.jdbc.SQLServerDriver" }));
        txtJdbcDriver.setMinimumSize(new java.awt.Dimension(180, 24));
        txtJdbcDriver.setPreferredSize(new java.awt.Dimension(180, 24));
        txtJdbcDriver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtJdbcDriverActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 2, 10);
        pnlDataSourceParameters.add(txtJdbcDriver, gridBagConstraints);

        cmdTestConnection.setIcon(IconLoader.getImageIcon("images/execute.png"));
        cmdTestConnection.setText("Test Connection");
        cmdTestConnection.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        cmdTestConnection.setContentAreaFilled(false);
        cmdTestConnection.setIconTextGap(5);
        cmdTestConnection.setMaximumSize(new java.awt.Dimension(110, 25));
        cmdTestConnection.setMinimumSize(new java.awt.Dimension(110, 25));
        cmdTestConnection.setPreferredSize(new java.awt.Dimension(110, 25));
        cmdTestConnection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdTestConnectionActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(8, 10, 10, 20);
        pnlDataSourceParameters.add(cmdTestConnection, gridBagConstraints);

        lblJdbcUrl.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        lblJdbcUrl.setForeground(new java.awt.Color(53, 113, 163));
        lblJdbcUrl.setText("Connection URL:");
        lblJdbcUrl.setFocusTraversalKeysEnabled(false);
        lblJdbcUrl.setFocusable(false);
        lblJdbcUrl.setMaximumSize(new java.awt.Dimension(130, 24));
        lblJdbcUrl.setMinimumSize(new java.awt.Dimension(130, 24));
        lblJdbcUrl.setPreferredSize(new java.awt.Dimension(130, 24));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(30, 10, 2, 20);
        pnlDataSourceParameters.add(lblJdbcUrl, gridBagConstraints);

        lblDatabaseUsername.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        lblDatabaseUsername.setForeground(new java.awt.Color(53, 113, 163));
        lblDatabaseUsername.setText("Database Username:");
        lblDatabaseUsername.setFocusTraversalKeysEnabled(false);
        lblDatabaseUsername.setFocusable(false);
        lblDatabaseUsername.setMaximumSize(new java.awt.Dimension(130, 24));
        lblDatabaseUsername.setMinimumSize(new java.awt.Dimension(130, 24));
        lblDatabaseUsername.setPreferredSize(new java.awt.Dimension(130, 24));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(3, 10, 2, 20);
        pnlDataSourceParameters.add(lblDatabaseUsername, gridBagConstraints);

        lblDatabasePassword.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        lblDatabasePassword.setForeground(new java.awt.Color(53, 113, 163));
        lblDatabasePassword.setText("Database Password:");
        lblDatabasePassword.setFocusTraversalKeysEnabled(false);
        lblDatabasePassword.setFocusable(false);
        lblDatabasePassword.setMaximumSize(new java.awt.Dimension(130, 24));
        lblDatabasePassword.setMinimumSize(new java.awt.Dimension(130, 24));
        lblDatabasePassword.setPreferredSize(new java.awt.Dimension(130, 24));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(3, 10, 2, 20);
        pnlDataSourceParameters.add(lblDatabasePassword, gridBagConstraints);

        lblJdbcDriver.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        lblJdbcDriver.setForeground(new java.awt.Color(53, 113, 163));
        lblJdbcDriver.setText("Driver class:");
        lblJdbcDriver.setFocusTraversalKeysEnabled(false);
        lblJdbcDriver.setFocusable(false);
        lblJdbcDriver.setMaximumSize(new java.awt.Dimension(130, 24));
        lblJdbcDriver.setMinimumSize(new java.awt.Dimension(130, 24));
        lblJdbcDriver.setPreferredSize(new java.awt.Dimension(130, 24));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(3, 10, 2, 20);
        pnlDataSourceParameters.add(lblJdbcDriver, gridBagConstraints);

        lblConnectionStatus.setFont(new java.awt.Font("Courier New", 1, 13)); // NOI18N
        lblConnectionStatus.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lblConnectionStatus.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 1, 1, 1));
        lblConnectionStatus.setFocusTraversalKeysEnabled(false);
        lblConnectionStatus.setFocusable(false);
        lblConnectionStatus.setMaximumSize(new java.awt.Dimension(180, 108));
        lblConnectionStatus.setMinimumSize(new java.awt.Dimension(180, 108));
        lblConnectionStatus.setPreferredSize(new java.awt.Dimension(180, 108));
        lblConnectionStatus.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 10, 10);
        pnlDataSourceParameters.add(lblConnectionStatus, gridBagConstraints);

        pnlCommandButton.setFocusable(false);
        pnlCommandButton.setMinimumSize(new java.awt.Dimension(210, 27));
        pnlCommandButton.setPreferredSize(new java.awt.Dimension(210, 27));
        pnlCommandButton.setLayout(new java.awt.GridBagLayout());

        cmdSave.setText("Save");
        cmdSave.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        cmdSave.setContentAreaFilled(false);
        cmdSave.setIconTextGap(5);
        cmdSave.setMargin(new java.awt.Insets(0, 0, 0, 0));
        cmdSave.setMaximumSize(new java.awt.Dimension(105, 25));
        cmdSave.setMinimumSize(new java.awt.Dimension(105, 25));
        cmdSave.setPreferredSize(new java.awt.Dimension(105, 25));
        cmdSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdSaveActionPerformed(evt);
            }
        });
        pnlCommandButton.add(cmdSave, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(30, 0, 0, 0);
        pnlDataSourceParameters.add(pnlCommandButton, gridBagConstraints);

        cmdHelp.setFont(new java.awt.Font("Dialog", 1, 13)); // NOI18N
        cmdHelp.setForeground(new java.awt.Color(53, 113, 163));
        cmdHelp.setIcon(IconLoader.getImageIcon("images/gtk-help.png"));
        cmdHelp.setText("<HTML><U>Help</U></HTML>");
        cmdHelp.setToolTipText("For information on JDBC connections go to: https://github.com/ontop/ontop/wiki/ObdalibPluginJDBC");
        cmdHelp.setBorderPainted(false);
        cmdHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdHelpActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        pnlDataSourceParameters.add(cmdHelp, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        add(pnlDataSourceParameters, gridBagConstraints);

        pnlInformation.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(pnlInformation, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void cmdHelpActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cmdHelpActionPerformed
        DialogUtils.open(URI.create("https://github.com/ontop/ontop/wiki/ObdalibPluginJDBC"));
    }// GEN-LAST:event_cmdHelpActionPerformed

    private void txtJdbcDriverActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtJdbcDriverActionPerformed
        fieldChangeHandler(null);
    }// GEN-LAST:event_txtJdbcDriverActionPerformed

    private void cmdSaveActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cmdNewActionPerformed
        if (currentDataSource == null) {
            if (!createNewDatasource()) {
                return;
            }
        }
        // save the obdaModel to an .obda file disk
        try {
            ProtegeManager.getInstance().saveEditorKit(owlEditorKit);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Exception occurred while saving the mapping", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

    }// GEN-LAST:event_cmdSaveActionPerformed

    private boolean createNewDatasource() {

        URI uri = URI.create("datasource1");

        //create new datasource
        OBDADataSource newDatasource = DATASOURCE_FACTORY.getDataSource(uri);
        String username = txtDatabaseUsername.getText();
        newDatasource.setParameter(RDBMSourceParameterConstants.DATABASE_USERNAME, username);
        String password = new String(txtDatabasePassword.getPassword());
        newDatasource.setParameter(RDBMSourceParameterConstants.DATABASE_PASSWORD, password);
        String driver = txtJdbcDriver.getSelectedIndex() == 0 ? "" : (String) txtJdbcDriver.getSelectedItem();
        newDatasource.setParameter(RDBMSourceParameterConstants.DATABASE_DRIVER, driver);
        String url = txtJdbcUrl.getText();
        newDatasource.setParameter(RDBMSourceParameterConstants.DATABASE_URL, url);
        currentDataSource = newDatasource;
        obdaModel.addSource(currentDataSource);


        return true;
    }

    private void cmdTestConnectionActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cmdTestConnectionActionPerformed

        if (currentDataSource == null) {
            if (!createNewDatasource()) {
                return;
            }
        }

        lblConnectionStatus.setText("Establishing connection...");
        lblConnectionStatus.setForeground(Color.BLACK);

        Runnable run = () -> {
            JDBCConnectionManager connm = JDBCConnectionManager.getJDBCConnectionManager();
            try {
                try {
                    connm.closeConnection();
                } catch (Exception e) {
                    // NO-OP
                }
                Connection conn = ConnectionTools.getConnection(currentDataSource);
                if (conn == null)
                    throw new SQLException("Error connecting to the database");
                lblConnectionStatus.setForeground(Color.GREEN.darker());
                lblConnectionStatus.setText("Connection is OK");
            } catch (SQLException e) { // if fails
                String help = "";
                if (e.getMessage().startsWith("No suitable driver")) {
                    help = "<br/><br/> HINT: To setup JDBC drivers, open the Preference panel and go to the \"JDBC Drives\" tab." +
                            " (Windows and Linux: Files &gt; Preferences..., Mac OS X: Protege &gt; Preferences...) " +
                            "<br/> More information is on the Wiki: " +
                            "<a href='https://github.com/ontop/ontop/wiki/FAQ'>https://github.com/ontop/ontop/wiki/FAQ</a>";
                }
                lblConnectionStatus.setForeground(Color.RED);
                lblConnectionStatus.setText(String.format("<html>%s (ERR-CODE: %s)%s</html>", e.getMessage(), e.getErrorCode(), help));
            }

        };
        SwingUtilities.invokeLater(run);

    }// GEN-LAST:event_cmdTestConnectionActionPerformed

    private void fieldChangeHandler(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_fieldChangeHandler
        timer.restart();
    }// GEN-LAST:event_fieldChangeHandler

    private void updateSourceValues() {
        if (currentDataSource == null) {
            createNewDatasource();
            return;

        }

        JDBCConnectionManager man = JDBCConnectionManager.getJDBCConnectionManager();
        try {
            man.closeConnection();
        } catch (SQLException e) {
            // do nothing
        }

        String username = txtDatabaseUsername.getText();
        currentDataSource.setParameter(RDBMSourceParameterConstants.DATABASE_USERNAME, username);
        String password = new String(txtDatabasePassword.getPassword());
        currentDataSource.setParameter(RDBMSourceParameterConstants.DATABASE_PASSWORD, password);
        String driver = txtJdbcDriver.getSelectedIndex() == 0 ? "" : (String) txtJdbcDriver.getSelectedItem();
        currentDataSource.setParameter(RDBMSourceParameterConstants.DATABASE_DRIVER, driver);
        String url = txtJdbcUrl.getText();
        currentDataSource.setParameter(RDBMSourceParameterConstants.DATABASE_URL, url);

        if (url.endsWith(" ")) {
            lblConnectionStatus.setForeground(Color.RED);
            lblConnectionStatus.setText("Warning the URL ends with a white space, this can give rise to connection problems");
        } else if (driver.endsWith(" ")) {
            lblConnectionStatus.setForeground(Color.RED);
            lblConnectionStatus.setText("Warning the Driver class ends with a white space, this can give rise to connection problems");
        } else if (password.endsWith(" ")) {
            lblConnectionStatus.setForeground(Color.RED);
            lblConnectionStatus.setText("Warning the password ends with a white space, this can give rise to connection problems");
        } else if (username.endsWith(" ")) {
            lblConnectionStatus.setForeground(Color.RED);
            lblConnectionStatus.setText("Warning the password ends with a white space, this can give rise to connection problems");
        } else {
            lblConnectionStatus.setText("");
        }

        obdaModel.fireSourceParametersUpdated();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cmdHelp;
    private javax.swing.JButton cmdSave;
    private javax.swing.JButton cmdTestConnection;
    private javax.swing.JLabel lblConnectionStatus;
    private javax.swing.JLabel lblDatabasePassword;
    private javax.swing.JLabel lblDatabaseUsername;
    private javax.swing.JLabel lblJdbcDriver;
    private javax.swing.JLabel lblJdbcUrl;
    private javax.swing.JPanel pnlCommandButton;
    private javax.swing.JPanel pnlDataSourceParameters;
    private javax.swing.JPanel pnlInformation;
    private javax.swing.JPasswordField txtDatabasePassword;
    private javax.swing.JTextField txtDatabaseUsername;
    private javax.swing.JComboBox<String> txtJdbcDriver;
    private javax.swing.JTextField txtJdbcUrl;
    // End of variables declaration//GEN-END:variables

    private void currentDatasourceChange(OBDADataSource currentsource) {

        comboListener.setNotify(false);
        if (currentsource == null) {
            currentDataSource = null;
            txtJdbcDriver.setSelectedIndex(0);
            txtDatabaseUsername.setText("");
            txtDatabasePassword.setText("");
            txtJdbcUrl.setText("");
            lblConnectionStatus.setText("");

        } else {
            currentDataSource = currentsource;
            String driverClass = currentsource.getParameter(RDBMSourceParameterConstants.DATABASE_DRIVER);
            if(driverClass.isEmpty())
            {
                txtJdbcDriver.setSelectedIndex(0);
            }
            else
            {
                txtJdbcDriver.setSelectedItem(driverClass);
            }
            txtDatabaseUsername.setText(currentsource.getParameter(RDBMSourceParameterConstants.DATABASE_USERNAME));
            txtDatabasePassword.setText(currentsource.getParameter(RDBMSourceParameterConstants.DATABASE_PASSWORD));
            txtJdbcUrl.setText(currentsource.getParameter(RDBMSourceParameterConstants.DATABASE_URL));
            lblConnectionStatus.setText("");

        }
        comboListener.setNotify(true);
    }

    @Override
    public void datasourceChanged(OBDADataSource oldSource, OBDADataSource newSource) {

        currentDatasourceChange(newSource);


    }


}
