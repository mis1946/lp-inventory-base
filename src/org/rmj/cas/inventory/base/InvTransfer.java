/**
 * Inventory Transfer BASE
 * @author Michael Torres Cuison
 * @since 2018.10.06
 */
package org.rmj.cas.inventory.base;

import com.mysql.jdbc.Connection;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
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
import org.rmj.cas.inventory.base.views.SubUnitController;
import org.rmj.cas.inventory.others.pojo.UnitInvTransferDetailOthers;
import org.rmj.cas.inventory.others.pojo.UnitInvTransferDetailExpiration;
import org.rmj.cas.inventory.pojo.UnitInvMaster;
import org.rmj.cas.inventory.pojo.UnitInvTransferDetail;
import org.rmj.cas.inventory.pojo.UnitInvTransferMaster;
import org.rmj.cas.parameter.agent.XMBranch;
import org.rmj.appdriver.agentfx.callback.IMasterDetail;

public class InvTransfer{
    public InvTransfer(GRider foGRider, String fsBranchCD, boolean fbWithParent){
        this.poGRider = foGRider;
        
        if (foGRider != null){
            this.pbWithParent = fbWithParent;
            this.psBranchCd = fsBranchCD;
            
            this.psUserIDxx = foGRider.getUserID();
            pnEditMode = EditMode.UNKNOWN;
        }
    }
    
