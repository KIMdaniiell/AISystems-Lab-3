import dataset.DataSet;

import java.util.*;
import java.util.function.Predicate;

public class DecisionTree {
    private final int classFieldId;
    private final int[] attributesId;
    private final HashSet<TreeNode> treeNodes = new HashSet<>();
    private final HashSet<TreeNode> treeLeaves = new HashSet<>();
    private final TreeNode rootNode;
    private int TP=0, TN=0, FP=0, FN=0;
    private boolean debugModeOn = false;

    public DecisionTree(DataSet rootDataSet) {
        this.classFieldId = rootDataSet.getNumberOfAttributes()-1;
        rootNode = new TreeNode(rootDataSet);
        treeNodes.add(rootNode);
        int numberOfAttributes = (int) (Math.ceil(Math.sqrt(rootDataSet.getNumberOfAttributes() - 1)));
        attributesId = generateAttrIndexes(numberOfAttributes, rootDataSet.getNumberOfAttributes()-1);
        Arrays.stream(attributesId).forEach(i -> System.out.print(i +" "));
        System.out.println();
    }

    public TreeNode build(int maxDepth) {
        int depth = 1;
        while (!treeNodes.isEmpty() && (depth <=maxDepth || maxDepth==0)) {
            if (debugModeOn) System.out.printf("[Углубление] Узлов=%d листьев=%d\n", treeNodes.size(), treeLeaves.size());
            /*Нужно выбрать узлы, которые будем рассматривать.
            Если все элементы принадлежат 1 классу - конец. Если пусто - конец. т.е. переносим в treeLeaves
            те, что остались разбиваем и убираем из tree nodes*/
            HashSet<TreeNode> currentLevelNodes = new HashSet<>(treeNodes);
            for (TreeNode node: currentLevelNodes) {
                if (node.dataSet.getNumberOfInstances() <= 1 || node.entropy <= 0 ) {
                    if (debugModeOn) System.out.println("[Отсечение ветви]");
                    treeLeaves.add(node);
                } else {
                    splitDataSet(node);
                }
                treeNodes.remove(node);
            }
            depth++;
        }
        if (depth > maxDepth) {
            treeLeaves.addAll(treeNodes);
        }
        return rootNode;
    }

    public void evaluate(double threshold) {
        TP=0; TN=0; FP=0; FN=0;
        if (debugModeOn) printTreeLeaves();
        for (TreeNode node: treeLeaves) {
            int predictedClass;
            if (node.prediction >= threshold) {
                predictedClass = 1;
            } else {
                predictedClass = 0;
            }
            for (int[] dataInstance: node.dataSet.getData()) {
                int realClass = dataInstance[classFieldId];
                if (predictedClass == 1 && predictedClass == realClass) TP ++;
                else if (predictedClass == 0 && predictedClass == realClass) TN ++;
                else if (predictedClass == 1) FP ++;
                else FN ++;
            }
        }
    }

    private void splitDataSet(TreeNode node) {
        if (debugModeOn) System.out.printf("[Деление множества T] Entropy = %f \t |T| = %d \n", node.entropy,node.dataSet.getNumberOfInstances());

        HashMap<Integer, Double> attributeIdToFullConditionalEntropy = new HashMap<>();
        for (int attributeId: attributesId) {
            double fullConditionalEntropy = 0;
            HashMap<Integer, Integer> attributeValueToCount = getAttributesValuesToCounts( node.dataSet, attributeId);
            for (int attrValue: attributeValueToCount.keySet()) {
                int[] dataInstancesId = getDataIdWhereAttrEqualValue(node.dataSet, attributeId, attrValue, attributeValueToCount.get(attrValue));
                DataSet conditionFilteredDataSet = new DataSet(node.dataSet, dataInstancesId);
                double conditionalEntropy = getEntropy(conditionFilteredDataSet , classFieldId);
                double mathExpectation = (double)attributeValueToCount.get(attrValue)/node.dataSet.getNumberOfInstances();
                fullConditionalEntropy += conditionalEntropy*mathExpectation;
            }
            attributeIdToFullConditionalEntropy.put(attributeId, fullConditionalEntropy);
        }
        int chosenAttribute = -1;
        for (int attributeId: attributeIdToFullConditionalEntropy.keySet()) {
            if (attributeIdToFullConditionalEntropy.get(attributeId)
                    == Collections.min(attributeIdToFullConditionalEntropy.values())) {
                if (node.entropy - attributeIdToFullConditionalEntropy.get(attributeId) > 0)
                    chosenAttribute = attributeId;
                break;
            }
        }

        if (chosenAttribute != -1) {
            HashMap<Integer, Integer> attributeValueToCount = getAttributesValuesToCounts(node.dataSet, chosenAttribute);
            if (debugModeOn) System.out.printf("\t\t - делим по атрибуту %d \t\t полная условная энтропия = %f\n", chosenAttribute, attributeIdToFullConditionalEntropy.get(chosenAttribute));
            for (int attrValue : attributeValueToCount.keySet() ) {
                if (debugModeOn) System.out.printf("\t\t\t\t - по значению %d \n", attrValue);
                int[] dataInstancesId = getDataIdWhereAttrEqualValue(
                        node.dataSet,
                        chosenAttribute,
                        attrValue,
                        attributeValueToCount.get(attrValue));
                DataSet conditionFilteredDataSet = new DataSet(node.dataSet, dataInstancesId);

                Predicate<Integer> predicate = integer -> {return integer == attrValue;};
                String textPredicate = String.format("Attr%d = %d",chosenAttribute, attrValue);
                TreeNode childTreeNode = new TreeNode(conditionFilteredDataSet);
                node.childrenWithAttrCondition.put(childTreeNode, new Condition(predicate, chosenAttribute,attrValue ,textPredicate));
                treeNodes.add(childTreeNode);
            }
        } else {
            if (debugModeOn) System.out.print("\t\t - атрибут дающий информационный прирост не найден\n");
            Predicate<Integer> predicate = integer -> {return true;};
            TreeNode childTreeNode = new TreeNode(node.dataSet);
            node.childrenWithAttrCondition.put(childTreeNode, new Condition(predicate, -1, -1,"AttrNotFound"));
            treeLeaves.add(childTreeNode);
        }
    }

