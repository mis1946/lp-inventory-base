package org.rmj.cas.inventory.base;

import com.mysql.jdbc.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.simple.JSONObject;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.cas.inventory.pojo.UnitInvMaster;
import org.rmj.cas.inventory.pojo.UnitInvSerial;
import org.rmj.cas.inventory.pojo.UnitInventory;
import org.rmj.cas.parameter.agent.XMBrand;
import org.rmj.cas.parameter.agent.XMCategory;
import org.rmj.cas.parameter.agent.XMCategoryLevel2;
import org.rmj.cas.parameter.agent.XMCategoryLevel3;
import org.rmj.cas.parameter.agent.XMCategoryLevel4;
import org.rmj.cas.parameter.agent.XMColor;
import org.rmj.cas.parameter.agent.XMInventoryType;
import org.rmj.cas.parameter.agent.XMMeasure;
import org.rmj.cas.parameter.agent.XMModel;
import org.rmj.cas.parameter.agent.XMInventoryLocation;

/**
 * Inventory Master BASE
 * @author Michael Torres Cuison
 * @since 2018.10.05
 */
public class InvMaster {
    public InvMaster(GRider foGRider, String fsBranchCD, boolean fbWithParent){
        this.poGRider = foGRider;
        
        if (foGRider != null){
            this.pbWithParent = fbWithParent;
            this.psBranchCd = fsBranchCD;
            
            this.psUserIDxx = foGRider.getUserID();
            pnEditMode = EditMode.UNKNOWN;
            
            poInventory = new Inventory(poGRider, psBranchCd, true);
        }
    }
    
    public boolean SearchSoldStock(String fsValue, String fsSerialID, boolean fbSearch, boolean fbByCode){
        String lsHeader = "Refer No.»Description»Unit»Model»Brand"; //On Hand»
        String lsColName = "xReferNox»sDescript»sMeasurNm»xModelNme»xBrandNme"; //nQtyOnHnd»       
        String lsSQL = "SELECT * FROM (" + getSQ_SoldStock() + ") a";
        
        JSONObject loJSON;
        ResultSet loRS;
        
        if (fbByCode){
            if (fsSerialID.equals(""))
                lsSQL = lsSQL + " WHERE a.sStockIDx = " + SQLUtil.toSQL(fsValue);
                //lsSQL = lsSQL.replace("xCondition", "a.sStockIDx = " + SQLUtil.toSQL(fsValue));
            else
                lsSQL = lsSQL + " WHERE a.sSerialID = " + SQLUtil.toSQL(fsSerialID);
               // lsSQL = lsSQL.replace("xCondition", "a.sSerialID = " + SQLUtil.toSQL(fsValue));
        } else {
            //lsSQL = lsSQL.replace("xCondition", "a.sDescript LIKE " + SQLUtil.toSQL(fsValue + "%"));
            lsSQL = lsSQL + " WHERE a.sDescript LIKE " + SQLUtil.toSQL(fsValue + "%");
        }    
        
        
        loRS = poGRider.executeQuery(lsSQL);
        long lnRow = MiscUtil.RecordCount(loRS); 
        
        if (lnRow == 0){
            pnEditMode = EditMode.UNKNOWN;
            return false;
        } else if (lnRow == 1){
            loJSON = CommonUtils.loadJSON(loRS);
        } else {
            loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
        }
        
        if (loJSON != null){
            psStockIDx = (String) loJSON.get("sStockIDx");
            psBarCodex = (String) loJSON.get("sBarCodex");
            psDescript = (String) loJSON.get("sDescript");
            
            if ("1".equals((String) loJSON.get("cSerialze"))){
                psSerialID = (String) loJSON.get("sSerialID");
                psSerial01 = (String) loJSON.get("xReferNox");
                psSerial02 = (String) loJSON.get("xReferNo1");
            }
        } else{
            psStockIDx = "";
            psBarCodex = "";
            psDescript = "";
            psSerialID = "";
            psSerial01 = "";
            psSerial02 = "";
            
            psWarnMsg = "No record found/selected. Please verify your entry.";
            psErrMsgx = "";
        }
                
        if (openInvRecord(psStockIDx)){
            poData = openRecord(psStockIDx);
            
            if (!psSerialID.trim().equals(""))
                poSerial = openSerial(psSerialID);
            
            if (poData == null && poSerial == null)
                return NewRecord();
            else return true;
        } else {
            pnEditMode = EditMode.UNKNOWN;
            return false;
        }
    }
    
