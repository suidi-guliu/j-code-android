package dev.blamspot.jcode;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import java.io.*;
import java.security.MessageDigest;

/** Test APK only. Read-only real SAF root; no permissions, provider or fixture added to main APK. */
public final class ZhKitDocumentsProvider extends DocumentsProvider {
    private static final String NAME="sui-code-zh-hans-1.0.1.suik";
    private static final String TITLE="sui-code test packages";
    private static final String[] ROOT_COLUMNS={"root_id","document_id","title","flags","mime_types"};
    private static final String[] DOC_COLUMNS={"document_id","_display_name","mime_type","flags","_size","last_modified"};
    @Override public boolean onCreate() { return true; }
    private Object[] values(String[] columns, java.util.Map<String,Object> data) {
        Object[] out=new Object[columns.length];for(int i=0;i<columns.length;i++) out[i]=data.get(columns[i]);return out;
    }
    @Override public Cursor queryRoots(String[] projection) {
        String[] cols=projection==null?ROOT_COLUMNS:projection;
        MatrixCursor cursor=new MatrixCursor(cols);java.util.Map<String,Object> row=new java.util.HashMap<>();
        row.put("root_id","sui-zh-tests");row.put("document_id","root");row.put("title",TITLE);
        row.put("flags",DocumentsContract.Root.FLAG_LOCAL_ONLY|DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD);
        row.put("mime_types","application/octet-stream");cursor.addRow(values(cols,row));return cursor;
    }
    private void document(MatrixCursor cursor,String[] cols,String id) throws FileNotFoundException {
        if(!id.equals("root")&&!id.equals("zh-hans")) throw new FileNotFoundException(id);
        java.util.Map<String,Object> row=new java.util.HashMap<>();
        row.put("document_id",id);row.put("_display_name",id.equals("root")?TITLE:NAME);
        row.put("mime_type",id.equals("root")?DocumentsContract.Document.MIME_TYPE_DIR:"application/octet-stream");
        row.put("flags",0);row.put("_size",id.equals("root")?0L:materialize().length());row.put("last_modified",0L);
        cursor.addRow(values(cols,row));
    }
    @Override public Cursor queryDocument(String id,String[] projection) throws FileNotFoundException {
        String[] cols=projection==null?DOC_COLUMNS:projection;MatrixCursor cursor=new MatrixCursor(cols);document(cursor,cols,id);return cursor;
    }
    @Override public Cursor queryChildDocuments(String parent,String[] projection,String sort) throws FileNotFoundException {
        if(!parent.equals("root")) throw new FileNotFoundException(parent);
        String[] cols=projection==null?DOC_COLUMNS:projection;MatrixCursor cursor=new MatrixCursor(cols);document(cursor,cols,"zh-hans");return cursor;
    }
    @Override public boolean isChildDocument(String parent,String child) { return parent.equals("root")&&child.equals("zh-hans"); }
    private synchronized File materialize() throws FileNotFoundException {
        try {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream input=getContext().getAssets().open(NAME)) {
                byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1) out.write(buffer,0,n);
            }
            byte[] bytes=out.toByteArray();StringBuilder digest=new StringBuilder();
            for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes)) digest.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            File file=new File(getContext().getCacheDir(),"kit-"+digest+".suik");
            if(!file.isFile()) try(FileOutputStream output=new FileOutputStream(file)) { output.write(bytes); }
            return file;
        } catch(Exception e) { FileNotFoundException failure=new FileNotFoundException(e.toString());failure.initCause(e);throw failure; }
    }
    @Override public ParcelFileDescriptor openDocument(String id,String mode,CancellationSignal signal) throws FileNotFoundException {
        if(!id.equals("zh-hans")||!mode.equals("r")) throw new FileNotFoundException("Read-only fixture");
        return ParcelFileDescriptor.open(materialize(),ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