    public boolean BrowseRecord(String fsValue, boolean fbByCode){
        String lsHeader = "Transfer No»Destination»Date";
        String lsColName = "sTransNox»sBranchNm»dTransact";
        String lsColCrit = "a.sTransNox»b.sBranchNm»a.dTransact";
        String lsSQL = MiscUtil.addCondition(getSQ_InvTransfer(), "a.sBranchCd = " + SQLUtil.toSQL(psBranchCd));
        
        System.out.print(lsSQL);
        JSONObject loJSON = showFXDialog.jsonSearch(poGRider, 
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
    
    public boolean BrowseAcceptance(String fsValue, boolean fbByCode){
        String lsHeader = "Transfer No»Destination»Date»Source";
        String lsColName = "a.sTransNox»b.sBranchNm»a.dTransact»c.sBranchNm";
        String lsColCrit = "a.sTransNox»b.sBranchNm»a.dTransact»c.sBranchNm";
        String lsSQL = MiscUtil.addCondition(getSQ_InvTransfer(), 
                                                "a.sDestinat = " + SQLUtil.toSQL(poGRider.getBranchCode()) +
                                                " AND a.cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_CLOSED));
        
        System.out.println(lsSQL);
        JSONObject loJSON = showFXDialog.jsonSearch(poGRider, 
                                                    lsSQL, 
                                                    fsValue, 
                                                    lsHeader, 
                                                    lsColName, 
                                                    lsColCrit, 
                                                    fbByCode ? 0 : 1);
        
        if(loJSON == null)
            return false;
        else
            System.out.println(loJSON.get("sTransNox") + " olala");
            return openTransaction((String) loJSON.get("sTransNox"));
    }
    
    public boolean addDetail() {
        if (paDetail.isEmpty()){
            paDetail.add(new UnitInvTransferDetail());
            paDetailOthers.add(new UnitInvTransferDetailOthers());
        }
        else{
            if (!paDetail.get(ItemCount()-1).getStockIDx().equals("") &&
                    Double.valueOf(paDetail.get(ItemCount()-1).getQuantity().toString())!= 0.00){
                paDetail.add(new UnitInvTransferDetail());
                paDetail.get(ItemCount()-1).setOrderNox(paDetail.get(ItemCount()-2).getOrderNox());
                
                paDetailOthers.add(new UnitInvTransferDetailOthers());
            }
                
        }
        return true;
    }

    public boolean deleteDetail(int fnRow) {
        paDetail.remove(fnRow);
        paDetailOthers.remove(fnRow);
        poData.setTranTotl(computeTotal());
        
        if (paDetail.isEmpty()){
            paDetail.add(new UnitInvTransferDetail());
            paDetailOthers.add(new UnitInvTransferDetailOthers());
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
                    if (foData instanceof Number){
                        if (Double.valueOf(foData.toString()) > Double.valueOf(paDetailOthers.get(fnRow).getValue("nQtyOnHnd").toString())){
                            confirmSelectParent(fnRow);
                          
                            if (paDetail.get(fnRow).getQuantity().doubleValue() == 0.00) {
                                 paDetail.get(fnRow).setValue(fnCol, foData);
//                                setDetail(fnRow, "nQuantity", Double.valueOf(paDetailOthers.get(fnRow).getValue("nQtyOnHnd").toString()));
                            }else{
                                 paDetail.get(fnRow).setValue(fnCol, Double.valueOf(paDetailOthers.get(fnRow).getValue("nQtyOnHnd").toString()));
//                                setDetail(fnRow, "nQuantity", Double.valueOf(foData.toString()));
                            }
//                            paDetail.get(fnRow).setValue(fnCol, Double.valueOf(paDetailOthers.get(fnRow).getValue("nQtyOnHnd").toString()));
                        }else{
                            paDetail.get(fnRow).setValue(fnCol, foData);
                        }
//                        addDetail();
                    }else paDetail.get(fnRow).setValue(fnCol, 0);
                } else if (fnCol == poDetail.getColumn("nInvCostx")){
                    if (foData instanceof Number){
                        paDetail.get(fnRow).setValue(fnCol, foData);
                    }else paDetail.get(fnRow).setValue(fnCol, 0.00);
                }else if (fnCol == poDetail.getColumn("dExpiryDt")){
                    if (foData instanceof Date){
                        paDetail.get(fnRow).setValue(fnCol, foData);
                    }else paDetail.get(fnRow).setValue(fnCol, poGRider.getServerDate());
                } else paDetail.get(fnRow).setValue(fnCol, foData);
                
                
                DetailRetreived(fnCol);
                
                poData.setTranTotl(computeTotal());
                MasterRetreived(12);
            }
        }
    }

    public void setDetail(int fnRow, String fsCol, Object foData) {
        setDetail(fnRow, poDetail.getColumn(fsCol), foData);
    }
    
    public void setDetailExp(int fnRow, int fnCol, Object foData) {
        paDetailExpiration.get(fnRow).setValue(fnCol, foData);
    }

    public void setDetailExp(int fnRow, String fsCol, Object foData) {
        setDetailExp(fnRow, poDetailExp.getColumn(fsCol), foData);

    }
    
    public Object getDetailOthers(int fnRow, String fsCol){
        switch(fsCol){
            case "sStockIDx":
            case "nQtyOnHnd":
            case "xQtyOnHnd":
            case "nResvOrdr":
            case "nBackOrdr":
            case "nReorderx":
            case "nLedgerNo":
            case "sBarCodex":
            case "sDescript":
            case "sOrigCode":
            case "sOrigDesc":
            case "sOrderNox":
            case "sMeasurNm":
                return paDetailOthers.get(fnRow).getValue(fsCol);
            default:
                return null;
        }
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
    
    
    public Object getDetailExp(int fnRow, int fnCol) {
        if(pnEditMode == EditMode.UNKNOWN)
         return null;
      else{
         return paDetailExpiration.get(fnRow).getValue(fnCol);
      }
    }

    public Object getDetailExp(int fnRow, String fsCol) {
        return getDetailExp(fnRow, poDetailExp.getColumn(fsCol));
    }
    
    
    public boolean newTransaction() {
        Connection loConn = null;
        loConn = setConnection();       
        
        poData = new UnitInvTransferMaster();
        poData.setTransNox(MiscUtil.getNextCode(poData.getTable(), "sTransNox", true, loConn, psBranchCd));
        poData.setTransact(poGRider.getServerDate());
        
        paDetail = new ArrayList<>();
        paDetailOthers = new ArrayList<>(); //detail other info storage
        paDetailExpiration = new ArrayList<>(); //detail other info storage
        addDetail();
        
        pnEditMode = EditMode.ADDNEW;
        return true;
    }
    
    private double computeTotal(){
        double lnTranTotal = 0;
        for (int lnCtr = 0; lnCtr <= ItemCount()-1; lnCtr ++){
            lnTranTotal += (Double.valueOf(getDetail(lnCtr, "nQuantity").toString()) * Double.valueOf(getDetail(lnCtr, "nInvCostx").toString()));
        }
        
        //add the freight charge to total order
        lnTranTotal += Double.valueOf(poData.getFreightx().toString());
        //less the discounts
        lnTranTotal = lnTranTotal - (lnTranTotal * Double.valueOf(poData.getDiscount().toString()));
        return lnTranTotal;
    }
    
    private boolean isInventoryOK(String fsValue){
        int lnMasRow = poData.getEntryNox();
        
        String lsSQL = MiscUtil.addCondition(getSQ_Detail(), "sTransNox = " + SQLUtil.toSQL(fsValue));
        
        try {
            ResultSet loRS = poGRider.executeQuery(lsSQL);
        
            if (MiscUtil.RecordCount(loRS) != lnMasRow){
                lsSQL = MiscUtil.makeSelect(new UnitInvTransferDetail());
                lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox = " + SQLUtil.toSQL(fsValue));

                loRS = poGRider.executeQuery(lsSQL);
                
                ResultSet loRSx;
                InvMaster loInvMaster = new InvMaster(poGRider, psBranchCd, false);
                
                while (loRS.next()){
                    lsSQL = MiscUtil.makeSelect(new UnitInvMaster());
                    lsSQL = MiscUtil.addCondition(lsSQL, "sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx")) +
                                                            " AND sBranchCD = " + SQLUtil.toSQL(psBranchCd));
                    System.out.println(lsSQL);
                    loRSx = poGRider.executeQuery(lsSQL);
                    if (!loRSx.next()){
                        if (loInvMaster.SearchInventory(loRS.getString("sStockIDx"), false, true)){
                            loInvMaster.NewRecord();
                            if (!loInvMaster.SaveRecord()) {
                                System.err.println(loInvMaster.getMessage());
                                return false;
                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            return false;
        }

        return true;
    }
    
    public boolean openTransaction(String fsTransNox){
        poData = loadTransaction(fsTransNox);
        
        if (poData != null){ 
            paDetail = loadTransactionDetail(fsTransNox);
            paDetailExpiration = loadTransactionDetailExpiration(fsTransNox);
            
            if (poData.getEntryNox() != paDetail.size()){
                setMessage("Transaction discrepancy detected... \n" +
                            "Detail count is not equal to the entry number...");
                return false;
            }
        } else{
            setMessage("Unable to load transaction.");
            return false;
        } 
        
        pnEditMode = EditMode.READY;
        return true;
    }
    
    public ResultSet getExpiration(String fsStockIDx, String fsParentID){
        String lsSQL="";
        if (!fsParentID.equals("")){
            lsSQL = "SELECT dExpiryDt, SUM(nQtyOnHnd) nQtyOnHnd FROM (SELECT * FROM Inv_Master_Expiration" +
                                " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx) +
                                    " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                                    " AND nQtyOnHnd > 0" +
                                " UNION SELECT * FROM (SELECT * FROM Inv_Master_Expiration" +
                                    " WHERE sStockIDx = " + SQLUtil.toSQL(fsParentID) +
                                        " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                                        " AND nQtyOnHnd > 0 ORDER BY dExpiryDt LIMIT 1)xxx) xxxTable" +
                                " GROUP BY dExpiryDt" +
                                " ORDER BY dExpiryDt";  
        }else{
            lsSQL = "SELECT * FROM Inv_Master_Expiration" +
                                " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx) +
                                    " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                                    " AND nQtyOnHnd > 0" +
                                " ORDER BY dExpiryDt";     
        }
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        return loRS;
    }

    public UnitInvTransferMaster loadTransaction(String fsTransNox) {
        UnitInvTransferMaster loObject = new UnitInvTransferMaster();
        
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
    
    private ArrayList<UnitInvTransferDetail> loadTransactionDetail(String fsTransNox){
        UnitInvTransferDetail loOcc = null;
        UnitInvTransferDetailOthers loOth = null;
        Connection loConn = null;
        loConn = setConnection();
        
        ArrayList<UnitInvTransferDetail> loDetail = new ArrayList<>();
        paDetailOthers = new ArrayList<>(); //reset detail others
        
        //2019.05.23
        //  Check first if the transferred items are in the destination's inventory
        if (!isInventoryOK(fsTransNox)) return null;
        
        String lsSQL = MiscUtil.addCondition(getSQ_Detail(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        try {
            System.out.println(lsSQL);
            ResultSet loRS = poGRider.executeQuery(lsSQL);  
            
            for (int lnCtr = 1; lnCtr <= MiscUtil.RecordCount(loRS); lnCtr ++){
                loRS.absolute(lnCtr);

                //load detail
                loOcc = new UnitInvTransferDetail();
                loOcc.setValue("sTransNox", loRS.getString("sTransNox"));        
                loOcc.setValue("nEntryNox", loRS.getInt("nEntryNox"));
                loOcc.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOcc.setValue("sOrigIDxx", loRS.getString("sOrigIDxx"));
                loOcc.setValue("sOrderNox", loRS.getString("sOrderNox"));
                loOcc.setValue("nQuantity", loRS.getDouble("nQuantity"));
                loOcc.setValue("nInvCostx", loRS.getDouble("nInvCostx"));
                loOcc.setValue("sRecvIDxx", loRS.getString("sRecvIDxx"));
                loOcc.setValue("nReceived", loRS.getDouble("nQuantity"));
                loOcc.setValue("sNotesxxx", loRS.getString("sNotesxxx"));
                loOcc.setValue("dExpiryDt", loRS.getDate("dExpiryDt"));
                loOcc.setValue("dModified", loRS.getDate("dModified"));
                loOcc.setValue("sParentID", loRS.getString("sParentID"));
                loOcc.setValue("nParntQty", loRS.getDouble("nParntQty"));
                loOcc.setValue("nSbItmQty", loRS.getDouble("nSbItmQty"));
                loDetail.add(loOcc);
                
                //load other info
                loOth = new UnitInvTransferDetailOthers();
                loOth.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOth.setValue("sBarCodex", loRS.getString("sBarCodex"));
                loOth.setValue("sDescript", loRS.getString("sDescript"));
                loOth.setValue("nQtyOnHnd", loRS.getDouble("nQtyOnHnd"));
                loOth.setValue("xQtyOnHnd", loRS.getDouble("xQtyOnHnd"));
                loOth.setValue("nResvOrdr", loRS.getDouble("nResvOrdr"));
                loOth.setValue("nBackOrdr", loRS.getDouble("nBackOrdr"));
                loOth.setValue("nReorderx", 0);
                loOth.setValue("nLedgerNo", loRS.getInt("nLedgerNo"));
                loOth.setValue("sOrigCode", loRS.getString("xBarCodex"));
                if (loRS.getString("sMeasurNm")!=null){
                   loOth.setValue("sMeasurNm", loRS.getString("sMeasurNm"));
                }else{
                   loOth.setValue("sMeasurNm", "");
                }
                paDetailOthers.add(loOth);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }        
        
        return loDetail;
    }
    
    private ArrayList<UnitInvTransferDetailExpiration> loadTransactionDetailExpiration(String fsTransNox){
        UnitInvTransferDetailExpiration loOcc = null;
        Connection loConn = null;
        loConn = setConnection();
        
        ArrayList<UnitInvTransferDetailExpiration> loDetail = new ArrayList<>();
        paDetailExpiration = new ArrayList<>(); //reset detail others
            
        String lsSQL = MiscUtil.addCondition(getSQ_DetailExpiration(), "a.sTransNox = " + SQLUtil.toSQL(fsTransNox));
        System.out.print(lsSQL);
        try {
            ResultSet loRS = poGRider.executeQuery(lsSQL);  
            
            for (int lnCtr = 1; lnCtr <= MiscUtil.RecordCount(loRS); lnCtr ++){
                loRS.absolute(lnCtr);

                //load detail
                loOcc = new UnitInvTransferDetailExpiration();
                loOcc.setValue("sTransNox", loRS.getString("sTransNox"));        
                loOcc.setValue("nEntryNox", loRS.getInt("nEntryNox"));
                loOcc.setValue("sDescript", loRS.getString("sDescript"));
                loOcc.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOcc.setValue("nQuantity", loRS.getDouble("nQuantity"));
                loOcc.setValue("nReceived", loRS.getDouble("nReceived"));
                loOcc.setValue("dExpiryDt", loRS.getDate("dExpiryDt"));
                loDetail.add(loOcc);
                
                paDetailExpiration.add(loOcc);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }        
        
        return loDetail;
    }
    
    private boolean saveInvTrans(){
        String lsSQL = "";
        String lsStockNo = "";
        ResultSet loRS = null;
        int lnCtr;
        Date ldParentExp = null;
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
        
        /*---------------------------------------------------------------------------------
         *   Credit from mother unit
         *---------------------------------------------------------------------------------*/
        lsStockNo = "";
        for (lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            if (!paDetail.get(lnCtr).getOrderNox().equals("")){
                if (lsStockNo.equals("")){
                    lsStockNo=paDetail.get(lnCtr).getOrderNox();
                }
            }
            
            if (!paDetail.get(lnCtr).getParentID().equals("")){
                InventoryTrans loInvDMTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
                loInvDMTrans.InitTransaction();
                
                lsSQL = "SELECT" +
                            "  a.nQtyOnHnd" +
                            ", a.nResvOrdr" +
                            ", a.nBackOrdr" +
                            ", a.nLedgerNo" +
                            ", b.dExpiryDt" +
                        " FROM Inv_Master a" + 
                            " LEFT JOIN Inv_Master_Expiration b" +
                                " ON a.sStockIDx = b.sStockIDx" +
                                " AND a.sBranchCd = b.sBranchCd" +
                                " AND b.nQtyOnHnd > 0" +
                        " WHERE a.sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getParentID()) + 
                            " AND a.sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                        " ORDER BY b.dExpiryDt" +
                        " LIMIT 1";

                loRS = poGRider.executeQuery(lsSQL);
                
                loInvDMTrans.setDetail(0, "sStockIDx", paDetail.get(lnCtr).getParentID());
                loInvDMTrans.setDetail(0, "nQuantity", paDetail.get(lnCtr).getParnQty());
                               
                if (MiscUtil.RecordCount(loRS) == 0){
                    loInvDMTrans.setDetail(0, "nQtyOnHnd", 0);
                    loInvDMTrans.setDetail(0, "nResvOrdr", 0);
                    loInvDMTrans.setDetail(0, "nBackOrdr", 0);
                } else{
                    try {
                        loRS.first();
                        loInvDMTrans.setDetail(0, "nQtyOnHnd", loRS.getDouble("nQtyOnHnd"));
                        loInvDMTrans.setDetail(0, "nResvOrdr", loRS.getDouble("nResvOrdr"));
                        loInvDMTrans.setDetail(0, "nBackOrdr", loRS.getDouble("nBackOrdr"));
                        loInvDMTrans.setDetail(0, "nLedgerNo", loRS.getInt("nLedgerNo"));
                                         
                        if(loRS.getDate("dExpiryDt")==null){
                            ldParentExp=poGRider.getSysDate();
                        }else{
                            ldParentExp=loRS.getDate("dExpiryDt");
                        }
                        loInvDMTrans.setDetail(0, "dExpiryDt", ldParentExp);
                    } catch (SQLException e) {
                        setMessage("Please inform MIS Department.");
                        setErrMsg(e.getMessage());
                        return false;
                    }
                }
                
                if (!loInvDMTrans.DebitMemo(poData.getTransNox(), poGRider.getServerDate(), EditMode.ADDNEW)){
                    setMessage(loInvDMTrans.getMessage());
                    setErrMsg(loInvDMTrans.getErrMsg());
                    return false;
                }
//                if (!loInvTrans.Delivery(poData.getTransNox(), poData.getTransact(), EditMode.ADDNEW)){
//                    setMessage(loInvTrans.getMessage());
//                    setErrMsg(loInvTrans.getErrMsg());
//                    return false;
//                }
            }
        }
        
        /*---------------------------------------------------------------------------------
         *   Debit to child unit
         *---------------------------------------------------------------------------------*/
        for (lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            if (!paDetail.get(lnCtr).getParentID().equals("")){
                InventoryTrans loInvCMTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
                loInvCMTrans.InitTransaction();
                lsSQL = "SELECT" +
                            "  nQtyOnHnd" +
                            ", nResvOrdr" +
                            ", nBackOrdr" +
                            ", nLedgerNo" +
                        " FROM Inv_Master" + 
                        " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) + 
                            " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);

                loRS = poGRider.executeQuery(lsSQL);
                
                loInvCMTrans.setDetail(0, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
                loInvCMTrans.setDetail(0, "dExpiryDt",ldParentExp);
                loInvCMTrans.setDetail(0, "nQuantity", paDetail.get(lnCtr).getSbItmQty());
                
                if (MiscUtil.RecordCount(loRS) == 0){
                    loInvCMTrans.setDetail(0, "nQtyOnHnd", 0);
                    loInvCMTrans.setDetail(0, "nResvOrdr", 0);
                    loInvCMTrans.setDetail(0, "nBackOrdr", 0);
                } else{
                    try {
                        loRS.first();
                        loInvCMTrans.setDetail(0, "nQtyOnHnd", loRS.getDouble("nQtyOnHnd"));
                        loInvCMTrans.setDetail(0, "nResvOrdr", loRS.getDouble("nResvOrdr"));
                        loInvCMTrans.setDetail(0, "nBackOrdr", loRS.getDouble("nBackOrdr"));
                        loInvCMTrans.setDetail(0, "nLedgerNo", loRS.getInt("nLedgerNo"));
                    } catch (SQLException e) {
                        setMessage("Please inform MIS Department.");
                        setErrMsg(e.getMessage());
                        return false;
                    }
                }
                
                if (!loInvCMTrans.CreditMemo(poData.getTransNox(), poGRider.getServerDate(), EditMode.ADDNEW)){
                    setMessage(loInvCMTrans.getMessage());
                    setErrMsg(loInvCMTrans.getErrMsg());
                    return false;
                }
            }
        }
        
        /*---------------------------------------------------------------------------------
         *   Save inventory trans of the items
         *---------------------------------------------------------------------------------*/
        loInvTrans.InitTransaction();
        for (lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            if (paDetail.get(lnCtr).getStockIDx().equals("")) break;
            
            lsSQL = "SELECT" +
                        "  nQtyOnHnd" +
                        ", nResvOrdr" +
                        ", nBackOrdr" +
                        ", nLedgerNo" +
                    " FROM Inv_Master" + 
                    " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) + 
                        " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);

            loRS = poGRider.executeQuery(lsSQL);
            
            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "sReplacID", paDetail.get(lnCtr).getOrigIDxx());
            loInvTrans.setDetail(lnCtr, "nQuantity", paDetail.get(lnCtr).getQuantity());
            
            if(ldParentExp==null){
                loInvTrans.setDetail(lnCtr, "dExpiryDt", paDetail.get(lnCtr).getDateExpiry());
            }else{
                loInvTrans.setDetail(lnCtr, "dExpiryDt", ldParentExp);
            }
            
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
        
        if (!loInvTrans.Delivery(poData.getTransNox(), poData.getTransact(), EditMode.ADDNEW)){
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }
        
        //TODO
            //update branch order info
        if (!lsStockNo.equals("")){
            if (postStockRequest(lsStockNo)){
                return false;
            }
        }
            
        return saveTransExpDetail();
    }
    
    private boolean postStockRequest(String fsTransNox){
        String lsSQL = "UPDATE Inv_Stock_Request_Master" + 
                        " SET  cTranStat = " + SQLUtil.toSQL(TransactionStatus.STATE_POSTED) + 
                            ", sModified = " + SQLUtil.toSQL(psUserIDxx) +
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                        " WHERE sTransNox = " + SQLUtil.toSQL(fsTransNox);
        
        
        
        if (poGRider.executeQuery(lsSQL, "Inv_Stock_Request_Master", "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
                return false;
            } else {
                setErrMsg("Unable to close transaction.");
                return false;
            }
        }
        
        return true;
    }
    
    private boolean saveTransExpDetail(){
        String lsSQL = "";
        ResultSet loRS = null;
        int lnCtr;
        
        for (lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            if (paDetail.get(lnCtr).getStockIDx().equals("")) break;
            lsSQL = "SELECT" +
                        "  nQtyOnHnd" +
                        ", dExpiryDt" +
                    " FROM Inv_Master_Expiration" + 
                    " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) + 
                        " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) + 
                        " AND nQtyOnHnd > 0" +           
                    " ORDER BY dExpiryDt";
            
            loRS = poGRider.executeQuery(lsSQL);
            /**
             * jovan
             * since 06-21-21
             * comment for debugging/revision of code
             */
            try {
                if(MiscUtil.RecordCount(loRS)==0){
                    if(!paDetailOthers.get(lnCtr).getValue("sParentID").toString().isEmpty()){
                        ResultSet loRSSub = null;
                        String lsSQLSub = "SELECT" +
                                                "  a.sStockIDx" + 
                                                ", b.nQuantity" + 
                                                ", b.nQtyOnHnd" + 
                                                ", b.dExpiryDt" +
                                            " FROM Inventory_Sub_Unit a" + 
                                                ", Inv_Master_Expiration b" +
                                            " WHERE a.sStockIDx = b.sStockIDx" + 
                                                " AND a.sItmSubID = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) + 
                                                " AND b.sBranchCd = " + SQLUtil.toSQL(psBranchCd) + 
                                                " AND b.nQtyOnHnd > 0" +
                                            " ORDER BY b.dExpiryDt";
                        loRSSub = poGRider.executeQuery(lsSQLSub);
                        
                        double lnQtyOut = Double.valueOf(paDetail.get(lnCtr).getQuantity().toString());
                        double lnQuantity =0;
                        while (loRSSub.next()){
                            if(lnQtyOut>=loRSSub.getDouble("nQtyOnHnd")){
                                lnQuantity = loRSSub.getDouble("nQtyOnHnd");
                            }else{
                                lnQuantity = lnQtyOut;
                            }
                            lsSQL = "INSERT INTO Inv_Transfer_Detail_Expiration SET" +          
                                        "  sTransNox = " + SQLUtil.toSQL(paDetail.get(lnCtr).getTransNox()) +
                                        ", nEntryNox = " + SQLUtil.toSQL(paDetail.get(lnCtr).getEntryNox()) +                        
                                        ", sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) +
                                        ", nQtyOnHnd = " + SQLUtil.toSQL(loRSSub.getDouble("nQtyOnHnd")) +
                                        ", nQuantity = " + SQLUtil.toSQL(lnQuantity) +                        
                                        ", nReceived = 0" +
                                        ", dExpiryDt = " + SQLUtil.toSQL(loRSSub.getDate("dExpiryDt"))+      
                                        ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate());

                            if(poGRider.executeQuery(lsSQL, "Inv_Transfer_Detail_Expiration", "", "") == 0){
                                if(!poGRider.getErrMsg().isEmpty())
                                    setErrMsg(poGRider.getErrMsg());
                                else 
                                    setMessage("No record updated");
                            }

                            if(lnQtyOut<=loRS.getInt("nQtyOnHnd")){
                                break;
                            }
                            lnQtyOut =  (double)Math.round((lnQtyOut - loRSSub.getInt("nQtyOnHnd"))*100)/100;
                        }
                    }else{
                        lsSQL = "INSERT INTO Inv_Transfer_Detail_Expiration SET" +          
                                        "  sTransNox = " + SQLUtil.toSQL(paDetail.get(lnCtr).getTransNox()) +
                                        ", nEntryNox = " + SQLUtil.toSQL(paDetail.get(lnCtr).getEntryNox()) +                        
                                        ", sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) +
                                        ", nQtyOnHnd = " + SQLUtil.toSQL(paDetail.get(lnCtr).getQuantity()) +
                                        ", nQuantity = " + SQLUtil.toSQL(paDetail.get(lnCtr).getQuantity()) +                        
                                        ", nReceived = 0" +
                                        ", dExpiryDt = " + SQLUtil.toSQL(poGRider.getSysDate())+      
                                        ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate());

                            if(poGRider.executeQuery(lsSQL, "Inv_Transfer_Detail_Expiration", "", "") == 0){
                                if(!poGRider.getErrMsg().isEmpty())
                                    setErrMsg(poGRider.getErrMsg());
                                else 
                                    setMessage("No record updated");
                            }
                    }          
                }else{
                    double lnQtyOut = Double.valueOf(paDetail.get(lnCtr).getQuantity().toString());
                    double lnQuantity =0;
                    while (loRS.next()){
                        if(lnQtyOut>=loRS.getDouble("nQtyOnHnd")){
                            lnQuantity = loRS.getDouble("nQtyOnHnd");
                        }else{
                            lnQuantity = lnQtyOut;
                        }
                        
                        lsSQL = "INSERT INTO Inv_Transfer_Detail_Expiration SET" +          
                                    "  sTransNox = " + SQLUtil.toSQL(paDetail.get(lnCtr).getTransNox()) +
                                    ", nEntryNox = " + SQLUtil.toSQL(paDetail.get(lnCtr).getEntryNox()) +
                                    ", sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) +
                                    ", nQtyOnHnd = " + SQLUtil.toSQL(loRS.getDouble("nQtyOnHnd")) +                        
                                    ", nQuantity = " + SQLUtil.toSQL(lnQuantity) +                        
                                    ", nReceived = 0" +
                                    ", dExpiryDt = " + SQLUtil.toSQL(loRS.getDate("dExpiryDt"))+      
                                    ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate());
                        
                        /**
                         * ", dExpiryDt = " + SQLUtil.toSQL(loRS.getDate("dExpiryDt"))+      
                         */
                        if(poGRider.executeQuery(lsSQL, "Inv_Transfer_Detail_Expiration", "", "") == 0){
                            if(!poGRider.getErrMsg().isEmpty())
                                setErrMsg(poGRider.getErrMsg());
                            else 
                                setMessage("No record updated");
                        }
                        
                        if(lnQtyOut<=loRS.getInt("nQtyOnHnd")){
//                            return saveInvExpiration(poData.getTransact());
                            break;
                        }
                        lnQtyOut =  (double)Math.round((lnQtyOut - loRS.getInt("nQtyOnHnd"))*100)/100;
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(InvTransfer.class.getName()).log(Level.SEVERE, null, ex);
            }
//            try {
//                    while (loRS.next()){
//                        lsSQL = "INSERT INTO Inv_Transfer_Detail_Expiration SET" +          
//                                    "  sTransNox = " + SQLUtil.toSQL(paDetail.get(lnCtr).getTransNox()) +
//                                    ", nEntryNox = " + SQLUtil.toSQL(paDetail.get(lnCtr).getEntryNox()) +
//                                    ", sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) +
//                                    ", nQtyOnHnd = " + SQLUtil.toSQL(loRS.getInt("nQtyOnHnd")) +                        
//                                    ", nQuantity = " + SQLUtil.toSQL(paDetail.get(lnCtr).getQuantity()) +                        
//                                    ", nReceived = 0" +
//                                    ", dExpiryDt = " + SQLUtil.toSQL(paDetail.get(lnCtr).getDateExpiry())+      
//                                    ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate());
//                        
//                        /**
//                         * ", dExpiryDt = " + SQLUtil.toSQL(loRS.getDate("dExpiryDt"))+      
//                         */
//                        if(poGRider.executeQuery(lsSQL, "Inv_Transfer_Detail_Expiration", "", "") == 0){
//                            if(!poGRider.getErrMsg().isEmpty())
//                                setErrMsg(poGRider.getErrMsg());
//                            else 
//                                setMessage("No record updated");
//                        }
//                    }
//            } catch (SQLException ex) {
//                Logger.getLogger(InvTransfer.class.getName()).log(Level.SEVERE, null, ex);
//            }
        }
        
        return saveInvExpiration(poData.getTransact());
    }
    
    private boolean unsaveInvTrans(){
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();
        
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "nQtyOnHnd", paDetailOthers.get(lnCtr).getValue("nQtyOnHnd"));
            loInvTrans.setDetail(lnCtr, "nResvOrdr", paDetailOthers.get(lnCtr).getValue("nResvOrdr"));
            loInvTrans.setDetail(lnCtr, "nBackOrdr", paDetailOthers.get(lnCtr).getValue("nBackOrdr"));
            loInvTrans.setDetail(lnCtr, "nLedgerNo", paDetailOthers.get(lnCtr).getValue("nLedgerNo"));
        }
        
        if (!loInvTrans.Delivery(poData.getTransNox(), poGRider.getServerDate(), EditMode.DELETE)){
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }
        
        //TODO
            //update branch order info
    
        return true;
    }
    
    private boolean unpostInvTrans(){
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();
        
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "nQtyOnHnd", paDetailOthers.get(lnCtr).getValue("nQtyOnHnd"));
            loInvTrans.setDetail(lnCtr, "nResvOrdr", paDetailOthers.get(lnCtr).getValue("nResvOrdr"));
            loInvTrans.setDetail(lnCtr, "nBackOrdr", paDetailOthers.get(lnCtr).getValue("nBackOrdr"));
            loInvTrans.setDetail(lnCtr, "nLedgerNo", paDetailOthers.get(lnCtr).getValue("nLedgerNo"));
        }
        
        if (!loInvTrans.AcceptDelivery(poData.getTransNox(), poGRider.getServerDate(), EditMode.DELETE)){
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }
        
        //TODO
            //update branch order info
    
        return true;
    }
    
    private boolean postInvTrans(Date fdReceived){
        String lsSQL = "";
        ResultSet loRS = null;
        int lnCtr;
        
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
              
        /*---------------------------------------------------------------------------------
         *   Save inventory trans of the items
         *---------------------------------------------------------------------------------*/
        loInvTrans.InitTransaction();
        for (lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            if (paDetail.get(lnCtr).getStockIDx().equals("")) break;
            
            lsSQL = "SELECT" +
                        "  nQtyOnHnd" +
                        ", nResvOrdr" +
                        ", nBackOrdr" +
                        ", nLedgerNo" +
                    " FROM Inv_Master" + 
                    " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) + 
                        " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);

            loRS = poGRider.executeQuery(lsSQL);
            
            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "sReplacID", paDetail.get(lnCtr).getOrigIDxx());
            loInvTrans.setDetail(lnCtr, "nQuantity", paDetail.get(lnCtr).getReceived());
                
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
            
            lsSQL = "UPDATE Inv_Transfer_Detail SET" + 
                        " nReceived = " + paDetail.get(lnCtr).getReceived() + 
                    " WHERE sTransNox = " + SQLUtil.toSQL(paDetail.get(lnCtr).getTransNox()) +
                        " AND sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx());
        
            if (poGRider.executeQuery(lsSQL, "Inv_Transfer_Detail", "", "") == 0){
                if (!poGRider.getErrMsg().isEmpty()){
                    setErrMsg(poGRider.getErrMsg());
                }
            }
        }
        
        if (!loInvTrans.AcceptDelivery(poData.getTransNox(),fdReceived, EditMode.ADDNEW)){
            setMessage(loInvTrans.getMessage());
            setErrMsg(loInvTrans.getErrMsg());
            return false;
        }
        
        //TODO
            //update branch order info
    
        return acceptInvExpiration(fdReceived);
    }
    
    private boolean acceptInvExpiration(Date fdTransact){
        String lsSQL;
        ResultSet loRS;
//            lsSQL = "SELECT" +
//                    "  sStockIDx" + 
//                    ", dExpiryDt" + 
//                    ", n" + 
//                " FROM Inv_Master_Expiration" +
//                " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) + 
//                    " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
//                    " AND nQtyOnHnd > 0" +
//                " ORDER BY dExpiryDt ASC";
//            
        
        InvExpiration loInvTrans = new InvExpiration(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();
        
        for (int lnCtr = 0; lnCtr <= paDetailExpiration.size() - 1; lnCtr ++){
            if (paDetailExpiration.get(lnCtr).getStockIDx().equals("")) break;
//            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetailExpiration.get(lnCtr).getColumn("sStockIDx"));
//            loInvTrans.setDetail(lnCtr, "dExpiryDt", paDetailExpiration.get(lnCtr).getColumn("dExpiryDt"));
//            loInvTrans.setDetail(lnCtr, "nQtyInxxx", paDetailExpiration.get(lnCtr).getColumn("nReceived"));

            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetailExpiration.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "dExpiryDt", paDetailExpiration.get(lnCtr).getDExpiryDt());
            loInvTrans.setDetail(lnCtr, "nQtyInxxx", paDetailExpiration.get(lnCtr).getNReceived());
        }
        
        if (!loInvTrans.AcceptDelivery(fdTransact, EditMode.ADDNEW)){
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
        int lnTemp;
        double lnTempQTY;
        
        InvExpiration loInvTrans = new InvExpiration(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();
        
        lnTemp=0;
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            if (paDetail.get(lnCtr).getStockIDx().equals("")) break;
            /**
             * jovan
             * since 06-21-21
             * comment part of debugging
             **/
            
//            if (paDetail.get(lnCtr).getParentID().equals("")){
                lsSQL = "SELECT" +
                    "  sStockIDx" + 
                    ", dExpiryDt" + 
                    ", nQtyOnHnd" + 
                " FROM Inv_Master_Expiration" +
                " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) + 
                    " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                    " AND nQtyOnHnd > 0" +
                " ORDER BY dExpiryDt ASC";
//            }else{
//                lsSQL = "SELECT dExpiryDt, SUM(nQtyOnHnd) nQtyOnHnd FROM (SELECT * FROM Inv_Master_Expiration" +
//                                " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) +
//                                    " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
//                                    " AND nQtyOnHnd > 0" +
//                                " UNION SELECT * FROM (SELECT * FROM Inv_Master_Expiration" +
//                                    " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getParentID()) +
//                                        " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
//                                        " AND nQtyOnHnd > 0 ORDER BY dExpiryDt LIMIT 1)xxx) xxxTable" +
//                                " GROUP BY dExpiryDt" +
//                                " ORDER BY dExpiryDt";  
//            }
                
            loRS = poGRider.executeQuery(lsSQL);
            try {
                if (MiscUtil.RecordCount(loRS) == 0){
                    if(!paDetail.get(lnCtr).getParentID().toString().isEmpty()){
                        ResultSet loRSSub = null;
                        String lsSQLSub = "SELECT dExpiryDt, SUM(nQtyOnHnd) nQtyOnHnd FROM (SELECT * FROM Inv_Master_Expiration" +
                                                " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) +
                                                    " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                                                    " AND nQtyOnHnd > 0" +
                                                " UNION SELECT * FROM (SELECT * FROM Inv_Master_Expiration" +
                                                    " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getParentID()) +
                                                        " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                                                        " AND nQtyOnHnd > 0 ORDER BY dExpiryDt LIMIT 1)xxx) xxxTable" +
                                                " GROUP BY dExpiryDt" +
                                                " ORDER BY dExpiryDt";
                        loRSSub = poGRider.executeQuery(lsSQLSub);
                        
                        double lnQtyOut = Double.valueOf(paDetail.get(lnCtr).getQuantity().toString());
                        double lnQuantity =0;
                        while (loRSSub.next()){
                            if(lnQtyOut>=loRSSub.getDouble("nQtyOnHnd")){
                                lnQuantity = loRSSub.getDouble("nQtyOnHnd");
                            }else{
                                lnQuantity = lnQtyOut;
                            }
                            
//                            lsSQL = "INSERT INTO Inv_Transfer_Detail_Expiration SET" +          
//                                        "  sTransNox = " + SQLUtil.toSQL(paDetail.get(lnCtr).getTransNox()) +
//                                        ", nEntryNox = " + SQLUtil.toSQL(paDetail.get(lnCtr).getEntryNox()) +                        
//                                        ", sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) +
//                                        ", nQtyOnHnd = " + SQLUtil.toSQL(loRSSub.getDouble("nQtyOnHnd")) +
//                                        ", nQuantity = " + SQLUtil.toSQL(lnQuantity) +                        
//                                        ", nReceived = 0" +
//                                        ", dExpiryDt = " + SQLUtil.toSQL(loRSSub.getDate("dExpiryDt"))+      
//                                        ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate());

                            loInvTrans.setDetail(lnTemp, "nQtyOutxx", lnQuantity);
                            loInvTrans.setDetail(lnTemp, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
                            loInvTrans.setDetail(lnTemp, "dExpiryDt", loRSSub.getDate("dExpiryDt"));

                            if(lnQtyOut<=loRSSub.getDouble("nQtyOnHnd")){
                                saveInvExpParent(fdTransact,
                                                paDetail.get(lnCtr).getParentID(),
                                                lnQuantity,
                                                loRSSub.getDate("dExpiryDt"));
                                
                                saveInvExpSub(fdTransact,
                                                paDetail.get(lnCtr).getStockIDx(),
                                                Double.valueOf(paDetail.get(lnCtr).getSbItmQty().toString()),
                                                loRSSub.getDate("dExpiryDt"));
                                break;
                            }
                            lnQtyOut =  (double)Math.round((lnQtyOut - loRSSub.getInt("nQtyOnHnd"))*100)/100;
                            saveInvExpParent(fdTransact,
                                                paDetail.get(lnCtr).getParentID(),
                                                lnQuantity,
                                                loRSSub.getDate("dExpiryDt"));
                            
                            saveInvExpSub(fdTransact,
                                                paDetail.get(lnCtr).getStockIDx(),
                                                Double.valueOf(paDetail.get(lnCtr).getSbItmQty().toString()),
                                                loRSSub.getDate("dExpiryDt"));
                        }
                         
                    }else{
                        loInvTrans.setDetail(lnTemp, "nQtyOutxx", paDetail.get(lnCtr).getQuantity());
                        loInvTrans.setDetail(lnTemp, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
                        loInvTrans.setDetail(lnTemp, "dExpiryDt", poGRider.getSysDate());
                    }
                
//                loInvTrans.setDetail(lnTemp, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
//                loInvTrans.setDetail(lnTemp, "dExpiryDt", paDetail.get(lnCtr).getDateExpiry());
//                loInvTrans.setDetail(lnTemp, "nQtyOutxx",Double.valueOf(paDetailOthers.get(lnCtr).getValue("nQtyOnHnd").toString()) -  Double.valueOf(paDetail.get(lnCtr).getQuantity().toString()));
                
//                if (!loInvTrans.Delivery(fdTransact, EditMode.ADDNEW)){
//                    setMessage(loInvTrans.getMessage());
//                    setErrMsg(loInvTrans.getErrMsg());
//                    return false;
//                }
                }else{
                    lnTempQTY=Double.valueOf(paDetail.get(lnCtr).getQuantity().toString());
                    loRS.first();
                    for (lnRow = 0; lnRow <= MiscUtil.RecordCount(loRS) - 1; lnRow ++){
                        if(lnTempQTY<=loRS.getInt("nQtyOnHnd")){
                            loInvTrans.setDetail(lnTemp, "nQtyOutxx", lnTempQTY);
                            loInvTrans.setDetail(lnTemp, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
                            loInvTrans.setDetail(lnTemp, "dExpiryDt", loRS.getDate("dExpiryDt"));
                                         
//                            if (!loInvTrans.Delivery(fdTransact, EditMode.ADDNEW)){
//                                setMessage(loInvTrans.getMessage());
//                                setErrMsg(loInvTrans.getErrMsg());
//                                return false;
//                             }
                            lnTemp++;
                            break;
                        }else{
                            loInvTrans.setDetail(lnTemp, "nQtyOutxx", loRS.getInt("nQtyOnHnd"));
                            loInvTrans.setDetail(lnTemp, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
                            loInvTrans.setDetail(lnTemp, "dExpiryDt", loRS.getDate("dExpiryDt"));
                            
                            lnTempQTY =  lnTempQTY - loRS.getInt("nQtyOnHnd");
                            
//                            if (!loInvTrans.Delivery(fdTransact, EditMode.ADDNEW)){
//                                setMessage(loInvTrans.getMessage());
//                                setErrMsg(loInvTrans.getErrMsg());
//                                return false;
//                            }
                        }    
                        lnTemp++;
                        loRS.next();
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(InvTransfer.class.getName()).log(Level.SEVERE, null, ex);
        }
//######################
//            lsSQL = "SELECT" +
//                    "  sStockIDx" + 
//                    ", dExpiryDt" + 
//                    ", nQtyOnHnd" + 
//                " FROM Inv_Master_Expiration" +
//                " WHERE sStockIDx = " + SQLUtil.toSQL(paDetail.get(lnCtr).getStockIDx()) + 
//                    " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
//                    " AND nQtyOnHnd > 0" +
//                    " AND dExpiryDt = " + SQLUtil.toSQL(paDetail.get(lnCtr).getDateExpiry());
//            
//            loRS = poGRider.executeQuery(lsSQL);
            
//            try{
//                loRS.first();
//                loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
//                loInvTrans.setDetail(lnCtr, "dExpiryDt", paDetail.get(lnCtr).getDateExpiry());
//                loInvTrans.setDetail(lnCtr, "nQtyOutxx", paDetail.get(lnCtr).getQuantity());
                
                 
//            }catch(SQLException ex){
//                Logger.getLogger(InvTransfer.class.getName()).log(Level.SEVERE,null,ex);
//            }
        }
        if (!loInvTrans.Delivery(fdTransact, EditMode.ADDNEW)){
                    setMessage(loInvTrans.getMessage());
                    setErrMsg(loInvTrans.getErrMsg());
                    return false;
                }
    
        return true;
    }
    
    
    private boolean saveInvExpParent(Date fdTransact,
                                        String fsStockIDx,
                                        Double fnQuantity,
                                        Date fdExpirtDt){
        InvExpiration loInvTrans = new InvExpiration(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();
               
        loInvTrans.setDetail(0, "nQtyOutxx", fnQuantity);
        loInvTrans.setDetail(0, "sStockIDx", fsStockIDx );
        loInvTrans.setDetail(0, "dExpiryDt", fdExpirtDt);

        if (!loInvTrans.DebitMemo(fdTransact, EditMode.ADDNEW)){
                                    setMessage(loInvTrans.getMessage());
                                    setErrMsg(loInvTrans.getErrMsg());
                                    return false;
                }
    
        return true;
    }
    
    private boolean saveInvExpSub(Date fdTransact,
                                        String fsStockIDx,
                                        Double fnQuantity,
                                        Date fdExpirtDt){
        InvExpiration loInvTrans = new InvExpiration(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();
               
        loInvTrans.setDetail(0, "nQtyInxxx", fnQuantity);
        loInvTrans.setDetail(0, "sStockIDx", fsStockIDx );
        loInvTrans.setDetail(0, "dExpiryDt", fdExpirtDt);

        if (!loInvTrans.CreditMemo(fdTransact, EditMode.ADDNEW)){
                                    setMessage(loInvTrans.getMessage());
                                    setErrMsg(loInvTrans.getErrMsg());
                                    return false;
                }
    
        return true;
    }

    public boolean saveTransaction() {
        String lsSQL = "";
        boolean lbUpdate = false;
        
        UnitInvTransferMaster loOldEnt = null;
        UnitInvTransferMaster loNewEnt = null;
        UnitInvTransferMaster loResult = null;
        
        // Check for the value of foEntity
        if (!(poData instanceof UnitInvTransferMaster)) {
            setErrMsg("Invalid Entity Passed as Parameter");
            return false;
        }
        
        // Typecast the Entity to this object
        loNewEnt = (UnitInvTransferMaster) poData;
                
        if (loNewEnt.getDestinat()== null || loNewEnt.getDestinat().equals("")){
            setMessage("Invalid destination detected.");
            return false;
        }       
               
        if (!pbWithParent) poGRider.beginTrans();
        
        poData.setTranTotl(computeTotal());
        
        //delete empty detail
        if (paDetail.get(ItemCount()-1).getStockIDx().equals("")) deleteDetail(ItemCount()-1);
        
        // Generate the SQL Statement
        if (pnEditMode == EditMode.ADDNEW){
            Connection loConn = null;
            loConn = setConnection();

            String lsTransNox = MiscUtil.getNextCode(loNewEnt.getTable(), "sTransNox", true, loConn, psBranchCd);

            loNewEnt.setTransNox(lsTransNox);
            loNewEnt.setBranchCd(poGRider.getBranchCode());            
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
            //lbUpdate = saveInvTrans(); //save inventory legder
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
        UnitInvTransferDetail loNewEnt = null;
        
        if (pnEditMode == EditMode.ADDNEW){
            Connection loConn = null;
            loConn = setConnection();  
            
            for (lnCtr = 0; lnCtr <= paDetail.size() -1; lnCtr++){
                loNewEnt = paDetail.get(lnCtr);
                
                if (!loNewEnt.getStockIDx().equals("")){
                    if (loNewEnt.getQuantity().doubleValue() == 0.00){
                       setMessage("Detail might not have item or zero quantity.");
                        return false;
                    }
                    
                    loNewEnt.setTransNox(fsTransNox);
                    loNewEnt.setEntryNox(lnCtr + 1);
                    
                    ResultSet loRS = getExpiration(loNewEnt.getStockIDx(), paDetail.get(lnCtr).getParentID());
                    try {
                        loRS.absolute(1);
                        loNewEnt.setDateExpiry(paDetail.get(lnCtr).getDateExpiry());
                    } catch (SQLException ex) {
                        Logger.getLogger(InvTransfer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
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
            ArrayList<UnitInvTransferDetail> laSubUnit = loadTransactionDetail(poData.getTransNox());
            
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
        UnitInvTransferMaster loObject = loadTransaction(string);
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
        UnitInvTransferMaster loObject = loadTransaction(string);
        boolean lbResult = false;
        
        if (loObject == null){
            setMessage("No record found...");
            return lbResult;
        }
        
        //if it is already closed, just return true
        if (loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CLOSED)) return true;
        
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
            } else setErrMsg("Unable to close transaction.");  
        } else lbResult = saveInvTrans();
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        
        return lbResult;
    }

    public boolean postTransaction(String string, Date received) {
        UnitInvTransferMaster loObject = loadTransaction(string);
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
                            ", sReceived = " + SQLUtil.toSQL(psUserIDxx) +
                            ", dReceived = " + SQLUtil.toSQL(received) + 
                        " WHERE sTransNox = " + SQLUtil.toSQL(loObject.getTransNox());
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
            if (!poGRider.getErrMsg().isEmpty()){
                setErrMsg(poGRider.getErrMsg());
            } else setErrMsg("Tranasction was not posted.");  
        } else lbResult = postInvTrans(received);
        
        if (!pbWithParent){
            if (getErrMsg().isEmpty()){
                poGRider.commitTrans();
            } else poGRider.rollbackTrans();
        }
        return lbResult;
    }

    public boolean voidTransaction(String string) {
        UnitInvTransferMaster loObject = loadTransaction(string);
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
        UnitInvTransferMaster loObject = loadTransaction(string);
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
        /**
         * author -jovan
         * since 2021-06-14
         * comment part of debugging cancellation of transaction/ error even if saving is success.
         * add new function to succeed when success
         */
//        if (poGRider.executeQuery(lsSQL, loObject.getTable(), "", "") == 0){
//            if (!poGRider.getErrMsg().isEmpty()){
//                setErrMsg(poGRider.getErrMsg());
//            } else setErrMsg("No record deleted.");  
//        } else {
//            if (loObject.getTranStat().equalsIgnoreCase(TransactionStatus.STATE_CLOSED))
//                lbResult = unsaveInvTrans();
//        }
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
    
    private void confirmSelectParent(int fnRow){
        ResultSet loRSParent;
        String [] laResult;
        
        loRSParent = poGRider.executeQuery(getSQ_Parent(paDetail.get(fnRow).getStockIDx()));
        if (MiscUtil.RecordCount(loRSParent) > 0){
            if (ShowMessageFX.YesNo("Item has no inventory but has parent unit.\n\n" + 
                                    "Do you want to use parent unit?",
                                    pxeModuleName, "Please confirm!!!")){

                String lsValue = showSelectParent(loRSParent,
                                                    (String) paDetailOthers.get(fnRow).getValue("sBarCodex"),
                                                    (String) paDetailOthers.get(fnRow).getValue("sDescript"),
                                                    (String) paDetailOthers.get(fnRow).getValue("sMeasurNm"),
                                                    (String) paDetailOthers.get(fnRow).getValue("sInvTypNm"));

                if (!lsValue.equals("")){
                    String [] lasValue = lsValue.split("»");

                    setDetail(fnRow, "sParentID", lasValue[0]);
                    setDetail(fnRow, "nParntQty", 1);
                    setDetail(fnRow, "nSbItmQty", Double.valueOf(lasValue[1]));
                    
                    paDetailOthers.get(fnRow).setValue("sParentID", lasValue[0]);
                    paDetailOthers.get(fnRow).setValue("xParntQty", Double.valueOf(paDetailOthers.get(fnRow).getValue("xParntQty").toString()) + 1);
                    paDetailOthers.get(fnRow).setValue("xQuantity", Double.valueOf(paDetailOthers.get(fnRow).getValue("xQuantity").toString()) + Double.valueOf(lasValue[1]));
                    paDetailOthers.get(fnRow).setValue("nQtyOnHnd", Double.valueOf(paDetailOthers.get(fnRow).getValue("nQtyOnHnd").toString()) + Double.valueOf(lasValue[1]));
                    
                    if (paDetail.get(fnRow).getQuantity().doubleValue() == 0.00) setDetail(fnRow, "nQuantity", 1);
                }
            }else{
                
            }
        }else{
//            setDetail(fnRow, "nQuantity", Double.valueOf(paDetailOthers.get(fnRow).getValue("nQtyOnHnd").toString()));
        }
    }
    
    private void confirmSelectSubItem(int fnRow, Object foData){
        ResultSet loRSParent;
        String [] laResult;
        
        loRSParent = poGRider.executeQuery(getSQ_SubItem(paDetail.get(fnRow).getStockIDx()));
        if (MiscUtil.RecordCount(loRSParent) > 0){
            if (ShowMessageFX.YesNo("Item has no inventory but has sub item unit.\n\n" + 
                                    "Do you want to use sub item unit?",
                                    pxeModuleName, "Please confirm!!!")){

                String lsValue = showSelectParent(loRSParent,
                                                    (String) paDetailOthers.get(fnRow).getValue("sBarCodex"),
                                                    (String) paDetailOthers.get(fnRow).getValue("sDescript"),
                                                    (String) paDetailOthers.get(fnRow).getValue("sMeasurNm"),
                                                    (String) paDetailOthers.get(fnRow).getValue("sInvTypNm"));

                if (!lsValue.equals("")){
                    String [] lasValue = lsValue.split("»");
                    if (Double.valueOf(lasValue[5] ) > (Double.valueOf(lasValue[2]) * Double.valueOf(foData.toString()))){
                    
                        setDetail(fnRow, "sParentID", lasValue[1]);
                        setDetail(fnRow, "nParntQty", Double.valueOf(foData.toString()));
                        setDetail(fnRow, "nSbItmQty", Double.valueOf(lasValue[2]));

                        paDetailOthers.get(fnRow).setValue("sParentID", lasValue[1]);
                        paDetailOthers.get(fnRow).setValue("xParntQty", Double.valueOf(paDetailOthers.get(fnRow).getValue("xParntQty").toString()) + 1);
                        paDetailOthers.get(fnRow).setValue("xQuantity", Double.valueOf(paDetailOthers.get(fnRow).getValue("xQuantity").toString()) + Double.valueOf(lasValue[1]));
                        paDetailOthers.get(fnRow).setValue("nQtyOnHnd", Double.valueOf(paDetailOthers.get(fnRow).getValue("nQtyOnHnd").toString()) + Double.valueOf(lasValue[1]));

                        if (paDetail.get(fnRow).getQuantity().doubleValue() == 0.00) setDetail(fnRow, "nQuantity", Double.valueOf(foData.toString()));
                    }else{
                        ShowMessageFX.Information("Item has insufficient inventory.", pxeModuleName, "INFO");
                        if (paDetail.get(fnRow).getQuantity().doubleValue() == 0.00) setDetail(fnRow, "nQuantity", Double.valueOf("0.00"));
                    }
                }
            }
        }
    }
    
    private String showSelectParent(ResultSet foRS, 
                                     String fsBarCodex,
                                     String fsDescript,
                                     String fsMeasurNm,
                                     String fsInvTypNm){
        SubUnitController loSubUnit = new SubUnitController();
        loSubUnit.setParentUnits(foRS);

        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("views/SubUnit.fxml"));
        fxmlLoader.setController(loSubUnit);
        
        try {

            loSubUnit.setBarCodex(fsBarCodex);
            loSubUnit.setDescript(fsDescript);
            loSubUnit.setMeasurNm(fsMeasurNm);
            loSubUnit.setInvTypNm(fsInvTypNm);

            Parent parent = fxmlLoader.load();

            Stage stage = new Stage();

            parent.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    xOffset = event.getSceneX();
                    yOffset = event.getSceneY();
                }
            });
            parent.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    stage.setX(event.getScreenX() - xOffset);
                    stage.setY(event.getScreenY() - yOffset); 

                }
            });

            Scene scene = new Scene(parent);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setAlwaysOnTop(true);
            stage.setScene(scene);
            stage.showAndWait();
            
            if (!loSubUnit.isCancelled())
                return loSubUnit.getValue();

        } catch (IOException ex) {
            ShowMessageFX.Error(ex.getMessage(), pxeModuleName, "Please inform MIS department.");
            System.exit(1);
        }
        
        return "";
    }
    
    public boolean SearchDetail(int fnRow, int fnCol, String fsValue, boolean fbSearch, boolean fbByCode){
        String lsHeader = "";
        String lsColName = "";
        String lsColCrit = "";
        String lsSQL = "";
        
        JSONObject loJSON;
        ResultSet loRS;
        
        setErrMsg("");
        setMessage("");
        
        switch(fnCol){
            case 5:
                lsHeader = "Order No»Branch»Date»Inv. Type";
                lsColName = "sTransNox»sBranchNm»dTransact»sDescript";
                lsColCrit = "a.sTransNox»c.sBranchNm»a.dTransact»b.sDescript";
                lsSQL = getSQ_Requests();
                
                if (fbByCode){
                    if (paDetailOthers.get(fnRow).getValue("sOrderNox").equals(fsValue)) return true;
                    
                    lsSQL = MiscUtil.addCondition(lsSQL, "a.sTransNox = " + SQLUtil.toSQL(fsValue));
                    
                    loRS = poGRider.executeQuery(lsSQL);
                    
                    loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
                } else {
                    if (!fbSearch){
                        if (paDetailOthers.get(fnRow).getValue("sOrderNox").equals(fsValue)) return true;
                        
                        loJSON = showFXDialog.jsonSearch(poGRider, 
                                                            lsSQL, 
                                                            fsValue, 
                                                            lsHeader, 
                                                            lsColName, 
                                                            lsColCrit, 
                                                            0);
                    } else
                        loJSON = showFXDialog.jsonSearch(poGRider, 
                                                            lsSQL, 
                                                            fsValue, 
                                                            lsHeader, 
                                                            lsColName, 
                                                            lsColCrit, 
                                                            1);
                }
                
                if (loJSON != null){
                    setDetail(fnRow, fnCol, (String) loJSON.get("sTransNox"));
                    paDetailOthers.get(fnRow).setValue("sOrderNox", (String) loJSON.get("sTransNox"));
                    loadRequest((String) loJSON.get("sTransNox"));
                    return true;
                } else{
                    setDetail(fnRow, fnCol, "");
                    paDetailOthers.get(fnRow).setValue("sOrderNox", "");
                    return false;
                }
            case 3:
                lsHeader = "Brand»Description»Unit»Model»Qty On Hnd»Inv. Type»Barcode»Stock ID";
                lsColName = "xBrandNme»sDescript»sMeasurNm»xModelNme»nQtyOnHnd»xInvTypNm»sBarCodex»sStockIDx";
                lsColCrit = "b.sDescript»a.sDescript»f.sMeasurNm»c.sDescript»e.nQtyOnHnd»d.sDescript»a.sBarCodex»a.sStockIDx";
                lsSQL = MiscUtil.addCondition(getSQ_Stocks(), "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE));
                
                if (fbByCode){
                    if (paDetailOthers.get(fnRow).getValue("sStockIDx").equals(fsValue)) return true;
                
                    lsSQL = MiscUtil.addCondition(lsSQL, "a.sStockIDx = " + SQLUtil.toSQL(fsValue));
                    
                    loRS = poGRider.executeQuery(lsSQL);
                    
                    loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
                }else {
                    if (!fbSearch){
                        if (paDetailOthers.get(fnRow).getValue("sBarCodex").equals(fsValue)) return true;
                        
                        loJSON = showFXDialog.jsonSearch(poGRider, 
                                                            lsSQL, 
                                                            fsValue, 
                                                            lsHeader, 
                                                            lsColName, 
                                                            lsColCrit, 
                                                            6);
                    } else{
                        if (paDetailOthers.get(fnRow).getValue("sDescript").equals(fsValue)) return true;
                        
                        loJSON = showFXDialog.jsonSearch(poGRider, 
                                                            lsSQL, 
                                                            fsValue, 
                                                            lsHeader, 
                                                            lsColName, 
                                                            lsColCrit, 
                                                            1);
                    }
                        
                }
                System.err.println(lsSQL);
                
                if (loJSON != null){
                    setDetail(fnRow, fnCol, (String) loJSON.get("sStockIDx"));
                    setDetail(fnRow, "nInvCostx", Double.valueOf((String) loJSON.get("nUnitPrce")));

                    paDetailOthers.get(fnRow).setValue("sStockIDx", (String) loJSON.get("sStockIDx"));
                    paDetailOthers.get(fnRow).setValue("sBarCodex", (String) loJSON.get("sBarCodex"));
                    paDetailOthers.get(fnRow).setValue("sDescript", (String) loJSON.get("sDescript"));
                    paDetailOthers.get(fnRow).setValue("nQtyOnHnd", Double.valueOf((String) loJSON.get("nQtyOnHnd")));
                    paDetailOthers.get(fnRow).setValue("nResvOrdr", Double.valueOf((String) loJSON.get("nResvOrdr")));
                    paDetailOthers.get(fnRow).setValue("nBackOrdr", Double.valueOf((String) loJSON.get("nBackOrdr")));
                    paDetailOthers.get(fnRow).setValue("nFloatQty", Double.valueOf((String) loJSON.get("nFloatQty")));
                    paDetailOthers.get(fnRow).setValue("nLedgerNo", Integer.valueOf((String) loJSON.get("nLedgerNo")));
                    paDetailOthers.get(fnRow).setValue("sInvTypNm", (String) loJSON.get("xInvTypNm"));
                    paDetailOthers.get(fnRow).setValue("sMeasurNm", (String) loJSON.get("sMeasurNm"));

                    //for selection of sub unit
                    if (Double.valueOf((String) loJSON.get("nQtyOnHnd")) > 0) 
                        setDetail(fnRow, "nQuantity", 1);
                    else confirmSelectParent(fnRow);


                    return true;
                } else{
                    setDetail(fnRow, fnCol, "");
                    setDetail(fnRow, "nInvCostx", 0.00);
                    setDetail(fnRow, "nQuantity", 0);
                    
                    paDetailOthers.get(fnRow).setValue("sStockIDx", "");
                    paDetailOthers.get(fnRow).setValue("sBarCodex", "");
                    paDetailOthers.get(fnRow).setValue("sDescript", "");
                    paDetailOthers.get(fnRow).setValue("sStockIDx", "");
                    paDetailOthers.get(fnRow).setValue("sParentID", "");
                    paDetailOthers.get(fnRow).setValue("nQtyOnHnd", 0);
                    paDetailOthers.get(fnRow).setValue("nResvOrdr", 0);
                    paDetailOthers.get(fnRow).setValue("nBackOrdr", 0);
                    paDetailOthers.get(fnRow).setValue("nFloatQty", 0);
                    paDetailOthers.get(fnRow).setValue("nLedgerNo", 0);
                    paDetailOthers.get(fnRow).setValue("xQuantity", 0);
                    paDetailOthers.get(fnRow).setValue("sMeasurNm", "");
                    return false;
                }
            case 4:
                lsHeader = "Barcode»Description»Inv. Type»Brand»Model»Stock ID";
                lsColName = "sBarCodex»sDescript»xInvTypNm»xBrandNme»xModelNme»sStockIDx";
                lsColCrit = "a.sBarCodex»a.sDescript»d.sDescript»b.sDescript»c.sDescript»a.sStockIDx";
                lsSQL = MiscUtil.addCondition(getSQ_Stocks(), "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE));
                
                if (fbByCode){
                    if (paDetail.get(fnRow).getOrigIDxx().equals(fsValue)) return true;
                    lsSQL = MiscUtil.addCondition(lsSQL, "a.sStockIDx = " + SQLUtil.toSQL(fsValue));
                    
                    loRS = poGRider.executeQuery(lsSQL);
                    
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
                    paDetailOthers.get(fnRow).setValue("sOrigCode", (String) loJSON.get("sBarCodex"));
                    paDetailOthers.get(fnRow).setValue("sOrigDesc", (String) loJSON.get("sDescript"));
                    
                    return true;
                } else{
                    setDetail(fnRow, fnCol, "");
                    paDetailOthers.get(fnRow).setValue("sOrigCode", "");
                    paDetailOthers.get(fnRow).setValue("sOrigDesc", "");
                    
                    return false;
                }
            default:
                return false;
        }
    }
    
    public boolean SearchDetail(int fnRow, String fsCol, String fsValue, boolean fbSearch, boolean fbByCode){
        return SearchDetail(fnRow, poDetail.getColumn(fsCol), fsValue, fbSearch, fbByCode);
    }
     
    public boolean SearchMaster(int fnCol, String fsValue, boolean fbByCode){       
        switch(fnCol){
            case 4: //sDestinat
                XMBranch loBranch = new XMBranch(poGRider, psBranchCd, true);
                if (loBranch.browseRecord(fsValue, fbByCode)){
                    setMaster(fnCol, (String) loBranch.getMaster("sBranchCd"));
                    MasterRetreived(fnCol);
                    return true;
                }

        }
        return false;
    }
    
    public boolean SearchMaster(String fsCol, String fsValue, boolean fbByCode){
        return SearchMaster(poData.getColumn(fsCol), fsValue, fbByCode);
    }
    
    public void setMaster(int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN){
            // Don't allow specific fields to assign values
            if(!(fnCol == poData.getColumn("sTransNox") ||
                fnCol == poData.getColumn("nEntryNox") ||
                fnCol == poData.getColumn("cTranStat") ||
                fnCol == poData.getColumn("sModified") ||
                fnCol == poData.getColumn("dModified"))){
                
                if (fnCol == poData.getColumn("nFreightx") ||
                    fnCol == poData.getColumn("nTranTotl") ||
                    fnCol == poData.getColumn("nDiscount")){
                    if (foData instanceof Number){
                        poData.setValue(fnCol, foData);
                    }else poData.setValue(fnCol, 0.00);
                } else poData.setValue(fnCol, foData);
                
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
        return MiscUtil.makeSelect(new UnitInvTransferMaster());
    }
    
    private String getSQ_Detail(){        
        return "SELECT" + 
                    "  a.sTransNox" +
                    ", a.nEntryNox" +
                    ", a.sStockIDx" +
                    ", a.sOrigIDxx" +
                    ", a.sOrderNox" +
                    ", a.nQuantity" +
                    ", a.nInvCostx" +
                    ", a.sRecvIDxx" +
                    ", a.nReceived" +
                    ", a.sNotesxxx" +
                    ", a.dModified" +
                    ", b.nQtyOnHnd" + 
                    ", b.nQtyOnHnd + a.nQuantity xQtyOnHnd" + 
                    ", b.nResvOrdr" +
                    ", b.nBackOrdr" +
                    ", b.nFloatQty" +
                    ", b.nLedgerNo" +
                    ", c.sBarCodex" +
                    ", c.sDescript" +
                    ", IFNULL(d.sBarCodex, '') xBarCodex" + 
                    ", a.dExpiryDt" +
                    ", e.sMeasurNm" +
                    ", a.sParentID" +
                    ", a.nParntQty" +
                    ", a.nSbItmQty" +
                " FROM Inv_Transfer_Detail a" + 
                        " LEFT JOIN Inventory d" + 
                            " ON a.sOrigIDxx = d.sStockIDx" + 
                    ", Inv_Master b" +
                        " LEFT JOIN Inventory c" + 
                            " ON b.sStockIDx = c.sStockIDx" +
                        " LEFT JOIN Measure e" + 
                            " ON c.sMeasurID = e.sMeasurID" + 
                " WHERE a.sStockIDx = b.sStockIDx" + 
                    " AND b.sBranchCD = " + SQLUtil.toSQL(psBranchCd) + 
                " ORDER BY a.nEntryNox";
    }
    
    private String getSQ_DetailExpiration(){        
        return "SELECT" +
                " a.sTransNox" +
                ", a.nEntryNox" +
                ", a.sStockIDx" +
                ", a.nQuantity" +
                ", a.nReceived" +
                ", a.dExpiryDt" +
                ", b.sDescript" +
                ", a.dExpiryDt" +
              " FROM Inv_Transfer_Detail_Expiration a" +
                ", Inventory b" +
                  " WHERE a.sStockIDx = b.sStockIDx" +
                " ORDER BY a.nEntryNox";
    }
    public int ItemCount(){
        return paDetail.size();
    }
    
    public int ItemCountExp(){
        return paDetailExpiration.size();
    }
    
     public Inventory GetInventory(String fsValue, boolean fbByCode, boolean fbSearch){        
        Inventory instance = new Inventory(poGRider, psBranchCd, fbSearch);
        instance.BrowseRecord(fsValue, fbByCode, false);
        return instance;
    }
     
    public XMBranch GetBranch(String fsValue, boolean fbByCode){
       if (fbByCode && fsValue.equals("")) return null;

       XMBranch instance  = new XMBranch(poGRider, psBranchCd, true);
       if (instance.browseRecord(fsValue, fbByCode))
           return instance;
       else
           return null;
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
    
    private String getSQ_InvTransfer(){        
        String lsTranStat = String.valueOf(pnTranStat);
        String lsCondition = "";
        String lsSQL = "SELECT " +
                            "  a.sTransNox" +
                            ", b.sBranchNm" +
                            ", a.dTransact" +
                            ", c.sBranchNm" + 
                        " FROM Inv_Transfer_Master a" +
                            " LEFT JOIN Branch b" +
                                " ON a.sDestinat = b.sBranchCd" +
                            " LEFT JOIN Branch c" +
                                " ON LEFT(a.sTransNox, 4) = c.sBranchCd";
                        //" WHERE a.sTransNox LIKE " + SQLUtil.toSQL(psBranchCd + "%");
        
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
    
    private String getSQ_Requests(){
        return "SELECT " +
                    "  a.sTransNox" +
                    ", c.sBranchNm" + 
                    ", b.sDescript" + 
                    ", a.dTransact" + 
                " FROM Inv_Stock_Request_Master a" + 
                    " LEFT JOIN Inv_Type b" + 
                        " ON a.sInvTypCd = b.sInvTypCd" + 
                    " LEFT JOIN Branch c" + 
                        " ON a.sBranchCd = c.sBranchCd" +
                " WHERE a.cTranStat = '1'" +
                    " AND a.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode());
    }
    
    private String getSQ_Parent(String fsStockIDx){
        return "SELECT" +
                    "  a.sStockIDx" + 
                    ", a.sItmSubID" + 
                    ", a.nQuantity" + 
                    ", c.sBarCodex" + 
                    ", c.sDescript" + 
                    ", b.nQtyOnHnd" + 
                    ", d.sMeasurNm" +
                " FROM Inventory_Sub_Unit a" + 
                    ", Inv_Master b" + 
                        " LEFT JOIN Inventory c" + 
                            " ON b.sStockIDx = c.sStockIDx" + 
                        " LEFT JOIN Measure d" +
                            " ON c.sMeasurID = d.sMeasurID" +
                " WHERE a.sStockIDx = b.sStockIDx" + 
                    " AND b.sBranchCd = " + SQLUtil.toSQL(psBranchCd) + 
                    " AND a.sItmSubID = " + SQLUtil.toSQL(fsStockIDx) + 
                    " AND b.nQtyOnHnd > 0";
    }
    
    private String getSQ_SubItem(String fsStockIDx){
        return "SELECT" +
                    "  a.sStockIDx" + 
                    ", a.sItmSubID" + 
                    ", a.nQuantity" + 
                    ", c.sBarCodex" + 
                    ", c.sDescript" + 
                    ", b.nQtyOnHnd" + 
                    ", d.sMeasurNm" +
                " FROM Inventory_Sub_Unit a" + 
                    ", Inv_Master b" + 
                        " LEFT JOIN Inventory c" + 
                            " ON b.sStockIDx = c.sStockIDx" + 
                        " LEFT JOIN Measure d" +
                            " ON c.sMeasurID = d.sMeasurID" +
                " WHERE a.sItmSubID= b.sStockIDx" + 
                    " AND b.sBranchCd = " + SQLUtil.toSQL(psBranchCd) + 
                    " AND a.sStockIDx = " + SQLUtil.toSQL(fsStockIDx) + 
                    " AND b.nQtyOnHnd > 0";
    }
    
    private String getSQ_StocksByRequest(){
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
                    ", b.sDescript" + 
                    ", c.sDescript" + 
                    ", d.sDescript xInvTypNm" + 
                    ", e.sTransNox" +
                    ", e.nQuantity" +
                    ", IFNULL(f.nQtyOnHnd,0) nQtyOnHnd" +
                    ", IFNULL(f.nBackOrdr,0) nBackOrdr" +
                    ", IFNULL(f.nResvOrdr,0) nResvOrdr" +
                    ", IFNULL(f.nFloatQty,0) nFloatQty" +
                    ", IFNULL(f.nLedgerNo,0) nLedgerNo" +
                    ", IFNULL(g.sMeasurNm,'') sMeasurNm" +
                " FROM Inventory a" + 
                        " LEFT JOIN Brand b" + 
                            " ON a.sBrandCde = b.sBrandCde" + 
                        " LEFT JOIN Model c" + 
                            " ON a.sModelCde = c.sModelCde" + 
                        " LEFT JOIN Inv_Type d" + 
                            " ON a.sInvTypCd = d.sInvTypCd" + 
                        " LEFT JOIN Inv_Master f" +
                            " ON a.sStockIDx = f.sStockIDx" +
                            " AND f.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode())+
                        " LEFT JOIN Measure g" +
                            " ON a.sMeasurID = g.sMeasurID" +
                    ", Inv_Stock_Request_Detail e" + 
                " WHERE a.sStockIDx = e.sStockIDx";
                    
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
                            ", e.nResvOrdr" +
                            ", e.nBackOrdr" + 
                            ", e.nFloatQty" + 
                            ", IFNULL(e.nLedgerNo, 0) nLedgerNo" + 
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
        
//                               " AND e.nQtyOnHnd > 0" +
        
        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty())
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type")));
        
        return lsSQL;
    }
    
    private void loadRequest(String fsOrderNox){
        java.sql.Connection loCon = poGRider.getConnection();

        boolean lbHasRec = false;
        Statement loStmt = null;
        ResultSet loRS = null;
        
        try {
            loStmt = loCon.createStatement();
            
            loRS = loStmt.executeQuery(MiscUtil.addCondition(getSQ_StocksByRequest(), " sTransNox = " + SQLUtil.toSQL(fsOrderNox)));
            System.out.println(MiscUtil.addCondition(getSQ_StocksByRequest(), " sTransNox = " + SQLUtil.toSQL(fsOrderNox)));
            loRS.beforeFirst();
            int lnCtr=0;
            while (loRS.next()) {
                setDetail(lnCtr,"sOrderNox", loRS.getString("sTransNox"));
                setDetail(lnCtr,"sStockIDx", loRS.getString("sStockIDx"));                
                setDetail(lnCtr,"nQuantity", loRS.getDouble("nQuantity"));

                paDetailOthers.get(lnCtr).setValue("sStockIDx", loRS.getString("sStockIDx"));
                paDetailOthers.get(lnCtr).setValue("sBarCodex", loRS.getString("sBarCodex"));
                paDetailOthers.get(lnCtr).setValue("sDescript", loRS.getString("sDescript"));
                paDetailOthers.get(lnCtr).setValue("nQtyOnHnd", loRS.getDouble("nQtyOnHnd"));
                paDetailOthers.get(lnCtr).setValue("nResvOrdr", loRS.getDouble("nResvOrdr"));
                paDetailOthers.get(lnCtr).setValue("nBackOrdr", loRS.getDouble("nBackOrdr"));
                paDetailOthers.get(lnCtr).setValue("nFloatQty", loRS.getDouble("nFloatQty"));
                paDetailOthers.get(lnCtr).setValue("nLedgerNo", loRS.getInt("nLedgerNo"));
                paDetailOthers.get(lnCtr).setValue("sInvTypNm", loRS.getString("xInvTypNm"));
                paDetailOthers.get(lnCtr).setValue("sMeasurNm", loRS.getString("sMeasurNm"));
                 
                if(!loRS.isLast()){
                    paDetail.add(new UnitInvTransferDetail());
                    paDetail.get(ItemCount()-1).setOrderNox(paDetail.get(ItemCount()-2).getOrderNox());
                    paDetailOthers.add(new UnitInvTransferDetailOthers());
                }
                lnCtr++;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        finally{
            MiscUtil.close(loRS);
            MiscUtil.close(loStmt);
        }
    }
    
    private String getStockRequestDetail(String fsOrderNox){
        return "SELECT " +
                    "  a.sTransNox" +
                    ", a.nEntryNox" + 
                    ", a.sStockIDx" + 
                    ", a.nQuantity" + 
                    ", a.nReceived" + 
                    ", a.nCancelld" + 
                " FROM Inv_Stock_Request_Detail a" + 
                " WHERE a.sTransNox = " + SQLUtil.toSQL(fsOrderNox) +
                    "AND a.nCancelld = " + SQLUtil.toSQL("0");
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
    
    private UnitInvTransferMaster poData = new UnitInvTransferMaster();
    private UnitInvTransferDetail poDetail = new UnitInvTransferDetail();
    private UnitInvTransferDetailExpiration poDetailExp = new UnitInvTransferDetailExpiration();
    private ArrayList<UnitInvTransferDetail> paDetail;
    private ArrayList<UnitInvTransferDetailOthers> paDetailOthers;
    private ArrayList<UnitInvTransferDetailExpiration> paDetailExpiration;
    
    private final String pxeModuleName = "InvTransfer";
    private double xOffset = 0; 
    private double yOffset = 0;
}
