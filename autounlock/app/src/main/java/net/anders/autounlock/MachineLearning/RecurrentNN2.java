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

public class RecurrentNN2 {
    private final static int NUM_SAMPLES = 3;
    private final static int N_INPUT = 2;
    private final static int N_OUTPUT = 2;

    // private static int NEURONS, BEST_NEURONS = 0;
    //private static double BEST_PROB, LEARNING, BEST_LEARNING = 0.0;

    private static DataNormalization normalizer;

    private static MultiLayerNetwork myNetwork;
    private static int miniBatchSize, numLabelClasses = 2;

        /*
    public static void main(String[] args) throws Exception {
        LEARNING = 0.01;
        for (NEURONS = 25; NEURONS < 301; NEURONS += 25) {
            System.out.println("Neurons = " + NEURONS);
            startTraining();
            getProbability(null);
        }
        System.out.println("Best neurons: " + BEST_NEURONS + " at prob: " + BEST_PROB);

        NEURONS = BEST_NEURONS;
        for (LEARNING = 0.05; LEARNING < 10.001; LEARNING += 0.05) {
            System.out.println("Learning Rate = " + LEARNING);
            startTraining();
            getProbability(null);
        }
        System.out.println("Best learning rate: " + BEST_LEARNING + " at prob: " + BEST_PROB);

        startTraining();
    }
        */

    public static void startTraining() throws Exception {
        deleteOldRecords();
        constructNetwork();
        //getProbability(null);
    }

    private static void deleteOldRecords() throws IOException {
        FileUtils.cleanDirectory(new File(CSVMaker.outputPath + "/train/features"));
        FileUtils.cleanDirectory(new File(CSVMaker.outputPath + "/train/labels"));
    }

    private static void constructNetwork() throws Exception {
/*
        DenseLayer inputLayer = new DenseLayer.Builder()
                .nIn(NUM_SAMPLES)
                .nOut(NEURONS)
                .name("Input")
                .build();*/


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
        //listBuilder.layer(0, inputLayer);
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

        //Map<Integer, Double[][]> trainData = DatabaseRetriever.getTupleDict();
        CSVMaker.convertToCSV(trainData, "1");

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

        //Map<Integer, Double[][]> trainData = DatabaseRetriever.getTupleDict();
        CSVMaker.convertToCSV(trainData, "1");

        miniBatchSize = unlockSize;

        Double[][] trainArray = createSequenceData(snapshot);

        CSVMaker.convertSingleArrayToCSV(trainArray, "1");

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
        if(myNetwork == null) {
            startTraining();
        }
        CSVMaker.convertSingleArrayToCSV(sequence, "1");

        File file = new File(CSVMaker.outputPath + "/train/features");
        int fileNumber = file.listFiles().length;

        // ----- Load the test data -----
        //Same process as for the training data.
        SequenceRecordReader testFeatures = new CSVSequenceRecordReader();
        testFeatures.initialize(new NumberedFileInputSplit("file:" + CSVMaker.outputPath + "/train/features/%d.csv", fileNumber, fileNumber));
        SequenceRecordReader testLabels = new CSVSequenceRecordReader();
        testLabels.initialize(new NumberedFileInputSplit("file:" + CSVMaker.outputPath + "/train/labels/%d.csv", fileNumber, fileNumber));

        DataSetIterator testData = new SequenceRecordReaderDataSetIterator(testFeatures, testLabels, miniBatchSize, numLabelClasses,
                false, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);
        if(normalizer != null) {
            testData.setPreProcessor(normalizer);   //Note that we are using the exact same normalization process as the training data
        }
        int nEpochs = 1;
        String str = "Test set evaluation at epoch %d: Accuracy = %.2f, F1 = %.2f";
        double prob = 0.0;
        for (int i = 0; i < nEpochs; i++) {
            //DataSet a = testData.next();
            //INDArray dat = a.getFeatures();
            //INDArray arr = myNetwork.output(dat);

            INDArray arr = myNetwork.output(testData);

            int length = arr.size(2);
            INDArray probs = arr.get(NDArrayIndex.point(0), NDArrayIndex.all(), NDArrayIndex.point(length-1));
            double prob0 = probs.getDouble(0);
            double prob1 = probs.getDouble(1);
            prob = prob1;
            System.out.println("Prob newshit 0 = " + prob0);
            System.out.println("Prob newshit 1 = " + prob1);
            testData.reset();

            //Evaluation evaluation = myNetwork.evaluate(testData);
            //System.out.println(String.format(str, i, evaluation.accuracy(), evaluation.f1()));
            //System.out.println(evaluation.stats());

            testData.reset();
        }

        return prob;
    }

