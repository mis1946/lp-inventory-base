package org.rmj.cas.inventory.base;

import com.mysql.jdbc.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.json.simple.JSONObject;
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.iface.GEntity;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ui.showFXDialog;
import org.rmj.cas.inventory.pojo.UnitInvSubUnit;
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

/**
 * Inventory BASE
 * @author Michael Torres Cuison
 * @since 2018.10.03
 */
public class Inventory{   
    public Inventory(GRider foGRider, String fsBranchCD, boolean fbWithParent){
        poGRider = foGRider;
        
        if (foGRider != null){
            pbWithParent = fbWithParent;
            psBranchCd = fsBranchCD;
            
            psInvTypCd = CommonUtils.getParameter(System.getProperty("store.inventory.type"));
            
            this.psUserIDxx = foGRider.getUserID();
            pnEditMode = EditMode.UNKNOWN;
        }
    }
    
    public boolean NewRecord() {
        poData = new UnitInventory();
        
        Connection loConn = null;
        loConn = setConnection();       
        
        //assign the primary values
        poData.setStockIDx(MiscUtil.getNextCode(poData.getTable(), "sStockIDx", true, loConn, psBranchCd));
        
        paSubUnit = new ArrayList<>();
        addSubUnit();
        
        pnEditMode = EditMode.ADDNEW;
        return true;
    }

    private ArrayList<UnitInvSubUnit> loadSubUnits(String fsValue){
        String lsSQL = MiscUtil.makeSelect(new UnitInvSubUnit()) + " ORDER BY nEntryNox";
        lsSQL = MiscUtil.addCondition(lsSQL, "sStockIDx = " + SQLUtil.toSQL(fsValue));
        
        
        System.out.println(lsSQL);
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        ArrayList<UnitInvSubUnit> laSubUnits = new ArrayList<>();
        UnitInvSubUnit loSubUnit = new UnitInvSubUnit();
        
        try {
            if (!loRS.next()){
                setMessage("No Record Found");
            }else{
                loRS.beforeFirst();
                while(loRS.next()){
                    for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                        loSubUnit.setValue(lnCol, loRS.getObject(lnCol));
                    }
                    laSubUnits.add(new UnitInvSubUnit());
                    laSubUnits.get(laSubUnits.size()-1).setStockIDx(loSubUnit.getStockIDx());
                    laSubUnits.get(laSubUnits.size()-1).setEntryNox(loSubUnit.getEntryNox());
                    laSubUnits.get(laSubUnits.size()-1).setItmSubID(loSubUnit.getItmSubID());
                    laSubUnits.get(laSubUnits.size()-1).setQuantity(loSubUnit.getQuantity());
                    laSubUnits.get(laSubUnits.size()-1).setDateModified(loSubUnit.getDateModified());
                }
            }              
        } catch (SQLException ex) {
            setErrMsg(ex.getMessage());
            return null;
        } finally{
            MiscUtil.close(loRS);
        }
        