    public boolean SearchStock(String fsValue, String fsSerialID, boolean fbSearch, boolean fbByCode){
        String lsHeader = "Refer No.»Description»Unit»Model»Brand"; //On Hand»
        String lsColName = "xReferNox»sDescript»sMeasurNm»xModelNme»xBrandNme"; //nQtyOnHnd»       
        String lsSQL = "SELECT * FROM (" + getSQ_AllStock() + ") a";
        
        JSONObject loJSON;
        ResultSet loRS;
        
        if (fbByCode){
            if (fsSerialID.equals(""))
                lsSQL = lsSQL + " WHERE a.sStockIDx = " + SQLUtil.toSQL(fsValue);
                //lsSQL = lsSQL.replace("xCondition", "a.sStockIDx = " + SQLUtil.toSQL(fsValue));
            else
                lsSQL = lsSQL + " WHERE a.sSerialID = " + SQLUtil.toSQL(fsSerialID);
               // lsSQL = lsSQL.replace("xCondition", "a.sSerialID = " + SQLUtil.toSQL(fsValue));
        } else {
            //lsSQL = lsSQL.replace("xCondition", "a.sDescript LIKE " + SQLUtil.toSQL(fsValue + "%"));
            lsSQL = lsSQL + " WHERE a.sDescript LIKE " + SQLUtil.toSQL(fsValue + "%");
        }    
        
        
        loRS = poGRider.executeQuery(lsSQL);
        long lnRow = MiscUtil.RecordCount(loRS); 
        
        if (lnRow == 0){
            pnEditMode = EditMode.UNKNOWN;
            return false;
        } else if (lnRow == 1){
            loJSON = CommonUtils.loadJSON(loRS);
        } else {
            loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
        }
        
        if (loJSON != null){
            psStockIDx = (String) loJSON.get("sStockIDx");
            psBarCodex = (String) loJSON.get("sBarCodex");
            psDescript = (String) loJSON.get("sDescript");
            
            if ("1".equals((String) loJSON.get("cSerialze"))){
                psSerialID = (String) loJSON.get("sSerialID");
                psSerial01 = (String) loJSON.get("xReferNox");
                psSerial02 = (String) loJSON.get("xReferNo1");
            }
        } else{
            psStockIDx = "";
            psBarCodex = "";
            psDescript = "";
            psSerialID = "";
            psSerial01 = "";
            psSerial02 = "";
            
            psWarnMsg = "No record found/selected. Please verify your entry.";
            psErrMsgx = "";
        }
                
        if (openInvRecord(psStockIDx)){
            poData = openRecord(psStockIDx);
            
            if (!psSerialID.trim().equals(""))
                poSerial = openSerial(psSerialID);
            
            if (poData == null && poSerial == null)
                return NewRecord();
            else return true;
        } else {
            pnEditMode = EditMode.UNKNOWN;
            return false;
        }
    }
    
    /**
     * SearchStock(String fsCategCd1, String fsValue, boolean fbSearch, boolean fbByCode)
     * 
     * @param fsCategCd1 category level 1
     * @param fsValue   value to search
     * @param fbSearch  search or browse
     * @param fbByCode by code or description
     * @return boolean
     */
    public boolean SearchStockByCategory(String fsCategCd1, String fsValue, boolean fbSearch, boolean fbByCode){
        String lsHeader = "Brand»Description»Unit»Model»On Hand»Inv. Type»Barcode»Stock ID";
        String lsColName = "xBrandNme»sDescript»sMeasurNm»xModelNme»nQtyOnHnd»xInvTypNm»sBarCodex»sStockIDx";
        String lsColCrit = "b.sDescript»a.sDescript»e.sMeasurNm»c.sDescript»f.nQtyOnHndd.sDescript»a.sBarCodex»a.sStockIDx";
        String lsSQL = MiscUtil.addCondition(getSQ_Stock(), "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE) +
                                                            " AND f.nQtyOnHnd > 0");
        
        if (!fsCategCd1.equals(""))
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sCategCd1 = " + SQLUtil.toSQL(fsCategCd1));
            
        JSONObject loJSON;
        
        if (fbByCode){
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sStockIDx = " + SQLUtil.toSQL(fsValue));
        
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) < 1) {
                pnEditMode = EditMode.UNKNOWN;
                return false;
            }
            
