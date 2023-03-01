/**
 * Inventory Adjustment BASE
 * @author Michael Torres Cuison
 * @since 2018.10.06
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
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.cas.inventory.others.pojo.UnitInvAdjustmentOthers;
import org.rmj.cas.inventory.pojo.UnitInvMaster;
import org.rmj.cas.inventory.pojo.UnitInvAdjustmentDetail;
import org.rmj.cas.inventory.pojo.UnitInvAdjustmentMaster;
import org.rmj.appdriver.agentfx.callback.IMasterDetail;

public class InvAdjustment{
    public InvAdjustment(GRider foGRider, String fsBranchCD, boolean fbWithParent){
        this.poGRider = foGRider;
        
        if (foGRider != null){
            this.pbWithParent = fbWithParent;
            this.psBranchCd = fsBranchCD;
            
            this.psUserIDxx = foGRider.getUserID();
            pnEditMode = EditMode.UNKNOWN;
        }
    }
    
    public boolean BrowseRecord(String fsValue, boolean fbByCode){
        String lsHeader = "Transaction No»Date»Remarks";
        String lsColName = "sTransNox»dTransact»sRemarksx";
        String lsColCrit = "a.sTransNox»»a.dTransact»a.sRemarksx";
        String lsSQL = MiscUtil.addCondition(getSQ_InvAdjustment(), "LEFT(a.sTransNox,4) = " + SQLUtil.toSQL(psBranchCd));
        
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
    
    public boolean addDetail() {
        if (paDetail.isEmpty()){
            paDetail.add(new UnitInvAdjustmentDetail());
            paDetailOthers.add(new UnitInvAdjustmentOthers());
        }
        else{
            if (!paDetail.get(ItemCount()-1).getStockIDx().equals("")){
                paDetail.add(new UnitInvAdjustmentDetail());
                paDetailOthers.add(new UnitInvAdjustmentOthers());
            }
        }
        return true;
    }

    public boolean deleteDetail(int fnRow) {
        paDetail.remove(fnRow);
        paDetailOthers.remove(fnRow);
        
        if (paDetail.isEmpty()){
            paDetail.add(new UnitInvAdjustmentDetail());
            paDetailOthers.add(new UnitInvAdjustmentOthers());
        }            
        
        return true;
    }
    
    public void setDetail(int fnRow, int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN){
            // Don't allow specific fields to assign values
            if(!(fnCol == poDetail.getColumn("sTransNox") ||
                fnCol == poDetail.getColumn("nEntryNox") ||
                fnCol == poDetail.getColumn("dModified"))){

                if (fnCol == poDetail.getColumn("nDebitQty")){
                    if (foData instanceof Number){
                        if (Double.valueOf(foData.toString()) > Double.valueOf(paDetailOthers.get(fnRow).getValue("nQtyOnHnd").toString()))
                            paDetail.get(fnRow).setValue(fnCol, Double.valueOf(paDetailOthers.get(fnRow).getValue("nQtyOnHnd").toString()));
                        else
                            paDetail.get(fnRow).setValue(fnCol, foData);
                        
                        addDetail();
                    }else paDetail.get(fnRow).setValue(fnCol, 0);
                } else if (fnCol == poDetail.getColumn("nCredtQty")){
                    if (foData instanceof Number){
//                        if (Double.valueOf(foData.toString()) > Double.valueOf(paDetailOthers.get(fnRow).getValue("nQtyOnHnd").toString()))
//                            paDetail.get(fnRow).setValue(fnCol, Double.valueOf(paDetailOthers.get(fnRow).getValue("nQtyOnHnd").toString()));
//                        else
                            paDetail.get(fnRow).setValue(fnCol, foData);
                        
                        addDetail();
                    }else paDetail.get(fnRow).setValue(fnCol, 0);    
                } else if (fnCol == poDetail.getColumn("dExpiryDt")){
                    if (foData instanceof Date){
                        paDetail.get(fnRow).setValue(fnCol, foData);
                    }else paDetail.get(fnRow).setValue(fnCol, poGRider.getServerDate());
                } else paDetail.get(fnRow).setValue(fnCol, foData);
                
                DetailRetreived(fnCol);
            }
        }
    }

    public void setDetail(int fnRow, String fsCol, Object foData) {
        setDetail(fnRow, poDetail.getColumn(fsCol), foData);
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

    public boolean newTransaction() {
        Connection loConn = null;
        loConn = setConnection();       
        
        poData = new UnitInvAdjustmentMaster();
        poData.setTransNox(MiscUtil.getNextCode(poData.getTable(), "sTransNox", true, loConn, psBranchCd));
        poData.setTransact(poGRider.getServerDate());
        
        paDetail = new ArrayList<>();
        paDetailOthers = new ArrayList<>(); //detail other info storage
        addDetail();
        
        pnEditMode = EditMode.ADDNEW;
        return true;
    }
    
    private boolean isInventoryOK(String fsValue){
        int lnMasRow = poData.getEntryNox();
        
        String lsSQL = MiscUtil.addCondition(getSQ_Detail(), "sTransNox = " + SQLUtil.toSQL(fsValue));
        
        try {
            ResultSet loRS = poGRider.executeQuery(lsSQL);
        
            if (MiscUtil.RecordCount(loRS) != lnMasRow){
                lsSQL = MiscUtil.makeSelect(new UnitInvAdjustmentDetail());
                lsSQL = MiscUtil.addCondition(lsSQL, "sTransNox = " + SQLUtil.toSQL(fsValue));

                loRS = poGRider.executeQuery(lsSQL);
                
                ResultSet loRSx;
                InvMaster loInvMaster = new InvMaster(poGRider, psBranchCd, false);
                
                while (loRS.next()){
                    lsSQL = MiscUtil.makeSelect(new UnitInvMaster());
                    lsSQL = MiscUtil.addCondition(lsSQL, "sStockIDx = " + SQLUtil.toSQL(loRS.getString("sStockIDx")) +
                                                            " AND sBranchCD = " + SQLUtil.toSQL(psBranchCd));
                    
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
    
    public ResultSet getExpiration(String fsStockIDx){
        String lsSQL = "SELECT * FROM Inv_Master_Expiration" +
                        " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx) +
                            " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                            " AND nQtyOnHnd > 0" +
                        " ORDER BY dExpiryDt";     
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        return loRS;
    }

    public UnitInvAdjustmentMaster loadTransaction(String fsTransNox) {
        UnitInvAdjustmentMaster loObject = new UnitInvAdjustmentMaster();
        
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
    
    private ArrayList<UnitInvAdjustmentDetail> loadTransactionDetail(String fsTransNox){
        UnitInvAdjustmentDetail loOcc = null;
        UnitInvAdjustmentOthers loOth = null;
        Connection loConn = null;
        loConn = setConnection();
        
        ArrayList<UnitInvAdjustmentDetail> loDetail = new ArrayList<>();
        paDetailOthers = new ArrayList<>(); //reset detail others
        
        //2019.05.23
        //  Check first if the transferred items are in the destination's inventory
        if (!isInventoryOK(fsTransNox)) return null;
        
        String lsSQL = MiscUtil.addCondition(getSQ_Detail(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        try {
            ResultSet loRS = poGRider.executeQuery(lsSQL);  
            
            for (int lnCtr = 1; lnCtr <= MiscUtil.RecordCount(loRS); lnCtr ++){
                loRS.absolute(lnCtr);

                //load detail
                loOcc = new UnitInvAdjustmentDetail();
                loOcc.setValue("sTransNox", loRS.getString("sTransNox"));        
                loOcc.setValue("nEntryNox", loRS.getInt("nEntryNox"));
                loOcc.setValue("sStockIDx", loRS.getString("sStockIDx"));
                loOcc.setValue("nCredtQty", loRS.getDouble("nCredtQty"));
                loOcc.setValue("nDebitQty", loRS.getDouble("nDebitQty"));
                loOcc.setValue("nInvCostx", loRS.getDouble("nInvCostx"));
                loOcc.setValue("dExpiryDt", loRS.getDate("dExpiryDt"));
                loOcc.setValue("dModified", loRS.getDate("dModified"));
                loDetail.add(loOcc);
                
                //load other info
                loOth = new UnitInvAdjustmentOthers();
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
    
    private boolean saveInvTrans(){
        String lsSQL = "";
        ResultSet loRS = null;
        int lnCtr=0;
        Double lnCreditQty=0.00;
        Double lnDebitQtyx=0.00;
        
        lsSQL = "SELECT" +
                    "  a.sStockIDx" +
                    ", (SUM(a.nCredtQty) - SUM(a.nDebitQty)) xValuexxx" +
                    ", ABS((SUM(a.nCredtQty) - SUM(a.nDebitQty))) xActualxx" +
                    ", a.nEntryNox" +
                    ", a.dExpiryDt" +
                " FROM Inv_Adjustment_Detail a" +
                    " LEFT JOIN Inv_Master_Expiration b" +
                        " ON a.sStockIDx = b.sStockIDx" +
                        " AND a.dExpiryDt = b.dExpiryDt" +
                        " AND b.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()) +
                    ", Inv_Master c" +
                " WHERE a.sTransNox = " + SQLUtil.toSQL(poData.getTransNox()) +
                    " AND a.sStockIDx = c.sStockIDx" +
                    " AND c.sBranchCd = " + SQLUtil.toSQL(poGRider.getBranchCode()) +
                " GROUP BY a.sStockIDx, a.dExpiryDt";
                
        loRS = poGRider.executeQuery(lsSQL);
        
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
//        loInvTrans.InitTransaction();
        try {
            
//            if (MiscUtil.RecordCount(loRS)==0){
//                if (poDetail.){
//                        if (!loInvTrans.CreditMemo(poData.getTransNox(), poData.getTransact(), EditMode.ADDNEW)){
//                            setMessage(loInvTrans.getMessage());
//                            setErrMsg(loInvTrans.getErrMsg());
//                            return false;
//                        }
//                    }else{
//                        if (!loInvTrans.DebitMemo(poData.getTransNox(), poData.getTransact(), EditMode.ADDNEW)){
//                            setMessage(loInvTrans.getMessage());
//                            setErrMsg(loInvTrans.getErrMsg());
//                            return false;
//                        }
//                    }
//            }
            while (loRS.next()){
                if (loRS.getDouble("xActualxx")!=0){
                    loInvTrans.InitTransaction();
                    loInvTrans.setDetail(0, "sStockIDx", loRS.getString("sStockIDx"));
                    
                    loInvTrans.setDetail(0, "nQuantity", loRS.getDouble("xActualxx"));
                    loInvTrans.setDetail(0, "nLedgerNo", loRS.getInt("nEntryNox"));
                    
                    lnCreditQty=0.00;
                    lnDebitQtyx=0.00;
                    if (loRS.getDouble("xValuexxx")>0){
                        if (!loInvTrans.CreditMemo(poData.getTransNox(), poData.getTransact(), EditMode.ADDNEW)){
                            setMessage(loInvTrans.getMessage());
                            setErrMsg(loInvTrans.getErrMsg());
                            return false;
                        }
                        lnCreditQty=loRS.getDouble("xActualxx");
                    }else{
                        if (!loInvTrans.DebitMemo(poData.getTransNox(), poData.getTransact(), EditMode.ADDNEW)){
                            setMessage(loInvTrans.getMessage());
                            setErrMsg(loInvTrans.getErrMsg());
                            return false;
                        }
                        lnDebitQtyx=loRS.getDouble("xActualxx");
                    }
                    
                    saveInvExpiration(poData.getTransact(),
                                        loRS.getDate("dExpiryDt"),
                                        loRS.getString("sStockIDx"),
                                        lnCreditQty,
                                        lnDebitQtyx);
                }
//                lnCtr=lnCtr+1;
            }
        } catch (SQLException ex) {
             Logger.getLogger(InvAdjustment.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        return true;
    }
    
    private boolean saveInvExpiration(Date fdTransact, 
                                            Date fdExpiry,
                                            String fsStockIDx,
                                            Double flCreditQty,
                                            Double flDebitQtyx){
        InvExpiration loInvTrans = new InvExpiration(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();
      
        loInvTrans.setDetail(0, "sStockIDx", fsStockIDx);
        loInvTrans.setDetail(0, "dExpiryDt", fdExpiry);
        loInvTrans.setDetail(0, "nQtyInxxx", flCreditQty);
        loInvTrans.setDetail(0, "nQtyOutxx", flDebitQtyx);
            
        if (flCreditQty>0.00){
            if (!loInvTrans.CreditMemo(fdTransact, EditMode.ADDNEW)){
                setMessage(loInvTrans.getMessage());
                setErrMsg(loInvTrans.getErrMsg());
                return false;
            }
        }else{
            if (!loInvTrans.DebitMemo(fdTransact, EditMode.ADDNEW)){
                setMessage(loInvTrans.getMessage());
                setErrMsg(loInvTrans.getErrMsg());
                return false;
            }
        }
        
        //TODO
            //update branch order info
    
        return true;
    }
    
    private boolean saveInvExpirationOld(Date fdTransact){
        InvExpiration loInvTrans = new InvExpiration(poGRider, poGRider.getBranchCode());
        loInvTrans.InitTransaction();
        
        for (int lnCtr = 0; lnCtr <= paDetail.size() - 1; lnCtr ++){
            if (paDetail.get(lnCtr).getStockIDx().equals("")) break;
            loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getStockIDx());
            loInvTrans.setDetail(lnCtr, "dExpiryDt", paDetail.get(lnCtr).getDateExpiry());
            loInvTrans.setDetail(lnCtr, "nQtyInxxx", paDetail.get(lnCtr).getCreditQTY());
            loInvTrans.setDetail(lnCtr, "nQtyOutxx", paDetail.get(lnCtr).getDebitQTY());
            
            if (paDetail.get(lnCtr).getCreditQTY().doubleValue()>0.00){
                if (!loInvTrans.CreditMemo(fdTransact, EditMode.ADDNEW)){
                    setMessage(loInvTrans.getMessage());
                    setErrMsg(loInvTrans.getErrMsg());
                    return false;
                }
            }else{
                if (!loInvTrans.DebitMemo(fdTransact, EditMode.ADDNEW)){
                    setMessage(loInvTrans.getMessage());
                    setErrMsg(loInvTrans.getErrMsg());
                    return false;
                }
            }
        }
        
        //TODO
            //update branch order info
    
        return true;
    }

    public boolean saveTransaction() {
        String lsSQL = "";
        boolean lbUpdate = false;
        
        UnitInvAdjustmentMaster loOldEnt = null;
        UnitInvAdjustmentMaster loNewEnt = null;
        UnitInvAdjustmentMaster loResult = null;
        
        // Check for the value of foEntity
        if (!(poData instanceof UnitInvAdjustmentMaster)) {
            setErrMsg("Invalid Entity Passed as Parameter");
            return false;
        }
        
        // Typecast the Entity to this object
        loNewEnt = (UnitInvAdjustmentMaster) poData;
               
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
        else if (paDetail.get(0).getStockIDx().equals("")){
            setMessage("Detail might not have item or zero quantity.");
            return false;
        }
        
        int lnCtr;
        String lsSQL;
        UnitInvAdjustmentDetail loNewEnt = null;
        
        if (pnEditMode == EditMode.ADDNEW){
            Connection loConn = null;
            loConn = setConnection();  
            
            for (lnCtr = 0; lnCtr <= paDetail.size() -1; lnCtr++){
                loNewEnt = paDetail.get(lnCtr);
                
                if (!loNewEnt.getStockIDx().equals("")){
//                    if (loNewEnt.getQuantity() == 0){
//                       setMessage("Detail might not have item or zero quantity.");
//                        return false;
//                    }
                    
                    loNewEnt.setTransNox(fsTransNox);
                    loNewEnt.setEntryNox(lnCtr + 1);
                    
//                    ResultSet loRS = getExpiration(loNewEnt.getStockIDx());
//                    try {
//                        loRS.absolute(1);
//                        loNewEnt.setDateExpiry(loRS.getDate("dExpiryDt"));
//                    } catch (SQLException ex) {
//                        Logger.getLogger(InvTransfer.class.getName()).log(Level.SEVERE, null, ex);
//                    }
                    
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
            ArrayList<UnitInvAdjustmentDetail> laSubUnit = loadTransactionDetail(poData.getTransNox());
            
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
//                    for(int lnCtr2 = lnCtr; lnCtr2 <= laSubUnit.size()-1; lnCtr2++){
//                        lsSQL = "DELETE FROM " + poDetail.getTable()+
//                                " WHERE sStockIDx = " + SQLUtil.toSQL(laSubUnit.get(lnCtr2).getStockIDx()) +
//                                    " AND nEntryNox = " + SQLUtil.toSQL(laSubUnit.get(lnCtr2).getEntryNox());
//
//                        if (!lsSQL.equals("")){
//                            if(poGRider.executeQuery(lsSQL, poDetail.getTable(), "", "") == 0){
//                                if(!poGRider.getErrMsg().isEmpty()){
//                                    setErrMsg(poGRider.getErrMsg());
//                                    return false;
//                                }
//                            } 
//                        }
//                    }
                    break;
                }
            }
        }

        return true;
    }

    public boolean deleteTransaction(String string) {
        UnitInvAdjustmentMaster loObject = loadTransaction(string);
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
        UnitInvAdjustmentMaster loObject = loadTransaction(string);
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

    public boolean voidTransaction(String string) {
        UnitInvAdjustmentMaster loObject = loadTransaction(string);
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
                
                if (loJSON != null){
                    setDetail(fnRow, fnCol, (String) loJSON.get("sStockIDx"));
                    setDetail(fnRow, 6, Double.valueOf((String) loJSON.get("nUnitPrce")));
                    paDetailOthers.get(fnRow).setValue("sStockIDx", (String) loJSON.get("sStockIDx"));
                    paDetailOthers.get(fnRow).setValue("sBarCodex", (String) loJSON.get("sBarCodex"));
                    paDetailOthers.get(fnRow).setValue("sDescript", (String) loJSON.get("sDescript"));
                    paDetailOthers.get(fnRow).setValue("nQtyOnHnd", Double.valueOf((String) loJSON.get("nQtyOnHnd")));
                    paDetailOthers.get(fnRow).setValue("nLedgerNo", Integer.valueOf((String) loJSON.get("nLedgerNo")));
                    paDetailOthers.get(fnRow).setValue("sInvTypNm", (String) loJSON.get("sInvTypNm"));
                    paDetailOthers.get(fnRow).setValue("sMeasurNm", (String) loJSON.get("sMeasurNm"));

                    //if (Integer.valueOf((String) loJSON.get("nQtyOnHnd")) > 0) 
                    //    setDetail(fnRow, "nQuantity", 1);
                    //else confirmSelectParent(fnRow);

                    return true;
                } else{
                    setDetail(fnRow, fnCol, "");
                    setDetail(fnRow, "nCredtQty", 0);
                    setDetail(fnRow, "nDebitQty", 0);
                    
                    paDetailOthers.get(fnRow).setValue("sStockIDx", "");
                    paDetailOthers.get(fnRow).setValue("sBarCodex", "");
                    paDetailOthers.get(fnRow).setValue("sDescript", "");
                    paDetailOthers.get(fnRow).setValue("nQtyOnHnd", 0);
                    paDetailOthers.get(fnRow).setValue("nLedgerNo", 0);
                    paDetailOthers.get(fnRow).setValue("xQuantity", 0);
                    paDetailOthers.get(fnRow).setValue("sMeasurNm", 0);
                    return false;
                }
            case 4:
                lsHeader = "Barcode»Description»Inv. Type»Brand»Model»Stock ID";
                lsColName = "sBarCodex»sDescript»xInvTypNm»xBrandNme»xModelNme»sStockIDx";
                lsColCrit = "a.sBarCodex»a.sDescript»d.sDescript»b.sDescript»c.sDescript»a.sStockIDx";
                lsSQL = MiscUtil.addCondition(getSQ_Stocks(), "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE));
                
                if (fbByCode){
//                    if (paDetail.get(fnRow).getOrigIDxx().equals(fsValue)) return true;
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
                    paDetailOthers.get(fnRow).setValue("nQtyOnHnd", Double.valueOf((String) loJSON.get("nQtyOnHnd")));
                    
                    paDetailOthers.get(fnRow).setValue("sStockIDx", (String) loJSON.get("sStockIDx"));
                    paDetailOthers.get(fnRow).setValue("nLedgerNo", Integer.valueOf((String) loJSON.get("nLedgerNo")));
                    paDetailOthers.get(fnRow).setValue("sInvTypNm", (String) loJSON.get("sInvTypNm"));
                    paDetailOthers.get(fnRow).setValue("sMeasurNm", (String) loJSON.get("sMeasurNm"));
                    
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
        return MiscUtil.makeSelect(new UnitInvAdjustmentMaster());
    }
    
    private String getSQ_Detail(){        
        return "SELECT" + 
                    "  a.sTransNox" +
                    ", a.nEntryNox" +
                    ", a.sStockIDx" +
                    ", a.nCredtQty" +
                    ", a.nDebitQty" +
                    ", a.nInvCostx" +
                    ", a.dExpiryDt" +
                    ", a.sRemarksx" +
                    ", a.dModified" +
                    ", b.nQtyOnHnd" + 
                    ", b.nQtyOnHnd  xQtyOnHnd" + 
                    ", b.nResvOrdr" +
                    ", b.nBackOrdr" +
                    ", b.nFloatQty" +
                    ", b.nLedgerNo" +
                    ", c.sBarCodex" +
                    ", c.sDescript" +
                    ", IFNULL(d.sBarCodex, '') xBarCodex" + 
                     ", e.sMeasurNm" +
                " FROM Inv_Adjustment_Detail a" + 
                        " LEFT JOIN Inventory d" + 
                            " ON a.sStockIDx = d.sStockIDx" + 
                    ", Inv_Master b" +
                        " LEFT JOIN Inventory c" + 
                            " ON b.sStockIDx = c.sStockIDx" + 
                        " LEFT JOIN Measure e" + 
                            " ON c.sMeasurID = e.sMeasurID" + 
                " WHERE a.sStockIDx = b.sStockIDx" + 
                    " AND b.sBranchCD = " + SQLUtil.toSQL(psBranchCd) + 
                " ORDER BY a.nEntryNox";
    }
    
    public int ItemCount(){
        return paDetail.size();
    }
    
     public Inventory GetInventory(String fsValue, boolean fbByCode, boolean fbSearch){        
        Inventory instance = new Inventory(poGRider, psBranchCd, fbSearch);
        instance.BrowseRecord(fsValue, fbByCode, false);
        return instance;
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
    
    private String getSQ_InvAdjustment(){        
        String lsTranStat = String.valueOf(pnTranStat);
        String lsCondition = "";
        String lsSQL = "SELECT " +
                            "  a.sTransNox" +
                            ", a.sReferNox" +
                            ", a.dTransact" +
                            ", a.sRemarksx" + 
                        " FROM Inv_Adjustment_Master a" +
                            ", Inv_Adjustment_Detail b" +
                            ", Inventory c" +
                        " WHERE a.sTransNox = b.sTransNox" +
                            " AND b.sStockIDx = c.sStockIDx" +
                        " GROUP BY a.sTransNox";
        
        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty())
            lsSQL = MiscUtil.addCondition(lsSQL, "c.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type")));
        
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
        
        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty())
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type")));
        
        return lsSQL;
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
    
    private UnitInvAdjustmentMaster poData = new UnitInvAdjustmentMaster();
    private UnitInvAdjustmentDetail poDetail = new UnitInvAdjustmentDetail();
    private ArrayList<UnitInvAdjustmentDetail> paDetail;
    private ArrayList<UnitInvAdjustmentOthers> paDetailOthers;
    
    private final String pxeModuleName = "InvAdjustment";
    private double xOffset = 0; 
    private double yOffset = 0;
}
