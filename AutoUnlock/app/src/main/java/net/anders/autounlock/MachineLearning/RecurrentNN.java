package net.anders.autounlock.MachineLearning;

/**
 * Created by tom-fire on 16/05/2017.
 */

import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.Map;

import static net.anders.autounlock.MachineLearning.DatabaseRetriever.getTupleDict;

public class RecurrentNN {
    private final static int NUM_SAMPLES = 49;
    private final static int N_INPUT = 2;
    private final static int N_OUTPUT = 1;

    private static MultiLayerNetwork myNetwork;

    public static void main(String[] args) throws Exception {
        constructNetwork();
        getProbability();
    }

    private static void constructNetwork() throws Exception {
        DenseLayer inputLayer = new DenseLayer.Builder()
                .nIn(N_INPUT)
                .nOut(3)
                .name("Input")
                .build();

        DenseLayer hiddenLayer = new DenseLayer.Builder()
                .nIn(3)
                .nOut(2)
                .name("Hidden")
                .build();

        RnnOutputLayer outputLayer = new RnnOutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                .activation(Activation.SOFTMAX)
                .nIn(2)
                .nOut(N_OUTPUT)
                .name("Output")
                .build();

        NeuralNetConfiguration.Builder nncBuilder = new NeuralNetConfiguration.Builder();
        nncBuilder.iterations(10000);
        nncBuilder.learningRate(0.01);


        NeuralNetConfiguration.ListBuilder listBuilder = nncBuilder.list();
        listBuilder.layer(0, inputLayer);
        listBuilder.layer(1, hiddenLayer);
        listBuilder.layer(2, outputLayer);

        //listBuilder.backprop(true);
        listBuilder.backpropType(BackpropType.TruncatedBPTT)
                .tBPTTForwardLength(100)
                .tBPTTBackwardLength(100);

        myNetwork = new MultiLayerNetwork(listBuilder.build());
        myNetwork.init();

        trainNetwork();
    }

    private static void trainNetwork() throws Exception {
        Map<Integer, Double[][]> trainData = DatabaseRetriever.getTupleDict();

        for (int i = 0; i < trainData.keySet().size(); i++) {
            System.out.println("Fitting " + (i + 1) + " of " + trainData.keySet().size());
            //System.out.println("Orientation = " + array[0][i] + ", velocity = " + array[1][i]);

            Double[][] array = trainData.get(i);
            INDArray trainingInputs = Nd4j.zeros(N_INPUT, array[0].length);
            INDArray trainingOutputs = Nd4j.zeros(N_OUTPUT, array[0].length);

            for (int j = 0; j < array[0].length; j++) {
                trainingInputs.putScalar(new int[]{0,j}, array[0][j]); //Orientation
                trainingInputs.putScalar(new int[]{1,j}, array[1][j]); //Velocity
            }

            DataSet myData = new DataSet(trainingInputs, trainingOutputs);

            myNetwork.fit(myData);
        }
    }

    /**
     * Gets probability of the last timestep, which would be the whether or not to unlock
     *
     * See: https://deeplearning4j.org/usingrnns
     */
    private static void getProbability() throws Exception {
        Map<Integer, Double[][]> trainData = DatabaseRetriever.getTupleDict();
        Double[][] array = trainData.get(5);

        INDArray newShit = Nd4j.zeros(N_INPUT, array[0].length);

        for (int i = 0; i < array[0].length; i++) {
            newShit.putScalar(new int[]{0,i}, array[0][i]); //Orientation
            newShit.putScalar(new int[]{1,i}, array[1][i]); //Velocity
        }

        INDArray arr = myNetwork.output(newShit);
        System.out.println(arr);
        int length = arr.size(2);
        INDArray probs = arr.get(NDArrayIndex.point(0), NDArrayIndex.all(), NDArrayIndex.point(length-1));
        System.out.println(probs);
    }
}