    /*
    public static double getProbability(Double[][] sequence1) throws Exception {
        //Map<Integer, Double[][]> trainData = DatabaseRetriever.getTupleDict();
        System.out.println();

        //Double[][] array = trainData.get(7);
        String[] tests = new String[]{"8-test","3-test","3-test-sucks"};
        String[] lines = new String[]{"13","1","1"};
        for (int j = 0; j < tests.length; j++) {
            System.out.println("Test set " + tests[j]);
            Double[][] sequence = DatabaseRetriever.readTestData(tests[j], lines[j]);
            if (sequence == null) {
                System.out.println("TestData is null");
                return 0;
            }


            INDArray newShit = Nd4j.zeros(N_INPUT, NUM_SAMPLES);
            INDArray newLabels = Nd4j.zeros(N_OUTPUT, NUM_SAMPLES);

            INDArray ori = Nd4j.zeros(N_INPUT, NUM_SAMPLES);
            INDArray vel = Nd4j.zeros(N_INPUT, NUM_SAMPLES);

            int range = NUM_SAMPLES < sequence[0].length ? NUM_SAMPLES : sequence[0].length;
            for (int i = 0; i < range; i++) {
                ori.putScalar(new int[]{0, i}, sequence[0][i]); //Orientation
                vel.putScalar(new int[]{1, i}, sequence[1][i]); //Velocity

                newShit.putScalar(new int[]{0, i}, sequence[0][i]); //Orientation
                newShit.putScalar(new int[]{1, i}, sequence[1][i]); //Velocity

                newLabels.putScalar(new int[]{0,i}, 1);
                newLabels.putScalar(new int[]{1,i}, 1);
            }

            INDArray arr = myNetwork.output(ori);
            //System.out.println(arr);
            int length = arr.size(2);
            INDArray probs = arr.get(NDArrayIndex.point(0), NDArrayIndex.all(), NDArrayIndex.point(length - 1));
            double prob = probs.getDouble(0);
            System.out.println("Prob ori = " + prob);

            arr = myNetwork.output(vel);
            //System.out.println(arr);
            length = arr.size(2);
            probs = arr.get(NDArrayIndex.point(0), NDArrayIndex.all(), NDArrayIndex.point(length - 1));
            prob = probs.getDouble(0);
            System.out.println("Prob vel = " + prob);

            INDArray arr = myNetwork.output(newShit);
            //System.out.println(arr);
            int length = arr.size(2);
            INDArray probs = arr.get(NDArrayIndex.point(0), NDArrayIndex.all(), NDArrayIndex.point(length - 1));
            double prob = probs.getDouble(0);
            System.out.println("Prob newShit = " + prob);
            //System.out.println("f1 score = " + myNetwork.f1Score(newShit,newLabels));


            if (prob > BEST_PROB) {
                BEST_PROB = prob;
                BEST_NEURONS = NEURONS;
                BEST_LEARNING = LEARNING;
            }

            //INDArray output = myNetwork.rnnTimeStep(ori);
            //System.out.println("rnnTimeStep ori = " + output.getDouble(0));

            //output = myNetwork.rnnTimeStep(vel);
            //System.out.println("rnnTimeStep vel = " + output.getDouble(0));

            INDArray output = myNetwork.rnnTimeStep(newShit);
            System.out.println("rnnTimeStep newShit = " + output.getDouble(0));

            System.out.println();
        }

        double prob=0.0;
        return  prob;
    }
    */

}