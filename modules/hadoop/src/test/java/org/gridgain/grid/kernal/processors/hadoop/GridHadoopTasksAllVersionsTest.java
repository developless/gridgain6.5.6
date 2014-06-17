/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.hadoop;

import com.google.common.base.*;
import org.apache.hadoop.io.*;
import org.gridgain.grid.*;
import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.hadoop.*;
import org.gridgain.grid.kernal.processors.hadoop.examples.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Tests of Map, Combine and Reduce task executions of any version of hadoop API.
 */
abstract class GridHadoopTasksAllVersionsTest extends GridHadoopAbstractWordCountTest {
    /** Empty hosts array. */
    private static final String[] HOSTS = new String[0];

    /**
     * Creates some grid hadoop job. Override this method to create tests for any job implementation.
     *
     * @param inFile Input file name for the job.
     * @param outFile Output file name for the job.
     * @return Hadoop job.
     * @throws IOException If fails.
     */
    public abstract GridHadoopJob getHadoopJob(String inFile, String outFile) throws Exception;

    /**
     * @return prefix of reducer output file name. It's "part-" for v1 and "part-r-" for v2 API
     */
    public abstract String getOutputFileNamePrefix();

    /**
     * Tests map task execution.
     *
     * @throws Exception If fails.
     */
    @SuppressWarnings("ConstantConditions")
    public void testMapTask() throws Exception {
        GridGgfsPath inDir = new GridGgfsPath(PATH_INPUT);

        ggfs.mkdirs(inDir);

        GridGgfsPath inFile = new GridGgfsPath(inDir, GridHadoopWordCount2.class.getSimpleName() + "-input");

        URI inFileUri = URI.create(ggfsScheme() + inFile.toString());

        try (PrintWriter pw = new PrintWriter(ggfs.create(inFile, true))) {
            pw.println("hello0 world0");
            pw.println("world1 hello1");
        }

        GridHadoopFileBlock fileBlock1 = new GridHadoopFileBlock(HOSTS, inFileUri, 0, ggfs.info(inFile).length() - 1);

        try (PrintWriter pw = new PrintWriter(ggfs.append(inFile, false))) {
            pw.println("hello2 world2");
            pw.println("world3 hello3");
        }
        GridHadoopFileBlock fileBlock2 = new GridHadoopFileBlock(HOSTS, inFileUri, fileBlock1.length(),
                ggfs.info(inFile).length() - fileBlock1.length());

        GridHadoopJob gridJob = getHadoopJob(ggfsScheme() + inFile.toString(), ggfsScheme() + PATH_OUTPUT);

        GridHadoopTestTaskContext ctx = new GridHadoopTestTaskContext(gridJob);

        ctx.mockOutput().clear();

        GridHadoopTaskInfo taskInfo = new GridHadoopTaskInfo(null, GridHadoopTaskType.MAP, gridJob.id(), 0, 0,
            fileBlock1);

        GridHadoopTask task = gridJob.createTask(taskInfo);

        task.run(ctx);

        assertEquals("hello0,1; world0,1; world1,1; hello1,1", Joiner.on("; ").join(ctx.mockOutput()));

        ctx.mockOutput().clear();

        taskInfo = new GridHadoopTaskInfo (null, GridHadoopTaskType.MAP, gridJob.id(), 0, 0, fileBlock2);

        task = gridJob.createTask(taskInfo);

        task.run(ctx);

        assertEquals("hello2,1; world2,1; world3,1; hello3,1", Joiner.on("; ").join(ctx.mockOutput()));
    }

    /**
     * Generates input data for reduce-like operation into mock context input and runs the operation.
     *
     * @param gridJob Job is to create reduce task from.
     * @param taskType Type of task - combine or reduce.
     * @param taskNum Number of task in job.
     * @param words Pairs of words and its counts.
     * @return Context with mock output.
     * @throws GridException If fails.
     */
    private GridHadoopTestTaskContext runTaskWithInput(GridHadoopJob gridJob, GridHadoopTaskType taskType,
                                                       int taskNum, String... words) throws GridException {
        GridHadoopTestTaskContext ctx = new GridHadoopTestTaskContext(gridJob);

        for (int i = 0; i < words.length; i+=2) {
            List<IntWritable> valList = new ArrayList<>();

            for (int j = 0; j < Integer.parseInt(words[i + 1]); j++)
                valList.add(new IntWritable(1));

            ctx.mockInput().put(new Text(words[i]), valList);
        }

        GridHadoopTaskInfo taskInfo = new GridHadoopTaskInfo(null, taskType, gridJob.id(), taskNum, 0, null);
        GridHadoopTask task = gridJob.createTask(taskInfo);

        task.run(ctx);

        return ctx;
    }

