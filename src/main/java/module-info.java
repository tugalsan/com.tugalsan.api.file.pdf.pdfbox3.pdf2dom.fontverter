module com.tugalsan.api.file.pdf.pdfbox3.pdf2dom.fontverter {
    requires java.desktop;
    requires org.apache.commons.io;
    requires com.google.common;
    requires org.apache.commons.lang3;
    requires org.reflections;
    requires org.slf4j;
    requires org.apache.fontbox;
    requires org.apache.pdfbox;
    requires com.tugalsan.api.function;
    exports org.mabb.fontverter;
    exports org.mabb.fontverter.pdf;
}