       return laSubUnits;
    }
    
    private UnitInventory openRecord(String fsValue) {
        poData = new UnitInventory();
        
        Connection loConn = null;
        loConn = setConnection();   
        
        String lsSQL = MiscUtil.addCondition(getSQ_Master(), "sStockIDx = " + SQLUtil.toSQL(fsValue));
        ResultSet loRS = poGRider.executeQuery(lsSQL);
        
        try {
            if (!loRS.next()){
                setMessage("No Record Found");
            }else{
                //load each column to the entity
                for(int lnCol=1; lnCol<=loRS.getMetaData().getColumnCount(); lnCol++){
                    poData.setValue(lnCol, loRS.getObject(lnCol));
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
        return poData;
    }
    
    public boolean BrowseRecord(String fsValue, boolean fbByCode, boolean fbSearch){
        String lsHeader = "Brand»Description»Unit»Model»Inv. Type»Barcode»Stock ID";
        String lsColName = "xBrandNme»sDescript»sMeasurNm»xModelNme»xInvTypNm»sBarCodex»sStockIDx";
        String lsColCrit = "b.sDescript»a.sDescript»e.sMeasurNm»c.sDescript»d.sDescript»a.sBarCodex»a.sStockIDx";
        String lsSQL = getSQ_Inventory();
        JSONObject loJSON;
        
        loJSON = showFXDialog.jsonSearch(poGRider, 
                                            lsSQL, 
                                            fsValue, 
                                            lsHeader, 
                                            lsColName, 
                                            lsColCrit, 
                                            fbByCode ? 6 : fbSearch ? 1 : 5);
        
        if(loJSON == null)
            return false;
        else{
            return OpenRecord((String) loJSON.get("sStockIDx"));
        } 
    }
    
    public boolean OpenRecord(String fsValue){
        paSubUnit = loadSubUnits(fsValue);
        
        if (paSubUnit.isEmpty()) addSubUnit();
        
        return openRecord(fsValue) != null;
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

    private boolean saveSubUnits(String fsValue){
        if (paSubUnit.isEmpty()) return true;
        
        int lnCtr;
        String lsSQL;
        UnitInvSubUnit loNewEnt = null;
        
        if (pnEditMode == EditMode.ADDNEW){
            Connection loConn = null;
            loConn = setConnection();  
            
            for (lnCtr = 0; lnCtr <= paSubUnit.size() -1; lnCtr++){
                loNewEnt = paSubUnit.get(lnCtr);
                
                if (!loNewEnt.getItmSubID().equals("")){
                    loNewEnt.setStockIDx(fsValue);
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
            ArrayList<UnitInvSubUnit> laSubUnit = loadSubUnits(poData.getStockIDx());
            
            for (lnCtr = 0; lnCtr <= paSubUnit.size()-1; lnCtr++){
                loNewEnt = paSubUnit.get(lnCtr);
                
                if (!loNewEnt.getItmSubID().equals("")){
                    if (lnCtr <= laSubUnit.size()-1){
                        if (loNewEnt.getEntryNox() != lnCtr+1) loNewEnt.setEntryNox(lnCtr+1);
                        
                        lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt, 
                                                (GEntity) laSubUnit.get(lnCtr), 
                                                "sStockIDx = " + SQLUtil.toSQL(loNewEnt.getValue(1)) +
                                                " AND nEntryNox = " + SQLUtil.toSQL(loNewEnt.getValue(2)));

                    } else{
                        loNewEnt.setStockIDx(fsValue);
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
                        lsSQL = "DELETE FROM " + poSubUnit.getTable()+
                                " WHERE sStockIDx = " + SQLUtil.toSQL(laSubUnit.get(lnCtr2).getStockIDx()) +
                                    " AND nEntryNox = " + SQLUtil.toSQL(laSubUnit.get(lnCtr2).getEntryNox());

                        if (!lsSQL.equals("")){
                            if(poGRider.executeQuery(lsSQL, poSubUnit.getTable(), "", "") == 0){
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
    
    private boolean isEntryOK(UnitInventory foNewEnt){
        if (foNewEnt.getBarCodex()== null || foNewEnt.getBarCodex().isEmpty()){
            poData.setBarCodex(CommonUtils.getNextBarcode(poGRider.getConnection(), "Inventory", "sBarCodex", true));
        }
        
        if (foNewEnt.getBarCodex().length() > 20){
            setMessage("Barcode size must not be over 12 characters.");
            return false;
        }
        
        if (foNewEnt.getDescript()== null || foNewEnt.getDescript().isEmpty()){
            setMessage("No description detected.");
            return false;
        }
        
        if (foNewEnt.getInvTypCd()== null || foNewEnt.getInvTypCd().isEmpty()){
            setMessage("No inventory type detected.");
            return false;
        }
               
        if (System.getProperty("store.inventory.strict.type").equals("1")){
            if (foNewEnt.getBriefDsc()== null || foNewEnt.getBriefDsc().isEmpty()){
                setMessage("No brief description detected.");
                return false;
            }

            if (foNewEnt.getBriefDsc().length() > 20){
                setMessage("Brief description size must not be over 20 characters.");
                return false;
            }

            if (foNewEnt.getMeasureID()== null || foNewEnt.getMeasureID().isEmpty()){
                setMessage("No measurement detected.");
                return false;
            }
        }
        
        return true;
    }
    
    public boolean SaveRecord() {
        String lsSQL = "";
        UnitInventory loOldEnt = null;
        UnitInventory loNewEnt = null;
        boolean lbUpdate = false;
                
        // Typecast the Entity to this object
        loNewEnt = (UnitInventory) poData;
        
        if (!isEntryOK(loNewEnt)) return false;
        
        loNewEnt.setModified(psUserIDxx);
        loNewEnt.setDateModified(poGRider.getServerDate());
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (pnEditMode == EditMode.ADDNEW){
            Connection loConn = null;
            loConn = setConnection();   
            
            loNewEnt.setStockIDx(MiscUtil.getNextCode(loNewEnt.getTable(), "sStockIDx", true, loConn, psBranchCd));
            if (!pbWithParent) MiscUtil.close(loConn);            
            
            lbUpdate = saveSubUnits(loNewEnt.getStockIDx());
            if (!lbUpdate) lsSQL = "";
            else lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt);
        }else{
            //Load previous transaction
            loOldEnt = openRecord(poData.getStockIDx());
            loOldEnt.setModified(psUserIDxx);
            
            lbUpdate = saveSubUnits(loNewEnt.getStockIDx());
            if (!lbUpdate) lsSQL = "";            
            else lsSQL = MiscUtil.makeSQL((GEntity) loNewEnt, (GEntity) loOldEnt, "sStockIDx = " + SQLUtil.toSQL(loNewEnt.getValue(1)));
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
        
        //save inventory master
        if (lbUpdate && pnEditMode == EditMode.ADDNEW){
            InvMaster loInvMaster = new InvMaster(poGRider, poGRider.getBranchCode(), true);
            loInvMaster.NewRecord();
            
            if (loInvMaster.SearchInventory(loNewEnt.getStockIDx(), true, true)){
                loInvMaster.setMaster("sBranchCd", poGRider.getBranchCode());
                loInvMaster.setMaster("dAcquired", poGRider.getServerDate());
                loInvMaster.setMaster("dBegInvxx", poGRider.getServerDate());

                if (!loInvMaster.SaveRecord()){
                    setErrMsg(loInvMaster.getErrMsg().isEmpty() ? loInvMaster.getMessage() : loInvMaster.getErrMsg());
                    poGRider.rollbackTrans();
                    System.exit(1);
                }
            }
            
            loInvMaster = null;
        }
        
        if (!pbWithParent) {
            if (!getErrMsg().isEmpty()){
                poGRider.rollbackTrans();
            } else poGRider.commitTrans();
        }        
        
        return lbUpdate;
    }
    
    public boolean DeleteRecord() {
        boolean lbResult = false;
        
        if (poData == null){
            setMessage("No record found...");
            return lbResult;
        }
        if (pnEditMode != EditMode.READY){
            setMessage("Invalid edit mode detected...");
            return lbResult;
        }
        
        String lsSQL = "DELETE FROM " + poData.getTable() + 
                        " WHERE sStockIDx = " + SQLUtil.toSQL(poData.getStockIDx());
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, poData.getTable(), "", "") == 0){
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

    public boolean DeactivateRecord(String fsValue) {
        boolean lbResult = false;
        
        if (poData == null){
            setMessage("No record found...");
            return lbResult;
        }
        if (pnEditMode != EditMode.READY){
            setMessage("Invalid edit mode detected...");
            return lbResult;
        }
        
        if (poData.getRecdStat().equalsIgnoreCase(RecordStatus.INACTIVE)){
            setMessage("Current record is inactive...");
            return lbResult;
        }
        
        String lsSQL = "UPDATE " + poData.getTable() + 
                        " SET  cRecdStat = " + SQLUtil.toSQL(RecordStatus.INACTIVE) + 
                            ", sModified = " + SQLUtil.toSQL(psUserIDxx) +
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                        " WHERE sStockIDx = " + SQLUtil.toSQL(poData.getStockIDx());
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, poData.getTable(), "", "") == 0){
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

    public boolean ActivateRecord(String fsValue) {
        boolean lbResult = false;
        
        if (poData == null){
            setMessage("No record found...");
            return lbResult;
        }
        if (pnEditMode != EditMode.READY){
            setMessage("Invalid edit mode detected...");
            return lbResult;
        }
        
        if (poData.getRecdStat().equalsIgnoreCase(RecordStatus.ACTIVE)){
            setMessage("Current record is active...");
            return lbResult;
        }
        
        String lsSQL = "UPDATE " + poData.getTable() + 
                        " SET  cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE) + 
                            ", sModified = " + SQLUtil.toSQL(psUserIDxx) +
                            ", dModified = " + SQLUtil.toSQL(poGRider.getServerDate()) + 
                        " WHERE sStockIDx = " + SQLUtil.toSQL(poData.getStockIDx());
        
        if (!pbWithParent) poGRider.beginTrans();
        
        if (poGRider.executeQuery(lsSQL, poData.getTable(), "", "") == 0){
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
    
    public void setMaster(int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN){
            // Don't allow specific fields to assign values
            if(!(fnCol == poData.getColumn("sStockIDx") ||
                fnCol == poData.getColumn("cRecdStat") ||
                fnCol == poData.getColumn("sModified") ||
                fnCol == poData.getColumn("dModified"))){
                
                if (fnCol == poData.getColumn("nUnitPrce") ||
                    fnCol == poData.getColumn("nSelPrice") ||
                    fnCol == poData.getColumn("nDiscLev1") ||
                    fnCol == poData.getColumn("nDiscLev2") ||
                    fnCol == poData.getColumn("nDiscLev3") ||
                    fnCol == poData.getColumn("nDealrDsc")){
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
    
    public String getMeasureMent(String fsMeasureID) {
        String lsSQL;
        ResultSet loRS = null;
        
        lsSQL = "SELECT" +
                " a.sMeasurID" +
                ", b.sMeasurNm" +
                " FROM Inventory a" +
                ", Measure b" +
                " WHERE a.sMeasurID = b.sMeasurID" +
                " AND a.sMeasurID =" + SQLUtil.toSQL(fsMeasureID) +
                " LIMIT 1";
        try{
            loRS = poGRider.executeQuery(lsSQL);
            loRS.first();
            
            return loRS.getString("sMeasurNm");
        }catch(SQLException e){
            return "";
        }
       
    }

    public String SearchMaster(int fnCol, String fsValue, boolean fbByCode){
        String lsHeader = "";
        String lsColName = "";
        String lsColCrit = "";
        String lsSQL = "";
        JSONObject loJSON;
        
        if (fsValue.equals("") && fbByCode) return "";
                
        switch(fnCol){
            case 6: //sCategCd1
                XMCategory loCategory = new XMCategory(poGRider, psBranchCd, true); 
                
                loJSON = loCategory.searchCategory(fsValue, fbByCode);
                
                if (loJSON != null){
                    setMaster(fnCol, (String) loJSON.get("sCategrCd"));
                    return (String) loJSON.get("sDescript");
                } else {
                    setMaster(fnCol, "");
                    return "";
                }
            case 7: //sCategCd2
                XMCategoryLevel2 loCategory2 = new XMCategoryLevel2(poGRider, psBranchCd, true);
                
                loJSON = loCategory2.searchCategory(fsValue, fbByCode);
                                
                if (loJSON != null){
                    setMaster(fnCol, (String) loJSON.get("sCategrCd"));
                    return (String) loJSON.get("sDescript");
                } else {
                    setMaster(fnCol, "");
                    return "";
                }
            case 8: //sCategCd3
                XMCategoryLevel3 loCategory3 = new XMCategoryLevel3(poGRider, psBranchCd, true);
                
                loJSON = loCategory3.searchCategory(fsValue, fbByCode);
                
                if (loJSON != null){
                    setMaster(fnCol, (String) loJSON.get("sCategrCd"));
                    return (String) loJSON.get("sDescript");
                } else {
                    setMaster(fnCol, "");
                    return "";
                }
            case 9: //sCategCd4
                XMCategoryLevel4 loCategory4 = new XMCategoryLevel4(poGRider, psBranchCd, true);
                
                loJSON = loCategory4.searchCategory(fsValue, fbByCode);
                
                if (loJSON != null){
                    setMaster(fnCol, (String) loJSON.get("sCategrCd"));
                    return (String) loJSON.get("sDescript");
                } else {
                    setMaster(fnCol, "");
                    return "";
                }
            case 10: //sBrandCde
                XMBrand loBrand = new XMBrand(poGRider, psBranchCd, true);
                
                loJSON = loBrand.searchBrand(fsValue, fbByCode);
                                
                if (loJSON != null){
                    setMaster(fnCol, (String) loJSON.get("sBrandCde"));
                    return (String) loJSON.get("sDescript");
                } else {
                    setMaster(fnCol, "");
                    return "";
                }
            case 11: //sModelCde
                XMModel loModel = new XMModel(poGRider, psBranchCd, false);
                
                loJSON = loModel.searchModel(fsValue, fbByCode);
                
                if (loJSON != null){
                    setMaster(fnCol, (String) loJSON.get("sModelCde"));
                    return String.valueOf(loJSON.get("sModelNme")).isEmpty() ? (String) loJSON.get("sDescript") : (String) loJSON.get("sModelNme");
                } else {
                    setMaster(fnCol, "");
                    return "";
                }
            case 12: //sColorCde
                XMColor loColor = new XMColor(poGRider, psBranchCd, false);
                
                loJSON = loColor.searchColor(fsValue, fbByCode);
                
                if (loJSON != null){
                    setMaster(fnCol, (String) loJSON.get("sColorCde"));
                    return (String) loJSON.get("sDescript");
                } else {
                    setMaster(fnCol, "");
                    return "";
                }
            case 13: //sInvTypCd
                XMInventoryType loInvType = new XMInventoryType(poGRider, psBranchCd, false);
                
                loJSON = loInvType.searchInvType(fsValue, fbByCode);
                
                if (loJSON != null){
                    setMaster(fnCol, (String) loJSON.get("sInvTypCd"));
                    return (String) loJSON.get("sDescript");
                } else {
                    setMaster(fnCol, "");
                    return "";
                }
            case 29: //sMeasurID
                XMMeasure loMeasure = new XMMeasure(poGRider, psBranchCd, false);
                
                loJSON = loMeasure.searchMeasure(fsValue, fbByCode);
                
                if (loJSON != null){
                    setMaster(fnCol, (String) loJSON.get("sMeasurID"));
                    return (String) loJSON.get("sMeasurNm");
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
    
    public void setSubUnit(int fnRow, int fnCol, Object foData) {
        if (pnEditMode != EditMode.UNKNOWN){
            // Don't allow specific fields to assign values
            if(!(fnCol == poSubUnit.getColumn("sStockIDx") ||
                fnCol == poSubUnit.getColumn("nEntryNox") ||
                fnCol == poSubUnit.getColumn("dModified"))){

                if (fnCol == poSubUnit.getColumn("nQuantity")){
                    if (foData instanceof Double){
                        paSubUnit.get(fnRow).setValue(fnCol, foData);
                        addSubUnit();
                    }else paSubUnit.get(fnRow).setValue(fnCol, 0);
                } else paSubUnit.get(fnRow).setValue(fnCol, foData);   
            }
        }
    }

    public void setSubUnit(int fnRow, String fsCol, Object foData) {
        setSubUnit(fnRow, poSubUnit.getColumn(fsCol), foData);
    }
    
    public Object getSubUnit(int fnRow, int fnCol) {
        if(pnEditMode == EditMode.UNKNOWN)
         return null;
      else{
         return paSubUnit.get(fnRow).getValue(fnCol);
      }
    }

    public Object getSubUnit(int fnRow, String fsCol) {
        return getSubUnit(fnRow, poSubUnit.getColumn(fsCol));
    }
    
    public String SearchSubUnit(int fnRow, int fnCol, String fsValue, boolean fbSearch, boolean fbByCode){
        String lsHeader = "";
        String lsColName = "";
        String lsColCrit = "";
        String lsSQL = "";
        ResultSet loRS;
        JSONObject loJSON;
        
        switch(fnCol){
            case 3:
                lsSQL = MiscUtil.addCondition(getSQ_Inventory(), 
                                                "sStockIDx <> " + SQLUtil.toSQL(poData.getStockIDx()) + 
                                                " AND a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE));
                lsHeader = "Brand»Description»Unit»Model»Inv. Type»Barcode»Stock ID";
                lsColName = "xBrandNme»sDescript»sMeasurNm»xModelNme»xInvTypNm»sBarCodex»sStockIDx";
                lsColCrit = "b.sDescript»a.sDescript»e.sMeasurNm»c.sDescript»d.sDescript»a.sBarCodex»a.sStockIDx";
                
                if (fbByCode){
                    lsSQL = MiscUtil.addCondition(lsSQL, "a.sStockIDx = " + SQLUtil.toSQL(fsValue));
                
                    loRS = poGRider.executeQuery(lsSQL);
                    
                    if (MiscUtil.RecordCount(loRS) == 1)
                        loJSON = CommonUtils.loadJSON(loRS);
                    else
                        loJSON = null;
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
                    setSubUnit(fnRow, fnCol, (String) loJSON.get("sStockIDx"));
                    setSubUnit(fnRow, 5 , (String) loJSON.get("sMeasurNm"));
                    
                    if (fbSearch) 
                        return (String) loJSON.get("sDescript");
                    else 
                        return (String) loJSON.get("sBarCodex");
                } else{
                    setSubUnit(fnRow, fnCol, "");
                }
            default:
                return "";
        }
    }
    
    public String SearchSubUnit(int fnRow, String fsCol, String fsValue, boolean fbSearch, boolean fbByCode){
        return SearchSubUnit(fnRow, poSubUnit.getColumn(fsCol), fsValue, fbSearch, fbByCode);
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

    public void setBranch(String foBranchCD) {
        this.psBranchCd = foBranchCD;
    }

    public void setWithParent(boolean fbWithParent) {
        this.pbWithParent = fbWithParent;
    }

//    public String getSQ_Master() {
//        return (MiscUtil.makeSelect(new UnitInventory()));
//    }
    
    public String getSQ_Master(){
        return "SELECT " +
                " a.sStockIDx " +
                ", a.sBarCodex " +
                ", a.sDescript " +
                ", a.sBriefDsc " +
                ", a.sAltBarCd " +
                ", a.sCategCd1 " +
                ", a.sCategCd2 " +
                ", a.sCategCd3 " +
                ", a.sCategCd4 " +
                ", a.sBrandCde " +
                ", a.sModelCde " +
                ", a.sColorCde " +
                ", a.sInvTypCd " +
                ", a.nUnitPrce " +
                ", a.nSelPrice " +
                ", a.nDiscLev1 " +
                ", a.nDiscLev2 " +
                ", a.nDiscLev3 " +
                ", a.nDealrDsc " +
                ", a.cComboInv " +
                ", a.cWthPromo " +
                ", a.cSerialze " +
                ", a.cUnitType " +
                ", a.cInvStatx " +
                ", a.sSupersed " +
                ", a.cRecdStat " +
                ", a.sModified " +
                ", a.dModified " +
                ", a.sMeasurID " +                
                ", a.cWthExprt " +
                ", b.sMeasurNm " +
              " FROM Inventory a" +
                " LEFT JOIN Measure b" +
                    " ON a.sMeasurID = b.sMeasurID";
    }
        
    
    private String getSQ_Inventory(){
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
                            ", a.cWthExprt" +
                            ", e.sMeasurNm" +
                        " FROM Inventory a" + 
                            " LEFT JOIN Brand b" + 
                                " ON a.sBrandCde = b.sBrandCde" + 
                            " LEFT JOIN Model c" + 
                                " ON a.sModelCde = c.sModelCde" + 
                            " LEFT JOIN Inv_Type d" + 
                                " ON a.sInvTypCd = d.sInvTypCd" + 
                            " LEFT JOIN Measure e" + 
                                " ON e.sMeasurID = a.sMeasurID" +
                        " WHERE a.cRecdStat <> '2'";
        
        //validate result based on the assigned inventory type.
        if (!System.getProperty("store.inventory.type").isEmpty())
            lsSQL = MiscUtil.addCondition(lsSQL, " a.sInvTypCd IN " + CommonUtils.getParameter(System.getProperty("store.inventory.type")));
        
        return lsSQL;
    }
    
    public void addSubUnit(){
        if (paSubUnit.isEmpty())
            paSubUnit.add(new UnitInvSubUnit());
        else{
            if (!paSubUnit.get(SubUnitCount() -1).getItmSubID().equals("") &&
                    paSubUnit.get(SubUnitCount() -1).getQuantity().doubleValue() != 0.00)
                paSubUnit.add(new UnitInvSubUnit());
        }
    }
    
    public void delSubUnit(int fnRow){
        paSubUnit.remove(fnRow);
        
        if (paSubUnit.isEmpty()) paSubUnit.add(new UnitInvSubUnit());
    }
    
    public int SubUnitCount(){
        return paSubUnit.size();
    }
    
    public void setGRider(GRider foGRider){
        this.poGRider = foGRider;
        this.psUserIDxx = foGRider.getUserID();
        
        if (psBranchCd.isEmpty()) psBranchCd = foGRider.getBranchCode();
    }
    
    public int getEditMode(){return pnEditMode;}
    
    public void printColumnsInventory(){poData.list();}
    public void printColumnsInvSubUnit(){poSubUnit.list();}
    
    private Connection setConnection(){
        Connection foConn;
        
        if (pbWithParent){
            foConn = (Connection) poGRider.getConnection();
            if (foConn == null) foConn = (Connection) poGRider.doConnect();
        }else foConn = (Connection) poGRider.doConnect();
        
        return foConn;
    }
    
    //Member Variables
    private GRider poGRider = null;
    private String psUserIDxx = "";
    private String psBranchCd = "";
    private String psInvTypCd = "";
    private String psWarnMsg = "";
    private String psErrMsgx = "";
    private boolean pbWithParent = false;
    private int pnEditMode;
    
    private UnitInventory poData = new UnitInventory();
    private UnitInvSubUnit poSubUnit = new UnitInvSubUnit();
    private ArrayList<UnitInvSubUnit> paSubUnit;
}
