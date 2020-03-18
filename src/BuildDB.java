// BuildDB.java
// JG Miller (JGM), Portland, OR, jimsemantic@gmail.com
// 3/15/2020

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

public class BuildDB {
    String inputFileName;
    Store store;
    Index primaryKeyIndex;
    ArrayList<Index> columnIndices;

    public BuildDB() throws IOException {
        inputFileName = "util/input_data";
        System.out.printf("Reading source data from %s\n", inputFileName);
        store = new Store("store.dat");
        columnIndices = new ArrayList<Index>();
    }

    public static void main(String[] args) {
        System.out.println("Started:  " + LocalDateTime.now());

        BuildDB buildDB = null;
        BufferedReader br = null;
        try {
            buildDB = new BuildDB();
            buildDB.primaryKeyIndex = buildDB.createPrimaryKeyIndex("primary_key");
            br = new BufferedReader(new FileReader(buildDB.inputFileName));
            ArrayList<String> fields = new ArrayList<String>();
            ArrayList<String> primaryKey = new ArrayList<String>();
            Long lineOffset;
            String headerLine = br.readLine();
            buildDB.store.addRecordToStore(headerLine);
            BuildDB finalBuildDB = buildDB;
            ArrayList<String> headers = new ArrayList(Arrays.asList(headerLine.split("\\|")));
            headers.forEach((h) -> {
                try {
                    finalBuildDB.createColumnIndex(h);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            String line;
            while ((line = br.readLine()) != null) {
                fields.clear();
                fields.addAll(Arrays.asList(line.split("\\|")));
                primaryKey.clear();
                primaryKey.add(fields.get(0));
                primaryKey.add(fields.get(1));
                primaryKey.add(fields.get(3));
                lineOffset = (Long) buildDB.primaryKeyIndex.lookup(primaryKey);
                if (lineOffset == null) {
                    lineOffset = buildDB.store.addRecordToStore(line);
                    buildDB.primaryKeyIndex.storeUnique(primaryKey, lineOffset);
                } else
                    buildDB.store.overwriteRecordInStore(line, lineOffset);
                for (int i = 0; i < buildDB.columnIndices.size(); i++)
                    buildDB.columnIndices.get(i).storeMultivalue(fields.get(i), lineOffset);
            }
            buildDB.primaryKeyIndex.serialize();
            for (int i = 0; i < buildDB.columnIndices.size(); i++)
                buildDB.columnIndices.get(i).serialize();
            buildDB.store.closeStore();
            System.out.printf("File backing store saved to %s\n", buildDB.store.storeFileName);
            System.out.println("Finished:  " + LocalDateTime.now());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Index createPrimaryKeyIndex(String h) throws IOException {
        return primaryKeyIndex = new Index<ArrayList<String>, Long>(h + ".idx");
    }

    private void createColumnIndex(String h) throws IOException {
        Index index = new Index<String, ArrayList<Long>>(h + ".idx");
        columnIndices.add(index);
    }
}