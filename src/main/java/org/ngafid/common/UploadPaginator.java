package org.ngafid.common;

import java.util.*;
import org.ngafid.flights.Upload;
import org.ngafid.Database;
import java.sql.SQLException;

public class UploadPaginator extends Paginator{

    private int fleetID, numUploads;
    public UploadPaginator(int bufferSize, int fleetID) throws SQLException{
        super(bufferSize);
        this.fleetID = fleetID;
	this.numUploads = Upload.getNumUploads(Database.getConnection(), fleetID);
    }

    public Page currentPage() throws SQLException{
	List<Upload> uploadData = Upload.getUploads(Database.getConnection(), fleetID);
	return new Page(uploadData.size(), uploadData, 0);
    }

    public String paginatorType(){
	return "Upload paginator";
    }
}
