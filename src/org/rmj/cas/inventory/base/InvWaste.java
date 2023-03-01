/**
 * Inventory Waste BASE
 * @author Michael Torres Cuison
 * @since 2018.10.10
 */
package org.rmj.cas.inventory.base;

import com.mysql.jdbc.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.appdriver.constants.TransactionStatus;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.appdriver.constants.UserRight;
import org.rmj.cas.inventory.others.pojo.UnitInvWasteDetailOthers;
import org.rmj.cas.inventory.pojo.UnitInvWasteDetail;
import org.rmj.cas.inventory.pojo.UnitInvWasteMaster;
import org.rmj.appdriver.agentfx.callback.IMasterDetail;

public class InvWaste{
    private final String MODULENAME = "InvWaste";
    
    public InvWaste(GRider foGRider, String fsBranchCD, boolean fbWithParent){
        this.poGRider = foGRider;
        
        if (foGRider != null){
            this.pbWithParent = fbWithParent;
            this.psBranchCd = fsBranchCD;
            
            this.psUserIDxx = foGRider.getUserID();
            pnEditMode = EditMode.UNKNOWN;
        }
    }
    
    public boolean BrowseRecord(String fsValue, boolean fbByCode){
        String lsHeader = "Trans. No»Date";
        String lsColName = "sTransNox»dTransact";
        String lsColCrit = "a.sTransNox»a.dTransact";
        String lsSQL = getSQ_InvWaste();
        JSONObject loJSON;
        
        loJSON = showFXDialog.jsonSearch(poGRider, 
                                            lsSQL, 
                                            fsValue, 
                                            lsHeader, 
                                            lsColName, 
                                            lsColCrit, 
                                            fbByCode ? 0 : 1);
        
        if(loJSON == null)
            return false;
        else
            return openTransaction((String) loJSON.get("sTransNox"));
    }
    
    public boolean addDetail() {
        if (paDetail.isEmpty()){
            paDetail.add(new UnitInvWasteDetail());
            
            paDetailOthers.add(new UnitInvWasteDetailOthers());
        } else{
            if (!paDetail.get(ItemCount()-1).getStockIDx().equals("") &&
                    paDetail.get(ItemCount() -1).getQuantity().doubleValue() != 0.00){
                paDetail.add(new UnitInvWasteDetail());
                
                paDetailOthers.add(new UnitInvWasteDetailOthers());
            }
        }
        return true;
    }

    public boolean deleteDetail(int fnRow) {
        paDetail.remove(fnRow);
        paDetailOthers.remove(fnRow);
        
        if (paDetail.isEmpty()){
            paDetail.add(new UnitInvWasteDetail());
            paDetailOthers.add(new UnitInvWasteDetailOthers());
        }

        return true;
    }
    
    public void setDetail(int fnRow, int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN){
            // Don't allow specific fields to assign values
            if(!(fnCol == poDetail.getColumn("sTransNox") ||
                fnCol == poDetail.getColumn("nEntryNox") ||
                fnCol == poDetail.getColumn("dModified"))){

                if (fnCol == poDetail.getColumn("nQuantity")){
                    if (foData instanceof Double){                       
                        if (Double.valueOf(foData.toString()) > Double.valueOf(paDetailOthers.get(fnRow).getValue("xQtyOnHnd").toString())){
                            ShowMessageFX.Information("Quantity to deduct is greater than the quantity on hand.", MODULENAME, "Invalid quantity");
                        } else {
                            paDetail.get(fnRow).setValue(fnCol, foData);
                            addDetail();
                        }
                    }else paDetail.get(fnRow).setValue(fnCol, 0);
                } else if (fnCol == poDetail.getColumn("nInvCostx")){
                    if (foData instanceof Number){
                        paDetail.get(fnRow).setValue(fnCol, foData);
                    }else paDetail.get(fnRow).setValue(fnCol, 0.00);
                } else paDetail.get(fnRow).setValue(fnCol, foData);   
                
                DetailRetreived(fnCol);
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
            case "xQtyOnHnd":
            case "sMeasurNm":
                return paDetailOthers.get(fnRow).getValue(fsCol);
            default:
                return null;
        }
    }

