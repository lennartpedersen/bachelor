package net.anders.autounlock.MachineLearning;

/**
 * Created by Lennart Pedersen on 16-06-2017.
 */

import net.anders.autounlock.CoreService;

import org.apache.commons.io.FileUtils;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import net.anders.autounlock.MachineLearning.NumberedFileInputSplit;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class RecurrentNN {
    private final static int NUM_SAMPLES = 3;
    private final static int N_OUTPUT = 2;

    private static DataNormalization normalizer;

    private static MultiLayerNetwork myNetwork;
    private static int miniBatchSize, numLabelClasses = 2;


    public static void startTraining() throws Exception {
        deleteOldRecords();
        constructNetwork();
    }

    private static void deleteOldRecords() throws IOException {
        FileUtils.cleanDirectory(new File(CSVMaker.outputPath + "/train/features"));
        FileUtils.cleanDirectory(new File(CSVMaker.outputPath + "/train/labels"));
    }

    private static void constructNetwork() throws Exception {

        GravesLSTM hiddenLayer = new GravesLSTM.Builder()
                .activation(Activation.TANH)
                .nIn(NUM_SAMPLES)
                .nOut(50)
                .build();

        RnnOutputLayer outputLayer = new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                .activation(Activation.SOFTMAX)
                .nIn(50)
                .nOut(N_OUTPUT)
                .build();

        NeuralNetConfiguration.Builder nncBuilder = new NeuralNetConfiguration.Builder()
                .seed(123)    //Random number generator seed for improved repeatability. Optional.
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).iterations(1)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .iterations(1)
                .learningRate(0.01);

        NeuralNetConfiguration.ListBuilder listBuilder = nncBuilder.list();
        listBuilder.layer(0, hiddenLayer);
        listBuilder.layer(1, outputLayer);

        listBuilder.backprop(true);
        listBuilder.backpropType(BackpropType.TruncatedBPTT)
                .tBPTTForwardLength(25)
                .tBPTTBackwardLength(25);

        myNetwork = new MultiLayerNetwork(listBuilder.build());
        myNetwork.init();

        trainNetworkOnStart();
    }

    public static void trainNetworkOnStart() throws Exception {
        Map<Integer, Double[][]> trainData = CoreService.RNN;

        int unlockSize = trainData.size();
        if (unlockSize < 1) return;

        CSVMaker.convertToCSV(trainData);

        miniBatchSize = unlockSize;

        SequenceRecordReader trainFeatures = new CSVSequenceRecordReader();
        trainFeatures.initialize(new NumberedFileInputSplit("file:" + CSVMaker.outputPath + "/train/features/%d.csv", 1, unlockSize-1));
        SequenceRecordReader trainLabels = new CSVSequenceRecordReader();
        trainLabels.initialize(new NumberedFileInputSplit("file:" + CSVMaker.outputPath + "/train/labels/%d.csv", 1, unlockSize-1));

        DataSetIterator trainIterator = new SequenceRecordReaderDataSetIterator(trainFeatures, trainLabels, miniBatchSize, numLabelClasses,
                false, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);

        //Normalize the training data
        normalizer = new NormalizerStandardize();
        normalizer.fit(trainIterator);              //Collect training data statistics
        trainIterator.reset();

        //Use previously collected statistics to normalize on-the-fly. Each DataSet returned by 'trainData' iterator will be normalized
        trainIterator.setPreProcessor(normalizer);

        myNetwork.fit(trainIterator);
    }

    public static Double[][] createSequenceData(WindowData[] snapshot){
        Double[][] sequence = new Double[2][];
        Double[] ori = new Double[snapshot.length];
        Double[] accX = new Double[snapshot.length];
        Double[] accY = new Double[snapshot.length];
        for (int i = 0; i < snapshot.length; i++) {
            WindowData window = snapshot[i];
            ori[i] = window.getOrientation();
            accX[i] = window.getAccelerationX();
            accY[i] = window.getAccelerationY();
        }
        sequence[0] = ori;
        sequence[1] = accX;
        sequence[2] = accY;

        return sequence;
    }

    public static void trainNetwork(WindowData[] snapshot) throws Exception {
        Map<Integer, Double[][]> trainData = CoreService.RNN;

        int unlockSize = trainData.size();
        if (unlockSize < 1) return;

        miniBatchSize = unlockSize;

        Double[][] trainArray = createSequenceData(snapshot);

        CSVMaker.convertSingleArrayToCSV(trainArray, unlockSize+1);

        SequenceRecordReader trainFeatures = new CSVSequenceRecordReader();
        trainFeatures.initialize(new NumberedFileInputSplit("file:" + CSVMaker.outputPath + "/train/features/%d.csv", 1, unlockSize-1));
        SequenceRecordReader trainLabels = new CSVSequenceRecordReader();
        trainLabels.initialize(new NumberedFileInputSplit("file:" + CSVMaker.outputPath + "/train/labels/%d.csv", 1, unlockSize-1));

        DataSetIterator trainIterator = new SequenceRecordReaderDataSetIterator(trainFeatures, trainLabels, miniBatchSize, numLabelClasses,
                false, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);

        //Normalize the training data
        normalizer = new NormalizerStandardize();
        normalizer.fit(trainIterator);              //Collect training data statistics
        trainIterator.reset();

        //Use previously collected statistics to normalize on-the-fly. Each DataSet returned by 'trainData' iterator will be normalized
        trainIterator.setPreProcessor(normalizer);

        myNetwork.fit(trainIterator);
    }

    /**
     * Gets probability of the last timestep, which would be the whether or not to unlock
     *
     * See: https://deeplearning4j.org/usingrnns
     */
    public static double getProbability(Double[][] sequence) throws Exception {
        //myNetwork will be null the first time getProbability is invoked.
        if(myNetwork == null) {
            startTraining();
        }


        File file = new File(CSVMaker.outputPath + "/train/features");
        int fileNumber = file.listFiles().length;

        CSVMaker.convertToTempCSVProbArray(sequence,"1");
        // ----- Load the test data -----
        //Same process as for the training data.
        SequenceRecordReader testFeatures = new CSVSequenceRecordReader();
        testFeatures.initialize(new NumberedFileInputSplit("file:" + CSVMaker.outputPath + "/train/features/%d.csv", fileNumber+1, fileNumber+1));
        SequenceRecordReader testLabels = new CSVSequenceRecordReader();
        testLabels.initialize(new NumberedFileInputSplit("file:" + CSVMaker.outputPath + "/train/labels/%d.csv", fileNumber+1, fileNumber+1));

        DataSetIterator testData = new SequenceRecordReaderDataSetIterator(testFeatures, testLabels, miniBatchSize, numLabelClasses,
                false, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);
        if(normalizer != null) {
            testData.setPreProcessor(normalizer);   //Note that we are using the exact same normalization process as for the training data
        }
        int nEpochs = 1;
        double prob = 0.0;
        for (int i = 0; i < nEpochs; i++) {

            INDArray arr = myNetwork.output(testData);

            int length = arr.size(2);
            INDArray probs = arr.get(NDArrayIndex.point(0), NDArrayIndex.all(), NDArrayIndex.point(length-1));
            double prob1 = probs.getDouble(1);
            prob = prob1;
            testData.reset();
        }

        if (prob < 0.5) {
            deleteTempFile(fileNumber+1);
        }

        return prob;
    }

    private static void deleteTempFile(int fileNumber) {
        String path = "file:" + CSVMaker.outputPath + String.format("train/features/%d.csv", fileNumber);
        File file = new File(path);
        file.delete();
        String path2 = "file:" + CSVMaker.outputPath + String.format("train/labels/%d.csv", fileNumber);
        File file2 = new File(path2);
        file2.delete();
    }

}