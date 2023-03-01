/**
 * Inventory Count BASE
 * @author Michael Torres Cuison
 * @since 2018.10.09
 */
package org.rmj.cas.inventory.base;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.SQLError;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.json.simple.JSONObject;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.cas.inventory.base.views.DateValidationController;
import org.rmj.cas.inventory.others.pojo.UnitInvCountDetailOthers;
import org.rmj.cas.inventory.pojo.UnitInvCountDetail;
import org.rmj.cas.inventory.pojo.UnitInvCountMaster;

public class InvCount{
    public InvCount(GRider foGRider, String fsBranchCD, boolean fbWithParent){
        this.poGRider = foGRider;
        
        if (foGRider != null){
            this.pbWithParent = fbWithParent;
            this.psBranchCd = fsBranchCD;
            
            this.psUserIDxx = foGRider.getUserID();
            pnEditMode = EditMode.UNKNOWN;
        }
    }
    
    public boolean BrowseRecord(String fsValue, boolean fbByCode){
        String lsHeader = "Trans. No»Inv. Type»Date";
        String lsColName = "a.sTransNox»b.sDescript»a.dTransact";
        String lsColCrit = "a.sTransNox»b.sDescript»a.dTransact";
        String lsSQL = getSQ_InvCount();
        JSONObject loJSON;
        
        if (fbByCode){
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox = " +  SQLUtil.toSQL(fsValue));
            
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            
            loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
        } else{
            loJSON = showFXDialog.jsonSearch(poGRider, 
                                                lsSQL, 
                                                fsValue, 
                                                lsHeader, 
                                                lsColName, 
                                                lsColCrit, 
                                                1);
        }
        
        if(loJSON == null)
            return false;
        else{
            return openTransaction((String) loJSON.get("sTransNox"));
        } 
    }
    
    public boolean addDetail() {
        if (paDetail.isEmpty()){
            paDetail.add(new UnitInvCountDetail());
            paDetailOthers.add(new UnitInvCountDetailOthers());
        } else{
            if (!paDetail.get(ItemCount()-1).getStockIDx().equals("")){ 
                paDetail.add(new UnitInvCountDetail());
                paDetailOthers.add(new UnitInvCountDetailOthers());
            }
        }
        return true;
    }

    public boolean deleteDetail(int fnRow) {
        paDetail.remove(fnRow);
        paDetailOthers.remove(fnRow);
        
        if (paDetail.isEmpty()){
            paDetail.add(new UnitInvCountDetail());
            paDetailOthers.add(new UnitInvCountDetailOthers());
        }
            
        return true;
    }
    
