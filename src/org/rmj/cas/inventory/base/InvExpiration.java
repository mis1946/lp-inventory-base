package org.rmj.cas.inventory.base;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.cas.inventory.constants.basefx.InvConstants;
import org.rmj.cas.inventory.pojo.UnitInvMasterExpiration;

/**
 * Inventory Transaction Ledger BASE
 * @author Michael Torres Cuison
 * @since 2018.10.12
 */

public class InvExpiration {
    public InvExpiration(GRider foGRider, String fsBranchCD){
        this.poGRider = foGRider;
        
        if (foGRider != null){
            psSourceCd = "";
            psSourceNo = "";
            psBranchCd = fsBranchCD.equals("") ? poGRider.getBranchCode() : fsBranchCD;
            pnEditMode = EditMode.UNKNOWN;
        }
    }
    
    public boolean InitTransaction(){
        if (!pbInitTran){
            if (poGRider == null){
                setMessage("GhostRider Application is not initialized.");
                return false;
            }

            if (psBranchCd.equals("")) psBranchCd = poGRider.getBranchCode();
            pbWarehous = poGRider.isWarehouse();
            pbInitTran = true;
        }
        
        poRSMaster = new ArrayList<>();
        return addDetail();
    }
    
    public void setDetail(int fnRow, String fsIndex, Object fsValue){
        if (!pbInitTran) return;
        
        if (fnRow > poRSMaster.size()) return;
        if (fnRow == poRSMaster.size()) addDetail();
        
        switch(fsIndex.toLowerCase()){
            case "sstockidx":
                poRSMaster.get(fnRow).setStockID((String) fsValue); break;
            case "dexpirydt":
                poRSMaster.get(fnRow).setDateExpiry((Date) fsValue); break;
            case "nqtyinxxx":
                poRSMaster.get(fnRow).setQtyInxxx(Double.valueOf(fsValue.toString())); break;
            case "nqtyoutxx":
                poRSMaster.get(fnRow).setQtyOutxx(Double.valueOf(fsValue.toString())); break;
        }
    }
    
    public boolean AcceptDelivery(Date fdTransDate,
                                    int fnUpdateMode){
        psSourceCd = InvConstants.ACCEPT_DELIVERY;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
    
        return saveTransaction();
    }
    
    public boolean Delivery(Date fdTransDate,
                            int fnUpdateMode){
        psSourceCd = InvConstants.DELIVERY;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        
        return saveTransaction();
    }
    
    public boolean POReceiving(Date fdTransDate,
                                int fnUpdateMode){        
        psSourceCd = InvConstants.PURCHASE_RECEIVING;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        
        return saveTransaction();
    }
    
    public boolean POReturn(Date fdTransDate,
                                int fnUpdateMode){        
        psSourceCd = InvConstants.PURCHASE_RETURN;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        
        return saveTransaction();
    }
    
    public boolean CreditMemo(Date fdTransDate,
                                    int fnUpdateMode){        
        psSourceCd = InvConstants.CREDIT_MEMO;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        
        return saveTransaction();
    }
    
    public boolean DebitMemo(Date fdTransDate,
                                    int fnUpdateMode){        
        psSourceCd = InvConstants.DEBIT_MEMO;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        
        return saveTransaction();
    }
    
    public boolean DailyProduction_IN(Date fdTransDate,
                                    int fnUpdateMode){        
        psSourceCd = InvConstants.DAILY_PRODUCTION_IN;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        
        return saveTransaction();
    }
    
    public boolean DailyProduction_OUT(Date fdTransDate,
                                    int fnUpdateMode){        
        psSourceCd = InvConstants.DAILY_PRODUCTION_OUT;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        
        return saveTransaction();
    }
    
    public boolean Sales(String fsSourceNo,
                            Date fdTransDate,
                            int fnUpdateMode){        
        psSourceCd = InvConstants.SALES;
        psSourceNo = fsSourceNo;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        
        return saveTransaction();
    }
    
    public boolean WasteInventory(Date fdTransDate,
                                    int fnUpdateMode){        
        psSourceCd = InvConstants.WASTE_INV;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        
        return saveTransaction();
    }
    
    private boolean saveTransaction(){
        setMessage("");
        setErrMsg("");
           
        if (!processInventory()) return false;
        
        return saveDetail();
    }
    
    private boolean addDetail(){
        poRSMaster.add(new UnitInvMasterExpiration());
        return true;
    }
    
