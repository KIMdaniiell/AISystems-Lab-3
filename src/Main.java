import dataset.DataSet;
import dataset.DataSetReader;

import java.util.Collections;
import java.util.LinkedList;

public class Main {

    public static void main(String[] args) {
        DataSetReader dsr = new DataSetReader(";", "DATA.csv");
        DataSet dataSet = dsr.getDataSetFromFile(33, 145, true);
        dataSet.transformData(dataInstance -> {
            if (dataInstance[dataInstance.length-1] > 2){
                dataInstance[dataInstance.length-1] = 1;
            } else {
                dataInstance[dataInstance.length-1] = 0;
            }
        });

        DecisionTree decisionTree = new DecisionTree( dataSet);
        decisionTree.setDebugModeOn(false);
        DecisionTree.TreeNode rootNode =  decisionTree.build(0);
        decisionTree.evaluate(0.5D);

        drawTree(rootNode);
        System.out.printf("Accuracy = %f\nPrecision = %f\nRecall = %f\n",
                decisionTree.getAccuracy(),
                decisionTree.getPrecision(),
                decisionTree.getRecall()
                );

        LinkedList<Double> predictedValues = new LinkedList<>(decisionTree.getPredictionValues());
        Collections.sort(predictedValues);
        predictedValues.addLast(1.000001D);

        System.out.println("threshold;\tTPR;\ttFPR;\tPrecision;\tRecall");
        for (double threshold : predictedValues) {
            decisionTree.evaluate(threshold);
            System.out.printf("%.5f;\t%.5f;\t%.5f;\t%.5f;\t%.5f\n",
                    threshold,
                    decisionTree.getTPR(),
                    decisionTree.getFPR(),
                    decisionTree.getPrecision(),
                    decisionTree.getRecall());
        }


    }

    private static void drawTree(DecisionTree.TreeNode root) {
        System.out.printf("Root [%d]\n", root.dataSet.getNumberOfInstances());
        System.out.printf("Entr=%.3f\n", root.entropy );
        System.out.printf("Value=%.3f\n", root.prediction );
        for (DecisionTree.TreeNode child: root.childrenWithAttrCondition.keySet()) {
            drawTreeR(child, root.childrenWithAttrCondition.get(child), 1);
        }
    }
    private static void drawTreeR(DecisionTree.TreeNode node,DecisionTree.Condition condition, int depth ){
        if (condition.attrId != -1) {
            System.out.printf("%s└── %s ─── Node[%d] \n",
                    "      ".repeat(depth),
                    condition.operatorSTR,
                    node.dataSet.getNumberOfInstances());
            System.out.printf("%s%s         Entropy=%.3f\n",
                    "      ".repeat(depth),
                    " ".repeat(condition.operatorSTR.length()),
                    node.entropy );
            System.out.printf("%s%s         Value=%.3f\n",
                    "      ".repeat(depth),
                    " ".repeat(condition.operatorSTR.length()),
                    node.prediction );
            if (!node.childrenWithAttrCondition.keySet().isEmpty()) {
                for (DecisionTree.TreeNode child: node.childrenWithAttrCondition.keySet()) {
                    drawTreeR(child, node.childrenWithAttrCondition.get(child), depth+1);
                }
            }
        }
    }
}
