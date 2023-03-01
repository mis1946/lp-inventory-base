package org.rmj.cas.inventory.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.FileUtil;

public class SerialUpload {
    GRider oApp;
    
    String sMessage;
    String sLocation;
    
    public SerialUpload(GRider foApp){
        oApp = foApp;
        sLocation = "";
    }
    
    public void setLocation(String fsValue){
        sLocation = fsValue;
    }
    
    public String getMessage(){return sMessage;}
    
    public boolean Process(){
        if (oApp == null){
            sMessage = "Application driver is not set.";
            return false;
        }
        
        if (sLocation.isEmpty()){
            sMessage = "File location is not set.";
        }
        
        File afile = new File(sLocation);
       
        if (afile.exists() && !afile.isDirectory()){
            
            try{
                FileInputStream file = new FileInputStream(afile);

                XSSFWorkbook workbook = new XSSFWorkbook(file);
                XSSFSheet sheet = workbook.getSheetAt(0);
                file.close();

                Map<String, Object[]> data = new TreeMap<String, Object[]>(); 
                Object [] laObj;

                String lsStockIDx;
                String lsSerial01;
                String lsSerial02;
                
                ResultSet loRS;
                String lsSQL;
                
                oApp.beginTrans();
                for (int lnCtr = 1; lnCtr <= sheet.getLastRowNum(); lnCtr++){
                    Cell cell = null;
                    
                    lsStockIDx = "";
                    lsSerial01 = "";
                    lsSerial02 = "";
                
                    cell = sheet.getRow(lnCtr).getCell(0);
                    lsStockIDx = cell.getStringCellValue().trim();
                    cell = sheet.getRow(lnCtr).getCell(1);
                    lsSerial01 = cell.getStringCellValue().trim();
                    
                    cell = sheet.getRow(lnCtr).getCell(2);
                    if (cell != null) lsSerial02 = cell.getStringCellValue().trim();
                    
                    lsSQL = "SELECT sSerialID FROM Inv_Serial" +
                            " WHERE sSerial01 = " + SQLUtil.toSQL(lsSerial01) +
                                " AND sSerial02 = " + SQLUtil.toSQL(lsSerial02) +
                                " AND sStockIDx = " + SQLUtil.toSQL(lsStockIDx) +
                                " AND sBranchCd = " + SQLUtil.toSQL(oApp.getBranchCode());
                    
                    loRS = oApp.executeQuery(lsSQL);
                    
                    if (loRS.next()){
                        lsSQL = "UPDATE Inv_Serial SET" +
                                    "  cLocation = '1'" +
                                    ", cSoldStat = '0'" +
                                    ", dModified = " + SQLUtil.toSQL(oApp.getServerDate()) +
                                " WHERE sSerialID = " + SQLUtil.toSQL(loRS.getString("sSerialID"));
                        
                        if (oApp.executeQuery(lsSQL, "Inv_Serial", oApp.getBranchCode(), "") <= 0){
                            if (!oApp.getErrMsg().isEmpty()){
                                sMessage = oApp.getErrMsg();
                                oApp.rollbackTrans();
                                return false;
                            }
                        }
                    } else {
                        lsSQL = MiscUtil.getNextCode("Inv_Serial", "sSerialID", true, oApp.getConnection(), oApp.getBranchCode());
                        
                        lsSQL = "INSERT INTO Inv_Serial SET" +
                                    "  sSerialID = " + SQLUtil.toSQL(lsSQL) +
                                    ", sBranchCd = " + SQLUtil.toSQL(oApp.getBranchCode()) +
                                    ", sSerial01 = " + SQLUtil.toSQL(lsSerial01) +
                                    ", sSerial02 = " + SQLUtil.toSQL(lsSerial02) +
                                    ", sStockIDx = " + SQLUtil.toSQL(lsStockIDx) +
                                    ", cLocation = '1'" +
                                    ", cSoldStat = '0'" +
                                    ", cUnitType = '1'" +
                                    ", dModified = " + SQLUtil.toSQL(oApp.getServerDate());
                                
                        if (oApp.executeQuery(lsSQL, "Inv_Serial", oApp.getBranchCode(), "") <= 0){
                            if (!oApp.getErrMsg().isEmpty()){
                                sMessage = oApp.getErrMsg();
                                oApp.rollbackTrans();
                                return false;
                            }
                        }
                    }

                    System.out.println(lsStockIDx + "\t" + lsSerial01 + "\t" + lsSerial02);
                }
                oApp.commitTrans();
            } catch(IOException | SQLException e){
                sMessage = e.getMessage();
                return false;
            }
            
            return true;
        }
       
        sMessage = "Directory is either not existing or a directory.";
        return false;
    }
}
