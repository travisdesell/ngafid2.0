package org.ngafid.common;

import java.util.*;
import org.ngafid.flights.Upload;
import org.ngafid.Database;
import java.sql.SQLException;

public class ImportPaginator extends Paginator{

    private int fleetID;
    public ImportPaginator(int bufferSize, int fleetID) throws SQLException{
        super(bufferSize);
        this.fleetID = fleetID;
        super.setNumElements(Upload.getNumUploads(Database.getConnection(), fleetID));
        System.out.println(numElements+" -------- ||||| ");
    }

    public Page currentPage() throws SQLException{
        List<Upload> uploadData = Upload.getUploads(Database.getConnection(), fleetID, new String[]{"IMPORTED", "ERROR"}, super.limitString());
        return new Page(numPages, uploadData, currentIndex);
    }

    public String paginatorType(){
        return "Import paginator";
    }
}
