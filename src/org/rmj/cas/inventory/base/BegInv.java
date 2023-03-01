package org.rmj.cas.inventory.base;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.constants.EditMode;

public class BegInv {
    public String getMessage(){
        return psMessage;
    }
    
    private void setMessage(String fsValue){
        psMessage = fsValue;
    }
    
    public ArrayList<InvMaster> Inventory(){
        return paDetail;
    }
    
    public BegInv(GRider foGRider, String fsBranchCD){
        this.poGRider = foGRider;
        
        if (foGRider != null){
            this.psBranchCd = fsBranchCD;
            
            this.psUserIDxx = foGRider.getUserID();
        }
    }
    
    public boolean NewTransaction(){
        if (poGRider == null){
            setMessage("GhostRider Application Driver was not set...");
            return false;
        }
        
        try {
            String lsSQL = getSQ_Stocks();
            ResultSet loRS = poGRider.executeQuery(lsSQL);

            if (MiscUtil.RecordCount(loRS) == 0){
                setMessage("No records to update beginning inventory...");
                return false;
            }
            
            paDetail = new ArrayList<>();
            for (int lnCtr = 0; lnCtr <= paDetail.size()-1; lnCtr++){
                loRS.absolute(lnCtr);
                
                paDetail.add(new InvMaster(poGRider, psBranchCd, true));
                if (!paDetail.get(lnCtr).SearchInventory(loRS.getString("sStockIDx"), true, true)){
                    setMessage(paDetail.get(lnCtr).getMessage());
                    return false;
                }
                if (!paDetail.get(lnCtr).UpdateRecord()){
                    setMessage(paDetail.get(lnCtr).getMessage());
                    return false;
                }
            }
            
        } catch (SQLException ex) {
            setMessage(ex.getMessage());
            return false;
        }
        
        return true;
    }
    
    public boolean SaveTransaction(){
        InventoryTrans loInvTrans = new InventoryTrans(poGRider, poGRider.getBranchCode());
        
        poGRider.beginTrans();
        for (int lnCtr = 0; lnCtr <= paDetail.size()-1; lnCtr++){
            if (Double.valueOf(paDetail.get(lnCtr).getMaster("nBegQtyxx").toString()) > 0){
                //update beginning inventory
                paDetail.get(lnCtr).setMaster("dBegQtyxx", poGRider.getServerDate());
                paDetail.get(lnCtr).setMaster("dLastTran", poGRider.getServerDate());
                if (!paDetail.get(lnCtr).SaveRecord()){
                    setMessage(paDetail.get(lnCtr).getMessage());
                    poGRider.rollbackTrans();
                    return false;
                }
                //update qty on hand
                loInvTrans.InitTransaction();
                loInvTrans.setDetail(lnCtr, "sStockIDx", paDetail.get(lnCtr).getMaster("sStockIDx"));
                loInvTrans.setDetail(lnCtr, "nQuantity", paDetail.get(lnCtr).getMaster("nBegQtyxx"));
                loInvTrans.setDetail(lnCtr, "nQtyOnHnd", 0);
                loInvTrans.setDetail(lnCtr, "nResvOrdr", 0);
                loInvTrans.setDetail(lnCtr, "nBackOrdr", 0);
                
                if (!loInvTrans.DebitMemo("X00119XXXXXX", poGRider.getServerDate(), EditMode.ADDNEW)){
                    setMessage(loInvTrans.getMessage() + "\n\n" + loInvTrans.getErrMsg());
                    poGRider.rollbackTrans();
                    return false;
                }
            }
        }
        poGRider.commitTrans();
        
        return true;
    }
    
    private String getSQ_Stocks(){
        return "SELECT" +
                    "  sStockIDx" +
                " FROM Inv_Master" +
                " WHERE nBegQtyxx = 0" +
                    " AND nQuantity = 0" +
                    " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);
    }
    
    //Member Variables
    private GRider poGRider = null;
    private String psUserIDxx = "";
    private String psBranchCd = "";
    private String psMessage = "";
    
    private ArrayList<InvMaster> paDetail;
}