    public boolean newTransaction() {
        Connection loConn = null;
        loConn = setConnection();
        
        poData = new UnitInvWasteMaster();
        poData.setTransNox(MiscUtil.getNextCode(poData.getTable(), "sTransNox", true, loConn, psBranchCd));
        poData.setTransact(poGRider.getServerDate());
        
        //init detail
        paDetail = new ArrayList<>();      
        paDetailOthers = new ArrayList<>(); //detail other info storage
        
        pnEditMode = EditMode.ADDNEW;
        
        addDetail();
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

    public UnitInvWasteMaster loadTransaction(String fsTransNox) {
        UnitInvWasteMaster loObject = new UnitInvWasteMaster();
        
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
    
    public ResultSet getExpiration(String fsStockIDx){
        String lsSQL = "SELECT * FROM Inv_Master_Expiration" +
                        " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx) +
                            " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                            " AND nQtyOnHnd > 0" +
                        " ORDER BY dExpiryDt";     
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        return loRS;
    }
    
    private ArrayList<UnitInvWasteDetail> loadTransactionDetail(String fsTransNox){
        UnitInvWasteDetail loOcc = null;
        UnitInvWasteDetailOthers loOth = null;
        Connection loConn = null;
        loConn = setConnection();
        
        ArrayList<UnitInvWasteDetail> loDetail = new ArrayList<>();
        paDetailOthers = new ArrayList<>(); //reset detail others
        
        ResultSet loRS = poGRider.executeQuery(
                            MiscUtil.addCondition(getSQ_Detail(), 
                                                    "sTransNox = " + SQLUtil.toSQL(fsTransNox)));
        try {
            for (int lnCtr = 1; lnCtr <= MiscUtil.RecordCount(loRS); lnCtr ++){
                loRS.absolute(lnCtr);

                loOcc = new UnitInvWasteDetail();
                loOcc.setValue("sTransNox", loRS.getString("sTransNox"));        
                loOcc.setValue("nEntryNox", loRS.getInt("nEntryNox"));
                loOcc.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOcc.setValue("nQuantity", loRS.getDouble("nQuantity"));
                loOcc.setValue("nInvCostx", loRS.getDouble("nInvCostx"));
                loOcc.setValue("sNotesxxx", loRS.getString("sNotesxxx"));
                loOcc.setValue("dExpiryDt", loRS.getDate("dExpiryDt"));
                loOcc.setValue("dModified", loRS.getDate("dModified"));
                loDetail.add(loOcc);
                
                loOth = new UnitInvWasteDetailOthers();
                loOth.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOth.setValue("sBarCodex", loRS.getString("sBarCodex"));
                loOth.setValue("sDescript", loRS.getString("sDescript"));
                loOth.setValue("xQtyOnHnd", loRS.getDouble("xQtyOnHnd"));
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
        
        UnitInvWasteMaster loOldEnt = null;
        UnitInvWasteMaster loNewEnt = null;
        UnitInvWasteMaster loResult = null;
        
        // Check for the value of foEntity
        if (!(poData instanceof UnitInvWasteMaster)) {
            setErrMsg("Invalid Entity Passed as Parameter");
            return false;
        }
        
        // Typecast the Entity to this object
        loNewEnt = (UnitInvWasteMaster) poData;        
               
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
        else if (paDetail.get(0).getStockIDx().equals("") ||
                paDetail.get(0).getQuantity().doubleValue() == 0.00){
            setMessage("Detail might not have item or zero quantity.");
            return false;
        }
        
        int lnCtr;
        String lsSQL;
        UnitInvWasteDetail loNewEnt = null;
        
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
            ArrayList<UnitInvWasteDetail> laSubUnit = loadTransactionDetail(poData.getTransNox());
            
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
        UnitInvWasteMaster loObject = loadTransaction(string);
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
        UnitInvWasteMaster loObject = loadTransaction(string);
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
        if (poGRider.getUserLevel() <= UserRight.ENCODER){
            JSONObject loJSON = showFXDialog.getApproval(poGRider);
            
            if (loJSON == null){
                ShowMessageFX.Warning("Approval failed.", pxeModuleName, "Unable to post transaction");
            }
            
            if ((int) loJSON.get("nUserLevl") <= UserRight.ENCODER){
                ShowMessageFX.Warning("User account has no right to approve.", pxeModuleName, "Unable to post transaction");
                return false;
            }
        }
        
        UnitInvWasteMaster loObject = loadTransaction(string);
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
        
        String lsSQL = "UPDATE " + loObject.getTable() + 
                        " SET  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_POSTED) + 
                            ", sModified = " + SQLUtil.toSQL(psUserIDxx) +
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                        " WHERE sTransNox = " + SQLUtil.toSQL(loObject.getTransNox());
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("No record deleted.");  
        } else lbResult = saveInvTrans();
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        return lbResult;
    }

    public boolean voidTransaction(String string) {
        UnitInvWasteMaster loObject = loadTransaction(string);
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
        UnitInvWasteMaster loObject = loadTransaction(string);
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
        String lsHeader = "";
        String lsColName = "";
        String lsColCrit = "";
        String lsSQL = "";
        JSONObject loJSON;
        
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
                    
                    ResultSet loRS = poGRider.executeQuery(lsSQL);
                    
                    loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
                } else {
                    loJSON = showFXDialog.jsonSearch(poGRider, 
                                                        lsSQL, 
                                                        fsValue, 
                                                        lsHeader, 
                                                        lsColName, 
                                                        lsColCrit, 
                                                        fbSearch ? 2 : 1);
                }
                
                if (loJSON != null){
                    setDetail(fnRow, fnCol, (String) loJSON.get("sStockIDx"));
                    setDetail(fnRow, "nInvCostx", Double.valueOf((String) loJSON.get("nUnitPrce")));
                    
                    paDetailOthers.get(fnRow).setValue("sStockIDx", (String) loJSON.get("sStockIDx"));
                    paDetailOthers.get(fnRow).setValue("sBarCodex", (String) loJSON.get("sBarCodex"));
                    paDetailOthers.get(fnRow).setValue("sDescript", (String) loJSON.get("sDescript"));
                    paDetailOthers.get(fnRow).setValue("xQtyOnHnd", Double.valueOf((String) loJSON.get("nQtyOnHnd")));
                    paDetailOthers.get(fnRow).setValue("sMeasurNm", (String) loJSON.get("sMeasurNm"));
                    
                    return true;
                } else{
                    setDetail(fnRow, fnCol, "");
                    
                    paDetailOthers.get(fnRow).setValue("sStockIDx", "");
                    paDetailOthers.get(fnRow).setValue("sBarCodex", "");
                    paDetailOthers.get(fnRow).setValue("sDescript", "");
                    paDetailOthers.get(fnRow).setValue("xQtyOnHnd", 0);
                    paDetailOthers.get(fnRow).setValue("sMeasurNm", "");
                    return false;
                }
            default:
                return false;
        }
    }
    
    public boolean SearchDetail(int fnRow, String fsCol, String fsValue, boolean fbSearch, boolean fbByCode){
        return SearchDetail(fnRow, poDetail.getColumn(fsCol), fsValue, fbSearch, fbByCode);
    }
    
    private boolean saveInvTrans(){
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();
        
        ResultSet loRS = null;
        String lsSQL = "";
        
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            if (paDetail.get(lnCtr).getStockIDx().equals("")) break;
            
            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "nQuantity", paDetail.get(lnCtr).getQuantity());
            
            lsSQL = "SELECT" +
                        "  nQtyOnHnd" +
                        ", nResvOrdr" +
                        ", nBackOrdr" +
                        ", nLedgerNo" +
                    " FROM Inv_Master" + 
                    " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) + 
                        " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);
            
            loRS = poGRider.executeQuery(lsSQL);
            loInvTrans.setDetail(lnCtr, "dExpiryDt", paDetail.get(lnCtr).getDateExpiry());
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
        }
        
//        if (!loInvTrans.CreditMemo(poData.getTransNox(), poGRider.getServerDate(), EditMode.ADDNEW)){
//            setMessage(loInvTrans.getMessage());
//            setErrMsg(loInvTrans.getErrMsg());
//            return false;
//        }

