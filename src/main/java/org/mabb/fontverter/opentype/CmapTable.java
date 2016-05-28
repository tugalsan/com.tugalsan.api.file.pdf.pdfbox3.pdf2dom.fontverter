package org.mabb.fontverter.opentype;


import org.mabb.fontverter.CharsetConverter.GlyphMapping;
import org.mabb.fontverter.io.FontDataInputStream;
import org.mabb.fontverter.io.FontDataOutputStream;
import org.mabb.fontverter.io.DataTypeProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static org.mabb.fontverter.opentype.CmapSubTable.*;
import static org.mabb.fontverter.opentype.CmapSubTable.CMAP_RECORD_BYTE_SIZE;

public class CmapTable extends OpenTypeTable {
    private static Logger log = LoggerFactory.getLogger(CmapTable.class);
    private static final int CMAP_HEADER_SIZE = 4;
    private Format4SubTable windowsTable;
    private Format4SubTable unixTable;
    private Format0SubTable macTable;

    private List<CmapSubTable> subTables = new ArrayList<CmapSubTable>();

    @DataTypeProperty(dataType = DataTypeProperty.DataType.USHORT)
    int version;

    @DataTypeProperty(dataType = DataTypeProperty.DataType.USHORT)
    int numTables() {
        return subTables.size();
    }

    public String getTableTypeName() {
        return "cmap";
    }

    @Override
    protected byte[] generateUnpaddedData() throws IOException {
        calculateOffsets();

        FontDataOutputStream writer = new FontDataOutputStream(FontDataOutputStream.OPEN_TYPE_CHARSET);
        writer.write(super.generateUnpaddedData());

        for (CmapSubTable tableOn : subTables) {
            writer.write(tableOn.getRecordData());
        }

        for (CmapSubTable tableOn : subTables)
            writer.write(tableOn.getData());

        return writer.toByteArray();
    }

    public void readData(byte[] data) throws IOException {
        FontDataInputStream input = new FontDataInputStream(data);
        version = input.readUnsignedShort();
        int numTables = input.readUnsignedShort();

        List<SubTableHeader> headers = new ArrayList<SubTableHeader>();
        for (int i = 0; i < numTables; i++) {
            SubTableHeader header = new SubTableHeader();
            header.platformID = input.readUnsignedShort();
            header.encodingID = input.readUnsignedShort();
            header.offset = input.readUnsignedInt();

            headers.add(header);
        }

        for (SubTableHeader header : headers) {
            CmapSubTable subTable = null;
            input.seek((int) header.offset);

            int format = input.readUnsignedShort();
            if (format == 0)
                subTable = new Format0SubTable();
            else if (format == 2)
                subTable = new Format2SubTable();
            else if (format == 4)
                subTable = new Format4SubTable();
            else if (format == 6)
                subTable = new Format6SubTable();
            else if (format == 8)
                subTable = new Format8SubTable();
            else if (format == 10)
                subTable = new Format10SubTable();
            else if (format == 12)
                subTable = new Format12SubTable();
            else if (format == 13)
                subTable = new Format13SubTable();
            else if (format == 14)
                subTable = new Format14SubTable();

            if (subTable == null)
                continue;

            subTable.readData(input);
            subTable.setEncodingId(header.encodingID);
            subTable.setPlatformId(header.platformID);
            subTables.add(subTable);
        }
    }

    static class SubTableHeader {
        int platformID;
        int encodingID;
        long offset;
    }

    public static CmapTable createDefaultTable() {
        CmapTable table = new CmapTable();
        table.version = 0;

        table.unixTable = new Format4SubTable();
        table.unixTable.setPlatformId(0);
        table.unixTable.setEncodingId(3);
        table.subTables.add(table.unixTable);

        table.macTable = new Format0SubTable();
        table.macTable.setPlatformId(1);
        table.macTable.setEncodingId(0);
        table.subTables.add(table.macTable);

        table.windowsTable = new Format4SubTable();
        table.windowsTable.setPlatformId(3);
        table.windowsTable.setEncodingId(1);
        table.subTables.add(table.windowsTable);

        return table;
    }

    public void addGlyphMapping(Integer charCode, Integer glyphId) {
        windowsTable.addGlyphMapping(charCode, glyphId);
        unixTable.addGlyphMapping(charCode, glyphId);
    }

    public void addGlyphMapping(List<GlyphMapping> mapping) {
        for (GlyphMapping mappingOn : mapping)
            addGlyphMapping(mappingOn.charCode, mappingOn.glyphId);
    }

    public int getGlyphCount() {
        if (subTables.size() == 0)
            return 0;
        return subTables.get(0).glyphCount();
    }

    public List<GlyphMapping> getGlyphMappings() {
        if (subTables.size() == 0)
            return new ArrayList<GlyphMapping>();

        return subTables.get(0).getGlyphMappings();
    }

    private void calculateOffsets() throws IOException {
        int offset = subTables.size() * CMAP_RECORD_BYTE_SIZE + CMAP_HEADER_SIZE;
        for (CmapSubTable tableOn : subTables) {
            tableOn.setSubTableOffset(offset);
            offset += tableOn.getData().length;
        }
    }

}
