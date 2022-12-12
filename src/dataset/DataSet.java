package dataset;

import java.util.function.Consumer;
import java.util.function.Function;

public class DataSet {
    private final boolean hasHeaders;
    private final int numberOfAttributes;
    private final int numberOfInstances;
    private String[] headers;
    private int[][] data;

    protected DataSet (boolean hasHeaders, int numberOfAttributes, int numberOfInstances) {
        this.hasHeaders = hasHeaders;
        this.numberOfAttributes = numberOfAttributes;
        this.numberOfInstances = numberOfInstances;
        data = new int[numberOfInstances][numberOfAttributes];
    }

    public DataSet(DataSet parentDataSet, int[] dataInstancesId) {
        this.hasHeaders = parentDataSet.hasHeaders;
        this.headers = parentDataSet.getHeaders();
        this.numberOfInstances = dataInstancesId.length;
        this.numberOfAttributes = parentDataSet.numberOfAttributes;
        data = new int[numberOfInstances][numberOfAttributes];
        for (int i = 0; i<numberOfInstances; i++) {
            System.arraycopy(parentDataSet.data[dataInstancesId[i]], 0, data[i], 0, numberOfAttributes);
        }
    }

    public void setHeaders(String[] src) {
        if (src.length != this.numberOfAttributes) {
            System.err.println("[ERROR] setHeaders: src.length != dataset.DataSet.numberOfAttributes");
            System.exit(1);
        }
        this.headers = src;
    }

    public void initData(int[][] src) {
        if (src.length != this.numberOfInstances) {
            System.err.printf("[ERROR] initData: src.length{%d} " +
                    "!= dataset.DataSet.numberOfInstances{%d}\n",
                    src.length, this.numberOfInstances);
            System.exit(1);
        }
        for (int i = 0; i < numberOfInstances; i++) {
            if (src[i].length != this.numberOfAttributes) {
                System.err.printf("[ERROR] initData: src[%d].length{%d}" +
                        " != dataset.DataSet.numberOfAttributes{%d}\n",
                        i, src[i].length, this.numberOfAttributes);
                System.exit(1);
            }
            System.arraycopy(src[i],0, data[i], 0, numberOfAttributes);
        }
    }

    public void setData(int[] src, int index) {
        if (src.length != this.numberOfAttributes) {
            System.err.printf("[ERROR] setData: src.length{%d}" +
                            " != dataset.DataSet.numberOfAttributes{%d}\n",
                    src.length, this.numberOfAttributes);
            System.exit(1);
        }
        if (index+1 > data.length) {
            System.err.printf("[ERROR] setData: index{%d}" +
                            " is out of array bounds{%d}\n",
                    index, data.length);
            System.exit(1);
        }
        System.arraycopy(src, 0, data[index], 0, numberOfAttributes);
    }

    public String[] getHeaders() {
        return headers;
    }

    public int[][] getData() {
        return data;
    }

    public boolean isHeaderSet() {
        return hasHeaders;
    }

    public void transformData (Consumer<int[]> consumer) {
        for (int[] dataInstance: data) {
            consumer.accept(dataInstance);
        }
    }

    public int getNumberOfAttributes() {
        return numberOfAttributes;
    }

    public int getNumberOfInstances() {
        return numberOfInstances;
    }
}