    public void setDetail(int fnRow, int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN){
            // Don't allow specific fields to assign values
            if(!(fnCol == poDetail.getColumn("sTransNox") ||
                fnCol == poDetail.getColumn("nEntryNox") ||
                fnCol == poDetail.getColumn("dModified"))){

                if (fnCol == poDetail.getColumn("nQtyOnHnd") ||
                    fnCol == poDetail.getColumn("nActCtr01") ||
                    fnCol == poDetail.getColumn("nActCtr02") ||
                    fnCol == poDetail.getColumn("nActCtr03") ||
                    fnCol == poDetail.getColumn("nFinalCtr")){
                    if (foData instanceof Number){
                        paDetail.get(fnRow).setValue(fnCol, foData);
                        addDetail();
                    }else paDetail.get(fnRow).setValue(fnCol, 0);
                } else paDetail.get(fnRow).setValue(fnCol, foData);   
            }
        }
    }

    public void setDetail(int fnRow, String fsCol, Object foData) {
        setDetail(fnRow, poDetail.getColumn(fsCol), foData);
    }
    
    public Object getDetail(int fnRow, int fnCol) {
        if(pnEditMode == EditMode.UNKNOWN)
         return null;
      else{
         return paDetail.get(fnRow).getValue(fnCol);
      }
    }

    public Object getDetail(int fnRow, String fsCol) {
        return getDetail(fnRow, poDetail.getColumn(fsCol));
    }
    
    public Object getDetailOthers(int fnRow, String fsCol){
        switch(fsCol){
            case "sStockIDx":
            case "sBarCodex":
            case "sDescript":
            case "sLocatnNm":
            case "sMeasurNm":
                return paDetailOthers.get(fnRow).getValue(fsCol);
            default:
                return null;
        }
    }

    public boolean newTransaction() {
        poData = new UnitInvCountMaster();
        Connection loConn = null;
        loConn = setConnection();
        
        poData = new UnitInvCountMaster();
        poData.setTransNox(MiscUtil.getNextCode(poData.getTable(), "sTransNox", true, loConn, psBranchCd));
        poData.setTransact(poGRider.getServerDate());
        poData.setInvTypCd("");
        
        //init detail
        paDetail = new ArrayList<>();
        paDetailOthers = new ArrayList<>(); //detail other info storage
        addDetail();
        
        pnEditMode = EditMode.ADDNEW;
        return true;
    }
    
    public boolean openTransaction(String fsTransNox){
        poData = loadTransaction(fsTransNox);
        
        if (poData != null) 
            paDetail = loadTransactionDetail(fsTransNox);
        else{
            setMessage("Unable to load transaction.");
            return false;
        } 
            
        pnEditMode = EditMode.READY;
        return true;
    }

    public UnitInvCountMaster loadTransaction(String fsTransNox) {
        UnitInvCountMaster loObject = new UnitInvCountMaster();
        
        Connection loConn = null;
        loConn = setConnection();   
        
        String lsSQL = MiscUtil.addCondition(getSQ_Master(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        try {
            if (!loRS.next()){
                setMessage("No Record Found");
            }else{
                //load each column to the entity
                for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                    loObject.setValue(lnCol, loRS.getObject(lnCol));
                }
            }              
        } catch (SQLException ex) {
            setErrMsg(ex.getMessage());
        } finally{
            MiscUtil.close(loRS);
            if (!pbWithParent) MiscUtil.close(loConn);
        }
        
        return loObject;
    }
    
    private ArrayList<UnitInvCountDetail> loadTransactionDetail(String fsTransNox){
        UnitInvCountDetail loOcc = null;
        UnitInvCountDetailOthers loOth = null;
        Connection loConn = null;
        loConn = setConnection();
        
        ArrayList<UnitInvCountDetail> loDetail = new ArrayList<>();
        paDetailOthers = new ArrayList<>(); //reset detail others
        
        ResultSet loRS = poGRider.executeQuery(
                            MiscUtil.addCondition(getSQ_Detail(), 
                                                    "sTransNox = " + SQLUtil.toSQL(fsTransNox)));
        try {
            for (int lnCtr = 1; lnCtr <= MiscUtil.RecordCount(loRS); lnCtr ++){
                loRS.absolute(lnCtr);

                loOcc = new UnitInvCountDetail();
                loOcc.setValue("sTransNox", loRS.getString("sTransNox"));        
                loOcc.setValue("nEntryNox", loRS.getInt("nEntryNox"));
                loOcc.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOcc.setValue("sLocatnCd", loRS.getString("sLocatnCd"));
                loOcc.setValue("nQtyOnHnd", loRS.getDouble("nQtyOnHnd"));
                loOcc.setValue("nActCtr01", loRS.getDouble("nActCtr01"));
                loOcc.setValue("nActCtr02", loRS.getDouble("nActCtr02"));
                loOcc.setValue("nActCtr03", loRS.getDouble("nActCtr03"));
                loOcc.setValue("nFinalCtr", loRS.getDouble("nFinalCtr"));
                loOcc.setValue("sRemarksx", loRS.getString("sRemarksx"));
                loOcc.setValue("dExpiryDt", loRS.getDate("dExpiryDt"));
                loOcc.setValue("dModified", loRS.getDate("dModified"));
                loDetail.add(loOcc);
                
                loOth = new UnitInvCountDetailOthers();
                loOth.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOth.setValue("sBarCodex", loRS.getString("sBarCodex"));
                loOth.setValue("sDescript", loRS.getString("sDescript"));
                loOth.setValue("sLocatnNm", loRS.getString("xLocatnNm"));
                if (loRS.getString("sMeasurNm")!=null){
                   loOth.setValue("sMeasurNm", loRS.getString("sMeasurNm"));
                }else{
                   loOth.setValue("sMeasurNm", "");
                }
                paDetailOthers.add(loOth);
            }
        } catch (SQLException e) {
            //log error message
            return null;
        }        
        
        return loDetail;
    }

    public boolean saveTransaction() {
        String lsSQL = "";
        boolean lbUpdate = false;
        
        UnitInvCountMaster loOldEnt = null;
        UnitInvCountMaster loNewEnt = null;
        UnitInvCountMaster loResult = null;
        
        // Check for the value of foEntity
        if (!(poData instanceof UnitInvCountMaster)) {
            setErrMsg("Invalid Entity Passed as Parameter");
            return false;
        }
        
        // Typecast the Entity to this object
        loNewEnt = (UnitInvCountMaster) poData;
        
        
        // Test if entry is ok
        if (loNewEnt.getInvTypCd()== null || loNewEnt.getInvTypCd().isEmpty()){
            setMessage("Invalid inventory type detected.");
            return false;
        }       
               
        if (!pbWithParent) poGRider.beginTrans();
        
        //delete empty detail
        if (paDetail.get(ItemCount()-1).getStockIDx().equals("")) deleteDetail(ItemCount()-1);
        
        // Generate the SQL Statement
        if (pnEditMode == EditMode.ADDNEW){
            Connection loConn = null;
            loConn = setConnection();

            String lsTransNox = MiscUtil.getNextCode(loNewEnt.getTable(), "sTransNox", true, loConn, psBranchCd);

            loNewEnt.setTransNox(lsTransNox);
            loNewEnt.setEntryNox(ItemCount());
            loNewEnt.setModified(psUserIDxx);
            loNewEnt.setDateModified(poGRider.getServerDate());

            if (!pbWithParent) MiscUtil.close(loConn);

            lbUpdate = saveDetail(loNewEnt.getTransNox());
            if (!lbUpdate) lsSQL = "";
            else lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt);
        }else{
            //Load previous transaction
            loOldEnt = loadTransaction(poData.getTransNox());

            loNewEnt.setEntryNox(ItemCount());
            loNewEnt.setDateModified(poGRider.getServerDate());

            lbUpdate = saveDetail(loNewEnt.getTransNox());
            if (!lbUpdate) lsSQL = "";            
            else lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt, (GEntity) loOldEnt, "sTransNox = " + SQLUtil.toSQL(loNewEnt.getValue(1)));
        }
                
        if (!lsSQL.equals("") && getErrMsg().isEmpty()){
            if(poGRider.executeQuery(lsSQL, loNewEnt.getTable(), "", "") == 0){
                if(!poGRider.getErrMsg().isEmpty())
                    setErrMsg(poGRider.getErrMsg());
                else 
                    setMessage("No record updated");
            } 
            lbUpdate = true;
        }
        
        if (!pbWithParent) {
            if (!getErrMsg().isEmpty()){
                poGRider.rollbackTrans();
            } else poGRider.commitTrans();
        }        
        
        return lbUpdate;
    }
    
    private boolean saveDetail(String fsTransNox){
        setMessage("");
        if (paDetail.isEmpty()){
            setMessage("Unable to save empty detail transaction.");
            return false;
        } 
        else if (paDetail.get(0).getStockIDx().equals("")){
            setMessage("Detail might not have item or zero quantity.");
            return false;
        }
        
        int lnCtr;
        String lsSQL;
        UnitInvCountDetail loNewEnt = null;
        
        if (pnEditMode == EditMode.ADDNEW){
            Connection loConn = null;
            loConn = setConnection();  
            
            for (lnCtr = 0; lnCtr <= paDetail.size() -1; lnCtr++){
                loNewEnt = paDetail.get(lnCtr);
                
                if (!loNewEnt.getStockIDx().equals("")){
                    loNewEnt.setTransNox(fsTransNox);
                    loNewEnt.setEntryNox(lnCtr + 1);
                    loNewEnt.setDateModified(poGRider.getServerDate());

                    lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt);

                    if (!lsSQL.equals("")){
                        if(poGRider.executeQuery(lsSQL, loNewEnt.getTable(), "", "") == 0){
                            if(!poGRider.getErrMsg().isEmpty()){
                                setErrMsg(poGRider.getErrMsg());
                                return false;
                            }
                        } 
                    }
                }
            }
        } else{
            ArrayList<UnitInvCountDetail> laSubUnit = loadTransactionDetail(poData.getTransNox());
            
            for (lnCtr = 0; lnCtr <= paDetail.size()-1; lnCtr++){
                loNewEnt = paDetail.get(lnCtr);
                
                if (!loNewEnt.getStockIDx().equals("")){
                    if (lnCtr <= laSubUnit.size()-1){
                        if (loNewEnt.getEntryNox() != lnCtr+1) loNewEnt.setEntryNox(lnCtr+1);
                        
                        lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt, 
                                                (GEntity) laSubUnit.get(lnCtr), 
                                                "sStockIDx = " + SQLUtil.toSQL(loNewEnt.getValue(1)) +
                                                " AND nEntryNox = " + SQLUtil.toSQL(loNewEnt.getValue(2)));

                    } else{
                        loNewEnt.setStockIDx(fsTransNox);
                        loNewEnt.setEntryNox(lnCtr + 1);
                        loNewEnt.setDateModified(poGRider.getServerDate());
                        lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt);
                    }
                    
                    if (!lsSQL.equals("")){
                        if(poGRider.executeQuery(lsSQL, loNewEnt.getTable(), "", "") == 0){
                            if(!poGRider.getErrMsg().isEmpty()){
                                setErrMsg(poGRider.getErrMsg());
                                return false;
                            }
                        } 
                    }
                } else{
                    for(int lnCtr2 = lnCtr; lnCtr2 <= laSubUnit.size()-1; lnCtr2++){
                        lsSQL = "DELETE FROM " + poDetail.getTable()+
                                " WHERE sStockIDx = " + SQLUtil.toSQL(laSubUnit.get(lnCtr2).getStockIDx()) +
                                    " AND nEntryNox = " + SQLUtil.toSQL(laSubUnit.get(lnCtr2).getEntryNox());

                        if (!lsSQL.equals("")){
                            if(poGRider.executeQuery(lsSQL, poDetail.getTable(), "", "") == 0){
                                if(!poGRider.getErrMsg().isEmpty()){
                                    setErrMsg(poGRider.getErrMsg());
                                    return false;
                                }
                            } 
                        }
                    }
                    break;
                }
            }
        }

        return true;
    }

    public boolean deleteTransaction(String string) {
        UnitInvCountMaster loObject = loadTransaction(string);
        boolean lbResult = false;
        
        if (loObject == null){
            setMessage("No record found...");
            return lbResult;
        }
        
        String lsSQL = "DELETE FROM " + loObject.getTable() + 
                        " WHERE sTransNox = " + SQLUtil.toSQL(string);
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("No record deleted.");  
        } else lbResult = true;
        
        //delete detail rows
        lsSQL = "DELETE FROM " + poDetail.getTable() +
                " WHERE sTransNox = " + SQLUtil.toSQL(string);
        
        if (poGRider.executeQuery(lsSQL, poDetail.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("No record deleted.");  
        } else lbResult = true;
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        
        return lbResult;
    }

    public boolean closeTransaction(String string) {
        UnitInvCountMaster loObject = loadTransaction(string);
        boolean lbResult = false;
        
        if (loObject == null){
            setMessage("No record found...");
            return lbResult;
        }
        
        if (!loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_OPEN)){
            setMessage("Unable to close closed/cancelled/posted/voided transaction.");
            return lbResult;
        }
        
        String lsSQL = "UPDATE " + loObject.getTable() + 
                        " SET  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CLOSED) + 
                            ", sModified = " + SQLUtil.toSQL(psUserIDxx) +
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                        " WHERE sTransNox = " + SQLUtil.toSQL(loObject.getTransNox());
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("No record deleted.");  
        } else lbResult = true;
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        return lbResult;
    }

    public boolean postTransaction(String string) {
        UnitInvCountMaster loObject = loadTransaction(string);
        boolean lbResult = false;
        
        if (loObject == null){
            setMessage("No record found...");
            return lbResult;
        }
        
        if (loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_POSTED) ||
                loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CANCELLED) ||
                loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_VOID)){
            setMessage("Unable to close proccesed transaction.");
            return lbResult;
        }
         
        if (!pbWithParent) poGRider.beginTrans();
        
        
        Date ldInvDate = null;
        boolean lbSveBegD=ShowMessageFX.YesNo("Begin new Inventory Period?", "Verify", null);

        if (lbSveBegD==true) {
            FXMLLoader fxmlLoader = new FXMLLoader();
//            fxmlLoader.setLocation(getClass().getResource("DateValidation.fxml"));
            fxmlLoader.setLocation(getClass().getResource("views/DateValidation.fxml"));
            DateValidationController datevalidation = new DateValidationController();
            datevalidation.setGRider(poGRider);

            fxmlLoader.setController(datevalidation);

            Parent parent;
            try {
                parent = fxmlLoader.load();
                Stage stage = new Stage();

                Scene scene = new Scene(parent);
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.initStyle(StageStyle.UNDECORATED);
                stage.setAlwaysOnTop(true);
                stage.setScene(scene);
                stage.showAndWait();

                if (!datevalidation.isCancelled()){
                    ldInvDate = datevalidation.pdBegDate;
                }else{
                    return false;
                }

                if (saveInvTrans()==true) {
                    if (lbSveBegD==true) {
                        trimLedger(ldInvDate);
                    }

                    String lsSQL = "UPDATE " + loObject.getTable() + " SET"  +
                                        "  sApproved = " + SQLUtil.toSQL(psUserIDxx) + 
                                        ", dApproved = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                                        ", cTranStat = "  + SQLUtil.toSQL(TransactionStatus.STATE_POSTED) + 
                                    " WHERE sTransNox = " + SQLUtil.toSQL(loObject.getTransNox());
                    
                    if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
                        if (!poGRider.getErrMsg().isEmpty()){
                            setErrMsg(poGRider.getErrMsg());
                        } else {
                            setErrMsg("No record updated.");
                        }  
                    } else 
                        return true;
                    }           
            } catch (IOException ex) {
                ShowMessageFX.Error(ex.getMessage(), "InvCount", "Please inform MIS department.");
                ex.printStackTrace();
                System.exit(1);
            }
        } 
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        return lbResult;
    }
    
    private boolean trimLedger(Date fdInvDate){
        ResultSet loRS = null;
        String lsSQL = "SELECT" +
                            "  b.sBarCodex" +
                            ", b.sDescript" +
                            ", a.*" +
                        " FROM Inv_Master a" +
                            ", Inventory b" +
                        " WHERE a.sStockIDx = b.sStockIDx" +
                            " AND a.sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode());
       
        loRS = poGRider.executeQuery(lsSQL);
        if (MiscUtil.RecordCount(loRS) == 0){
            
        } else{
            try {
                while(loRS.next()){
                        
                }
            } catch (SQLException e) {
                setMessage("Please inform MIS Department.");
                setErrMsg(e.getMessage());
                return false;
            }
        }
                            
        return true;
    }
  
    private boolean trimStocks(String fsStockIDx, Date fdTransact, Double fnBegQtyxx){
        ResultSet loRS = null;
        String lsSQL = "SELECT" +
                            "  dTransact" +
                            ", nQtyOnHnd" +
                        " FROM Inv_Ledger" +
                        " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx) +
                            " AND sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()) +
                            " AND dTransact <= " + SQLUtil.toSQL(fdTransact) +
                       " ORDER BY dTransact DESC, nLedgerNo DESC" +
                       " LIMIT 1";
        
        loRS = poGRider.executeQuery(lsSQL);
        if (MiscUtil.RecordCount(loRS) == 0){
            
        } else{
            try {
                loRS.first();
                lsSQL = "INSERT INTO Inv_Hist_Ledger" +
                            "( sStockIDx" +
                            ", sBranchCd" +
                            ", nLedgerNo" +
                            ", dTransact" +
                            ", sSourceCd" +
                            ", sSourceNo" +
                            ", nQtyInxxx" +
                            ", nQtyOutxx" +
                            ", nQtyOrder" +
                            ", nQtyIssue" +
                            ", nPurPrice" +
                            ", nUnitPrce" +
                            ", nQtyOnHnd" +
                            ", dExpiryxx" +
                            ", sModified" +
                            ", dModified" +
                        " SELECT *" + 
                        " FROM Inv_Ledger" +
                        " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx) +
                            " AND sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()) +
                            " AND dTransact <= " + SQLUtil.toSQL(fdTransact);
                
                if (poGRider.executeQuery(lsSQL, "Inv_Hist_Ledger", "", "") == 0){
                    if (!poGRider.getErrMsg().isEmpty()){
                        setErrMsg(poGRider.getErrMsg());
                    } else {
                        setErrMsg("No record updated.");
                    }  
                } else {
                    return false;
                }           
                        
                lsSQL = "DELETE FROM Inv_Ledger" +
                        " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx) +
                            " AND sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()) +
                            " AND dTransact <= " + SQLUtil.toSQL(fdTransact);

                if (poGRider.executeQuery(lsSQL, "Inv_Ledger", "", "") == 0){
                    if (!poGRider.getErrMsg().isEmpty()){
                        setErrMsg(poGRider.getErrMsg());
                    } else {
                        setErrMsg("No record updated.");
                    }  
                } else {
                    return false;
                }
   
                lsSQL = "UPDATE Inv_Master SET" +
                            "  nBegQtyxx = " + SQLUtil.toSQL(loRS.getDouble("nQtyOnHnd")) +
                            ", dBegInvxx = " + SQLUtil.toSQL(loRS.getDate("dTransact")) +
                        " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx) +
                            " AND sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode());
                
                if (poGRider.executeQuery(lsSQL, "Inv_Master", "", "") == 0){
                    if (!poGRider.getErrMsg().isEmpty()){
                        setErrMsg(poGRider.getErrMsg());
                    } else {
                        setErrMsg("No record updated.");
                    }  
                } else {
                    return false;
                }
      
                fnBegQtyxx = loRS.getDouble("nQtyOnHnd");
            } catch (SQLException ex) {
                Logger.getLogger(InvCount.class.getName()).log(Level.SEVERE, null, ex);
            }                        
        }
        return true;
    }
            
    public boolean voidTransaction(String string) {
        UnitInvCountMaster loObject = loadTransaction(string);
        boolean lbResult = false;
        
        if (loObject == null){
            setMessage("No record found...");
            return lbResult;
        }
        
        if (loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_POSTED) ||
                loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CANCELLED) ||
                loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_VOID)){
            setMessage("Unable to close processed transaction.");
            return lbResult;
        }
        
        String lsSQL = "UPDATE " + loObject.getTable() + 
                        " SET  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_VOID) + 
                            ", sModified = " + SQLUtil.toSQL(psUserIDxx) +
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                        " WHERE sTransNox = " + SQLUtil.toSQL(loObject.getTransNox());
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("No record deleted.");  
        } else lbResult = true;
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        return lbResult;
    }

    public boolean cancelTransaction(String string) {
        UnitInvCountMaster loObject = loadTransaction(string);
        boolean lbResult = false;
        
        if (loObject == null){
            setMessage("No record found...");
            return lbResult;
        }
        
        if (loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_POSTED) ||
                loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CANCELLED) ||
                loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_VOID)){
            setMessage("Unable to close processed transaction.");
            return lbResult;
        }
        
        String lsSQL = "UPDATE " + loObject.getTable() + 
                        " SET  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CANCELLED) + 
                            ", sModified = " + SQLUtil.toSQL(psUserIDxx) +
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                        " WHERE sTransNox = " + SQLUtil.toSQL(loObject.getTransNox());
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("No record deleted.");  
        } else lbResult = true;
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        return lbResult;
    }
    
    public boolean SearchDetail(int fnRow, int fnCol, String fsValue, boolean fbSearch, boolean fbByCode){
        String lsHeader;
        String lsColName;
        String lsColCrit;
        String lsSQL;

        JSONObject loJSON;
        ResultSet loRS;
        int lnRow;
        
        setErrMsg("");
        setMessage("");
        
        switch(fnCol){
            case 3:                
                lsSQL = MiscUtil.addCondition(getSQ_Stocks(), "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE));
                
                lsHeader = "Brand»Description»Unit»Model»Inv. Type»Barcode»Stock ID";
                lsColName = "xBrandNme»sDescript»sMeasurNm»xModelNme»xInvTypNm»sBarCodex»sStockIDx";
                lsColCrit = "b.sDescript»a.sDescript»f.sMeasurNm»c.sDescript»d.sDescript»a.sBarCodex»a.sStockIDx";
                
                if (fbByCode){
                    if (paDetailOthers.get(fnRow).getValue("sStockIDx").equals(fsValue)) return true;
                    
                    lsSQL = MiscUtil.addCondition(lsSQL, "a.sBarCodex = " + SQLUtil.toSQL(fsValue));
                    
                    loRS = poGRider.executeQuery(lsSQL);
                    
                    loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
                } else {
                    if (!fbSearch){
                        if (paDetailOthers.get(fnRow).getValue("sDescript").equals(fsValue)) return true;
                        
                        loJSON = showFXDialog.jsonSearch(poGRider, 
                                                            lsSQL, 
                                                            fsValue, 
                                                            lsHeader, 
                                                            lsColName, 
                                                            lsColCrit, 
                                                            1);
                    } else{
                        if (paDetailOthers.get(fnRow).getValue("sDescript").equals(fsValue)) return true;
                        
                        loJSON = showFXDialog.jsonSearch(poGRider, 
                                                            lsSQL, 
                                                            fsValue, 
                                                            lsHeader, 
                                                            lsColName, 
                                                            lsColCrit, 
                                                            2);
                    }
                        
                }
                
                if (loJSON != null){
                    setDetail(fnRow, fnCol, (String) loJSON.get("sStockIDx"));
                    setDetail(fnRow, "nQtyOnHnd", Double.valueOf((String) loJSON.get("nQtyOnHnd")));
                    
                    paDetailOthers.get(fnRow).setValue("sStockIDx", (String) loJSON.get("sStockIDx"));
                    paDetailOthers.get(fnRow).setValue("sBarCodex", (String) loJSON.get("sBarCodex"));
                    paDetailOthers.get(fnRow).setValue("sDescript", (String) loJSON.get("sDescript"));
                    paDetailOthers.get(fnRow).setValue("sMeasurNm", (String) loJSON.get("sMeasurNm"));
                    
                    return true;
                } else{
                    setDetail(fnRow, fnCol, "");
                    setDetail(fnRow, "nQtyOnHnd", 0);
                    
                    paDetailOthers.get(fnRow).setValue("sStockIDx", "");
                    paDetailOthers.get(fnRow).setValue("sBarCodex", "");
                    paDetailOthers.get(fnRow).setValue("sDescript", "");
                    paDetailOthers.get(fnRow).setValue("sMeasurNm", "");
                    
                    return false;
                }
            case 4: //sLocatnCd
                lsHeader = "Code»Brief Desc.»Description";
                lsColName = "sLocatnCd»sBriefDsc»sDescript";
                lsColCrit = "sLocatnCd»sBriefDsc»sDescript";
                
                lsSQL = "SELECT " +
                            "  sLocatnCd" +
                            ", sBriefDsc" + 
                            ", sDescript" + 
                            ", cRecdStat" + 
                        " FROM Inv_Location";
                
                lsSQL = MiscUtil.addCondition(lsSQL, "cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE));
                
                if (fbByCode){
                    if (getDetail(fnRow, "sLocatnCd").equals(fsValue)) return true;
                    
                    lsSQL = MiscUtil.addCondition(lsSQL, "sLocatnCd = " + SQLUtil.toSQL(fsValue));
                    
                    loRS = poGRider.executeQuery(lsSQL);
                    
                    loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
                } else {
                    if (!fbSearch){
                        if (getDetail(fnRow, "sLocatnCd").equals(fsValue)) return true;
                        
                        loJSON = showFXDialog.jsonSearch(poGRider, lsSQL, fsValue, lsHeader, lsColName, lsColCrit, 0);
                    } else{
                        if (paDetailOthers.get(fnRow).getValue("sDescript").equals(fsValue)) return true;
                        
                        loJSON = showFXDialog.jsonSearch(poGRider, lsSQL, fsValue, lsHeader, lsColName, lsColCrit, 2);
                    }
                        
                }
                
                if (loJSON != null){
                    setMaster(fnCol, (String) loJSON.get("sLocatnCd"));
                    paDetailOthers.get(fnRow).setValue("sLocatnNm", (String) loJSON.get("sDescript"));
                    return true;
                } else {
                    setMaster(fnCol, "");
                    paDetailOthers.get(fnRow).setValue("sLocatnNm", "");
                    return false;
                }
            default:
                return false;
        }
    }
    
    public boolean SearchDetail(int fnRow, String fsCol, String fsValue, boolean fbSearch, boolean fbByCode){
        return SearchDetail(fnRow, poDetail.getColumn(fsCol), fsValue, fbSearch, fbByCode);
    }
    
    public String SearchMaster(int fnCol, String fsValue, boolean fbByCode){
        String lsHeader;
        String lsColName;
        String lsColCrit;
        String lsSQL;
        ResultSet loRS;
        JSONObject loJSON;
        
        if (fsValue.equals("") && fbByCode) return "";
        
        switch(fnCol){
            case 2: //sInvTypCd
                lsHeader = "Code»Name";
                lsColName = "sInvTypCd»sDescript";
                lsColCrit = "sInvTypCd»sDescript";
                
                lsSQL = "SELECT " +
                            "  sInvTypCd" +
                            ", sDescript" + 
                            ", cRecdStat" + 
                        " FROM Inv_Type";
                
                if (fbByCode){
                    lsSQL = MiscUtil.addCondition(lsSQL, "sInvTypCd = " + SQLUtil.toSQL(fsValue));
                    
                    loRS = poGRider.executeQuery(lsSQL);
                    
                    loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
                } else
                    loJSON = showFXDialog.jsonSearch(poGRider, 
                                                        lsSQL, 
                                                        fsValue, 
                                                        lsHeader, 
                                                        lsColName, 
                                                        lsColCrit, 1);
                
                if (loJSON == null){
                    setMaster(fnCol, "");
                    return "";
                } else {
                    setMaster(fnCol, (String) loJSON.get("sInvTypCd"));
                    return (String) loJSON.get("sDescript");
                }
            default:
                return "";
        }
    }
    
    public String SearchMaster(String fsCol, String fsValue, boolean fbByCode){
        return SearchMaster(poData.getColumn(fsCol), fsValue, fbByCode);
    }
    
    public void setMaster(int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN){
            // Don't allow specific fields to assign valuesa
            if(!(fnCol == poData.getColumn("sTransNox") ||
                fnCol == poData.getColumn("nEntryNox") ||
                fnCol == poData.getColumn("cTranStat") ||
                fnCol == poData.getColumn("sModified") ||
                fnCol == poData.getColumn("dModified"))){
                
                poData.setValue(fnCol, foData);   
            }
        }
    }
    
    private boolean saveInvTrans(){
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();
        
        ResultSet loRS = null;
        String lsSQL = "";
        
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            if (paDetail.get(lnCtr).getStockIDx().equals("")) break;
            
            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "nQuantity", paDetail.get(lnCtr).getFinalCtr());
            
            lsSQL = "SELECT" +
                        "  nQtyOnHnd" +
                        ", nResvOrdr" +
                        ", nBackOrdr" +
                        ", nLedgerNo" +
                    " FROM Inv_Master" + 
                    " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) + 
                        " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);
            
            loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) == 0){
                loInvTrans.setDetail(lnCtr, "nQtyOnHnd", 0);
                loInvTrans.setDetail(lnCtr, "nResvOrdr", 0);
                loInvTrans.setDetail(lnCtr, "nBackOrdr", 0);
            } else{
                try {
                    loRS.first();
                    loInvTrans.setDetail(lnCtr, "nQtyOnHnd", loRS.getDouble("nQtyOnHnd"));
                    loInvTrans.setDetail(lnCtr, "nResvOrdr", loRS.getDouble("nResvOrdr"));
                    loInvTrans.setDetail(lnCtr, "nBackOrdr", loRS.getDouble("nBackOrdr"));
                    loInvTrans.setDetail(lnCtr, "nLedgerNo", loRS.getInt("nLedgerNo"));
                } catch (SQLException e) {
                    setMessage("Please inform MIS Department.");
                    setErrMsg(e.getMessage());
                    return false;
                }
            }
           
            if (paDetail.get(lnCtr).getQtyOnHnd().doubleValue() <paDetail.get(lnCtr).getFinalCtr().doubleValue()){
                if (!loInvTrans.CreditMemo(poData.getTransNox(), poData.getTransact(), EditMode.ADDNEW)){
                    setMessage(loInvTrans.getMessage());
                    setErrMsg(loInvTrans.getErrMsg());
                    return false;
                }
            }else if (paDetail.get(lnCtr).getQtyOnHnd().doubleValue()>paDetail.get(lnCtr).getFinalCtr().doubleValue()){
                if (!loInvTrans.DebitMemo(poData.getTransNox(), poData.getTransact(), EditMode.ADDNEW)){
                    setMessage(loInvTrans.getMessage());
                    setErrMsg(loInvTrans.getErrMsg());
                    return false;
                }
            }
        }
        