            loJSON = CommonUtils.loadJSON(loRS);
            //loJSON = showFXDialog.jsonBrowse(poGRider, loRS, lsHeader, lsColName);
        } else {
            loJSON = showFXDialog.jsonSearch(poGRider, 
                                                lsSQL, 
                                                fsValue, 
                                                lsHeader, 
                                                lsColName, 
                                                lsColCrit, 
                                                fbSearch ? 1 : 5);
        }    
        
        if (loJSON != null){
            psStockIDx = (String) loJSON.get("sStockIDx");
            psBarCodex = (String) loJSON.get("sBarCodex");
            psDescript = (String) loJSON.get("sDescript");
        } else{
            psStockIDx = "";
            psBarCodex = "";
            psDescript = "";
            
            psWarnMsg = "No record found/selected. Please verify your entry.";
            psErrMsgx = "";
        }
                
        if (openInvRecord(psStockIDx)){
            poData = openRecord(psStockIDx);
            if (poData == null)
                return NewRecord();
            else return true;
        } else {
            pnEditMode = EditMode.UNKNOWN;
            return false;
        }
    }
    
    public boolean SearchInventory(String fsValue, boolean fbSearch, boolean fbByCode){
        String lsHeader = "Brand»Description»Unit»Model»Inv. Type»Barcode»Stock ID";
        String lsColName = "xBrandNme»sDescript»sMeasurNm»xModelNme»xInvTypNm»sBarCodex»sStockIDx";
        String lsColCrit = "b.sDescript»a.sDescript»e.sMeasurNm»c.sDescript»d.sDescript»a.sBarCodex»a.sStockIDx";
        String lsSQL = MiscUtil.addCondition(getSQ_Inventory(), "a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE));
        JSONObject loJSON;
        
        if (fbByCode){
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sStockIDx = " + SQLUtil.toSQL(fsValue));
        
            ResultSet loRS = poGRider.executeQuery(lsSQL);
            
            loJSON = CommonUtils.loadJSON(loRS);
        } else {
            loJSON = showFXDialog.jsonSearch(poGRider, 
                                                lsSQL, 
                                                fsValue, 
                                                lsHeader, 
                                                lsColName, 
                                                lsColCrit, 
                                                fbSearch ? 1 : 5);
        }    
        
        if (loJSON != null){
            psStockIDx = (String) loJSON.get("sStockIDx");
            psBarCodex = (String) loJSON.get("sBarCodex");
            psDescript = (String) loJSON.get("sDescript");
        } else{
            psStockIDx = "";
            psBarCodex = "";
            psDescript = "";
        }
                
        if (openInvRecord(psStockIDx)){
            poData = openRecord(psStockIDx);
            if (poData == null)
                return NewRecord();
            else return true;
        } else {
            pnEditMode = EditMode.UNKNOWN;
            return false;
        }
    }
    
    public ResultSet GetHistory(){
        String lsSQL = "SELECT" +
                                "  a.dTransact" +
                                ", b.sDescript" +
                                ", a.sSourceNo" +
                                ", a.nQtyInxxx" +
                                ", a.nQtyOutxx" +
                                ", a.nQtyOnHnd" +
                        " FROM Inv_Ledger a" +
                                " LEFT JOIN xxxSource_Transaction b" +
                                        " ON a.sSourceCd = b.sSourceCd" +
                        " WHERE a.sBranchCd = " + SQLUtil.toSQL(poData.getBranchCd()) + 
                                " AND a.sStockIDx = " + SQLUtil.toSQL(poData.getStockIDx()) + 
                        " ORDER BY a.dTransact ASC, a.nLedgerNo ASC";

        ResultSet loRS = poGRider.executeQuery(lsSQL);
        System.err.println(lsSQL);
        
        return loRS;
    }
    
    private UnitInvSerial openSerial(String fsSerialID){
        UnitInvSerial loData = null;
        
        Connection loConn = null;
        loConn = setConnection();   
        
        String lsSQL = MiscUtil.addCondition(getSQ_Serial(), "sSerialID = " + SQLUtil.toSQL(fsSerialID));
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        try {
            if (!loRS.next()){
                setMessage("No Record Found");
            }else{
                loData = new UnitInvSerial();
                for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                    loData.setValue(lnCol, loRS.getObject(lnCol));
                }
            }              
        } catch (SQLException ex) {
            setErrMsg(ex.getMessage());
            return null;
        } finally{
            MiscUtil.close(loRS);
            if (!pbWithParent) MiscUtil.close(loConn);
        }
        
        pnEditMode = EditMode.READY;
        return loData;
    }
    
    private UnitInvMaster openRecord(String fsValue) {
        UnitInvMaster loData = null;
        
        Connection loConn = null;
        loConn = setConnection();   
        
        String lsSQL = MiscUtil.addCondition(getSQ_Master(), 
                            "sStockIDx = " + SQLUtil.toSQL(fsValue) + 
                                " AND sBranchCd = " + SQLUtil.toSQL(psBranchCd));
        
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        try {
            if (!loRS.next()){
                setMessage("No Record Found");
            }else{
                loData = new UnitInvMaster();
                for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                    loData.setValue(lnCol, loRS.getObject(lnCol));
                }
            }              
        } catch (SQLException ex) {
            setErrMsg(ex.getMessage());
            return null;
        } finally{
            MiscUtil.close(loRS);
            if (!pbWithParent) MiscUtil.close(loConn);
        }
        
        pnEditMode = EditMode.READY;
        return loData;
    }
    
    private boolean openInvRecord(String fsStockIDx){
        if (fsStockIDx.equals("")) return false;
        
        return poInventory.OpenRecord(fsStockIDx);
    }
    
    public boolean NewRecord(){
        Connection loConn = null;
        loConn = setConnection();       
        
        poData = new UnitInvMaster();
        poData.setAcquired(poGRider.getServerDate());
        poData.setBegInvxx(poGRider.getServerDate());

        poData.setStockIDx(psStockIDx);
        poData.setBranchCd(psBranchCd);
        
        pnEditMode = EditMode.ADDNEW;
        return true;
    }
    
    public boolean UpdateRecord() {
        if(pnEditMode != EditMode.READY) {
         return false;
      }
      else{
         pnEditMode = EditMode.UPDATE;
         return true;
      }
    }
    
    public boolean SaveRecord(){
        String lsSQL = "";
        UnitInvMaster loOldEnt = null;
        UnitInvMaster loNewEnt = null;
        UnitInvMaster loResult = null;
        
        loNewEnt = (UnitInvMaster) poData;

        // Test if entry is ok
        if (loNewEnt.getStockIDx() == null || loNewEnt.getStockIDx().isEmpty()){
            setMessage("Invalid stock id detected.");
            return false;
        }
        
        if (loNewEnt.getBranchCd() == null || loNewEnt.getBranchCd().isEmpty()){
            setMessage("Invalid branch detected.");
            return false;
        }
        
        loNewEnt.setModified(psUserIDxx);
        loNewEnt.setDateModified(poGRider.getServerDate());
        
        // Generate the SQL Statement
        if (pnEditMode == EditMode.ADDNEW){
            Connection loConn = null;
            loConn = setConnection();   
            
            if (!pbWithParent) MiscUtil.close(loConn);
            
            //Generate the SQL Statement
            lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt);
        }else{
            //Load previous transaction
            loOldEnt = openRecord(psStockIDx);
            
            //Generate the Update Statement
            lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt, 
                                    (GEntity) loOldEnt, 
                                    "sStockIDx = " + SQLUtil.toSQL(loNewEnt.getValue(1)) + 
                                        " AND sBranchCd = " + SQLUtil.toSQL(loNewEnt.getValue(2)));
        }
        
        //No changes have been made
        if (lsSQL.equals("")){
            setMessage("Record is not updated");
            return false;
        }
        
        if (!pbWithParent) poGRider.beginTrans();
        
        boolean lbUpdate = false;
        
        if(poGRider.executeQuery(lsSQL, loNewEnt.getTable(), "", "") == 0){
            if(!poGRider.getErrMsg().isEmpty())
                setErrMsg(poGRider.getErrMsg());
            else
            setMessage("No record updated");
        } else lbUpdate = true;
        
        if (!pbWithParent) {
            if (!getErrMsg().isEmpty()){
                poGRider.rollbackTrans();
            } else poGRider.commitTrans();
        }        
        
        return lbUpdate;
    }
    
    public Object getInventory(String fsCol){
        return getInventory(poDataInv.getColumn(fsCol));
    }
    public Object getInventory(int fnCol){
        return poInventory.getMaster(fnCol);
    }
    
    public Object getSerial(String fsCol){
        return poSerial.getValue(fsCol);
    }
    public Object getSerial(int fnCol){
        return getSerial(poSerial.getColumn(fnCol));
    }
    
    public String SearchInventory(int fnCol, String fsValue, boolean fbByCode){
        String lsHeader;
        String lsColName;
        String lsColCrit;
        String lsSQL;
        JSONObject loJSON;
        
        if (fsValue.equals("") && fbByCode) return "";
        
        switch(fnCol){
            case 6: //sCategCd1
                XMCategory loCategory = new XMCategory(poGRider, psBranchCd, true); 
                
                loJSON = loCategory.searchCategory(fsValue, fbByCode);
                
                if (loJSON == null)
                    return "";
                else
                    return (String) loJSON.get("sDescript");
            case 7: //sCategCd2
                XMCategoryLevel2 loCategory2 = new XMCategoryLevel2(poGRider, psBranchCd, true);
                
                loJSON = loCategory2.searchCategory(fsValue, fbByCode);
                
                if (loJSON == null)
                    return "";
                else
                    return (String) loJSON.get("sDescript");
            case 8: //sCategCd3
                XMCategoryLevel3 loCategory3 = new XMCategoryLevel3(poGRider, psBranchCd, true);
                
                loJSON = loCategory3.searchCategory(fsValue, fbByCode);
                
                if (loJSON == null)
                    return "";
                else
                    return (String) loJSON.get("sDescript");
            case 9: //sCategCd4
                XMCategoryLevel4 loCategory4 = new XMCategoryLevel4(poGRider, psBranchCd, true);
                
                loJSON = loCategory4.searchCategory(fsValue, fbByCode);
                
                if (loJSON == null)
                    return "";
                else
                    return (String) loJSON.get("sDescript");
            case 10: //sBrandCde
                XMBrand loBrand = new XMBrand(poGRider, psBranchCd, true);
                
                loJSON = loBrand.searchBrand(fsValue, fbByCode);
                
                if (loJSON == null)
                    return "";
                else
                    return (String) loJSON.get("sDescript");
            case 11: //sModelCde
                XMModel loModel = new XMModel(poGRider, psBranchCd, false);
                
                loJSON = loModel.searchModel(fsValue, fbByCode);
                
                if (loJSON == null)
                    return "";
                else
                    return (String) loJSON.get("sModelNme");
            case 12: //sColorCde
                XMColor loColor = new XMColor(poGRider, psBranchCd, false);
                
                loJSON = loColor.searchColor(fsValue, fbByCode);
                
                if (loJSON == null)
                    return "";
                else
                    return (String) loJSON.get("sDescript");
            case 13: //sInvTypCd
                XMInventoryType loInvType = new XMInventoryType(poGRider, psBranchCd, false);
                
                loJSON = loInvType.searchInvType(fsValue, fbByCode);
                
                if (loJSON == null)
                    return "";
                else
                    return (String) loJSON.get("sDescript");
            case 29: //sMeasurID
                XMMeasure loMeasure = new XMMeasure(poGRider, psBranchCd, false);
                
                loJSON = loMeasure.searchMeasure(fsValue, fbByCode);
                
                if (loJSON == null)
                    return "";
                else
                    return (String) loJSON.get("sMeasurNm");
            default:
                return "";
        }
    }
    
    public String SearchInventory(String fsCol, String fsValue, boolean fbByCode){
        return SearchInventory(poDataInv.getColumn(fsCol), fsValue, fbByCode);
    }
    
    public String SearchMaster(int fnCol, String fsValue, boolean fbByCode){
        String lsHeader = "";
        String lsColName = "";
        String lsColCrit = "";
        String lsSQL = "";
        JSONObject loJSON;
        
        if (fsValue.equals("") && fbByCode) return "";
        
        switch(fnCol){
            case 3: //sLocatnCd
                XMInventoryLocation loLocation = new XMInventoryLocation(poGRider, psBranchCd, false);
                
                loJSON = loLocation.searchLocation(fsValue, fbByCode);
                
                if (loJSON != null){
                    setMaster(fnCol, (String) loJSON.get("sLocatnCd"));
                    return (String) loJSON.get("sDescript");
                } else {
                    setMaster(fnCol, "");
                    return "";
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
            // Don't allow specific fields to assign values
            if(!(fnCol == poData.getColumn("sStockIDx") ||
                fnCol == poData.getColumn("cRecdStat") ||
                fnCol == poData.getColumn("sModified") ||
                fnCol == poData.getColumn("dModified"))){
                
                if (fnCol == poData.getColumn("nBegQtyxx") ||
                    fnCol == poData.getColumn("nQtyOnHnd") ||
                    fnCol == poData.getColumn("nMinLevel") ||
                    fnCol == poData.getColumn("nMaxLevel") ||
                    fnCol == poData.getColumn("nAvgMonSl") ||
                    fnCol == poData.getColumn("nBackOrdr") ||
                    fnCol == poData.getColumn("nResvOrdr") ||
                    fnCol == poData.getColumn("nFloatQty")){
                    if (foData instanceof Number){
                        poData.setValue(fnCol, foData);
                    }else poData.setValue(fnCol, 0);
                } else if (fnCol == poData.getColumn("nFloatQty")){
                    if (foData instanceof Number){
                        poData.setValue(fnCol, foData);
                    }else poData.setValue(fnCol, 0.00);
                } else poData.setValue(fnCol, foData);   
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
    
    public void setGRider(GRider foGRider){
        this.poGRider = foGRider;
        this.psUserIDxx = foGRider.getUserID();
        
        if (psBranchCd.isEmpty()) psBranchCd = foGRider.getBranchCode();
    }
    
    public void setUserID(String fsUserID){
        this.psUserIDxx  = fsUserID;
    }    
    
    public int getEditMode(){return pnEditMode;}
    
    public void printColumnsInvMaster(){poData.list();}
    public void printColumnsInventory(){poDataInv.list();}
    
    private Connection setConnection(){
        Connection foConn;
        
        if (pbWithParent){
            foConn = (Connection) poGRider.getConnection();
            if (foConn == null) foConn = (Connection) poGRider.doConnect();
        }else foConn = (Connection) poGRider.doConnect();
        
        return foConn;
    }
    
    private String getSQ_Inventory(){
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
                            ", e.sMeasurNm" + 
                            ", a.cWthExprt" + 
                        " FROM Inventory a" + 
                            " LEFT JOIN Brand b" + 
                                " ON a.sBrandCde = b.sBrandCde" + 
                            " LEFT JOIN Model c" + 
                                " ON a.sModelCde = c.sModelCde" + 
                            " LEFT JOIN Inv_Type d" + 
                                " ON a.sInvTypCd = d.sInvTypCd" +
                            " LEFT JOIN Measure e" + 
                                " ON e.sMeasurID = a.sMeasurID";
        
        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty())
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type")));
        
        return lsSQL;
    }
    
    private String getSQ_AllStock(){
        String lsSQL =  "SELECT" +
                            " a.sStockIDx," +
                            " a.sBarCodex xReferNox," +
                            " a.sDescript," +
                            " a.sBriefDsc," +
                            " a.sAltBarCd," +
                            " a.sCategCd1," +
                            " a.sCategCd2," +
                            " a.sCategCd3," +
                            " a.sCategCd4," +
                            " a.sBrandCde," +
                            " a.sModelCde," +
                            " a.sColorCde," +
                            " a.sInvTypCd," +
                            " a.nUnitPrce," +
                            " a.nSelPrice," +
                            " a.nDiscLev1," +
                            " a.nDiscLev2," +
                            " a.nDiscLev3," +
                            " a.nDealrDsc," +
                            " a.cComboInv," +
                            " a.cWthPromo," +
                            " a.cSerialze," +
                            " a.cUnitType," +
                            " a.cInvStatx," +
                            " a.sSupersed," +
                            " a.cRecdStat," +
                            " b.sDescript xBrandNme," +
                            " c.sDescript xModelNme," +
                            " d.sDescript xInvTypNm," +
                            " e.sMeasurNm," +
                            " f.nQtyOnHnd," +
                            " '' sReferNo1," +
                            " '' sSerialID" +
                        " FROM Inventory a" + 
                            " LEFT JOIN Brand b" + 
                                    " ON a.sBrandCde = b.sBrandCde" + 
                            " LEFT JOIN Model c" + 
                                    " ON a.sModelCde = c.sModelCde" + 
                            " LEFT JOIN Inv_Type d" + 
                                    " ON a.sInvTypCd = d.sInvTypCd" + 
                            " LEFT JOIN Measure e" + 
                                    " ON e.sMeasurID = a.sMeasurID," +
                            " Inv_Master f" + 
                        " WHERE a.sStockIDx = f.sStockIDx" + 
                            " AND f.sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                            " AND a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE) +
                            " AND a.cSerialze = '0'"; //" AND f.nQtyOnHnd > 0"
        
        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty())
            lsSQL = lsSQL + " AND a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type"));
        
        lsSQL = lsSQL + " UNION SELECT" +
                            " a.sStockIDx," + 
                            " g.sSerial01 xReferNox," + 
                            " a.sDescript," + 
                            " a.sBriefDsc," +
                            " a.sAltBarCd," +
                            " a.sCategCd1," +
                            " a.sCategCd2," +
                            " a.sCategCd3," +
                            " a.sCategCd4," +
                            " a.sBrandCde," +
                            " a.sModelCde," +
                            " a.sColorCde," +
                            " a.sInvTypCd," +
                            " a.nUnitPrce," +
                            " a.nSelPrice," +
                            " a.nDiscLev1," +
                            " a.nDiscLev2," +
                            " a.nDiscLev3," +
                            " a.nDealrDsc," +
                            " a.cComboInv," +
                            " a.cWthPromo," +
                            " a.cSerialze," +
                            " a.cUnitType," +
                            " a.cInvStatx," +
                            " a.sSupersed," +
                            " a.cRecdStat," +
                            " b.sDescript xBrandNme," + 
                            " c.sDescript xModelNme," + 
                            " d.sDescript xInvTypNm," + 
                            " e.sMeasurNm," + 
                            " 1 nQtyOnHnd," + 
                            " IFNULL(g.sSerial02, '') xReferNo1," +  
                            " g.sSerialID" +
                        " FROM Inventory a" +  
                            " LEFT JOIN Brand b" +  
                                " ON a.sBrandCde = b.sBrandCde" +  
                            " LEFT JOIN Model c" +  
                                " ON a.sModelCde = c.sModelCde" +  
                            " LEFT JOIN Inv_Type d" +  
                                " ON a.sInvTypCd = d.sInvTypCd" +  
                            " LEFT JOIN Measure e" +  
                                " ON e.sMeasurID = a.sMeasurID," + 
                            " Inv_Master f," + 
                            " Inv_Serial g" + 
                        " WHERE a.sStockIDx = f.sStockIDx" + 
                            " AND f.sStockIDx = g.sStockIDx" + 
                            " AND a.cSerialze = '1'" + 
                            " AND g.cLocation = '1'" + 
                            " AND g.cSoldStat = '0'" + 
                            " AND f.sBranchCd = " + SQLUtil.toSQL(psBranchCd) + 
                            " AND a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE); //" AND f.nQtyOnHnd > 0"
        
        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty())
            lsSQL = lsSQL + " AND a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type"));
        
        return lsSQL;
    }
    
    private String getSQ_SoldStock(){
        String lsSQL =  "SELECT" +
                            " a.sStockIDx," +
                            " a.sBarCodex xReferNox," +
                            " a.sDescript," +
                            " a.sBriefDsc," +
                            " a.sAltBarCd," +
                            " a.sCategCd1," +
                            " a.sCategCd2," +
                            " a.sCategCd3," +
                            " a.sCategCd4," +
                            " a.sBrandCde," +
                            " a.sModelCde," +
                            " a.sColorCde," +
                            " a.sInvTypCd," +
                            " a.nUnitPrce," +
                            " a.nSelPrice," +
                            " a.nDiscLev1," +
                            " a.nDiscLev2," +
                            " a.nDiscLev3," +
                            " a.nDealrDsc," +
                            " a.cComboInv," +
                            " a.cWthPromo," +
                            " a.cSerialze," +
                            " a.cUnitType," +
                            " a.cInvStatx," +
                            " a.sSupersed," +
                            " a.cRecdStat," +
                            " b.sDescript xBrandNme," +
                            " c.sDescript xModelNme," +
                            " d.sDescript xInvTypNm," +
                            " e.sMeasurNm," +
                            " f.nQtyOnHnd," +
                            " '' sReferNo1," +
                            " '' sSerialID" +
                        " FROM Inventory a" + 
                            " LEFT JOIN Brand b" + 
                                    " ON a.sBrandCde = b.sBrandCde" + 
                            " LEFT JOIN Model c" + 
                                    " ON a.sModelCde = c.sModelCde" + 
                            " LEFT JOIN Inv_Type d" + 
                                    " ON a.sInvTypCd = d.sInvTypCd" + 
                            " LEFT JOIN Measure e" + 
                                    " ON e.sMeasurID = a.sMeasurID," +
                            " Inv_Master f" + 
                        " WHERE a.sStockIDx = f.sStockIDx" + 
                            " AND f.sBranchCd = " + SQLUtil.toSQL(psBranchCd) +
                            " AND a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE) +
                            " AND a.cSerialze = '0'";
        
        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty())
            lsSQL = lsSQL + " AND a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type"));
        
        lsSQL = lsSQL + " UNION SELECT" +
                            " a.sStockIDx," + 
                            " g.sSerial01 xReferNox," + 
                            " a.sDescript," + 
                            " a.sBriefDsc," +
                            " a.sAltBarCd," +
                            " a.sCategCd1," +
                            " a.sCategCd2," +
                            " a.sCategCd3," +
                            " a.sCategCd4," +
                            " a.sBrandCde," +
                            " a.sModelCde," +
                            " a.sColorCde," +
                            " a.sInvTypCd," +
                            " a.nUnitPrce," +
                            " a.nSelPrice," +
                            " a.nDiscLev1," +
                            " a.nDiscLev2," +
                            " a.nDiscLev3," +
                            " a.nDealrDsc," +
                            " a.cComboInv," +
                            " a.cWthPromo," +
                            " a.cSerialze," +
                            " a.cUnitType," +
                            " a.cInvStatx," +
                            " a.sSupersed," +
                            " a.cRecdStat," +
                            " b.sDescript xBrandNme," + 
                            " c.sDescript xModelNme," + 
                            " d.sDescript xInvTypNm," + 
                            " e.sMeasurNm," + 
                            " 1 nQtyOnHnd," + 
                            " IFNULL(g.sSerial02, '') xReferNo1," +  
                            " g.sSerialID" +
                        " FROM Inventory a" +  
                            " LEFT JOIN Brand b" +  
                                " ON a.sBrandCde = b.sBrandCde" +  
                            " LEFT JOIN Model c" +  
                                " ON a.sModelCde = c.sModelCde" +  
                            " LEFT JOIN Inv_Type d" +  
                                " ON a.sInvTypCd = d.sInvTypCd" +  
                            " LEFT JOIN Measure e" +  
                                " ON e.sMeasurID = a.sMeasurID," + 
                            " Inv_Master f," + 
                            " Inv_Serial g" + 
                        " WHERE a.sStockIDx = f.sStockIDx" + 
                            " AND f.sStockIDx = g.sStockIDx" + 
                            " AND a.cSerialze = '1'" + 
                            " AND f.sBranchCd = " + SQLUtil.toSQL(psBranchCd) + 
                            " AND a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE);
        
        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty())
            lsSQL = lsSQL + " AND a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type"));
        
        return lsSQL;
    }
    
    private String getSQ_Stock(){
        String lsSQL =  "SELECT " +
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
                    ", e.sMeasurNm" + 
                    ", f.nQtyOnHnd" + 
                " FROM Inventory a" + 
                    " LEFT JOIN Brand b" + 
                        " ON a.sBrandCde = b.sBrandCde" + 
                    " LEFT JOIN Model c" + 
                        " ON a.sModelCde = c.sModelCde" + 
                    " LEFT JOIN Inv_Type d" + 
                        " ON a.sInvTypCd = d.sInvTypCd" +
                    " LEFT JOIN Measure e" + 
                        " ON e.sMeasurID = a.sMeasurID" + 
                    ", Inv_Master f" + 
                " WHERE a.sStockIDx = f.sStockIDx" + 
                    " AND f.sBranchCd = " + SQLUtil.toSQL(psBranchCd);
        
        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty())
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type")));
        
        return lsSQL;
    }
    
    private String getSQ_Master(){return MiscUtil.makeSelect(new UnitInvMaster());}
    
    private String getSQ_Serial(){return MiscUtil.makeSelect(new UnitInvSerial());}
    
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
    
    //Member Variables
    private GRider poGRider = null;
    private String psUserIDxx = "";
    private String psBranchCd = "";
    private String psWarnMsg = "";
    private String psErrMsgx = "";
    private boolean pbWithParent = false;
    private int pnEditMode;
    
    private String psStockIDx = "";
    private String psBarCodex = "";
    private String psDescript = "";
    private String psSerialID = "";
    private String psSerial01 = "";
    private String psSerial02 = "";
    
    private UnitInvMaster poData = new UnitInvMaster();
    private UnitInvSerial poSerial = new UnitInvSerial();
    private UnitInventory poDataInv = new UnitInventory();
    private Inventory poInventory = null;
}