    private boolean processInventory(){
        String lsSQL;
        ResultSet loRS;
        int lnRow;
        
        poRSProcessd = new ArrayList<>();
        
        for (int lnCtr = 0; lnCtr <= poRSMaster.size()-1; lnCtr++){
//            lnRow = findOnProcInventory("sStockIDx", poRSMaster.get(lnCtr).getStockID());
            
            //-1 if no record found on filter
//            if (lnRow == -1){
                lsSQL = "SELECT" +
                            "  a.dExpiryDt" + 
                            ", a.nQtyOnHnd" + 
                            ", a.dTimeStmp" +
                            ", a.sStockIDx" +
                            ", b.dExpiryDt xExpiryDt" +
                            ", b.nQtyOnHnd xQtyOnHnd" +
                        " FROM Inv_Master_Expiration a" +
                            " LEFT JOIN Inv_Master_Expiration b" +
                                " ON a.sStockIDx = b.sStockIDx" +
                                " AND a.sBranchCd = b.sBranchCd" + 
                                " AND b.dExpiryDt = " + SQLUtil.toSQL(poRSMaster.get(lnCtr).getDateExpiry()) +
                        " WHERE a.sStockIDx = " + SQLUtil.toSQL(poRSMaster.get(lnCtr).getStockID()) + 
                            " AND a.sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                        " ORDER BY" +
                            "  a.dExpiryDt ASC" +
                            ", a.nQtyOnHnd DESC" +
                        " LIMIT 1";
                
                loRS = poGRider.executeQuery(lsSQL);
                
                poRSProcessd.add(new UnitInvMasterExpiration());
                lnRow = poRSProcessd.size()-1;
                
                if (MiscUtil.RecordCount(loRS) == 0){     
                    poRSProcessd.get(lnRow).IsNewParts("1");
                    System.out.print(poRSMaster.get(lnCtr).getStockID());
                    poRSProcessd.get(lnRow).setStockID(poRSMaster.get(lnCtr).getStockID());
                    poRSProcessd.get(lnRow).setBegInventory(pdTransact);
                    poRSProcessd.get(lnRow).setDateExpiry(pdTransact);
                    poRSProcessd.get(lnRow).setQtyOnHnd(0);
                } else {
                    try {
                        loRS.first();
                        if(loRS.getDate("xExpiryDt") == null) {
                            poRSProcessd.get(lnRow).IsNewParts("1");
                            poRSProcessd.get(lnRow).setDateExpiry(loRS.getDate("dExpiryDt"));
                            poRSProcessd.get(lnRow).setQtyOnHnd(loRS.getDouble("nQtyOnHnd"));
                        }else{    
                            poRSProcessd.get(lnRow).IsNewParts("0");
                            poRSProcessd.get(lnRow).setDateExpiry(loRS.getDate("xExpiryDt"));
                            poRSProcessd.get(lnRow).setQtyOnHnd(loRS.getDouble("xQtyOnHnd"));
                        }       
                        
                        poRSProcessd.get(lnRow).setStockID(loRS.getString("sStockIDx"));
                        poRSProcessd.get(lnRow).setTimeStmp(loRS.getDate("dTimeStmp"));
                    } catch (SQLException e) {
                        setErrMsg(e.getMessage());
                        return false;
                    }    
                }
//            }
            
            switch (psSourceCd){
                case InvConstants.ACCEPT_DELIVERY:
//                    poRSProcessd.get(lnRow).setQtyInxxx(poRSProcessd.get(lnRow).getQtyInxxx() 
//                                                        + poRSMaster.get(lnCtr).getQuantity());
                    
                        
                    if (poRSProcessd.get(lnCtr).IsNewParts().equals("1")) poRSProcessd.get(lnRow).setQtyOnHnd(0);

                    poRSProcessd.get(lnRow).setQtyInxxx(poRSProcessd.get(lnRow).getQtyInxxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());

                    poRSProcessd.get(lnRow).setBegInventory(pdTransact);
                    //testing for null values
                    poRSProcessd.get(lnRow).setDateExpiry(poRSMaster.get(lnCtr).getDateExpiry());
                    poRSProcessd.get(lnRow).setQuantity(poRSProcessd.get(lnRow).getQtyOnHnd().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQtyInxxx().doubleValue());
                    break;
                case InvConstants.BRANCH_ORDER:
                case InvConstants.BRANCH_ORDER_CONFIRM:
                case InvConstants.CUSTOMER_ORDER:
                case InvConstants.RETAIL_ORDER:
                case InvConstants.CANCEL_RETAIL_ORDER:
                case InvConstants.CANCEL_WHOLESALE_ORDER:
                case InvConstants.DELIVERY:
                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());

                    poRSProcessd.get(lnRow).setBegInventory(pdTransact);
                    poRSProcessd.get(lnRow).setDateExpiry(poRSMaster.get(lnCtr).getDateExpiry());
                    poRSProcessd.get(lnRow).setQuantity(poRSProcessd.get(lnRow).getQtyOnHnd().doubleValue()
                                                        - poRSMaster.get(lnCtr).getQtyOutxx().doubleValue());
                       
//                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx()
//                                                        + poRSMaster.get(lnCtr).getQuantity());
                    
//                    if (poRSMaster.get(lnCtr).getReplacID().equals("")){
//                        poRSProcessd.get(lnRow).setQtyIssue(poRSProcessd.get(lnRow).getQtyIssue() 
//                                                            + poRSMaster.get(lnCtr).getQuantity());
//                    }
                    break;
                case InvConstants.JOB_ORDER:
                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    
//                    if (poRSMaster.get(lnCtr).getReplacID().equals("")){
//                        poRSProcessd.get(lnRow).setQtyIssue(poRSProcessd.get(lnRow).getQtyIssue() 
//                                                            + poRSMaster.get(lnCtr).getResvOrdr());
//                    }        
                    break;
                case InvConstants.PURCHASE:
//                    poRSProcessd.get(lnRow).setQtyOrder(poRSProcessd.get(lnRow).getQtyOrder()
//                                                        + poRSMaster.get(lnCtr).getQtyOrder());
//                    poRSProcessd.get(lnRow).setQtyIssue(poRSProcessd.get(lnRow).getQtyIssue()
//                                                        + poRSMaster.get(lnCtr).getQtyIssue());
//                    break;
                    break;
                case InvConstants.PURCHASE_RECEIVING:
//                    if (!CommonUtils.xsDateShort(poRSMaster.get(lnCtr).getDateExpiry()).equals(pdTransact.toString())){
                        if (poRSProcessd.get(lnRow).IsNewParts().equals("1")) poRSProcessd.get(lnRow).setQtyOnHnd(0);
//                    }
                   
                    poRSProcessd.get(lnRow).setQtyInxxx(poRSProcessd.get(lnRow).getQtyInxxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());

                    poRSProcessd.get(lnRow).setBegInventory(pdTransact);
                    poRSProcessd.get(lnRow).setDateExpiry(poRSMaster.get(lnCtr).getDateExpiry());
                    poRSProcessd.get(lnRow).setQuantity(poRSProcessd.get(lnRow).getQtyOnHnd().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQtyInxxx().doubleValue());
//                    if (poRSMaster.get(lnCtr).getReplacID().equals("")){
//                        poRSProcessd.get(lnRow).setQtyOrder(poRSProcessd.get(lnRow).getQtyOrder() 
//                                                            - poRSMaster.get(lnCtr).getQuantity());
//                    }
                    break;
                case InvConstants.PURCHASE_RETURN:
                     poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());

                    poRSProcessd.get(lnRow).setBegInventory(pdTransact);
                    poRSProcessd.get(lnRow).setDateExpiry(poRSMaster.get(lnCtr).getDateExpiry());
                    poRSProcessd.get(lnRow).setQuantity(poRSProcessd.get(lnRow).getQtyOnHnd().doubleValue()
                                                        - poRSMaster.get(lnCtr).getQtyOutxx().doubleValue());
//                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx()
//                                                        + poRSMaster.get(lnCtr).getQuantity());
                    break;
                case InvConstants.PURCHASE_REPLACEMENT:
                    poRSProcessd.get(lnRow).setQtyInxxx(poRSProcessd.get(lnRow).getQtyInxxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    break;
                case InvConstants.WHOLESALE:
                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    break;
                case InvConstants.WHOLESALE_RETURN:
                    poRSProcessd.get(lnRow).setQtyInxxx(poRSProcessd.get(lnRow).getQtyInxxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    break;
                case InvConstants.WHOLESALE_REPLACAMENT:
                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    break;
                case InvConstants.SALES:
                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                   
                    /*if (!poRSMaster.get(lnCtr).getReplacID().equals("")){
                        poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx()
                                                            + poRSMaster.get(lnCtr).getQuantity());
                    }*/
                    break;
                case InvConstants.SALES_RETURN:
                    poRSProcessd.get(lnRow).setQtyInxxx(poRSProcessd.get(lnRow).getQtyInxxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    break;
                case InvConstants.SALES_REPLACEMENT:
                case InvConstants.SALES_GIVE_AWAY:
                case InvConstants.WARRANTY_RELEASE:
                case InvConstants.CREDIT_MEMO:
                    if (poRSProcessd.get(lnCtr).IsNewParts().equals("1")) poRSProcessd.get(lnRow).setQtyOnHnd(0);
                   
                    poRSProcessd.get(lnRow).setQtyInxxx(poRSProcessd.get(lnRow).getQtyInxxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());

                    poRSProcessd.get(lnRow).setBegInventory(pdTransact);
                    poRSProcessd.get(lnRow).setDateExpiry(poRSMaster.get(lnCtr).getDateExpiry());
                    poRSProcessd.get(lnRow).setQuantity(poRSProcessd.get(lnRow).getQtyOnHnd().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQtyInxxx().doubleValue());
                    break;
                case InvConstants.WASTE_INV:
//                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx()
//                                                        + poRSMaster.get(lnCtr).getQuantity());
//                    break;
                    
                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());

                    poRSProcessd.get(lnRow).setBegInventory(pdTransact);
                    poRSProcessd.get(lnRow).setDateExpiry(poRSMaster.get(lnCtr).getDateExpiry());
                    poRSProcessd.get(lnRow).setQuantity(poRSProcessd.get(lnRow).getQtyOnHnd().doubleValue()
                                                        - poRSMaster.get(lnCtr).getQtyOutxx().doubleValue());
                    break;
                case InvConstants.DEBIT_MEMO:
                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());

                    poRSProcessd.get(lnRow).setBegInventory(pdTransact);
                    poRSProcessd.get(lnRow).setDateExpiry(poRSMaster.get(lnCtr).getDateExpiry());
                    poRSProcessd.get(lnRow).setQuantity(poRSProcessd.get(lnRow).getQtyOnHnd().doubleValue()
                                                        - poRSMaster.get(lnCtr).getQtyOutxx().doubleValue());
                    break;
                case InvConstants.DAILY_PRODUCTION_IN:
//                    if (!CommonUtils.xsDateShort(poRSMaster.get(lnCtr).getDateExpiry()).equals(pdTransact.toString())){
                        if (poRSProcessd.get(lnRow).IsNewParts().equals("1")) poRSProcessd.get(lnRow).setQtyOnHnd(0);
//                    }
                   
                    poRSProcessd.get(lnRow).setQtyInxxx(poRSProcessd.get(lnRow).getQtyInxxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());

                    poRSProcessd.get(lnRow).setBegInventory(pdTransact);
                    poRSProcessd.get(lnRow).setDateExpiry(poRSMaster.get(lnCtr).getDateExpiry());
                    poRSProcessd.get(lnRow).setQuantity(poRSProcessd.get(lnRow).getQtyOnHnd().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQtyInxxx().doubleValue());
//                    if (poRSMaster.get(lnCtr).getReplacID().equals("")){
//                        poRSProcessd.get(lnRow).setQtyOrder(poRSProcessd.get(lnRow).getQtyOrder() 
//                                                            - poRSMaster.get(lnCtr).getQuantity());
//                    }
                    break;
                 case InvConstants.DAILY_PRODUCTION_OUT:
                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());

                    poRSProcessd.get(lnRow).setBegInventory(pdTransact);
                    poRSProcessd.get(lnRow).setDateExpiry(poRSMaster.get(lnCtr).getDateExpiry());
                    poRSProcessd.get(lnRow).setQuantity(poRSProcessd.get(lnRow).getQtyOnHnd().doubleValue()
                                                        - poRSMaster.get(lnCtr).getQtyOutxx().doubleValue());
                    break;
            }
            
//            if (!poRSMaster.get(lnCtr).getReplacID().equals("")){
//                lnRow = findOnProcInventory("sStockIDx", poRSMaster.get(lnCtr).getReplacID());
//                
//                if (lnRow == -1){
//                    lsSQL = "SELECT" +
//                                "  a.nQtyOnHnd" + 
//                                ", a.nBackOrdr" + 
//                                ", a.nResvOrdr" + 
//                                ", a.nFloatQty" + 
//                                ", a.nLedgerNo" + 
//                                ", a.dBegInvxx" + 
//                                ", a.dAcquired" + 
//                                ", a.nBegQtyxx" + 
//                                ", a.cRecdStat" + 
//                                ", IFNULL(c.nQtyOnHnd, 0) xQtyOnHnd" +
//                                ", b.dTransact dLastTran" +
//                                ", a.dExpiryDt" +
//                            " FROM Inv_Master a" +
//                                " LEFT JOIN Inv_Ledger b" + 
//                                    " ON a.sBranchCd = b.sBranchCd" +
//                                        " AND a.sStockIDx = b.sStockIDx" +
//                                        " AND a.nLedgerNo = b.nLedgerNo" +
//                                " LEFT JOIN Inv_Ledger c" +
//                                    " ON a.sBranchCd = c.sBranchCd" +
//                                        " AND a.sStockIDx = c.sStockIDx" +
//                                        " AND c.dTransact <= " + SQLUtil.toSQL(pdTransact) + 
//                            " WHERE a.sStockIDx = " + SQLUtil.toSQL(poRSMaster.get(lnCtr).getReplacID()) + 
//                                " AND a.sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
//                            " ORDER BY c.dTransact DESC" +
//                                ", c.nLedgerNo DESC" +
//                            " LIMIT 1";
//
//                    loRS = poGRider.executeQuery(lsSQL);
//
//                    poRSProcessd.add(new UnitInventoryTrans());
//                    lnRow = poRSProcessd.size()-1;
//                    
//                    if (MiscUtil.RecordCount(loRS) == 0){
//                        poRSProcessd.get(lnRow).IsNewParts("1");
//                        poRSProcessd.get(lnRow).setQtyOnHnd(0);
//                        poRSProcessd.get(lnRow).setLedgerNo(0);
//                        poRSProcessd.get(lnRow).setBackOrdr(0);
//                        poRSProcessd.get(lnRow).setResvOrdr(0);
//                        poRSProcessd.get(lnRow).setFloatQty(0);
//                        poRSProcessd.get(lnRow).setDateLastTran(pdTransact);
//                        poRSProcessd.get(lnRow).setRecdStat(RecordStatus.ACTIVE);
//                    } else {
//                        try {
//                            loRS.first();
//                            poRSProcessd.get(lnRow).IsNewParts("0");
//                            poRSProcessd.get(lnRow).setQtyOnHnd(loRS.getInt("nQtyOnHnd"));
//                            poRSProcessd.get(lnRow).setLedgerNo(loRS.getInt("nLedgerNo"));
//                            poRSProcessd.get(lnRow).setBackOrdr(loRS.getInt("nBackOrdr"));
//                            poRSProcessd.get(lnRow).setResvOrdr(loRS.getInt("nResvOrdr"));
//                            poRSProcessd.get(lnRow).setFloatQty(loRS.getInt("nFloatQty"));
//                            poRSProcessd.get(lnRow).setRecdStat(loRS.getString("cRecdStat"));
//
//                            if (loRS.getDate("dAcquired") != null) poRSProcessd.get(lnRow).setDateAcquired(loRS.getDate("dAcquired"));
//                            if (loRS.getDate("dLastTran") != null){
//                                long diffInMillies = Math.abs(pdTransact.getTime() - loRS.getDate("dLastTran").getTime());
//                                long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
//
//                                if (diffInDays > 0){
//                                    if (loRS.getInt("nLedgerNo") == 0){
//                                        poRSProcessd.get(lnRow).setLedgerNo(1);
//                                        poRSProcessd.get(lnRow).setQtyOnHnd(loRS.getInt("nBegQtyxx"));
//                                    }else {
//                                        poRSProcessd.get(lnRow).setLedgerNo(loRS.getInt("nLedgerNo"));
//                                        poRSProcessd.get(lnRow).setQtyOnHnd(loRS.getInt("xQtyOnHnd"));
//                                    }
//                                    poRSProcessd.get(lnRow).setDateLastTran(loRS.getDate("dLastTran"));
//                                }else{
//                                    poRSProcessd.get(lnRow).setDateLastTran(new SimpleDateFormat("yyyy-MM-dd").parse(pxeLastTran));
//                                }
//                            }
//                        } catch (SQLException | ParseException e) {
//                            setErrMsg(e.getMessage());
//                            return false;
//                        } 
//                    }
//
//                    poRSProcessd.get(lnRow).setStockIDx(poRSMaster.get(lnCtr).getReplacID());
//                    poRSProcessd.get(lnRow).setQtyInxxx(0);
//                    poRSProcessd.get(lnRow).setQtyOutxx(0);
//                    poRSProcessd.get(lnRow).setQtyIssue(0);
//                    poRSProcessd.get(lnRow).setQtyOrder(0);
//                }
//                
//                switch (psSourceCd){
//                    case InvConstants.ACCEPT_DELIVERY:
//                        if (!pbWarehous){
//                            poRSProcessd.get(lnRow).setQtyOrder(poRSProcessd.get(lnRow).getQtyOrder()
//                                                                - poRSMaster.get(lnCtr).getQuantity());
//                        }
//                        break;
//                    case InvConstants.DELIVERY:
//                    case InvConstants.JOB_ORDER:
//                        poRSProcessd.get(lnRow).setQtyIssue(poRSProcessd.get(lnRow).getQtyIssue()
//                                                            + poRSMaster.get(lnCtr).getQuantity());
//                        break;
//                    case InvConstants.PURCHASE_RECEIVING:
//                        poRSProcessd.get(lnRow).setQtyOrder(poRSProcessd.get(lnRow).getQtyOrder()
//                                                            - poRSMaster.get(lnCtr).getQuantity());
//                        break;
//                    case InvConstants.SALES:
//                        poRSProcessd.get(lnRow).setQtyIssue(poRSProcessd.get(lnRow).getQtyIssue()
//                                                            + poRSMaster.get(lnCtr).getResvOrdr());
//                        break;
//                }
//            }
        }
        return true;
    }
    
    private int findOnProcInventory(String fsIndex, Object fsValue){
        if (poRSProcessd.isEmpty()) return -1;
        
        for (int lnCtr=0; lnCtr <= poRSProcessd.size()-1; lnCtr++){
            if (poRSProcessd.get(lnCtr).getValue(fsIndex).equals(fsValue)) 
                return lnCtr;
        }
        return -1;
    }
    
    private boolean delDetail(){
        String lsMasSQL;
        String lsLgrSQL;
        
        try {
            for (int lnCtr = 1; lnCtr <= MiscUtil.RecordCount(poRSDetail); lnCtr++){
                poRSDetail.absolute(lnCtr);
                lsMasSQL = "UPDATE Inv_Master SET" +
                                "  nQtyOnHnd = nQtyOnHnd + " + (poRSDetail.getInt("nQtyOutxx") -  poRSDetail.getInt("nQtyInxxx")) +
                                ", nBackOrdr = nBackOrdr - " + poRSDetail.getInt("nQtyOrder") +
                                ", nResvOrdr = nResvOrdr + " + poRSDetail.getInt("nQtyIssue") +
                                ", nLedgerNo = " + (poRSDetail.getInt("nLedgerNo") - 1) +
                                ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) +
                            " WHERE sStockIDx = " + SQLUtil.toSQL(poRSDetail.getString("sStockIDx")) +
                                " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);
                
                lsLgrSQL = "DELETE FROM Inv_Ledger" +
                            " WHERE sStockIDx = " + SQLUtil.toSQL(poRSDetail.getString("sStockIDx")) +
                               " AND sSourceCd = " + SQLUtil.toSQL(psSourceCd) +
                               " AND sSourceNo = " + SQLUtil.toSQL(psSourceNo) + 
                                " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);
                
                if (poGRider.executeQuery(lsMasSQL, "Inv_Master", psBranchCd, "") <= 0){
                    setErrMsg(poGRider.getErrMsg() + "\n" + poGRider.getMessage());
                    return false;
                }
                
                if (poGRider.executeQuery(lsLgrSQL, "Inv_Ledger", psBranchCd, "") <= 0){
                    setErrMsg(poGRider.getErrMsg() + "\n" + poGRider.getMessage());
                    return false;
                }
                
                //TODO: re align on hand
            }
        } catch (SQLException ex) {
            setMessage("Please inform MIS Deparment.");
            setErrMsg(ex.getMessage());
            return false;
        }

        return true;
    }
    
    private boolean saveDetail(){
        Number lnQtyOnHnd;
        Number lnBegQtyxx;
        int lnRow;
        boolean lbNewInvxx = false;
        String lsMasSQL, lsLgrSQL;
        
        for (int lnCtr = 0; lnCtr <= poRSProcessd.size()-1; lnCtr++){
            if (psSourceCd.equals(InvConstants.ACCEPT_DELIVERY) ||
                psSourceCd.equals(InvConstants.ACCEPT_WARRANTY_TRANSFER) ||
                psSourceCd.equals(InvConstants.BRANCH_ORDER) ||
                psSourceCd.equals(InvConstants.BRANCH_ORDER_CONFIRM) ||
                psSourceCd.equals(InvConstants.CUSTOMER_ORDER) ||
                psSourceCd.equals(InvConstants.PURCHASE) ||
                psSourceCd.equals(InvConstants.PURCHASE_RECEIVING)||
                psSourceCd.equals(InvConstants.PURCHASE_RETURN)||
                psSourceCd.equals(InvConstants.CREDIT_MEMO)||
                psSourceCd.equals(InvConstants.DEBIT_MEMO)||
                psSourceCd.equals(InvConstants.DAILY_PRODUCTION_IN) ||
                psSourceCd.equals(InvConstants.DAILY_PRODUCTION_OUT) ||
                psSourceCd.equals(InvConstants.DELIVERY) ||
                psSourceCd.equals(InvConstants.WASTE_INV)){
                
                lbNewInvxx = poRSProcessd.get(lnCtr).IsNewParts().equals("1");
            }
            
            lsMasSQL = "";
            lsLgrSQL = "";
            
            if (lbNewInvxx){
                lnQtyOnHnd = 0;
                
                if (poRSProcessd.get(lnCtr).getDateExpiry()==null) {
                    lsMasSQL = "INSERT INTO Inv_Master_Expiration SET" +
                                "  sStockIDx = " + SQLUtil.toSQL(poRSProcessd.get(lnCtr).getStockID()) +
                                ", sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                                ", dExpiryDt = " + SQLUtil.toSQL(pdTransact) +
                                ", nQtyOnHnd = " + poRSProcessd.get(lnCtr).getQuantity();
                }else{
                     String lsSQL = "SELECT nQtyOnHnd" + 
                                    " FROM Inv_Master_Expiration" + 
                                    " WHERE sBranchCd = " + SQLUtil.toSQL(psBranchCd) + 
                                        " AND sStockIDx = " + SQLUtil.toSQL(poRSProcessd.get(lnCtr).getStockID()) + 
                                        " AND dExpiryDt = " + SQLUtil.toSQL(CommonUtils.xsDateShort(poRSProcessd.get(lnCtr).getDateExpiry()));
                                    
                    ResultSet loRS = poGRider.executeQuery(lsSQL);
                    
                    if (MiscUtil.RecordCount(loRS) == 0){
                        lsMasSQL = "INSERT INTO Inv_Master_Expiration SET" +
                                "  sStockIDx = " + SQLUtil.toSQL(poRSProcessd.get(lnCtr).getStockID()) +
                                ", sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                                ", dExpiryDt = " + SQLUtil.toSQL(poRSProcessd.get(lnCtr).getDateExpiry()) +
                                ", nQtyOnHnd = " + poRSProcessd.get(lnCtr).getQuantity();
                    }else{
                        lnQtyOnHnd = poRSProcessd.get(lnCtr).getQtyOnHnd().doubleValue() + poRSProcessd.get(lnCtr).getQtyInxxx().doubleValue() - poRSProcessd.get(lnCtr).getQtyOutxx().doubleValue(); 
                        lsMasSQL = "UPDATE Inv_Master_Expiration SET" + 
                                        "  nQtyOnHnd =  " + poRSProcessd.get(lnCtr).getQuantity() +
                                    " WHERE sStockIDx = " + SQLUtil.toSQL(poRSProcessd.get(lnCtr).getStockID()) + 
                                        " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                                        " AND dExpiryDt = " + SQLUtil.toSQL(CommonUtils.xsDateShort(poRSProcessd.get(lnCtr).getDateExpiry()));
                    }
                }
                
            } else {
                lnQtyOnHnd = poRSProcessd.get(lnCtr).getQtyOnHnd().doubleValue() + poRSProcessd.get(lnCtr).getQtyInxxx().doubleValue() - poRSProcessd.get(lnCtr).getQtyOutxx().doubleValue(); 
                lsMasSQL = "UPDATE Inv_Master_Expiration SET" + 
                                "  nQtyOnHnd =  " + poRSProcessd.get(lnCtr).getQuantity() +
                            " WHERE sStockIDx = " + SQLUtil.toSQL(poRSProcessd.get(lnCtr).getStockID()) + 
                                " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                                " AND dExpiryDt = " + SQLUtil.toSQL(CommonUtils.xsDateShort(poRSProcessd.get(lnCtr).getDateExpiry()));
            }    
                
            if (poGRider.executeQuery(lsMasSQL, "Inv_Master_Expiration", psBranchCd, "") <= 0){
                setErrMsg(poGRider.getErrMsg() + "\n" + poGRider.getMessage());
                return false;
            }

            //TODO:
            //realign on hand   
        }

        return true;
    }
    
    private Date getBegInv(String fsStockIDx){
        String lsSQL = "SELECT dTransact" + 
                        " FROM Inv_Ledger" + 
                        " WHERE sBranchCd = " + SQLUtil.toSQL(psBranchCd) + 
                            " AND sStockIDx = " + SQLUtil.toSQL(fsStockIDx) + 
                        " ORDER BY dTransact" + 
                        " LIMIT 1";
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        if (MiscUtil.RecordCount(loRS) == 1){
            try {
                loRS.first();
                return loRS.getDate("dTransact");
            } catch (SQLException e) {
                setErrMsg(e.getMessage());
                return null;
            }
        }
        return pdTransact;
    }
    
    private Date getAcquisition(String fsStockIDx, Date fdBegInvxx){
        if (fdBegInvxx == null)
            return pdTransact;
        else{
            String lsSQL = "SELECT dTransact" + 
                            " FROM Inv_Ledger" + 
                            " WHERE sBranchCd = " + SQLUtil.toSQL(psBranchCd) + 
                                " AND sStockIDx = " + SQLUtil.toSQL(fsStockIDx) + 
                                " AND nQtyInxxx + nQtyOutxx > 0" + 
                            " ORDER BY dTransact" + 
                            " LIMIT 1";
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) == 1){
                try {
                    loRS.first();
                    return loRS.getDate("dTransact");
                } catch (SQLException e) {
                    setErrMsg(e.getMessage());
                    return null;
                }   
            }
        }        
        
        return pdTransact;
    }
    
    private boolean deleteTransaction(){
        return delDetail();
    }
    
    private boolean loadTransaction(){
        String lsSQL = "SELECT" + 
                            "  a.sStockIDx" + 
                            ", a.nLedgerNo" +
                            ", a.dTransact" +
                            ", a.nQtyInxxx" + 
                            ", a.nQtyOutxx" + 
                            ", a.nQtyOrder" + 
                            ", a.nQtyIssue" + 
                            ", a.nQtyOnHnd" + 
                            ", b.nBackOrdr" + 
                            ", b.nResvOrdr" + 
                            ", b.nLedgerNo xLedgerNo" + 
                        " FROM Inv_Master_Expiration" +  
                        " WHERE a.sStockIDx = b.sStockIDx" + 
                            " AND a.sBranchCd = b.sBranchCd";
                    
        if (pnEditMode == EditMode.ADDNEW){
            lsSQL = lsSQL + " AND 0=1";
        } else {
            lsSQL = lsSQL + 
                        " AND a.sBranchCd = " + SQLUtil.toSQL(psBranchCd) + 
                        " AND a.sSourceCd = " + SQLUtil.toSQL(psSourceCd) +
                        " AND a.sSourceNo = " + SQLUtil.toSQL(psSourceNo) + 
                    " ORDER BY a.sStockIDx";
        }
        
        poRSDetail = poGRider.executeQuery(lsSQL);
        return true;
    }
    
    public String getMessage() {
        return psWarnMsg;
    }

    public void setMessage(String fsMessage) {
        this.psWarnMsg = fsMessage;
    }

    public String getErrMsg() {
        return psErrMsgx;
    }

    public void setErrMsg(String fsErrMsg) {
        this.psErrMsgx = fsErrMsg;
    }
        
    //member variables
    private GRider poGRider;
    private String psBranchCd;
    private boolean pbWarehous;
    private boolean pbInitTran;
    
    private String psSourceCd;
    private String psSourceNo;
    private Date pdTransact;
    private int pnEditMode;
    
    private String psWarnMsg = "";
    private String psErrMsgx = "";
    
    private ArrayList<UnitInvMasterExpiration> poRSMaster;
    private ArrayList<UnitInvMasterExpiration> poRSProcessd;
    private ResultSet poRSDetail;
    
    private final String pxeLastTran = "2018-10-01";
    private final String pxeModuleName = "InvExpiration";
}