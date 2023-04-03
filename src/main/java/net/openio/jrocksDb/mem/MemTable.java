package net.openio.jrocksDb.mem;

import net.openio.jrocksDb.db.ColumnFamilyHandle;
import net.openio.jrocksDb.db.ColumnFamilyId;
import net.openio.jrocksDb.db.Value;
import net.openio.jrocksDb.log.WALLog;
import net.openio.jrocksDb.log.WalStorage;
import net.openio.jrocksDb.tool.Serializer;
import net.openio.jrocksDb.transaction.CommitId;

import java.util.List;

public class MemTable {

    String walFile;

    WALLog walLog;

    private MemTableRep memTableRep;

    private BloomFilter bloomFilter;

    public volatile boolean  needFlush;



    public KeyValueEntry get(KeyValueEntry key){
        if(bloomFilter.get(key.getKey())) return null;
        return memTableRep.getValue(key);
    }

    public void put(KeyValueEntry key){
        walLog.write(key,walFile);
         memTableRep.addKeyValue(key);
         bloomFilter.add(key.key);

    }

    public List<KeyValueEntry> getAllKeyValue(){
        return memTableRep.getKeyValue(bloomFilter);
    }

    public CommitId getMaxCommit(){
        return memTableRep.getMaxCommit();
    }

    public void needFlush(){
        needFlush=true;
    }

    public boolean getNeedFlush(){
        return needFlush;
    }

    public int getSerializerSize(){
        return memTableRep.getSerializerSize();
    }

    public int getKeySize(){
        return memTableRep.getKeySize();
    }

    public MemTable(String walFile,MemTableRep memTableRep,boolean needFlush,WALLog walLog){
        bloomFilter=new BloomFilter();
        this.needFlush=needFlush;
        this.walFile=walFile;
        this.memTableRep=memTableRep;
        this.walLog=walLog;
    }

    public MemTable(MemTableRep memTableRep, ColumnFamilyHandle columnFamilyHandle,WALLog walLog){
        bloomFilter=new BloomFilter();
        this.needFlush=false;
        this.walLog=walLog;
        walFile=walLog.createWalFile(columnFamilyHandle);
        this.memTableRep=memTableRep;
    }

    public int[] getBloom(){
        return bloomFilter.getData();
    }

}