    public double getAccuracy() {
        return (double)(TP+TN)/(TP+TN+FP+FN);
    }
    public double getPrecision() {
        if (TP == 0 && FP == 0)
            return 1;
        return (double)TP/(TP+FP);
    }
    public double getRecall() {
        return (double)TP/(TP+FN);
    }
    public double getTPR() {
        return (double)TP/(TP+FN);
    }
    public double getFPR() {
        return (double)FP/(FP+TN);
    }

    public HashSet<Double> getPredictionValues () {
        HashSet<Double> predictedValues = new HashSet<>();
        for (TreeNode node: treeLeaves) {
            predictedValues.add( node.prediction);
        }
        return  predictedValues;
    }

    private double getEntropy(DataSet dataSet, int attributeId) {
        HashMap<Integer, Integer> mapCount = new HashMap<>();
        HashMap<Integer, Double> mapFreq = new HashMap<>();
        int[][] data = dataSet.getData();

        int numberOfLines = data.length;

        for (int[] dataLine: data) {
            if (mapCount.containsKey(dataLine[attributeId])){
                mapCount.put(dataLine[attributeId], mapCount.get(dataLine[attributeId])+1);
            } else {
                mapCount.put(dataLine[attributeId], 1);
            }
        }
        for (Integer key: mapCount.keySet()){
            mapFreq.put(key, mapCount.get(key).doubleValue()/numberOfLines);
        }
        double entropy = 0;
        for (double freq: mapFreq.values()) {
            entropy -= freq * Math.log(freq)/Math.log(2);
        }
        return entropy;
    }

    private HashMap<Integer, Integer> getAttributesValuesToCounts(DataSet dataset, int attributeId ) {
        HashMap<Integer, Integer> attributeValueToCount = new HashMap<>();
        for (int[] dataInstance: dataset.getData()) {
            int attrValue = dataInstance[attributeId];
            if (attributeValueToCount.containsKey(attrValue)) {
                attributeValueToCount.put(attrValue, attributeValueToCount.get(attrValue)+1);
            } else {
                attributeValueToCount.put(attrValue, 1);
            }

        }
        return  attributeValueToCount;
    }

    private int[] getDataIdWhereAttrEqualValue(DataSet dataset, int attributeId, int attrValue, int amount) {
        int[] dataInstancesId = new int[amount];
        int dataInstanceCounter = 0;
        for (int i = 0; i < dataset.getNumberOfInstances(); i++) {
            int[] dataInstance = dataset.getData()[i];
            if (dataInstance[attributeId] == attrValue) {
                dataInstancesId[dataInstanceCounter] = i;
                dataInstanceCounter++;
            }
        }
        return  dataInstancesId;
    }

    private int[] generateAttrIndexes (int numberOfAttributes, int bound) {
        final int[] ints = new Random().ints(0, bound).distinct().limit(numberOfAttributes).toArray();
        return ints;
    }

    private double getClassPrediction(DataSet dataSet) {
        HashMap<Integer, Integer> attributeValueToCount = getAttributesValuesToCounts(dataSet, classFieldId);
        int size = dataSet.getNumberOfInstances();
        double prediction = attributeValueToCount.keySet().stream()
                .mapToInt(key -> key*attributeValueToCount.get(key)).sum();
        return prediction/size;
    }

    private void printTreeLeaves() {
        System.out.println("=".repeat(100));
        for (TreeNode node: treeLeaves) {
            double e = node.entropy;
            double v = node.prediction;
            int size = node.dataSet.getNumberOfInstances();
            System.out.printf("[Лист дерева]\t entropy = %.3f \t value =%.3f \t |T| = %d \n", e, v, size);
        }
        System.out.println("=".repeat(100));
        System.out.println();
    }

    public void setDebugModeOn(boolean debugModeOn) {
        this.debugModeOn = debugModeOn;
    }

    class TreeNode {
        double entropy;
        DataSet dataSet;
        double prediction;
        HashMap<TreeNode, Condition> childrenWithAttrCondition;
        public TreeNode(DataSet dataSet) {
            this.dataSet = dataSet;
            this.entropy = getEntropy(dataSet, classFieldId);
            this.childrenWithAttrCondition = new HashMap<>();
            this.prediction = getClassPrediction(dataSet);
        }
    }

    class Condition {
        Predicate<Integer> predicate;
        int threshold, attrId;
        String operatorSTR;

        Condition(Predicate<Integer> predicate, int attrId, int threshold, String operatorSTR) {
            this.predicate = predicate;
            this.attrId = attrId;
            this.threshold = threshold;
            this.operatorSTR = operatorSTR;
        }
    }
}