    /**
     * Tests reduce task execution.
     *
     * @throws Exception If fails.
     */
    public void testReduceTask() throws Exception {
        GridHadoopJob gridJob = getHadoopJob(ggfsScheme() + PATH_INPUT, ggfsScheme() + PATH_OUTPUT);

        runTaskWithInput(gridJob, GridHadoopTaskType.REDUCE, 0, "word1", "5", "word2", "10");
        runTaskWithInput(gridJob, GridHadoopTaskType.REDUCE, 1, "word3", "7", "word4", "15");

        assertEquals(
            "word1\t5\n" +
            "word2\t10\n",
            readAndSortFile(PATH_OUTPUT + "/_temporary/0/task_00000000-0000-0000-0000-000000000000_0000_r_000000/" +
                    getOutputFileNamePrefix() + "00000")
        );

        assertEquals(
            "word3\t7\n" +
            "word4\t15\n",
            readAndSortFile(PATH_OUTPUT + "/_temporary/0/task_00000000-0000-0000-0000-000000000000_0000_r_000001/" +
                    getOutputFileNamePrefix() + "00001")
        );
    }

    /**
     * Tests combine task execution.
     *
     * @throws Exception If fails.
     */
    public void testCombinerTask() throws Exception {
        GridHadoopJob gridJob = getHadoopJob("/", "/");

        GridHadoopTestTaskContext ctx =
                runTaskWithInput(gridJob, GridHadoopTaskType.COMBINE, 0, "word1", "5", "word2", "10");

        assertEquals("word1,5; word2,10", Joiner.on("; ").join(ctx.mockOutput()));

        ctx = runTaskWithInput(gridJob, GridHadoopTaskType.COMBINE, 1, "word3", "7", "word4", "15");

        assertEquals("word3,7; word4,15", Joiner.on("; ").join(ctx.mockOutput()));
    }

    /**
     * Runs chain of map-combine task on file block.
     *
     * @param fileBlock block of input file to be processed.
     * @param gridJob Hadoop job implementation.
     * @return Context of combine task with mock output.
     * @throws GridException If fails.
     */
    private GridHadoopTestTaskContext runMapCombineTask(GridHadoopFileBlock fileBlock, GridHadoopJob gridJob) throws GridException {
        GridHadoopTestTaskContext mapCtx = new GridHadoopTestTaskContext(gridJob);

        GridHadoopTaskInfo taskInfo = new GridHadoopTaskInfo(null, GridHadoopTaskType.MAP, gridJob.id(), 0, 0, fileBlock);
        GridHadoopTask task = gridJob.createTask(taskInfo);

        task.run(mapCtx);

        //Prepare input for combine
        GridHadoopTestTaskContext combineCtx = new GridHadoopTestTaskContext(gridJob);
        combineCtx.makeTreeOfWritables(mapCtx.mockOutput());

        taskInfo = new GridHadoopTaskInfo(null, GridHadoopTaskType.COMBINE, gridJob.id(), 0, 0, null);
        task = gridJob.createTask(taskInfo);

        task.run(combineCtx);

        return combineCtx;
    }

    /**
     * Tests all job in complex.
     * Runs 2 chains of map-combine tasks and sends result into one reduce task.
     *
     * @throws Exception If fails.
     */
    @SuppressWarnings("ConstantConditions")
    public void testAllTasks() throws Exception {
        GridGgfsPath inDir = new GridGgfsPath(PATH_INPUT);

        ggfs.mkdirs(inDir);

        GridGgfsPath inFile = new GridGgfsPath(inDir, GridHadoopWordCount2.class.getSimpleName() + "-input");

        URI inFileUri = URI.create(ggfsScheme() + inFile.toString());

        generateTestFile(inFile.toString(), "red", 100, "blue", 200, "green", 150, "yellow", 70);

        //Split file into two blocks
        long fileLen = ggfs.info(inFile).length();

        Long l = fileLen / 2;

        GridHadoopFileBlock fileBlock1 = new GridHadoopFileBlock(HOSTS, inFileUri, 0, l);
        GridHadoopFileBlock fileBlock2 = new GridHadoopFileBlock(HOSTS, inFileUri, l, fileLen - l);

        GridHadoopJob gridJob = getHadoopJob(inFileUri.toString(), ggfsScheme() + PATH_OUTPUT);

        GridHadoopTestTaskContext combine1Ctx = runMapCombineTask(fileBlock1, gridJob);

        GridHadoopTestTaskContext combine2Ctx = runMapCombineTask(fileBlock2, gridJob);

        //Prepare input for combine
        GridHadoopTestTaskContext reduceCtx = new GridHadoopTestTaskContext(gridJob);

        reduceCtx.makeTreeOfWritables(combine1Ctx.mockOutput());
        reduceCtx.makeTreeOfWritables(combine2Ctx.mockOutput());

        GridHadoopTaskInfo taskInfo = new GridHadoopTaskInfo(null, GridHadoopTaskType.REDUCE, gridJob.id(), 0, 0, null);

        GridHadoopTask task = gridJob.createTask(taskInfo);

        task.run(reduceCtx);

        taskInfo = new GridHadoopTaskInfo(null, GridHadoopTaskType.COMMIT, gridJob.id(), 0, 0, null);

        task = gridJob.createTask(taskInfo);

        task.run(reduceCtx);

        assertEquals(
            "blue\t200\n" +
                "green\t150\n" +
                "red\t100\n" +
                "yellow\t70\n",
            readAndSortFile(PATH_OUTPUT + "/" + getOutputFileNamePrefix() + "00000")
        );
    }
}