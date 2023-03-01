package org.rmj.cas.inventory.base.views;

import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyBooleanPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ShowMessageFX;

/**
 * FXML Controller class
 *
 * @author jovanalic
 * since 06-28-2021
 */
public class DateValidationController implements Initializable {

    @FXML private TextField txtField01;
    @FXML private Button btnOkay;
    @FXML private Button btnCancel;
    
    private boolean pbCancelled = true;
    private GRider oApp;
    private boolean pbLoaded = false;
    private final String pxeDateFormat = "yyyy-MM-dd";
    private final String pxeModuleName = "DailyProductionController";
    private final String pxeDateDefault = java.time.LocalDate.now().toString();
    
    
    public Date pdBegDate = null;
    public void setGRider(GRider foApp){oApp = foApp;}
    public boolean isCancelled(){return pbCancelled;}

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        btnCancel.setOnAction(this::cmdButton_Click);
        btnOkay.setOnAction(this::cmdButton_Click);
        
        txtField01.focusedProperty().addListener(txtField_Focus);
        txtField01.setOnKeyPressed(this::txtField_KeyPressed);
        txtField01.setText(CommonUtils.xsDateMedium(oApp.getServerDate()));
        pbLoaded = true;
    }

    private void cmdButton_Click(ActionEvent event) {
        String lsButton = ((Button)event.getSource()).getId();
        
        switch (lsButton){
            case "btnCancel":
                pbCancelled = true;
                pdBegDate = CommonUtils.toDate(txtField01.getText());
                break;
            case "btnOkay":
                pbCancelled = false;
                break;
        }
        
        unloadScene();
    }
    
    private void txtField_KeyPressed(KeyEvent event){
        TextField txtField = (TextField) event.getSource();
        
        switch (event.getCode()){
        case ENTER:
        case DOWN:
            CommonUtils.SetNextFocus(txtField);
            break;
        case UP:
            CommonUtils.SetPreviousFocus(txtField);
        }
    }
    
    final ChangeListener<? super Boolean> txtField_Focus = (o,ov,nv)->{
        if (!pbLoaded) return;
        
        TextField txtField = (TextField)((ReadOnlyBooleanPropertyBase)o).getBean();
        int lnIndex = Integer.parseInt(txtField.getId().substring(8, 10));
        String lsValue = txtField.getText();
        
        if (lsValue == null) return;
        
        if(!nv){ /*Lost Focus*/
            switch (lnIndex){
                case 1: /*dExpiryDt*/
                    if (CommonUtils.isDate(txtField.getText(), pxeDateFormat)){
                        if (CommonUtils.toDate(txtField.getText()).compareTo(CommonUtils.toDate(oApp.getServerDate().toString())) <0){
                            txtField.setText(CommonUtils.xsDateLong(CommonUtils.toDate(txtField.getText())));
                        }else{
                            txtField.setText(CommonUtils.xsDateLong(CommonUtils.toDate(pxeDateDefault)));
                        }
                    } else{
                        ShowMessageFX.Warning("Invalid date entry.", pxeModuleName, "Date format must be yyyy-MM-dd (e.g. 1991-07-07)");
                        txtField.setText(CommonUtils.xsDateLong(CommonUtils.toDate(pxeDateDefault)));
                    }
                    return;
            }
        } else{
            switch (lnIndex){
                case 1:
                    try{
                        txtField.setText(CommonUtils.xsDateShort(lsValue));
                    }catch(ParseException e){
                        ShowMessageFX.Error(e.getMessage(), pxeModuleName, null);
                    }
                    txtField.selectAll();
                    break;
                default:
            }
            txtField.selectAll();
        }
    };
    
    private Stage getStage(){
        return (Stage) btnOkay.getScene().getWindow();
    }
    
    private void unloadScene(){
        Stage stage = getStage();
        stage.close();
    }
    
}