        if (!loInvTrans.WasteInventory(poData.getTransNox(), poGRider.getServerDate(), EditMode.ADDNEW)){
                    setMessage(loInvTrans.getMessage());
                    setErrMsg(loInvTrans.getErrMsg());
                    return false;
        }
        
        //TODO
        //update branch order info
        return saveInvExpiration(poData.getTransact());
    }
    
    private boolean unsaveInvTrans(){
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();
        
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "nQtyOnHnd", paDetailOthers.get(lnCtr).getValue("nQtyOnHnd"));
            loInvTrans.setDetail(lnCtr, "nResvOrdr", paDetailOthers.get(lnCtr).getValue("nResvOrdr"));
            loInvTrans.setDetail(lnCtr, "nLedgerNo", paDetailOthers.get(lnCtr).getValue("nLedgerNo"));
        }
        
        if (!loInvTrans.CreditMemo(poData.getTransNox(), poGRider.getServerDate(), EditMode.DELETE)){
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }
        
        //TODO
            //update branch order info
    
        return true;
    }
    
    private boolean saveInvExpiration(Date fdTransact){
        String lsSQL;
        ResultSet loRS;
        int lnRow;
        int lnTempQTY;
        
        InvExpiration loInvTrans = new InvExpiration(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();
        
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            if (paDetail.get(lnCtr).getStockIDx().equals("")) break;
            
            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "dExpiryDt", paDetail.get(lnCtr).getDateExpiry());
            loInvTrans.setDetail(lnCtr, "nQtyOutxx", paDetail.get(lnCtr).getQuantity());
        }
        
        if (!loInvTrans.WasteInventory(fdTransact, EditMode.ADDNEW)){
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }
        return true;
    }
        
    public void setMaster(int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN){
            // Don't allow specific fields to assign values
            if(!(fnCol == poData.getColumn("sTransNox") ||
                fnCol == poData.getColumn("nEntryNox") ||
                fnCol == poData.getColumn("cTranStat") ||
                fnCol == poData.getColumn("sModified") ||
                fnCol == poData.getColumn("dModified"))){
                
                poData.setValue(fnCol, foData);   
                MasterRetreived(fnCol);
            }
        }
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
        return MiscUtil.makeSelect(new UnitInvWasteMaster());
    }
    
    private String getSQ_Detail(){
        return "SELECT" +
                    "  a.sTransNox" +
                    ", a.nEntryNox" +
                    ", a.sStockIDx" +
                    ", a.nQuantity" +
                    ", a.nInvCostx" +
                    ", a.sNotesxxx" +
                    ", a.dModified" +
                    ", c.sBarCodex" +
                    ", c.sDescript" +
                    ", a.dExpiryDt" +
                    ", b.nQtyOnHnd xQtyOnHnd" +
                    ", d.sMeasurNm" +
                " FROM Inv_Waste_Detail a" +
                    ", Inv_Master b" +
                        " LEFT JOIN Inventory c" +
                            " ON b.sStockIDx = c.sStockIDx" +
                        " LEFT JOIN Measure d" +
                            " ON c.sMeasurID = d.sMeasurID" +
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
    
    private String getSQ_InvWaste(){
        String lsTranStat = String.valueOf(pnTranStat);
        String lsCondition = "";
        String lsSQL = "SELECT " +
                            "  a.sTransNox" +
                            ", a.dTransact" + 
                        " FROM Inv_Waste_Master a" + 
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
        return "SELECT " +
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
                    ", f.sMeasurNm" +
                    ", e.nQtyOnHnd" +
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
                    " AND e.sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                    " AND e.nQtyOnHnd > 0";
    }
    
    public void printColumnsMaster(){poData.list();}
    public void printColumnsDetail(){poDetail.list();}
    public void setTranStat(int fnValue){this.pnTranStat = fnValue;}
    
    //callback methods
    public void setCallBack(IMasterDetail foCallBack){
        poCallBack = foCallBack;
    }
    
    private void MasterRetreived(int fnRow){
        if (poCallBack == null) return;
        
        poCallBack.MasterRetreive(fnRow);
    }
    
    private void DetailRetreived(int fnRow){
        if (poCallBack == null) return;
        
        poCallBack.DetailRetreive(fnRow);
    }
    
    //Member Variables
    private GRider poGRider = null;
    private String psUserIDxx = "";
    private String psBranchCd = "";
    private String psWarnMsg = "";
    private String psErrMsgx = "";
    private boolean pbWithParent = false;
    private int pnEditMode;
    private int pnTranStat = 0;
    private IMasterDetail poCallBack;
    
    private UnitInvWasteMaster poData = new UnitInvWasteMaster();
    private UnitInvWasteDetail poDetail = new UnitInvWasteDetail();
    private ArrayList<UnitInvWasteDetail> paDetail;
    private ArrayList<UnitInvWasteDetailOthers> paDetailOthers;
    
    private final String pxeModuleName = InvWaste.class.getSimpleName();
}