//        if (paDetail.get(lnCtr).getStockIDx() == 0){
//        
//        }
        
        //TODO
        //update branch order info
        return saveInvExpiration(poData.getTransact());
    }
    
    private boolean saveInvExpiration(Date fdTransact){
        InvExpiration loInvTrans = new InvExpiration(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();
        
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            if (paDetail.get(lnCtr).getStockIDx().equals("")) break;
            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "dExpiryDt", paDetail.get(lnCtr).getDateExpiry());
            loInvTrans.setDetail(lnCtr, "nQtyInxxx", paDetail.get(lnCtr).getFinalCtr());
        }
        
        
        if (!loInvTrans.DailyProduction_IN(fdTransact, EditMode.ADDNEW)){
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }
       
        //TODO
            //update branch order info
    
        return true;
    }

    public void setMaster(String fsCol, Object foData) {
        setMaster(poData.getColumn(fsCol), foData);
    }

    public Object getMaster(int fnCol) {
        if(pnEditMode == EditMode.UNKNOWN)
         return null;
      else{
         return poData.getValue(fnCol);
      }
    }

    public Object getMaster(String fsCol) {
        return getMaster(poData.getColumn(fsCol));
    }

    public String getMessage() {
        return psWarnMsg;
    }

    public void setMessage(String string) {
        psWarnMsg = string;
    }

    public String getErrMsg() {
        return psErrMsgx;
    }

    public void setErrMsg(String string) {
        psErrMsgx = string;
    }

    public void setBranch(String string) {
        psBranchCd = string;
    }

    public void setWithParent(boolean bln) {
        pbWithParent = bln;
    }

    public String getSQ_Master() {
        return MiscUtil.makeSelect(new UnitInvCountMaster());
    }
    
    private String getSQ_Detail(){
        return "SELECT" +
                    "  a.sTransNox" +
                    ", a.nEntryNox" +
                    ", a.sStockIDx" +
                    ", a.sLocatnCd" +
                    ", a.nQtyOnHnd" +
                    ", a.nActCtr01" +
                    ", a.nActCtr02" +
                    ", a.nActCtr03" +
                    ", a.nFinalCtr" +
                    ", a.sRemarksx" +
                    ", a.dModified" +
                    ", c.sBarCodex" +     
                    ", c.sDescript" +
                    ", IFNULL(d.sDescript, '') xLocatnNm" +
                    ", a.dExpiryDt" +
                    ", e.sMeasurNm" +
                " FROM Inv_Count_Detail a" +
                        " LEFT JOIN Inv_Location d" +
                            " ON a.sLocatnCd = d.sLocatnCd" + 
                    ", Inv_Master b" +
                        " LEFT JOIN Inventory c" +
                            " ON b.sStockIDx = c.sStockIDx" +
                         " LEFT JOIN Measure e" + 
                            " ON c.sMeasurID = e.sMeasurID" + 
                " WHERE a.sStockIDx = b.sStockIDx" +
                    " AND b.sBranchCD = " + SQLUtil.toSQL(poGRider.getBranchCode());
    }
    
    public int ItemCount(){
        return paDetail.size();
    }
    
    private Connection setConnection(){
        Connection foConn;
        
        if (pbWithParent){
            foConn = (Connection) poGRider.getConnection();
            if (foConn == null) foConn = (Connection) poGRider.doConnect();
        }else foConn = (Connection) poGRider.doConnect();
        
        return foConn;
    }  
    
    public int getEditMode(){return pnEditMode;}
    
    private String getSQ_InvCount(){
        String lsTranStat = String.valueOf(pnTranStat);
        String lsCondition = "";
        String lsSQL = "SELECT " +
                            "  a.sTransNox" +
                            ", b.sDescript" + 
                            ", a.dTransact" + 
                        " FROM Inv_Count_Master a" + 
                            " LEFT JOIN Inv_Type b" + 
                                " ON a.sInvTypCd = b.sInvTypCd" + 
                        " WHERE a.sTransNox LIKE " + SQLUtil.toSQL(psBranchCd + "%");
        
        if (lsTranStat.length() == 1) {
            lsCondition = "a.cTranStat = " + SQLUtil.toSQL(lsTranStat);
        } else {
            for (int lnCtr = 0; lnCtr <= lsTranStat.length() -1; lnCtr++){
                lsCondition = lsCondition + SQLUtil.toSQL(String.valueOf(lsTranStat.charAt(lnCtr))) + ",";
            }
            lsCondition = "(" + lsCondition.substring(0, lsCondition.length()-1) + ")";
            lsCondition = "a.cTranStat IN " + lsCondition;
        }
        
        lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        return lsSQL;
    }
    
    private String getSQ_Stocks(){
        String lsSQL = "SELECT " +
                            "  a.sStockIDx" +
                            ", a.sBarCodex" + 
                            ", a.sDescript" + 
                            ", a.sBriefDsc" + 
                            ", a.sAltBarCd" + 
                            ", a.sCategCd1" + 
                            ", a.sCategCd2" + 
                            ", a.sCategCd3" + 
                            ", a.sCategCd4" + 
                            ", a.sBrandCde" + 
                            ", a.sModelCde" + 
                            ", a.sColorCde" + 
                            ", a.sInvTypCd" + 
                            ", a.nUnitPrce" + 
                            ", a.nSelPrice" + 
                            ", a.nDiscLev1" + 
                            ", a.nDiscLev2" + 
                            ", a.nDiscLev3" + 
                            ", a.nDealrDsc" + 
                            ", a.cComboInv" + 
                            ", a.cWthPromo" + 
                            ", a.cSerialze" + 
                            ", a.cUnitType" + 
                            ", a.cInvStatx" + 
                            ", a.sSupersed" + 
                            ", a.cRecdStat" + 
                            ", b.sDescript xBrandNme" + 
                            ", c.sDescript xModelNme" + 
                            ", d.sDescript xInvTypNm" + 
                            ", e.nQtyOnHnd" + 
                            ", f.sMeasurNm" +
                        " FROM Inventory a" + 
                                " LEFT JOIN Brand b" + 
                                    " ON a.sBrandCde = b.sBrandCde" + 
                                " LEFT JOIN Model c" + 
                                    " ON a.sModelCde = c.sModelCde" + 
                                " LEFT JOIN Inv_Type d" + 
                                    " ON a.sInvTypCd = d.sInvTypCd" + 
                                " LEFT JOIN Measure f" +
                                    " ON a.sMeasurID = f.sMeasurID" +
                            ", Inv_Master e" + 
                        " WHERE a.sStockIDx = e.sStockIDx" + 
                            " AND e.sBranchCd = " + SQLUtil.toSQL(psBranchCd);
        
                //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty())
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type")));
        
        return lsSQL;
    }
    
    public void printColumnsMaster(){poData.list();}
    public void printColumnsDetail(){poDetail.list();}
    public void setTranStat(int fnValue){this.pnTranStat = fnValue;}
    
    //Member Variables
    private GRider poGRider = null;
    private String psUserIDxx = "";
    private String psBranchCd = "";
    private String psWarnMsg = "";
    private String psErrMsgx = "";
    private boolean pbWithParent = false;
    private int pnEditMode;
    private int pnTranStat = 0;
    
    private UnitInvCountMaster poData = new UnitInvCountMaster();
    private UnitInvCountDetail poDetail = new UnitInvCountDetail();
    private ArrayList<UnitInvCountDetail> paDetail;
    private ArrayList<UnitInvCountDetailOthers> paDetailOthers;
}
