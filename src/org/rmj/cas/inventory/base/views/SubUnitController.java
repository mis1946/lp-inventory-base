/**
 * Michael Torres Cuison
 *
 * @since 2018-11-09
 */
package org.rmj.cas.inventory.base.views;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ShowMessageFX;


public class SubUnitController implements Initializable {

    @FXML
    private Button btnExit;
    @FXML
    private TableView table;
    @FXML
    private TableColumn index01;
    @FXML
    private TableColumn index02;
    @FXML
    private TableColumn index03;
    @FXML
    private TableColumn index04;
    @FXML
    private TableColumn index05;
    @FXML
    private TableColumn index06;
    @FXML
    private TableColumn index07;
    @FXML
    private TextField txtField03;
    @FXML
    private TextField txtField80;
    @FXML
    private TextField txtField82;
    @FXML
    private TextField txtField81;
    @FXML
    private Button btnOk;
    @FXML
    private Button btnCancel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        pbCancelled = true;
        psValue = "";
        
        btnExit.setOnAction(this::cmdButton_Click);
        btnOk.setOnAction(this::cmdButton_Click);
        btnCancel.setOnAction(this::cmdButton_Click);
        
        txtField03.setText(sBarCodex);
        txtField80.setText(sDescript);
        txtField81.setText(sMeasurNm);
        txtField82.setText(sInvTypNm);
        
        initGridLedger();
        loadDetail2Grid();
    }    
    
    public void cmdButton_Click(ActionEvent event) {
        String lsButton = ((Button)event.getSource()).getId();
               
        switch (lsButton){
            case "btnExit":
            case "btnCancel":
                CommonUtils.closeStage(btnExit);
                break;
            case "btnOk":
                pbCancelled = false;
                CommonUtils.closeStage(btnOk);
                break;
            default: 
                ShowMessageFX.Warning(null, pxeModuleName, "Button with name " + lsButton + " not registered.");
        }     
    }
    
     private void initGridLedger(){
        index01.setStyle("-fx-alignment: CENTER;");
        index02.setStyle("-fx-alignment: CENTER-LEFT;");
        index03.setStyle("-fx-alignment: CENTER-LEFT;");
        index04.setStyle("-fx-alignment: CENTER-LEFT;");
        index05.setStyle("-fx-alignment: CENTER;");
        index06.setStyle("-fx-alignment: CENTER;");
        index07.setStyle("-fx-alignment: CENTER;");
     
        index01.setCellValueFactory(new PropertyValueFactory<org.rmj.cas.inventory.base.views.TableModel,String>("index01"));
        index02.setCellValueFactory(new PropertyValueFactory<org.rmj.cas.inventory.base.views.TableModel,String>("index02"));
        index03.setCellValueFactory(new PropertyValueFactory<org.rmj.cas.inventory.base.views.TableModel,String>("index03"));
        index04.setCellValueFactory(new PropertyValueFactory<org.rmj.cas.inventory.base.views.TableModel,String>("index04"));
        index05.setCellValueFactory(new PropertyValueFactory<org.rmj.cas.inventory.base.views.TableModel,String>("index05"));
        index06.setCellValueFactory(new PropertyValueFactory<org.rmj.cas.inventory.base.views.TableModel,String>("index06"));
        index07.setCellValueFactory(new PropertyValueFactory<org.rmj.cas.inventory.base.views.TableModel,String>("index07"));
        
        table.setItems(data);
    }
    
    public void loadDetail2Grid() {               
        data.clear();
        
        if (poRS == null) return;
        
        try {
            poRS.first();
            for (int lnCtr = 1; lnCtr <= MiscUtil.RecordCount(poRS); lnCtr++){
               
                    poRS.absolute(lnCtr);
                    data.add(new TableModel(String.valueOf(lnCtr), 
                                            poRS.getString("sStockIDx"),
                                            poRS.getString("sBarCodex"),
                                            poRS.getString("sDescript"),
                                            poRS.getString("nQtyOnHnd"),
                                            poRS.getString("sMeasurNm"),
                                            "1:" + poRS.getString("nQuantity"),
                                            "",
                                            "",
                                            ""));
            }
            
            pnRow = 0;
            table_Clicked();
        } catch (SQLException ex) {
            ShowMessageFX.Error(ex.getMessage(), pxeModuleName, "Please inform MIS Department.");
            System.exit(1);
        }
    }
    
    @FXML
    private void table_Clicked(){
        try {
            poRS.absolute(pnRow + 1);
            
            psValue = poRS.getString("sStockIDx") + "»" + poRS.getString("nQuantity");
        } catch (SQLException e) {
            ShowMessageFX.Error(getStage(), e.getMessage(), pxeModuleName, "Please inform MIS Department.");
            System.exit(1);
        }
    }
    
    private void table_Clicked(MouseEvent event) {
        pnRow = table.getSelectionModel().getSelectedIndex();
        table_Clicked();
    }
    
    private Stage getStage(){
        return (Stage) btnOk.getScene().getWindow();
    }
    
    private ObservableList<TableModel> data = FXCollections.observableArrayList();
    public void setBarCodex(String fsBarCodex){sBarCodex = fsBarCodex;}
    public void setDescript(String fsDescript){sDescript = fsDescript;}
    public void setMeasurNm(String fsMeasurNm){sMeasurNm = fsMeasurNm;}
    public void setInvTypNm(String fsInvTypNm){sInvTypNm = fsInvTypNm;}
    
    public String getBarCodex(){return sBarCodex;}
    public String getDescript(){return sDescript;}
    public String getMeasurNm(){return sMeasurNm;}
    public String getInvTypNm(){return sInvTypNm;}
    public String getValue(){return psValue;}
    public boolean isCancelled(){return pbCancelled;}
    
    public void setParentUnits(ResultSet foRS){this.poRS = foRS;}
    
    public void setGRider(GRider foGRider){this.poGRider = foGRider;}
   
    private static GRider poGRider;
    private final String pxeModuleName = "SubUnitController";
    private static String sMeasurNm = "";
    private static String sBarCodex = "";
    private static String sDescript = "";
    private static String sInvTypNm = "";
    
    private ResultSet poRS = null;
    private boolean pbCancelled;
    private String psValue;
    private int pnRow;
}
