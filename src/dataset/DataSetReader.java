package dataset;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class DataSetReader {
    private final String DELIMITER;
    private final File DATA_FILE;

    public DataSetReader( String delimiter, String filePath) {
        this.DELIMITER = delimiter;
        this.DATA_FILE = new File(filePath);
    }

    public DataSet getDataSetFromFile(int numberOfAttributes,
                                             int numberOfInstances,
                                             boolean hasHeaders) {
        DataSet dataSet = new DataSet(hasHeaders, numberOfAttributes, numberOfInstances);

        try (Scanner scanner = new Scanner(DATA_FILE)) {
            if (hasHeaders) {
                String headersLine = scanner.nextLine();
                dataSet.setHeaders(headersLine.strip().split(DELIMITER));
            }

            int lineCount = 0;
            while (scanner.hasNextLine()) {
                String dataLine = scanner.nextLine();
                String[] dataStringArray = dataLine.strip().split(DELIMITER);
                //dataStringArray[0] = dataStringArray[0].split("STUDENT")[1];

                int[] data = new int[dataStringArray.length];
                for (int i = 0; i < dataStringArray.length; i++ ) {
                    data[i] = Integer.parseInt(dataStringArray[i]);
                }
                dataSet.setData(data, lineCount);
                lineCount++;
            }
        } catch (FileNotFoundException e) {
            System.err.printf("[ERROR] dataset.DataSetReader.getDataSetFromFile(%s): FileNotFoundException\n", DATA_FILE);
            System.exit(1);
        }
        return dataSet;
    }

}
