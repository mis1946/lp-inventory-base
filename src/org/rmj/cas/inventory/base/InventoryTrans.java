package org.rmj.cas.inventory.base;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.cas.inventory.constants.basefx.InvConstants;
import org.rmj.cas.inventory.pojo.UnitInventoryTrans;

/**
 * Inventory Transaction Ledger BASE
 * @author Michael Torres Cuison
 * @since 2018.10.12
 */

public class InventoryTrans {
    public InventoryTrans(GRider foGRider, String fsBranchCD){
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
                poRSMaster.get(fnRow).setStockIDx((String) fsValue); break;
            case "nquantity":
                poRSMaster.get(fnRow).setQuantity(Double.valueOf(fsValue.toString())); break;
            case "nqtyonhnd":
                poRSMaster.get(fnRow).setQtyOnHnd(Double.valueOf(fsValue.toString())); break;
            case "nbackordr":
                poRSMaster.get(fnRow).setBackOrdr(Double.valueOf(fsValue.toString())); break;
            case "nresvordr":
                poRSMaster.get(fnRow).setResvOrdr(Double.valueOf(fsValue.toString())); break;
            case "nledgerno":
                poRSMaster.get(fnRow).setLedgerNo((int) fsValue); break;
            case "sreplacid":
                poRSMaster.get(fnRow).setReplacID((String) fsValue); break;
            case "nqtyorder":
                poRSMaster.get(fnRow).setQtyOrder(Double.valueOf(fsValue.toString())); break;
            case "dexpirydt":
                System.out.println("LEDGER2 :" + fsValue);
                poRSMaster.get(fnRow).setDateExpire((Date) fsValue); break;
            case "npurchase":
                poRSMaster.get(fnRow).setPurchase((double) fsValue); break;
        }
    }
    
    public boolean AcceptDelivery(String fsSourceNo,
                                    Date fdTransDate,
                                    int fnUpdateMode){
        psSourceCd = InvConstants.ACCEPT_DELIVERY;
        psSourceNo = fsSourceNo;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
    
        return saveTransaction();
    }
    
    public boolean Delivery(String fsSourceNo,
                            Date fdTransDate,
                            int fnUpdateMode){
        psSourceCd = InvConstants.DELIVERY;
        psSourceNo = fsSourceNo;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        
        return saveTransaction();
    }
    
    public boolean POReceiving(String fsSourceNo,
                                Date fdTransDate,
                                String fsSupplier,
                                int fnUpdateMode){        
        psSourceCd = InvConstants.PURCHASE_RECEIVING;
        psSourceNo = fsSourceNo;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        psClientID = fsSupplier;
        
        return saveTransaction();
    }
    
    public boolean POReturn(String fsSourceNo,
                                Date fdTransDate,
                                String fsSupplier,
                                int fnUpdateMode){        
        psSourceCd = InvConstants.PURCHASE_RETURN;
        psSourceNo = fsSourceNo;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        psClientID = fsSupplier;
        
        return saveTransaction();
    }
    
    public boolean CreditMemo(String fsSourceNo,
                                    Date fdTransDate,
                                    int fnUpdateMode){        
        psSourceCd = InvConstants.CREDIT_MEMO;
        psSourceNo = fsSourceNo;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        
        return saveTransaction();
    }
    
    public boolean DebitMemo(String fsSourceNo,
                                    Date fdTransDate,
                                    int fnUpdateMode){        
        psSourceCd = InvConstants.DEBIT_MEMO;
        psSourceNo = fsSourceNo;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        
        return saveTransaction();
    }
    
    public boolean DailyProduction_IN(String fsSourceNo,
                                    Date fdTransDate,
                                    int fnUpdateMode){        
        psSourceCd = InvConstants.DAILY_PRODUCTION_IN;
        psSourceNo = fsSourceNo;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        
        return saveTransaction();
    }
    
    public boolean DailyProduction_OUT(String fsSourceNo,
                                        Date fdTransDate,
                                        int fnUpdateMode){        
        psSourceCd = InvConstants.DAILY_PRODUCTION_OUT;
        psSourceNo = fsSourceNo;
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
    
    public boolean WasteInventory(String fsSourceNo,
                                    Date fdTransDate,
                                    int fnUpdateMode){        
        psSourceCd = InvConstants.WASTE_INV;
        psSourceNo = fsSourceNo;
        pdTransact = fdTransDate;
        pnEditMode = fnUpdateMode;
        
        return saveTransaction();
    }
    
    private boolean saveTransaction(){
        setMessage("");
        setErrMsg("");
        
        //check update mode
        if (!(pnEditMode == EditMode.ADDNEW ||
                pnEditMode == EditMode.DELETE)){
            setMessage("Invalid Update Mode Detected.");
            return false;
        }
        
        if (!loadTransaction()){
            setMessage("Unable to load transaction.");
            return false;
        } 
        
        if (pnEditMode == EditMode.DELETE) return deleteTransaction();
           
        if (!processInventory()) return false;
        
        return saveDetail();
    }
    
    private boolean addDetail(){
        poRSMaster.add(new UnitInventoryTrans());
        return true;
    }
    
    private boolean processInventory(){
        String lsSQL;
        ResultSet loRS;
        int lnRow;
        
        poRSProcessd = new ArrayList<>();
       
        for (int lnCtr = 0; lnCtr <= poRSMaster.size()-1; lnCtr++){
            lnRow = findOnProcInventory("sStockIDx", poRSMaster.get(lnCtr).getStockIDx());
            
            //-1 if no record found on filter
            if (lnRow == -1){
                lsSQL = "SELECT" +
                            "  a.nQtyOnHnd" + 
                            ", a.nBackOrdr" + 
                            ", a.nResvOrdr" + 
                            ", a.nFloatQty" + 
                            ", a.nLedgerNo" + 
                            ", a.dBegInvxx" + 
                            ", a.dAcquired" + 
                            ", a.nBegQtyxx" + 
                            ", a.cRecdStat" + 
                            ", c.nLedgerNo xLedgerNo" + 
                            ", IFNULL(c.nQtyOnHnd, 0) xQtyOnHnd" +
                            ", b.dTransact dLastTran" +
                            ", a.sStockIDx" +
                        " FROM Inv_Master a" +
                            " LEFT JOIN Inv_Ledger b" + 
                                " ON a.sBranchCd = b.sBranchCd" +
                                    " AND a.sStockIDx = b.sStockIDx" +
                                    " AND a.nLedgerNo = b.nLedgerNo" +
                            " LEFT JOIN Inv_Ledger c" +
                                " ON a.sBranchCd = c.sBranchCd" +
                                    " AND a.sStockIDx = c.sStockIDx" +
                                    " AND c.dTransact <= " + SQLUtil.toSQL(pdTransact) + 
                        " WHERE a.sStockIDx = " + SQLUtil.toSQL(poRSMaster.get(lnCtr).getStockIDx()) + 
                            " AND a.sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                        " ORDER BY c.dTransact DESC" +
                            ", c.nLedgerNo DESC" +
                        " LIMIT 1";
                
                loRS = poGRider.executeQuery(lsSQL);
                
                poRSProcessd.add(new UnitInventoryTrans());
                lnRow = poRSProcessd.size()-1;
                
                if (MiscUtil.RecordCount(loRS) == 0){
                    poRSProcessd.get(lnRow).IsNewParts("1");
                    poRSProcessd.get(lnRow).setQtyOnHnd(0);
                    poRSProcessd.get(lnRow).setLedgerNo(0);
                    poRSProcessd.get(lnRow).setBackOrdr(0);
                    poRSProcessd.get(lnRow).setResvOrdr(0);
                    poRSProcessd.get(lnRow).setFloatQty(0);
                    poRSProcessd.get(lnRow).setDateLastTran(pdTransact);
                    poRSProcessd.get(lnRow).setDateExpire(pdTransact);

                    poRSProcessd.get(lnRow).setRecdStat(RecordStatus.ACTIVE);
                } else {
                    try {
                        loRS.first();
                        poRSProcessd.get(lnRow).IsNewParts("0");
                        poRSProcessd.get(lnRow).setQtyOnHnd(loRS.getDouble("nQtyOnHnd"));
                        poRSProcessd.get(lnRow).setLedgerNo(loRS.getInt("nLedgerNo"));
                        poRSProcessd.get(lnRow).setBackOrdr(loRS.getDouble("nBackOrdr"));
                        poRSProcessd.get(lnRow).setResvOrdr(loRS.getDouble("nResvOrdr"));
                        poRSProcessd.get(lnRow).setFloatQty(loRS.getDouble("nFloatQty"));
                        poRSProcessd.get(lnRow).setRecdStat(loRS.getString("cRecdStat"));
                        
                        if (loRS.getDate("dAcquired") != null) poRSProcessd.get(lnRow).setDateAcquired(loRS.getDate("dAcquired"));
                        if (loRS.getDate("dLastTran") != null){
                            long diffInMillies = Math.abs(pdTransact.getTime() - loRS.getDate("dLastTran").getTime());
                            long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
                            
                            if (diffInDays > 0){
                                if (loRS.getInt("xLedgerNo") == 0){
                                    poRSProcessd.get(lnRow).setLedgerNo(1);
                                    poRSProcessd.get(lnRow).setQtyOnHnd(loRS.getDouble("nBegQtyxx"));
                                }else {
                                    poRSProcessd.get(lnRow).setLedgerNo(loRS.getInt("xLedgerNo"));
                                    poRSProcessd.get(lnRow).setQtyOnHnd(loRS.getDouble("xQtyOnHnd"));
                                }
                                poRSProcessd.get(lnRow).setDateLastTran(loRS.getDate("dLastTran"));
                            }else{
                                poRSProcessd.get(lnRow).setDateLastTran(new SimpleDateFormat("yyyy-MM-dd").parse(pxeLastTran));
                            }
                        }
                    } catch (SQLException | ParseException e) {
                        setErrMsg(e.getMessage());
                        return false;
                    } 
                }
                
                poRSProcessd.get(lnRow).setStockIDx(poRSMaster.get(lnCtr).getStockIDx());
                poRSProcessd.get(lnRow).setQtyInxxx(0);
                poRSProcessd.get(lnRow).setQtyOutxx(0);
                poRSProcessd.get(lnRow).setQtyIssue(0);
                poRSProcessd.get(lnRow).setQtyOrder(0);
            }
            
            switch (psSourceCd){
                case InvConstants.ACCEPT_DELIVERY:
                    poRSProcessd.get(lnRow).setQtyInxxx( poRSProcessd.get(lnRow).getQtyInxxx().doubleValue()
                                                        +  poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    if (pbWarehous){
                        if (poRSMaster.get(lnCtr).getReplacID().equals("")){
                            poRSProcessd.get(lnRow).setQtyOrder(poRSProcessd.get(lnRow).getQtyOrder().doubleValue()
                                                                - poRSMaster.get(lnCtr).getQuantity().doubleValue());
                        }
                    } 
                    break;
                case InvConstants.BRANCH_ORDER:
                    poRSProcessd.get(lnRow).setQtyOrder(poRSProcessd.get(lnRow).getQtyOrder().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    break;
                case InvConstants.BRANCH_ORDER_CONFIRM:
                case InvConstants.CUSTOMER_ORDER:
                case InvConstants.RETAIL_ORDER:
                    poRSProcessd.get(lnRow).setQtyIssue(poRSProcessd.get(lnRow).getQtyIssue().doubleValue()
                                                        - poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    break;
                case InvConstants.CANCEL_RETAIL_ORDER:
                case InvConstants.CANCEL_WHOLESALE_ORDER:
                    poRSProcessd.get(lnRow).setQtyIssue(poRSProcessd.get(lnRow).getQtyIssue().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    break;
                case InvConstants.DELIVERY:
//                    poRSProcessd.get(lnRow).setDateExpire(poRSMaster.get(lnCtr).getDateExpire());
                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    
                    if (poRSMaster.get(lnCtr).getReplacID().equals("")){
                        poRSProcessd.get(lnRow).setQtyIssue(poRSProcessd.get(lnRow).getQtyIssue().doubleValue()
                                                            + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    }
                    break;
                case InvConstants.JOB_ORDER:
                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    
                    if (poRSMaster.get(lnCtr).getReplacID().equals("")){
                        poRSProcessd.get(lnRow).setQtyIssue(poRSProcessd.get(lnRow).getQtyIssue().doubleValue()
                                                            + poRSMaster.get(lnCtr).getResvOrdr().doubleValue());
                    }        
                    break;
                case InvConstants.PURCHASE:
                    poRSProcessd.get(lnRow).setQtyOrder(poRSProcessd.get(lnRow).getQtyOrder().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQtyOrder().doubleValue());
                    poRSProcessd.get(lnRow).setQtyIssue(poRSProcessd.get(lnRow).getQtyIssue().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQtyIssue().doubleValue());
                    break;
                case InvConstants.PURCHASE_RECEIVING:
                    poRSProcessd.get(lnRow).setQtyInxxx(poRSProcessd.get(lnRow).getQtyInxxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
//                    poRSProcessd.get(lnRow).setDateExpire(poRSMaster.get(lnCtr).getDateExpire());
                    poRSProcessd.get(lnRow).setPurchase(poRSMaster.get(lnCtr).getPurchase());

                    if (poRSMaster.get(lnCtr).getReplacID().equals("")){
                        poRSProcessd.get(lnRow).setQtyOrder(poRSProcessd.get(lnRow).getQtyOrder().doubleValue()
                                                            - poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    }
                    break;
                case InvConstants.PURCHASE_RETURN:
                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
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
                case InvConstants.DEBIT_MEMO:
                    poRSProcessd.get(lnRow).setQtyOutxx(poRSMaster.get(lnCtr).getQuantity().doubleValue());
//                    poRSProcessd.get(lnRow).setDateExpire(pdTransact);
                    break;
                case InvConstants.WASTE_INV:
                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    break;
                case InvConstants.CREDIT_MEMO:
                    poRSProcessd.get(lnRow).setQtyInxxx(poRSMaster.get(lnCtr).getQuantity().doubleValue());
//                    poRSProcessd.get(lnRow).setDateExpire(pdTransact);
                    break;
                case InvConstants.DAILY_PRODUCTION_IN:
                    poRSProcessd.get(lnRow).setDateExpire(poRSMaster.get(lnCtr).getDateExpire());
                    poRSProcessd.get(lnRow).setQtyInxxx(poRSProcessd.get(lnRow).getQtyInxxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    break;
                case InvConstants.DAILY_PRODUCTION_OUT:
                    poRSProcessd.get(lnRow).setDateExpire(pdTransact);
                    poRSProcessd.get(lnRow).setQtyOutxx(poRSProcessd.get(lnRow).getQtyOutxx().doubleValue()
                                                        + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    
                    if (poRSMaster.get(lnCtr).getReplacID().equals("")){
                        poRSProcessd.get(lnRow).setQtyIssue(poRSProcessd.get(lnRow).getQtyIssue().doubleValue()
                                                            + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                    }
                    break;
            }
            
            if (!poRSMaster.get(lnCtr).getReplacID().equals("")){
                lnRow = findOnProcInventory("sStockIDx", poRSMaster.get(lnCtr).getReplacID());
                
                if (lnRow == -1){
                    lsSQL = "SELECT" +
                                "  a.nQtyOnHnd" + 
                                ", a.nBackOrdr" + 
                                ", a.nResvOrdr" + 
                                ", a.nFloatQty" + 
                                ", a.nLedgerNo" + 
                                ", a.dBegInvxx" + 
                                ", a.dAcquired" + 
                                ", a.nBegQtyxx" + 
                                ", a.cRecdStat" + 
                                ", IFNULL(c.nQtyOnHnd, 0) xQtyOnHnd" +
                                ", b.dTransact dLastTran" +
                            " FROM Inv_Master a" +
                                " LEFT JOIN Inv_Ledger b" + 
                                    " ON a.sBranchCd = b.sBranchCd" +
                                        " AND a.sStockIDx = b.sStockIDx" +
                                        " AND a.nLedgerNo = b.nLedgerNo" +
                                " LEFT JOIN Inv_Ledger c" +
                                    " ON a.sBranchCd = c.sBranchCd" +
                                        " AND a.sStockIDx = c.sStockIDx" +
                                        " AND c.dTransact <= " + SQLUtil.toSQL(pdTransact) + 
                            " WHERE a.sStockIDx = " + SQLUtil.toSQL(poRSMaster.get(lnCtr).getReplacID()) + 
                                " AND a.sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                            " ORDER BY c.dTransact DESC" +
                                ", c.nLedgerNo DESC" +
                            " LIMIT 1";

                    loRS = poGRider.executeQuery(lsSQL);

                    poRSProcessd.add(new UnitInventoryTrans());
                    lnRow = poRSProcessd.size()-1;
                    
                    if (MiscUtil.RecordCount(loRS) == 0){
                        poRSProcessd.get(lnRow).IsNewParts("1");
                        poRSProcessd.get(lnRow).setQtyOnHnd(0);
                        poRSProcessd.get(lnRow).setLedgerNo(0);
                        poRSProcessd.get(lnRow).setBackOrdr(0);
                        poRSProcessd.get(lnRow).setResvOrdr(0);
                        poRSProcessd.get(lnRow).setFloatQty(0);
                        poRSProcessd.get(lnRow).setDateLastTran(pdTransact);
                        poRSProcessd.get(lnRow).setRecdStat(RecordStatus.ACTIVE);
                    } else {
                        try {
                            loRS.first();
                            poRSProcessd.get(lnRow).IsNewParts("0");
                            poRSProcessd.get(lnRow).setQtyOnHnd(loRS.getDouble("nQtyOnHnd"));
                            poRSProcessd.get(lnRow).setLedgerNo(loRS.getInt("nLedgerNo"));
                            poRSProcessd.get(lnRow).setBackOrdr(loRS.getDouble("nBackOrdr"));
                            poRSProcessd.get(lnRow).setResvOrdr(loRS.getDouble("nResvOrdr"));
                            poRSProcessd.get(lnRow).setFloatQty(loRS.getDouble("nFloatQty"));
                            poRSProcessd.get(lnRow).setRecdStat(loRS.getString("cRecdStat"));

                            if (loRS.getDate("dAcquired") != null) poRSProcessd.get(lnRow).setDateAcquired(loRS.getDate("dAcquired"));
                            if (loRS.getDate("dLastTran") != null){
                                long diffInMillies = Math.abs(pdTransact.getTime() - loRS.getDate("dLastTran").getTime());
                                long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

                                if (diffInDays > 0){
                                    if (loRS.getInt("nLedgerNo") == 0){
                                        poRSProcessd.get(lnRow).setLedgerNo(1);
                                        poRSProcessd.get(lnRow).setQtyOnHnd(loRS.getDouble("nBegQtyxx"));
                                    }else {
                                        poRSProcessd.get(lnRow).setLedgerNo(loRS.getInt("nLedgerNo"));
                                        poRSProcessd.get(lnRow).setQtyOnHnd(loRS.getDouble("xQtyOnHnd"));
                                    }
                                    poRSProcessd.get(lnRow).setDateLastTran(loRS.getDate("dLastTran"));
                                }else{
                                    poRSProcessd.get(lnRow).setDateLastTran(new SimpleDateFormat("yyyy-MM-dd").parse(pxeLastTran));
                                }
                            }
                        } catch (SQLException | ParseException e) {
                            setErrMsg(e.getMessage());
                            return false;
                        } 
                    }

                    poRSProcessd.get(lnRow).setStockIDx(poRSMaster.get(lnCtr).getReplacID());
                    poRSProcessd.get(lnRow).setQtyInxxx(0);
                    poRSProcessd.get(lnRow).setQtyOutxx(0);
                    poRSProcessd.get(lnRow).setQtyIssue(0);
                    poRSProcessd.get(lnRow).setQtyOrder(0);
                }
                
                switch (psSourceCd){
                    case InvConstants.ACCEPT_DELIVERY:
                        if (!pbWarehous){
                            poRSProcessd.get(lnRow).setQtyOrder(poRSProcessd.get(lnRow).getQtyOrder().doubleValue()
                                                                - poRSMaster.get(lnCtr).getQuantity().doubleValue());
                        }
                        break;
                    case InvConstants.DELIVERY:
                    case InvConstants.JOB_ORDER:
                        poRSProcessd.get(lnRow).setQtyIssue(poRSProcessd.get(lnRow).getQtyIssue().doubleValue()
                                                            + poRSMaster.get(lnCtr).getQuantity().doubleValue());
                        break;
                    case InvConstants.PURCHASE_RECEIVING:
                        poRSProcessd.get(lnRow).setQtyOrder(poRSProcessd.get(lnRow).getQtyOrder().doubleValue()
                                                            - poRSMaster.get(lnCtr).getQuantity().doubleValue());
                        break;
                    case InvConstants.SALES:
                        poRSProcessd.get(lnRow).setQtyIssue(poRSProcessd.get(lnRow).getQtyIssue().doubleValue()
                                                            + poRSMaster.get(lnCtr).getResvOrdr().doubleValue());
                        break;
                }
            }
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
        Number lnQtyOnHnd, lnBackOrdr;
        Number lnBegQtyxx, lnResvOrdr;
        int lnLedgerNo, lnRow;
        boolean lbActivate = false, lbNewInvxx = false;
        String lsMasSQL, lsLgrSQL;
        Date ldAcquired;
        ResultSet loRS;
        
        for (int lnCtr = 0; lnCtr <= poRSProcessd.size()-1; lnCtr++){
            if (psSourceCd.equals(InvConstants.ACCEPT_DELIVERY) ||
                psSourceCd.equals(InvConstants.ACCEPT_WARRANTY_TRANSFER) ||
                psSourceCd.equals(InvConstants.BRANCH_ORDER) ||
                psSourceCd.equals(InvConstants.BRANCH_ORDER_CONFIRM) ||
                psSourceCd.equals(InvConstants.CUSTOMER_ORDER) ||
                psSourceCd.equals(InvConstants.PURCHASE) ||
                psSourceCd.equals(InvConstants.PURCHASE_RECEIVING) ||
                psSourceCd.equals(InvConstants.PURCHASE_RETURN) ||
                psSourceCd.equals(InvConstants.CREDIT_MEMO) ||
                psSourceCd.equals(InvConstants.DAILY_PRODUCTION_IN) ||
                psSourceCd.equals(InvConstants.DAILY_PRODUCTION_OUT) ||
                psSourceCd.equals(InvConstants.DELIVERY) ||
                psSourceCd.equals(InvConstants.DEBIT_MEMO)){
                
                lbNewInvxx = poRSProcessd.get(lnCtr).IsNewParts().equals("1");
                lbActivate = poRSProcessd.get(lnCtr).getRecdStat().equals(RecordStatus.INACTIVE);
            }
            
            lsMasSQL = "";
            lsLgrSQL = "";
            
            if (lbNewInvxx){
                lnQtyOnHnd = poRSProcessd.get(lnCtr).getQtyInxxx();
                lnBackOrdr = poRSProcessd.get(lnCtr).getQtyOrder();
                lnResvOrdr = Math.abs(poRSProcessd.get(lnCtr).getQtyIssue().doubleValue());
                lnLedgerNo = 1;

                lsMasSQL = "INSERT INTO Inv_Master SET" +
                                "  sStockIDx = " + SQLUtil.toSQL(poRSProcessd.get(lnCtr).getStockIDx()) +
                                ", sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                                ", sLocatnCd = " + SQLUtil.toSQL("") +
                                ", nBinNumbr = " + 0 +
                                ", nBegQtyxx = " + 0 +
                                ", nQtyOnHnd = " + poRSProcessd.get(lnCtr).getQtyInxxx() +
                                ", nMinLevel = " + 0 +
                                ", nMaxLevel = " + 0 +
                                ", nAvgMonSl = " + 0 +
                                ", nAvgCostx = " + 0.00 +
                                ", cClassify = " + SQLUtil.toSQL("F") +
                                ", nBackOrdr = " + lnBackOrdr +
                                ", nResvOrdr = " + lnResvOrdr +
                                ", nFloatQty = " + 0 +
                                ", nLedgerNo = " + lnLedgerNo +
                                ", dBegInvxx = " + SQLUtil.toSQL(poRSProcessd.get(lnCtr).getDateExpire()) +
                                ", cRecdStat = " + SQLUtil.toSQL("1") +
                                ", sModified = " + SQLUtil.toSQL(poGRider.getUserID()) +
                                ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate());
                
                if (poRSProcessd.get(lnCtr).getQtyInxxx().doubleValue() > 0.00)
                    lsMasSQL = lsMasSQL + ", dAcquired = " + SQLUtil.toSQL(pdTransact);
            } else {
                lnQtyOnHnd = poRSProcessd.get(lnCtr).getQtyOnHnd().doubleValue() + poRSProcessd.get(lnCtr).getQtyInxxx().doubleValue() - poRSProcessd.get(lnCtr).getQtyOutxx().doubleValue();
                lnBackOrdr = poRSProcessd.get(lnCtr).getBackOrdr().doubleValue() + poRSProcessd.get(lnCtr).getQtyOrder().doubleValue();
                lnResvOrdr = poRSProcessd.get(lnCtr).getResvOrdr().doubleValue() - poRSProcessd.get(lnCtr).getQtyIssue().doubleValue();
                lnLedgerNo = poRSProcessd.get(lnCtr).getLedgerNo() + 1;
                         
                /*if (lnQtyOnHnd < 0){
                    if (ShowMessageFX.YesNo("Transaction resulted to some part/s having negative inventory!", pxeModuleName, "Continue saving anyway?") == false){
                        setMessage("Update cancelled by the user.");
                        return false;
                    }
                }*/
                
                if (lnBackOrdr.doubleValue() < 0.00) 
                    lsMasSQL = lsMasSQL + ", nBackOrdr = 0";
                else 
                    lsMasSQL = lsMasSQL + ", nBackOrdr = nBackOrdr + " + poRSProcessd.get(lnCtr).getQtyOrder();
                
                if (lnResvOrdr.doubleValue() < 0.00) 
                    lsMasSQL = lsMasSQL + ", nResvOrdr = 0";
                else 
                    lsMasSQL = lsMasSQL + ", nResvOrdr = nResvOrdr + " + poRSProcessd.get(lnCtr).getQtyIssue();
                
                if (lbActivate)
                    lsMasSQL = lsMasSQL + ", cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE);
                
                if (poRSProcessd.get(lnCtr).getDateAcquired() == null){
                    if (poRSProcessd.get(lnCtr).getQtyInxxx().doubleValue() + poRSProcessd.get(lnCtr).getQtyOutxx().doubleValue() > 0){
                        ldAcquired = getAcquisition(poRSProcessd.get(lnCtr).getStockIDx(), poRSProcessd.get(lnCtr).getDateAcquired());
                        lsMasSQL = lsMasSQL + ", dAcquired = " + SQLUtil.toSQL(ldAcquired);
                    }   
                    
                    if (poRSProcessd.get(lnCtr).getDateBegInvxx() == null)
                        lsMasSQL = lsMasSQL + ", dBegInvxx = " + SQLUtil.toSQL(poRSProcessd.get(lnCtr).getDateBegInvxx());
                    else if(poRSProcessd.get(lnCtr).getDateBegInvxx() == SQLUtil.toDate("1900-01-01", "yyyy-MM-dd"))
                        lsMasSQL = lsMasSQL + ", dBegInvxx = " + SQLUtil.toSQL(poRSProcessd.get(lnCtr).getDateBegInvxx());
                }
                
                lsMasSQL = "UPDATE Inv_Master SET" + 
                                "  nQtyOnHnd = nQtyOnHnd + " + 
                                                (poRSProcessd.get(lnCtr).getQtyInxxx().doubleValue() - poRSProcessd.get(lnCtr).getQtyOutxx().doubleValue()) + 
                                lsMasSQL + 
                                ", nLedgerNo = " + lnLedgerNo + 
                                ", dLastTran = " + SQLUtil.toSQL(poRSProcessd.get(lnCtr).getDateExpire()) +
                                ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                            " WHERE sStockIDx = " + SQLUtil.toSQL(poRSProcessd.get(lnCtr).getStockIDx()) + 
                                " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd);
                System.out.println(lsMasSQL);
            }    
                
            lsLgrSQL = "INSERT INTO Inv_Ledger SET" +
                            "  sStockIDx = " + SQLUtil.toSQL(poRSProcessd.get(lnCtr).getStockIDx()) +
                            ", nLedgerNo = " + lnLedgerNo +
                            ", sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                            ", dTransact = " + SQLUtil.toSQL(pdTransact) +
                            ", sSourceCd = " + SQLUtil.toSQL(psSourceCd) +
                            ", sSourceNo = " + SQLUtil.toSQL(psSourceNo) +
                            ", nQtyInxxx = " + poRSProcessd.get(lnCtr).getQtyInxxx()+
                            ", nQtyOutxx = " + poRSProcessd.get(lnCtr).getQtyOutxx()+
                            ", nQtyIssue = " + poRSProcessd.get(lnCtr).getQtyIssue()+
                            ", nQtyOrder = " + poRSProcessd.get(lnCtr).getQtyOrder()+
                            ", nQtyOnHnd = " + lnQtyOnHnd +
                            ", sModified = " + SQLUtil.toSQL(poGRider.getUserID()) + 
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate());
            
            if (psSourceCd == InvConstants.PURCHASE_RECEIVING){
                lsLgrSQL = lsLgrSQL +
                            ", dExpiryxx = " + SQLUtil.toSQL(poRSMaster.get(lnCtr).getDateExpire()) +
                            ", nPurPrice = " + poRSMaster.get(lnCtr).getPurchase();
            }
                  
            if (poGRider.executeQuery(lsMasSQL, "Inv_Master", psBranchCd, "") <= 0){
                setErrMsg(poGRider.getErrMsg() + "\n" + poGRider.getMessage());
                return false;
            }

            if (poGRider.executeQuery(lsLgrSQL, "Inv_Ledger", psBranchCd, "") <= 0){
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
                        " FROM Inv_Ledger a" +  
                            ", Inv_Master b" + 
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
    private String psClientID;
    private Date pdTransact;
    private int pnEditMode;
    
    private String psWarnMsg = "";
    private String psErrMsgx = "";
    
    private ArrayList<UnitInventoryTrans> poRSMaster;
    private ArrayList<UnitInventoryTrans> poRSProcessd;
    private ResultSet poRSDetail;
    
    private final String pxeLastTran = "2018-10-01";
    private final String pxeModuleName = "InventoryTrans";
}