import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class MapReduceWordCount {

    public static class WordMapper
            extends Mapper<Object, Text, Text, IntWritable> {

        private static final IntWritable ONE = new IntWritable(1);
        private final Text word = new Text();

        @Override
        protected void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {

            String line = value.toString().toLowerCase();

            String[] words = line.split("[^a-z0-9]+");

            for (String w : words) {
                if (!w.isEmpty()) {
                    word.set(w);
                    context.write(word, ONE);
                }
            }
        }
    }

    public static class WordReducer
            extends Reducer<Text, IntWritable, Text, IntWritable> {

        private final IntWritable result = new IntWritable();

        @Override
        protected void reduce(Text key, Iterable<IntWritable> values,
                               Context context)
                throws IOException, InterruptedException {

            int sum = 0;

            for (IntWritable value : values) {
                sum += value.get();
            }

            result.set(sum);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.out.println("Usage: java -jar mapreduce-exp7-1.0.jar <input> <output>");
            System.exit(1);
        }

        Configuration conf = new Configuration();

        // Run Hadoop MapReduce locally for the lab experiment.
        conf.set("mapreduce.framework.name", "local");
        conf.set("fs.defaultFS", "file:///");

        Path input = new Path(args[0]);
        Path output = new Path(args[1]);

        FileSystem fs = output.getFileSystem(conf);

        if (fs.exists(output)) {
            fs.delete(output, true);
        }

        Job job = Job.getInstance(conf, "Distributed Computing Project - MapReduce");
        job.setJarByClass(MapReduceWordCount.class);

        job.setMapperClass(WordMapper.class);
        job.setReducerClass(WordReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job, input);
        FileOutputFormat.setOutputPath(job, output);

        System.out.println("==============================================");
        System.out.println(" EXPERIMENT 7 - MAPREDUCE WORD COUNT");
        System.out.println("==============================================");
        System.out.println("Input  : " + input);
        System.out.println("Output : " + output);
        System.out.println("Mode   : Hadoop Local MapReduce");
        System.out.println();
        System.out.println("MAP phase  : converting words into (word, 1)");
        System.out.println("REDUCE phase: combining values for each word");
        System.out.println();

        boolean success = job.waitForCompletion(true);

        if (success) {
            System.out.println();
            System.out.println("MapReduce job completed successfully.");
            System.out.println("Final result is available in: " + output);
        } else {
            System.out.println("MapReduce job failed.");
            System.exit(1);
        }
    }
}
